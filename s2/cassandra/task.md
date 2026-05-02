## Инициализация БД с репликацией

```bash
cqlsh> CREATE KEYSPACE university
   ... WITH replication = {
   ...   'class': 'SimpleStrategy',
   ...   'replication_factor': 2
   ... };
   
cqlsh> USE university;
```


## Создание таблицы и данных

```bash
cqlsh:university> CREATE TABLE student_grades (
              ...     student_id uuid,
              ...     created_at timestamp,
              ...     subject text,
              ...     grade int,
              ...     PRIMARY KEY (student_id, created_at)
              ... ) WITH CLUSTERING ORDER BY (created_at DESC)
```

```bash
cqlsh:university> SELECT uuid() AS student1_id FROM system.local;

 student1_id
--------------------------------------
 789844b1-ea03-4c03-97b0-b4a7bf85a0c2
```

```bash
cqlsh:university> SELECT uuid() AS student2_id FROM system.local;

 student2_id
--------------------------------------
 31dfe833-b0cb-4d67-ab5e-9bd43cec3b3e
```

```bash
INSERT INTO student_grades (student_id, created_at, subject, grade)
VALUES (789844b1-ea03-4c03-97b0-b4a7bf85a0c2, '2026-05-01 10:00:00', 'Databases', 5);

INSERT INTO student_grades (student_id, created_at, subject, grade)
VALUES (789844b1-ea03-4c03-97b0-b4a7bf85a0c2, '2026-05-02 10:00:00', 'Math', 4);
```

```bash
INSERT INTO student_grades (student_id, created_at, subject, grade)
VALUES (31dfe833-b0cb-4d67-ab5e-9bd43cec3b3e, '2026-05-01 11:00:00', 'Databases', 3);

INSERT INTO student_grades (student_id, created_at, subject, grade)
VALUES (31dfe833-b0cb-4d67-ab5e-9bd43cec3b3e, '2026-05-02 11:00:00', 'Math', 5);
```

```bash
cqlsh:university> SELECT * FROM student_grades;

 student_id                           | created_at                      | grade | subject
--------------------------------------+---------------------------------+-------+-----------
 31dfe833-b0cb-4d67-ab5e-9bd43cec3b3e | 2026-05-02 11:00:00.000000+0000 |     5 |      Math
 31dfe833-b0cb-4d67-ab5e-9bd43cec3b3e | 2026-05-01 11:00:00.000000+0000 |     3 | Databases
 789844b1-ea03-4c03-97b0-b4a7bf85a0c2 | 2026-05-02 10:00:00.000000+0000 |     4 |      Math
 789844b1-ea03-4c03-97b0-b4a7bf85a0c2 | 2026-05-01 10:00:00.000000+0000 |     5 | Databases
```

## Проверка распределения данных (Partitioning)

```bash
docker exec -it cassandra-node1 nodetool getendpoints university student_grades 789844b1-ea03-4c03-97b0-b4a7bf85a0c2
```

```bash
172.20.0.3
172.20.0.2
```

```bash
docker exec -it cassandra-node1 nodetool getendpoints university student_grades 31dfe833-b0cb-4d67-ab5e-9bd43cec3b3e
```

```bash
172.20.0.3
172.20.0.2
```

## Работа с фильтрацией

```bash
USE university;
CONSISTENCY QUORUM;
```

```bash
cqlsh:university> SELECT *
              ... FROM student_grades
              ... WHERE subject = 'Math';
InvalidRequest: Error from server: code=2200 [Invalid query] message="Cannot execute this query as it might involve data filtering and thus may have unpredictable performance. If you want to execute this query despite the performance unpredictability, use ALLOW FILTERING"   
```
Так как атрибут по которому ищем не в Primary Key для поиска требуется полное сканирование таблицы, из-за этого Cassandra требует подтверждение и по умолчанию не выполнится

```bash
cqlsh:university> SELECT *
              ... FROM student_grades
              ... WHERE subject = 'Math'
              ... ALLOW FILTERING;

 student_id                           | created_at                      | grade | subject
--------------------------------------+---------------------------------+-------+---------
 31dfe833-b0cb-4d67-ab5e-9bd43cec3b3e | 2026-05-02 11:00:00.000000+0000 |     5 |    Math
 789844b1-ea03-4c03-97b0-b4a7bf85a0c2 | 2026-05-02 10:00:00.000000+0000 |     4 |    Math

```