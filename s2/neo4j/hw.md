**ДЗ**

Задать структуру:

```
CREATE (alex:User {name: "Alex"}),
       (maria:User {name: "Maria"}),
       (john:User {name: "John"})

CREATE (inception:Movie {title: "Inception"}),
       (matrix:Movie {title: "The Matrix"})

MATCH (a:User {name: "Alex"}), (m:User {name: "Maria"})
CREATE (a)-[:FRIENDS]->(m)

MATCH (a:User {name: "Alex"}), (i:Movie {title: "Inception"})
CREATE (a)-[:WATCHED {rating: 5}]->(i)
```

Выполнить запросы:

- Найти всех друзей Алекса

- Найти фильмы, которые смотрели друзья Алекса, но не смотрел сам Алекс

Сравнить:

- Написать аналогичный запрос на SQL

- Сравнить сложность запросов
