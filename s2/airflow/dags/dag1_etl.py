"""DAG 1: ETL — загрузка данных из CSV и JSON в PostgreSQL."""

import csv
import json
from datetime import datetime, timedelta

from airflow.decorators import dag, task
from airflow.providers.postgres.hooks.postgres import PostgresHook


DEFAULT_ARGS = {
    "owner": "student",
    "retries": 1,
    "retry_delay": timedelta(minutes=2),
}

DATA_DIR = "/opt/airflow/data"


@dag(
    dag_id="etl_csv_json_to_postgres",
    default_args=DEFAULT_ARGS,
    start_date=datetime(2025, 1, 1),
    schedule=None,
    catchup=False,
    tags=["etl", "homework"],
    description="Загрузка заказов (CSV) и справочника товаров (JSON) в PostgreSQL",
)
def etl_dag():

    @task(task_id="extract_products_from_json")
    def extract_products():
        with open(f"{DATA_DIR}/products.json", encoding="utf-8") as f:
            raw = json.load(f)

        if not isinstance(raw, list) or len(raw) == 0:
            raise ValueError("products.json должен быть непустым списком")

        required = {"product_id", "name", "category", "cost"}
        cleaned = []
        for i, item in enumerate(raw):
            missing = required - set(item.keys())
            if missing:
                raise ValueError(f"Товар #{i}: отсутствуют поля {missing}")
            cleaned.append({
                "product_id": str(item["product_id"]),
                "name": str(item["name"]),
                "category": str(item["category"]),
                "supplier": str(item.get("supplier", "")),
                "cost": float(item["cost"]),
            })

        print(f"Извлечено товаров: {len(cleaned)}")
        return cleaned

    @task(task_id="extract_orders_from_csv")
    def extract_orders():
        with open(f"{DATA_DIR}/orders.csv", encoding="utf-8") as f:
            reader = csv.DictReader(f)
            rows = list(reader)

        if len(rows) == 0:
            raise ValueError("orders.csv пуст или не содержит строк данных")

        expected_cols = {"order_id", "customer_id", "product_id", "quantity", "price", "order_date"}
        actual_cols = set(reader.fieldnames or [])
        missing_cols = expected_cols - actual_cols
        if missing_cols:
            raise ValueError(f"В CSV отсутствуют колонки: {missing_cols}")

        cleaned = []
        for i, row in enumerate(rows):
            try:
                cleaned.append({
                    "order_id": int(row["order_id"]),
                    "customer_id": int(row["customer_id"]),
                    "product_id": str(row["product_id"]),
                    "quantity": int(row["quantity"]),
                    "price": float(row["price"]),
                    "order_date": row["order_date"],
                })
            except (ValueError, KeyError) as e:
                raise ValueError(f"Строка #{i + 1}: ошибка формата — {e}") from e

        print(f"Извлечено заказов: {len(cleaned)}")
        return cleaned

    @task(task_id="load_products_to_pg")
    def load_products(products: list[dict]):
        hook = PostgresHook(postgres_conn_id="project_pg")
        engine = hook.get_sqlalchemy_engine()

        with engine.begin() as conn:
            conn.execute("TRUNCATE TABLE orders CASCADE")
            conn.execute("TRUNCATE TABLE products CASCADE")

        with engine.begin() as conn:
            for p in products:
                conn.execute(
                    """
                    INSERT INTO products (product_id, name, category, supplier, cost)
                    VALUES (%(product_id)s, %(name)s, %(category)s, %(supplier)s, %(cost)s)
                    ON CONFLICT (product_id) DO UPDATE SET
                        name = EXCLUDED.name,
                        category = EXCLUDED.category,
                        supplier = EXCLUDED.supplier,
                        cost = EXCLUDED.cost
                    """,
                    p,
                )

        print(f"Загружено товаров в PG: {len(products)}")

    @task(task_id="load_orders_to_pg")
    def load_orders(orders: list[dict]):
        hook = PostgresHook(postgres_conn_id="project_pg")
        engine = hook.get_sqlalchemy_engine()

        with engine.begin() as conn:
            for o in orders:
                conn.execute(
                    """
                    INSERT INTO orders (order_id, customer_id, product_id, quantity, price, order_date)
                    VALUES (%(order_id)s, %(customer_id)s, %(product_id)s, %(quantity)s, %(price)s, %(order_date)s)
                    ON CONFLICT (order_id) DO UPDATE SET
                        customer_id = EXCLUDED.customer_id,
                        product_id = EXCLUDED.product_id,
                        quantity = EXCLUDED.quantity,
                        price = EXCLUDED.price,
                        order_date = EXCLUDED.order_date
                    """,
                    o,
                )

        print(f"Загружено заказов в PG: {len(orders)}")

    @task(task_id="run_quality_checks")
    def quality_checks():
        hook = PostgresHook(postgres_conn_id="project_pg")
        engine = hook.get_sqlalchemy_engine()

        with engine.connect() as conn:
            for tbl in ("products", "orders"):
                row = conn.execute(f"SELECT COUNT(*) FROM {tbl}").fetchone()
                if row[0] == 0:
                    raise ValueError(f"Таблица {tbl} пуста — загрузка не выполнена")
                print(f"Таблица {tbl}: {row[0]} строк")

            for tbl, col in [("products", "product_id"), ("orders", "order_id")]:
                nulls = conn.execute(
                    f"SELECT COUNT(*) FROM {tbl} WHERE {col} IS NULL"
                ).fetchone()[0]
                if nulls > 0:
                    raise ValueError(f"Найдено {nulls} NULL в {tbl}.{col}")
                print(f"Проверка {tbl}.{col} IS NULL: OK")

            orphans = conn.execute(
                """
                SELECT COUNT(*) FROM orders o
                LEFT JOIN products p ON o.product_id = p.product_id
                WHERE p.product_id IS NULL
                """
            ).fetchone()[0]
            if orphans > 0:
                raise ValueError(f"Найдено {orphans} заказов с несуществующим product_id")
            print("Проверка ссылочной целостности: OK")

            neg_qty = conn.execute(
                "SELECT COUNT(*) FROM orders WHERE quantity <= 0 OR price < 0"
            ).fetchone()[0]
            if neg_qty > 0:
                raise ValueError(f"Найдено {neg_qty} заказов с отрицательными quantity/price")
            print("Проверка отрицательных значений: OK")

        print("Все проверки качества пройдены")

    products = extract_products()
    orders = extract_orders()

    loaded_products = load_products(products)
    loaded_orders = load_orders(orders)

    loaded_products >> loaded_orders
    checks = quality_checks()
    loaded_orders >> checks


etl_dag()
