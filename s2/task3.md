## Индексы

### GIN

---

### 1

```sql
CREATE INDEX idx_gin_booking_contact_data
    ON booking USING GIN (contact_data);
```

```sql
EXPLAIN ANALYZE
SELECT id, contact_data
FROM booking
WHERE contact_data @> '{"email": "test@example.com"}';
```

![img.png](images/img31.png)

```sql
DROP INDEX IF EXISTS idx_gin_booking_contact_data;
EXPLAIN ANALYZE
SELECT id FROM booking WHERE contact_data @> '{"email": "test@example.com"}';
```

![img.png](images/img32.png)

В сравнении с GIN индексом и без него видно значительное ускорение при поиске в JSONB

### 2

```sql
CREATE INDEX idx_gin_passenger_search_vector
    ON passenger USING GIN (search_vector);
```

```sql
EXPLAIN ANALYZE
SELECT id, first_name, last_name
FROM passenger
WHERE search_vector @@ to_tsquery('russian', 'Иван');
```

![img.png](images/img33.png)

```sql
DROP INDEX IF EXISTS idx_gin_passenger_search_vector;
EXPLAIN ANALYZE
SELECT id FROM passenger WHERE search_vector @@ to_tsquery('russian', 'Иван');
```

![img.png](images/img34.png)

В сравнении с GIN индексом и без него видно значительное ускорение

### 3

```sql
CREATE INDEX idx_gin_passenger_tags
    ON passenger USING GIN (tags);
```

```sql
EXPLAIN ANALYZE
SELECT id, first_name, last_name, tags
FROM passenger
WHERE tags @> ARRAY['VIP'];
```

![img.png](images/img35.png)

```sql
DROP INDEX IF EXISTS idx_gin_passenger_tags;
EXPLAIN ANALYZE
SELECT id FROM passenger WHERE tags @> ARRAY['VIP'];
```

![img.png](images/img36.png)

В сравнении с GIN индексом и без него видно значительное ускорение

### 4

```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_gin_booking_token
    ON booking USING GIN (CAST(booking_token AS text) gin_trgm_ops);
```

```sql
EXPLAIN ANALYZE
SELECT id FROM booking WHERE booking_token::text LIKE '%a1b2%';
```

![img.png](images/img37.png)

```sql
DROP INDEX IF EXISTS idx_gin_booking_token;
EXPLAIN ANALYZE
SELECT id FROM booking WHERE booking_token::text LIKE '%a1b2%';
```

![img.png](images/img38.png)

В сравнении с GIN индексом и без него видно значительное ускорение

### 5

```sql
CREATE INDEX idx_gin_passenger_tags
    ON passenger USING GIN (tags);
```

```sql
EXPLAIN ANALYZE
SELECT id, first_name, last_name, tags
FROM passenger
WHERE tags && ARRAY['VIP', 'wheelchair'];
```

![img.png](images/img39.png)

```sql
DROP INDEX IF EXISTS idx_gin_passenger_tags;
EXPLAIN ANALYZE
SELECT id FROM passenger WHERE tags && ARRAY['VIP', 'wheelchair'];
```

![img.png](images/img40.png)

В сравнении с GIN индексом и без него видно значительное ускорение

---

### GiST

### 1

```sql
CREATE INDEX idx_gist_booking_user_location
    ON booking USING GiST (user_location);
```

```sql
EXPLAIN ANALYZE
SELECT id, user_location
FROM booking
WHERE user_location <-> POINT(37.62, 55.75) < 0.5;
```

![img.png](images/img41.png)

```sql
DROP INDEX IF EXISTS idx_gist_booking_user_location;
EXPLAIN ANALYZE
SELECT id FROM booking
WHERE user_location <-> POINT(37.62, 55.75) < 0.5;
```

![img.png](images/img42.png)

Значительное ускорение при поиске с геометрией

### 2

```sql
CREATE INDEX idx_gist_flight_time_range
    ON flight USING GiST (flight_time_range);
```

```sql
EXPLAIN ANALYZE
SELECT id, flight_number, flight_time_range
FROM flight
WHERE flight_time_range && tstzrange(
        '2024-06-01 00:00:00+03',
        '2024-06-02 00:00:00+03'
                           );
```

![img.png](images/img43.png)

```sql
DROP INDEX IF EXISTS idx_gist_flight_time_range;
EXPLAIN ANALYZE
SELECT id FROM flight
WHERE flight_time_range && tstzrange('2024-06-01+03','2024-06-02+03');
```

Значительное ускорение при пересечении диапозонов

![img.png](images/img44.png)

### 3

```sql
CREATE INDEX idx_gist_flight_time_range
    ON flight USING GiST (flight_time_range);
```

```sql
EXPLAIN ANALYZE
SELECT id, flight_number, flight_time_range
FROM flight
WHERE flight_time_range <@ tstzrange(
        '2024-06-01 00:00:00+03',
        '2024-12-31 23:59:59+03'
                           );
```

![img.png](images/img45.png)

```sql
DROP INDEX IF EXISTS idx_gist_flight_time_range;
EXPLAIN ANALYZE
SELECT id, flight_number, flight_time_range
FROM flight
WHERE flight_time_range <@ tstzrange(
        '2024-06-01 00:00:00+03',
        '2024-12-31 23:59:59+03'
                           );
```

![img.png](images/img46.png)

Ускорении, при точном совпадении диапазона

### 4

```sql
CREATE INDEX idx_gist_passenger_search_vector
    ON passenger USING GiST (search_vector);
```

```sql
EXPLAIN ANALYZE
SELECT id, first_name, last_name
FROM passenger
WHERE search_vector @@ to_tsquery('russian', 'Иван');
```

![img.png](images/img47.png)

```sql
DROP INDEX IF EXISTS idx_gist_passenger_search_vector;
EXPLAIN ANALYZE
SELECT id, first_name, last_name
FROM passenger
WHERE search_vector @@ to_tsquery('russian', 'Иван');
```

![img.png](images/img48.png)

Сравнение полнотекстового поиска с GIN, медленее, но быстрее вставка будет

### 5

```sql
CREATE INDEX idx_gist_booking_location_knn
    ON booking USING GiST (user_location);
```

```sql
EXPLAIN ANALYZE
SELECT id, user_location,
       user_location <-> POINT(37.62, 55.75) AS distance
FROM booking
ORDER BY user_location <-> POINT(37.62, 55.75)
    LIMIT 10;
```

![img.png](images/img49.png)

```sql
DROP INDEX IF EXISTS idx_gist_booking_location_knn;
EXPLAIN ANALYZE
SELECT id, user_location,
       user_location <-> POINT(37.62, 55.75) AS distance
FROM booking
ORDER BY user_location <-> POINT(37.62, 55.75)
    LIMIT 10;
```

![img.png](images/img50.png)

### JOIN

### 1

```sql
EXPLAIN ANALYZE
SELECT t.id        AS ticket_id,
       p.first_name,
       p.last_name,
       b.booking_date,
       b.total_cost
FROM ticket t
         JOIN passenger p ON t.passenger_id = p.id
         JOIN booking b   ON t.booking_id   = b.id;
```

![img.png](images/img51.png)

Hash Join, так как таблицы большие и условие равенства

### 2

```sql
EXPLAIN ANALYZE
SELECT c.first_name, c.last_name, b.booking_date, b.total_cost
FROM client c
         JOIN booking b ON b.client_id = c.id
    LIMIT 100;
```

![img.png](images/img52.png)

Nested из-за limit 100

### 3

```sql
EXPLAIN ANALYZE
SELECT t.seat_number, t.is_online_checkin, p.first_name, p.last_name
FROM ticket t
         JOIN passenger p ON t.passenger_id = p.id
```

![img.png](images/img53.png)

### 4

```sql
EXPLAIN ANALYZE
SELECT a.iata_code, a.name AS airport, c.name AS city
FROM airport a
         JOIN city c ON a.city_id = c.id;
```

![img.png](images/img54.png)

### 5

```sql
EXPLAIN ANALYZE
SELECT f.id, fn.number, f.departure_time
FROM flight f
         JOIN flight_number fn ON f.flight_number = fn.number
ORDER BY fn.number;
```

![img.png](images/img55.png)

![img.png](images/img555.png)