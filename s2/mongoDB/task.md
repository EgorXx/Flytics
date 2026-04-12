## Поднять в docker compose mongoDB

```yaml
services:
  mongodb:
    image: mongo:7
    container_name: mongodb
    ports:
      - "27017:27017"
    volumes:
      - mongodb-data:/data/db
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: secret

volumes:
  mongodb-data:
```

## Создать минимум 3 коллекции, хотя бы 2 из которых связаны ObjectId, хотя бы 1 из документов в коллекции хранят JSON объекты либо массивы

```bash
db.users.insertOne({ username: "Egor", email: "egorik@gmail.com", skills: ["front", "back"] })
db.users.insertOne({ username: "Arinka", email: "arinka@gmail.com", skills: ["fuulstack"] })
db.users.insertOne({ username: "Nikita", email: "nikitka@gmail.com", skills: ["fuulstack", "presentation"] })

db.projects.insertMany([
  {
    name: "Task Tracker",
    description: "Сервис для управления задачами команды",
    memberIds: [
      ObjectId("69dbd60082f5c1b0bc3d88b3"),
      ObjectId("69dbd62e82f5c1b0bc3d88b4")
    ],
    technologies: ["Java", "Spring Boot", "MongoDB"]
  },
  {
    name: "Portfolio Website",
    description: "Личный сайт с кейсами и статьями",
    memberIds: [
      ObjectId("69dbd60082f5c1b0bc3d88b3"),
      ObjectId("69dbd64b82f5c1b0bc3d88b5")
    ],
    technologies: ["React", "Node.js", "MongoDB"]
  },
  {
    name: "Presentation Builder",
    description: "Приложение для создания презентаций",
    memberIds: [
      ObjectId("69dbd62e82f5c1b0bc3d88b4"),
      ObjectId("69dbd64b82f5c1b0bc3d88b5")
    ],
    technologies: ["Vue", "Java", "PostgreSQL"]
  }
])

db.tasks.insertMany([
  {
    title: "Поднять MongoDB подключение",
    status: "IN_PROGRESS",
    projectId: ObjectId("69dbd71b82f5c1b0bc3d88b6"),
    assigneeId: ObjectId("69dbd60082f5c1b0bc3d88b3"),
    labels: ["backend", "database"],
    comments: ["создать конфиг", "проверить подключение"]
  },
  {
    title: "Сверстать страницу логина",
    status: "NEW",
    projectId: ObjectId("69dbd71b82f5c1b0bc3d88b6"),
    assigneeId: ObjectId("69dbd62e82f5c1b0bc3d88b4"),
    labels: ["frontend", "ui"],
    comments: ["добавить форму", "сделать валидацию"],
    priority: "MEDIUM"
  },
  {
    title: "Добавить страницу со статьями",
    status: "IN_PROGRESS",
    projectId: ObjectId("69dbd71b82f5c1b0bc3d88b7"),
    assigneeId: ObjectId("69dbd64b82f5c1b0bc3d88b5"),
    labels: ["fullstack", "content"],
    comments: ["список статей", "детальная страница"],
    priority: "HIGH"
  },
  {
    title: "Сделать редактирование профиля",
    status: "DONE",
    projectId: ObjectId("69dbd71b82f5c1b0bc3d88b7"),
    assigneeId: ObjectId("69dbd60082f5c1b0bc3d88b3"),
    labels: ["profile", "backend"],
    comments: ["обновление email", "обновление skills"]
  },
  {
    title: "Подготовить шаблон презентации",
    status: "NEW",
    projectId: ObjectId("69dbd71b82f5c1b0bc3d88b8"),
    assigneeId: ObjectId("69dbd64b82f5c1b0bc3d88b5"),
    labels: ["presentation", "design"],
    comments: ["сделать 3 шаблона"]
  }
])
```

### Написать 2 find запроса, хотя бы 1 с projection ({ field1: 0, field2: 1})

```bash
test> db.projects.find({technologies: "Java"})
[
  {
    _id: ObjectId('69dbd71b82f5c1b0bc3d88b6'),
    name: 'Task Tracker',
    description: 'Сервис для управления задачами команды',
    memberIds: [
      ObjectId('69dbd60082f5c1b0bc3d88b3'),
      ObjectId('69dbd62e82f5c1b0bc3d88b4')
    ],
    technologies: [ 'Java', 'Spring Boot', 'MongoDB' ]
  },
  {
    _id: ObjectId('69dbd71b82f5c1b0bc3d88b8'),
    name: 'Presentation Builder',
    description: 'Приложение для создания презентаций',
    memberIds: [
      ObjectId('69dbd62e82f5c1b0bc3d88b4'),
      ObjectId('69dbd64b82f5c1b0bc3d88b5')
    ],
    technologies: [ 'Vue', 'Java', 'PostgreSQL' ]
  }
]

test> db.tasks.find({status: "NEW"}, {_id: 0, title: 1})
[
  { title: 'Сверстать страницу логина' },
  { title: 'Подготовить шаблон презентации' }
]
```

### Написать 2 update запроса

```bash
test> db.users.updateOne({username: "Egor"}, {$set: {username: "Egorik"}})
{
  acknowledged: true,
  insertedId: null,
  matchedCount: 1,
  modifiedCount: 1,
  upsertedCount: 0
}

test> db.tasks.updateMany({status: "NEW"}, {$set: {status: "IN_PROGRESS"}})
{
  acknowledged: true,
  insertedId: null,
  matchedCount: 2,
  modifiedCount: 2,
  upsertedCount: 0
}
```

### Написать 1 любой запрос с aggregate

```bash
test> db.tasks.aggregate([{$group: {_id: "$assigneeId", tasksCount: {$sum: 1}}}])
[
  { _id: ObjectId('69dbd64b82f5c1b0bc3d88b5'), tasksCount: 2 },
  { _id: ObjectId('69dbd60082f5c1b0bc3d88b3'), tasksCount: 2 },
  { _id: ObjectId('69dbd62e82f5c1b0bc3d88b4'), tasksCount: 1 }
]
```
