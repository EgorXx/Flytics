-- Создаем отдельную БД для данных проекта
CREATE DATABASE project;

\c project

CREATE TABLE IF NOT EXISTS products (
    product_id   VARCHAR(10) PRIMARY KEY,
    name         VARCHAR(200) NOT NULL,
    category     VARCHAR(100) NOT NULL,
    supplier     VARCHAR(100),
    cost         NUMERIC(12, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
    order_id     INTEGER PRIMARY KEY,
    customer_id  INTEGER NOT NULL,
    product_id   VARCHAR(10) NOT NULL REFERENCES products(product_id),
    quantity     INTEGER NOT NULL CHECK (quantity > 0),
    price        NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    order_date   DATE NOT NULL
);
