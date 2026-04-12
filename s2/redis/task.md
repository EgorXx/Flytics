## Домашнее задание

### 1. Запуск Redis через Docker

```yaml
services:
  redis:
    image: redis:7
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    command: ["redis-server", "--appendonly", "yes"]

volumes:
  redis-data:
```

### Создать Hash с данными о 3 студентах (имя, группа, средний балл)


```bash
127.0.0.1:6379> HSET student:1 name "Egor" group "11-400" gpa 4.5
(integer) 0
127.0.0.1:6379> HSET student:2 name "Arina" group "11-400" gpa 5
(integer) 3
127.0.0.1:6379> HSET student:3 name "Nikita" group "11-400" gpa 4.5
(integer) 3
```

### Реализовать лидерборд (Sorted Set) по среднему баллу. Вывести топ-3

```bash
127.0.0.1:6379> ZADD leaderboard 5 studetn:2
(integer) 1
127.0.0.1:6379> ZADD leaderboard 4.5 studetn:1
(integer) 1
127.0.0.1:6379> ZADD leaderboard 4.5 studetn:3
(integer) 1
```

```bash
127.0.0.1:6379> ZREVRANGE leaderboard 0 2 WITHSCORES
1) "studetn:2"
2) "5"
3) "studetn:3"
4) "4.5"
5) "studetn:1"
6) "4.5"
```

### Реализовать простую очередь задач (List): добавить 5 задач, забрать 3

```bash
127.0.0.1:6379> RPUSH tasks "task1" "task2" "task3" "task4" "task5"
(integer) 5
127.0.0.1:6379> LLEN tasks
(integer) 5
127.0.0.1:6379> LPOP tasks
"task1"
127.0.0.1:6379> LPOP tasks
"task2"
127.0.0.1:6379> LPOP tasks
"task3"
127.0.0.1:6379> LLEN tasks
(integer) 2
```

### Установить TTL на один из ключей, убедиться, что он удалился

```bash
127.0.0.1:6379> HSET student:temp:1 name "Egorik"
(integer) 1
127.0.0.1:6379> EXPIRE student:temp:1 60
(integer) 1
127.0.0.1:6379> TTL student:temp:1
(integer) 45
127.0.0.1:6379> TTL student:temp:1
(integer) -2
127.0.0.1:6379> HGETALL student:temp:1
(empty array)
```

### Выполнить транзакцию MULTI/EXEC: перевод «баллов» между двумя студентами

```bash
127.0.0.1:6379> MULTI
OK
127.0.0.1:6379(TX)> HINCRBYFLOAT student:3 gpa -0.5
QUEUED
127.0.0.1:6379(TX)> HINCRBYFLOAT student:1 gpa 0.5
QUEUED
127.0.0.1:6379(TX)> EXEC
1) "4"
2) "5"
```


