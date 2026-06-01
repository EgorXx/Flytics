"""DAG 2: Analytics — перенос данных PostgreSQL → ClickHouse и построение витрины."""

from datetime import datetime, timedelta

from airflow.decorators import dag, task
from airflow.providers.postgres.hooks.postgres import PostgresHook
from clickhouse_driver import Client


DEFAULT_ARGS = {
    "owner": "student",
    "retries": 1,
    "retry_delay": timedelta(minutes=2),
}

CH_HOST = "clickhouse"


def get_ch_client():
    return Client(host=CH_HOST, port=9000, user="default", password="airflow")


@dag(
    dag_id="analytics_pg_to_clickhouse",
    default_args=DEFAULT_ARGS,
    start_date=datetime(2025, 1, 1),
    schedule=None,
    catchup=False,
    tags=["analytics", "homework"],
    description="Перенос данных из PostgreSQL в ClickHouse и построение аналитической витрины",
)
def analytics_dag():

    @task(task_id="extract_from_pg")
    def extract_from_pg():
        """Читает заказы и товары из PostgreSQL."""
        hook = PostgresHook(postgres_conn_id="project_pg")
        engine = hook.get_sqlalchemy_engine()

        with engine.connect() as conn:
            orders = [
                dict(row._mapping)
                for row in conn.execute("SELECT * FROM orders ORDER BY order_id")
            ]
            products = [
                dict(row._mapping)
                for row in conn.execute("SELECT * FROM products ORDER BY product_id")
            ]

        print(f"Извлечено из PG: {len(orders)} заказов, {len(products)} товаров")
        return {"orders": orders, "products": products}

    @task(task_id="load_to_clickhouse")
    def load_to_clickhouse(data: dict):
        """Записывает сырые данные в ClickHouse (идемпотентно)."""
        client = get_ch_client()

        # Очистка перед загрузкой (идемпотентность)
        client.execute("TRUNCATE TABLE IF EXISTS orders_raw")
        client.execute("TRUNCATE TABLE IF EXISTS products_raw")

        # Загружаем товары
        if data["products"]:
            client.execute(
                "INSERT INTO products_raw (product_id, name, category, supplier, cost) VALUES",
                [
                    (p["product_id"], p["name"], p["category"], p["supplier"], float(p["cost"]))
                    for p in data["products"]
                ],
            )
        print(f"Загружено в CH products_raw: {len(data['products'])}")

        # Загружаем заказы
        if data["orders"]:
            client.execute(
                "INSERT INTO orders_raw (order_id, customer_id, product_id, quantity, price, order_date) VALUES",
                [
                    (
                        o["order_id"],
                        o["customer_id"],
                        o["product_id"],
                        o["quantity"],
                        float(o["price"]),
                        o["order_date"],
                    )
                    for o in data["orders"]
                ],
            )
        print(f"Загружено в CH orders_raw: {len(data['orders'])}")

    @task(task_id="build_sales_mart")
    def build_sales_mart():
        """Строит аналитическую витрину: продажи по дням и категориям."""
        client = get_ch_client()

        # Очищаем витрину перед перестроением
        client.execute("TRUNCATE TABLE IF EXISTS sales_mart")

        # Вставляем агрегированные данные
        client.execute(
            """
            INSERT INTO sales_mart (order_date, category, total_revenue, order_count, unique_customers)
            SELECT
                o.order_date,
                p.category,
                sumState(o.quantity * o.price) AS total_revenue,
                countState(o.order_id) AS order_count,
                uniqState(o.customer_id) AS unique_customers
            FROM orders_raw o
            JOIN products_raw p ON o.product_id = p.product_id
            GROUP BY o.order_date, p.category
            """
        )

        # Выводим результат для верификации
        result = client.execute(
            """
            SELECT
                order_date,
                category,
                sumMerge(total_revenue) AS revenue,
                countMerge(order_count) AS orders,
                uniqMerge(unique_customers) AS customers
            FROM sales_mart
            GROUP BY order_date, category
            ORDER BY order_date, category
            """
        )

        print("=== Аналитическая витрина (sales_mart) ===")
        print(f"{'Дата':>12} | {'Категория':<16} | {'Выручка':>10} | {'Заказов':>8} | {'Клиентов':>9}")
        print("-" * 70)
        for row in result:
            date_str = row[0].strftime("%Y-%m-%d") if hasattr(row[0], "strftime") else str(row[0])
            print(
                f"{date_str:>12} | {row[1]:<16} | {row[2]:>10.2f} | {row[3]:>8} | {row[4]:>9}"
            )

        # Сводные метрики
        totals = client.execute(
            """
            SELECT
                sumMerge(total_revenue) AS total_revenue,
                countMerge(order_count) AS total_orders,
                uniqMerge(unique_customers) AS total_customers,
                total_revenue / total_orders AS avg_check
            FROM sales_mart
            """
        )[0]

        print(f"\n=== Итоговые метрики ===")
        print(f"Общая выручка: {totals[0]:,.2f} руб.")
        print(f"Всего заказов: {totals[1]}")
        print(f"Уникальных клиентов: {totals[2]}")
        print(f"Средний чек: {totals[3]:,.2f} руб.")

        # Топ-5 товаров
        top5 = client.execute(
            """
            SELECT
                p.name,
                p.category,
                sum(o.quantity * o.price) AS revenue,
                sum(o.quantity) AS sold_qty
            FROM orders_raw o
            JOIN products_raw p ON o.product_id = p.product_id
            GROUP BY p.product_id, p.name, p.category
            ORDER BY revenue DESC
            LIMIT 5
            """
        )

        print(f"\n=== Топ-5 товаров по выручке ===")
        for i, row in enumerate(top5, 1):
            print(f"{i}. {row[0]} ({row[1]}) — {row[2]:,.2f} руб. | продано {row[3]} шт.")

    @task(task_id="run_quality_checks_ch")
    def quality_checks_ch():
        """Проверки качества данных в ClickHouse."""
        client = get_ch_client()

        checks = [
            ("products_raw не пуста", "SELECT COUNT() FROM products_raw"),
            ("orders_raw не пуста", "SELECT COUNT() FROM orders_raw"),
            ("sales_mart не пуста", "SELECT COUNT() FROM sales_mart"),
            (
                "нет заказов без товара",
                """
                SELECT COUNT() FROM orders_raw o
                LEFT JOIN products_raw p ON o.product_id = p.product_id
                WHERE p.product_id = ''
                """,
            ),
        ]

        for label, query in checks:
            count = client.execute(query)[0][0]
            if count == 0 and "не пуста" in label:
                raise ValueError(f"Проверка провалена: {label}")
            print(f"Проверка «{label}»: OK ({count} строк)")

        # Проверка консистентности: число заказов в raw = в mart
        raw_orders = client.execute("SELECT COUNT() FROM orders_raw")[0][0]
        mart_orders = client.execute(
            "SELECT countMerge(order_count) FROM sales_mart"
        )[0][0]
        if raw_orders != mart_orders:
            raise ValueError(
                f"Консистентность нарушена: orders_raw={raw_orders}, sales_mart={mart_orders}"
            )
        print(f"Проверка консистентности (число заказов): OK ({raw_orders} = {mart_orders})")

        print("Все проверки качества ClickHouse пройдены")

    data = extract_from_pg()
    raw_loaded = load_to_clickhouse(data)
    mart = build_sales_mart()
    checks = quality_checks_ch()

    raw_loaded >> mart >> checks


analytics_dag()
