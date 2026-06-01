-- Таблицы аналитического слоя (ClickHouse)

CREATE TABLE IF NOT EXISTS orders_raw (
    order_id     Int32,
    customer_id  Int32,
    product_id   String,
    quantity     Int32,
    price        Decimal(12, 2),
    order_date   Date
) ENGINE = MergeTree()
ORDER BY (order_date, order_id);

CREATE TABLE IF NOT EXISTS products_raw (
    product_id   String,
    name         String,
    category     String,
    supplier     String,
    cost         Decimal(12, 2)
) ENGINE = MergeTree()
ORDER BY product_id;

-- Аналитическая витрина: продажи по дням и категориям
CREATE TABLE IF NOT EXISTS sales_mart (
    order_date   Date,
    category     String,
    total_revenue  AggregateFunction(sum, Decimal(12, 2)),
    order_count    AggregateFunction(count, Int32),
    unique_customers AggregateFunction(uniq, Int32)
) ENGINE = AggregatingMergeTree()
ORDER BY (order_date, category);
