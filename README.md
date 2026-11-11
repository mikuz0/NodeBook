# NodeBook

**Менеджер баз знаний** — десктопное приложение для ведения структурированных баз знаний с иерархическим деревом узлов, хранящихся на файловой системе.

## Особенности

- 📚 **Книги знаний** — каждая база знаний это папка с метаданными (`book.json`) и разделами
- 🌳 **Иерархические узлы** — произвольная глубина вложенности: контейнеры (папки) и листья (файлы)
- 📂 **Файловая система** — узлы хранятся как директории с `node.json` или файлы `.node`
- 🔍 **Рекурсивный обход** — автоматическое построение дерева разделов
- ⚡ **Фоновый I/O** — загрузка дерева и создание книг в отдельных потоках
- 💾 **Сохранение настроек** — XDG-конфиг (`~/.config/nodebook/settings.json`)

## Стек

| Компонент | Технология |
|-----------|-----------|
| Язык | Kotlin (JVM) |
| Версия Java | 21 |
| UI | JavaFX |
| Градострой | Gradle (Kotlin DSL) |
| Сериализация | kotlinx.serialization |
| Логирование | SLF4J + Logback |
| Тестирование | JUnit 5 |

## Структура проекта

```
nodebook/
├── nodebook-core/     ← Модель, репозитории, JSON, конфиги (без JavaFX)
├── nodebook-app/      ← JavaFX-приложение: менеджер книг
└── nodebook-ui/       ← Общие UI-утилиты и окна разделов
```

### Пакеты

| Модуль | Пакет | Назначение |
|--------|-------|-----------|
| `nodebook-core` | `ru.nodebook.core.model` | `NodeMeta`, `NodeType`, `NodeRef`, `KnowledgeBase` |
| `nodebook-core` | `ru.nodebook.core.repo` | `NodeRepository`, `KnowledgeBaseRepository` |
| `nodebook-core` | `ru.nodebook.core.config` | `Settings`, `SettingsStore`, `AppPaths` |
| `nodebook-app` | `ru.nodebook.app` | `MainApp`, `BooksView`, `BooksController` |
| `nodebook-ui` | `ru.nodebook.ui` | `BookAware`, `SectionWindow`, `UiUtils` |

## Структура данных на диске

```
workspace/
└── my-electronics/              ← книга знаний
    ├── book.json                ← KnowledgeBase(schemaVersion, name, version, sections)
    ├── components/              ← раздел
    │   ├── node.json            ← NodeMeta(type=SECTION, slug="components")
    │   ├── manufacturer-1/      ← папка-узел (контейнер)
    │   │   ├── node.json        ← NodeMeta(type=MANUFACTURER)
    │   │   ├── microchips/      ← вложенный контейнер
    │   │   │   ├── node.json
    │   │   │   └── stm32f103.node ← лист-узел (файл с NodeMeta)
    │   └── manufacturer-2/
    ├── modules/                 ← раздел
    │   └── node.json
    └── standards/               ← раздел
        └── node.json
```

### Правила хранения

- **Контейнер** = директория + `node.json` внутри
- **Лист** = файл `<slug>.node` с полным `NodeMeta`
- **Раздел** = директория верхнего уровня + `node.json` с `type=SECTION`
- **Slug** = имя папки/файла без расширения; генерируется из name: lowercase, пробелы → `-`, только `[a-z0-9-]`, коллизии → `-1`, `-2`

## Установка и запуск

### Требования

- Java 21+
- Gradle 8.11+ (или используйте `gradlew`)

### Сборка

```bash
./gradlew build
```

### Запуск

```bash
./gradlew :nodebook-app:run
```

### Запуск тестов

```bash
./gradlew :nodebook-core:test
```

## Использование

1. **Выбор workspace** — нажмите `Choose workspace...` и выберите пустую директорию
2. **Создание книги** — введите имя и нажмите `Create`
3. **Проверка на диске** — в workspace появится папка с `book.json` и разделами (`components/`, `modules/`, `standards/`)
4. **Открытие раздела** — нажмите кнопку раздела (Components/Modules/Standards) для просмотра дерева узлов

## Архитектура

### Паттерны

- **Object-репозитории** — `NodeRepository`, `KnowledgeBaseRepository`, `SettingsStore`
- **View + Controller** — `BooksView` с callback-свойствами, `BooksController` с фоном через `Task`
- **BookAware** — интерфейс для окон разделов (`openSection()`, `showWindow()`, `getStage()`)
- **Фоновый I/O** — `Task` + `Executors.newFixedThreadPool()` для загрузки дерева и создания книг

### JSON-сериализация

```kotlin
val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}
```

## Конфигурация

Настройки хранятся в `$XDG_CONFIG_HOME/nodebook/settings.json` (или `~/.config/nodebook/settings.json`):

```json
{
  "workspace": "/home/user/knowledge-base"
}
```

## Лицензия

MIT
