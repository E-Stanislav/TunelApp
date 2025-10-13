# Структура проекта TunelApp

## Обзор

TunelApp - это современное нативное Android приложение для VLESS VPN с поддержкой смартфонов и Android TV.

## Структура файлов

```
TunelApp/
│
├── app/                                    # Основной модуль приложения
│   ├── build.gradle.kts                   # Gradle конфигурация модуля
│   ├── proguard-rules.pro                 # ProGuard правила
│   │
│   ├── src/
│   │   ├── main/                          # Основной исходный код
│   │   │   ├── AndroidManifest.xml        # Манифест приложения
│   │   │   │
│   │   │   ├── java/com/tunelapp/
│   │   │   │   ├── TunelApplication.kt    # Application класс
│   │   │   │   │
│   │   │   │   ├── core/                  # Ядро VPN логики
│   │   │   │   │   ├── XrayConfig.kt      # Генератор конфигурации Xray
│   │   │   │   │   └── XrayManager.kt     # Менеджер Xray-core
│   │   │   │   │
│   │   │   │   ├── data/                  # Модели данных и БД
│   │   │   │   │   ├── VlessServer.kt     # Модель VLESS сервера
│   │   │   │   │   ├── VlessDatabase.kt   # Room Database
│   │   │   │   │   ├── VlessServerDao.kt  # Data Access Object
│   │   │   │   │   └── VlessRepository.kt # Repository слой
│   │   │   │   │
│   │   │   │   ├── parser/                # VLESS парсер
│   │   │   │   │   └── VlessParser.kt     # Парсинг VLESS URL
│   │   │   │   │
│   │   │   │   ├── service/               # VPN сервис
│   │   │   │   │   └── TunelVpnService.kt # VPN Service
│   │   │   │   │
│   │   │   │   ├── ui/                    # UI компоненты
│   │   │   │   │   ├── mobile/            # UI для смартфонов
│   │   │   │   │   │   ├── MainActivity.kt
│   │   │   │   │   │   └── MainScreen.kt
│   │   │   │   │   │
│   │   │   │   │   ├── tv/                # UI для Android TV
│   │   │   │   │   │   ├── TvMainActivity.kt
│   │   │   │   │   │   └── TvMainScreen.kt
│   │   │   │   │   │
│   │   │   │   │   └── theme/             # Material Design тема
│   │   │   │   │       ├── Color.kt
│   │   │   │   │       ├── Theme.kt
│   │   │   │   │       └── Type.kt
│   │   │   │   │
│   │   │   │   ├── viewmodel/             # ViewModels
│   │   │   │   │   └── MainViewModel.kt   # Главная ViewModel
│   │   │   │   │
│   │   │   │   └── utils/                 # Утилиты
│   │   │   │       └── Utils.kt           # Вспомогательные функции
│   │   │   │
│   │   │   └── res/                       # Ресурсы
│   │   │       ├── drawable/              # Drawable ресурсы
│   │   │       │   ├── ic_vpn.xml         # VPN иконка
│   │   │       │   └── tv_banner.xml      # TV баннер
│   │   │       │
│   │   │       ├── mipmap-anydpi-v26/     # Adaptive icons
│   │   │       │   ├── ic_launcher.xml
│   │   │       │   └── ic_launcher_round.xml
│   │   │       │
│   │   │       ├── values/                # Значения
│   │   │       │   ├── colors.xml         # Цвета
│   │   │       │   ├── strings.xml        # Строки
│   │   │       │   └── themes.xml         # Темы
│   │   │       │
│   │   │       └── xml/                   # XML конфигурации
│   │   │           ├── backup_rules.xml
│   │   │           └── data_extraction_rules.xml
│   │   │
│   │   ├── tv/                            # TV-специфичные ресурсы
│   │   │   └── AndroidManifest.xml        # TV манифест
│   │   │
│   │   └── test/                          # Unit тесты
│   │       └── java/com/tunelapp/
│   │           ├── VlessParserTest.kt     # Тесты парсера
│   │           └── UtilsTest.kt           # Тесты утилит
│   │
│   └── libs/                              # Внешние библиотеки
│       └── README.md                      # Инструкции по Xray
│
├── gradle/                                # Gradle wrapper
│   └── wrapper/
│       └── gradle-wrapper.properties
│
├── build.gradle.kts                       # Root Gradle конфигурация
├── settings.gradle.kts                    # Gradle настройки
├── gradle.properties                      # Gradle свойства
├── .gitignore                             # Git ignore правила
│
├── README.md                              # Основная документация
├── SETUP.md                               # Инструкция по настройке
├── CONTRIBUTING.md                        # Руководство для контрибьюторов
├── PROJECT_STRUCTURE.md                   # Этот файл
└── LICENSE                                # Лицензия MIT

```

## Ключевые компоненты

### 1. Core (Ядро)
- **XrayConfig**: Генерирует JSON конфигурацию для Xray-core
- **XrayManager**: Управляет жизненным циклом Xray-core

### 2. Data (Данные)
- **VlessServer**: Entity модель сервера с аннотациями Room
- **VlessDatabase**: Room Database singleton
- **VlessServerDao**: DAO с Flow для реактивности
- **VlessRepository**: Repository pattern для абстракции данных

### 3. Parser (Парсер)
- **VlessParser**: Парсинг и генерация VLESS URL
  - Поддержка всех стандартных параметров
  - Валидация конфигурации
  - URL encoding/decoding

### 4. Service (Сервис)
- **TunelVpnService**: VpnService implementation
  - TUN interface управление
  - Foreground service с уведомлениями
  - Интеграция с Xray

### 5. UI (Интерфейс)

#### Mobile (Смартфоны)
- **MainActivity**: Точка входа для смартфонов
- **MainScreen**: Compose UI с:
  - Анимированной кнопкой подключения
  - Списком серверов
  - Статистикой трафика
  - FAB для импорта

#### TV (Android TV)
- **TvMainActivity**: Точка входа для TV
- **TvMainScreen**: TV-оптимизированный UI с:
  - D-pad навигацией
  - Крупными элементами
  - Упрощенным интерфейсом

#### Theme (Тема)
- Material Design 3
- Dynamic Colors (Android 12+)
- Dark/Light режимы

### 6. ViewModel
- **MainViewModel**: MVVM ViewModel с:
  - StateFlow для реактивного UI
  - Корутины для асинхронности
  - Управление состоянием VPN

### 7. Utils (Утилиты)
- Форматирование данных
- Валидация
- Вспомогательные функции

## Архитектурные паттерны

### MVVM (Model-View-ViewModel)
```
UI (Compose) → ViewModel → Repository → DAO → Database
                              ↓
                          XrayManager
                              ↓
                          VpnService
```

### Dependency Flow
```
Activity → ViewModel → Repository → Database
                    → XrayManager → VpnService
```

### State Management
```
ViewModel StateFlow → Compose collectAsState() → UI Recomposition
```

## Gradle Build Variants

### Product Flavors
- **mobile**: Смартфон версия
- **tv**: Android TV версия

### Build Types
- **debug**: Отладочная сборка
- **release**: Релизная сборка (с ProGuard)

### Combinations
1. **mobileDebug** - Смартфон отладка
2. **mobileRelease** - Смартфон релиз
3. **tvDebug** - TV отладка
4. **tvRelease** - TV релиз

## Зависимости

### Core
- Kotlin 1.9.20
- Android SDK 34
- Min SDK 21

### Jetpack
- Compose BOM 2023.10.01
- Material3
- Lifecycle & ViewModel
- Navigation Compose
- Room 2.6.1

### Async
- Kotlinx Coroutines 1.7.3

### Other
- Gson 2.10.1
- Leanback 1.0.0 (TV)

### External (требуется)
- Xray-core (libXray.aar)

## Конфигурационные файлы

### Gradle
- `build.gradle.kts` (root): Плагины и репозитории
- `app/build.gradle.kts`: Зависимости и конфигурация
- `settings.gradle.kts`: Модули проекта
- `gradle.properties`: Gradle свойства

### Android
- `AndroidManifest.xml`: Разрешения, компоненты
- `proguard-rules.pro`: ProGuard правила
- Resource XMLs: Strings, colors, themes

### Project
- `.gitignore`: Git ignore правила
- `LICENSE`: MIT лицензия
- Документация: README, SETUP, CONTRIBUTING

## Размер проекта

### Исходный код
- **Kotlin файлы**: ~20 файлов
- **XML ресурсы**: ~10 файлов
- **Тесты**: 2 test файла

### Build Output (примерно)
- **Debug APK (mobile)**: ~8-10 MB (без Xray)
- **Release APK (mobile)**: ~5-7 MB (с ProGuard, без Xray)
- **С Xray**: +3-5 MB

## Следующие шаги

### Необходимо для production:
1. ✅ Интегрировать реальный Xray-core
2. ✅ Реализовать tun2socks
3. ✅ Добавить реальную статистику
4. ✅ Инструментальные тесты
5. ✅ CI/CD pipeline

### Опционально:
- Split tunneling
- Widgets
- Shortcuts
- Экспорт/импорт конфигураций
- Мультиязычность

## Полезные команды

```bash
# Просмотр структуры проекта
tree -I 'build|.gradle|.idea'

# Подсчет строк кода
find app/src/main/java -name "*.kt" | xargs wc -l

# Размер APK
ls -lh app/build/outputs/apk/mobile/debug/*.apk

# Анализ зависимостей
./gradlew :app:dependencies
```

## Документация

- **README.md** - Общее описание и установка
- **SETUP.md** - Детальная настройка окружения
- **CONTRIBUTING.md** - Руководство для разработчиков
- **PROJECT_STRUCTURE.md** - Этот файл
- **app/libs/README.md** - Интеграция Xray

---

Создано: 2025-10-12  
Версия: 1.0.0  
Статус: ✅ Готово к разработке (требуется Xray интеграция)



