# Инструкция по настройке TunelApp

## Быстрый старт

### 1. Установка необходимых инструментов

#### Android Studio
1. Скачайте [Android Studio](https://developer.android.com/studio) (версия Hedgehog или новее)
2. Установите следующие компоненты через SDK Manager:
   - Android SDK 34
   - Android SDK Platform-Tools
   - Android SDK Build-Tools 34.0.0
   - Android Emulator (для тестирования)

#### Java Development Kit
Убедитесь, что установлен JDK 17:
```bash
java -version  # Должна быть версия 17
```

Если нет, установите:
- macOS: `brew install openjdk@17`
- Linux: `sudo apt install openjdk-17-jdk`
- Windows: Скачайте с [Oracle](https://www.oracle.com/java/technologies/downloads/)

### 2. Клонирование проекта

Проект уже находится в:
```bash
/Users/stanislave/Documents/Projects/TunelApp
```

### 3. Настройка проекта

#### Открытие в Android Studio
```bash
cd /Users/stanislave/Documents/Projects/TunelApp
open -a "Android Studio" .
```

#### Синхронизация Gradle
1. Android Studio автоматически начнет синхронизацию
2. Дождитесь завершения (может занять несколько минут при первом запуске)
3. Если возникли ошибки - нажмите "Sync Now" или "Try Again"

### 4. Интеграция Xray-core (ВАЖНО!)

Для полной функциональности VPN необходима библиотека Xray.

#### Вариант A: Использование готовой библиотеки

1. **Скачайте libXray.aar** из одного из источников:
   - [v2rayNG releases](https://github.com/2dust/v2rayNG) (извлеките из APK)
   - [Xray-core Android](https://github.com/XTLS/Xray-core)

2. **Поместите в проект**:
   ```bash
   cp path/to/libXray.aar app/libs/
   ```

3. **Раскомментируйте зависимость** в `app/build.gradle.kts`:
   ```kotlin
   implementation(files("libs/libxray.aar"))
   ```

4. **Синхронизируйте Gradle**: File → Sync Project with Gradle Files

#### Вариант B: Сборка из исходников

```bash
# 1. Установите Go
brew install go  # macOS
# или скачайте с https://go.dev/

# 2. Установите Android NDK
# В Android Studio: Tools → SDK Manager → SDK Tools → NDK

# 3. Клонируйте Xray
git clone https://github.com/XTLS/Xray-core.git
cd Xray-core

# 4. Соберите для Android
export ANDROID_NDK_HOME=$HOME/Library/Android/sdk/ndk/25.2.9519653
go get -v
GOOS=android GOARCH=arm64 CGO_ENABLED=1 \
  CC=$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/darwin-x86_64/bin/aarch64-linux-android30-clang \
  go build -buildmode=c-shared -o libxray.so

# 5. Создайте AAR (требуется дополнительная настройка)
```

#### Вариант C: Без Xray (для демонстрации UI)

Приложение можно собрать и запустить без Xray для демонстрации интерфейса:
- VPN будет запускаться, но не будет маршрутизировать трафик
- Все UI функции будут работать
- Парсинг VLESS URL будет работать
- База данных серверов будет работать

### 5. Выбор Build Variant

1. В Android Studio откройте: **Build → Select Build Variant**
2. Выберите нужный вариант:
   - **mobileDebug** - для смартфона (отладка)
   - **mobileRelease** - для смартфона (релиз)
   - **tvDebug** - для Android TV (отладка)
   - **tvRelease** - для Android TV (релиз)

### 6. Запуск приложения

#### На физическом устройстве

1. **Включите режим разработчика** на Android устройстве:
   - Настройки → О телефоне → Нажмите 7 раз на "Номер сборки"

2. **Включите отладку по USB**:
   - Настройки → Для разработчиков → Отладка по USB

3. **Подключите устройство** к компьютеру

4. **Запустите**:
   - Нажмите зеленую кнопку "Run" (▶) в Android Studio
   - Или: `./gradlew installMobileDebug` для смартфона

#### На эмуляторе

1. **Создайте эмулятор**:
   - Tools → Device Manager → Create Device
   - Выберите модель (Pixel 5 рекомендуется)
   - Выберите System Image (API 34)
   - Finish

2. **Запустите эмулятор** и нажмите Run

#### Для Android TV

1. **Эмулятор TV**:
   - Device Manager → Create Device
   - Выберите TV категорию
   - Android TV (1080p)
   - API 34

2. **Или на реальном TV**:
   - Включите режим разработчика
   - Подключитесь по ADB over network:
     ```bash
     adb connect TV_IP_ADDRESS:5555
     ```

### 7. Сборка APK

#### Debug APK
```bash
./gradlew assembleMobileDebug
# Результат: app/build/outputs/apk/mobile/debug/app-mobile-debug.apk
```

#### Release APK (требуется keystore)

1. **Создайте keystore**:
   ```bash
   keytool -genkey -v -keystore tunelapp.keystore \
     -alias tunelapp -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **Настройте signing** в `app/build.gradle.kts`:
   ```kotlin
   signingConfigs {
       create("release") {
           storeFile = file("../tunelapp.keystore")
           storePassword = System.getenv("KEYSTORE_PASSWORD")
           keyAlias = "tunelapp"
           keyPassword = System.getenv("KEY_PASSWORD")
       }
   }
   ```

3. **Соберите**:
   ```bash
   export KEYSTORE_PASSWORD=your_password
   export KEY_PASSWORD=your_password
   ./gradlew assembleMobileRelease
   ```

### 8. Тестирование

#### Unit тесты
```bash
./gradlew test
```

#### Инструментальные тесты
```bash
./gradlew connectedAndroidTest
```

#### Lint проверка
```bash
./gradlew lint
# Отчет: app/build/reports/lint-results.html
```

### 9. Отладка

#### Просмотр логов
```bash
# Все логи приложения
adb logcat -s TunelApp

# Конкретные компоненты
adb logcat -s TunelVpnService XrayManager MainViewModel

# Очистка и просмотр
adb logcat -c && adb logcat -s TunelApp:V
```

#### Инспектор базы данных
1. В Android Studio: **View → Tool Windows → App Inspection**
2. Выберите вкладку **Database Inspector**
3. Просмотрите таблицу `vless_servers`

## Решение проблем

### Проблема: Gradle sync failed

**Решение**:
```bash
./gradlew clean
rm -rf .gradle
# В Android Studio: File → Invalidate Caches and Restart
```

### Проблема: SDK not found

**Решение**:
1. Создайте `local.properties`:
   ```properties
   sdk.dir=/Users/stanislave/Library/Android/sdk
   ```

### Проблема: VPN не подключается

**Причины**:
1. Xray библиотека не интегрирована - см. шаг 4
2. Неверный формат VLESS URL
3. Нет интернет соединения
4. Проверьте логи: `adb logcat -s TunelVpnService`

### Проблема: Приложение крашится на старте

**Решение**:
1. Проверьте минимальную версию Android (API 21+)
2. Очистите кэш: `./gradlew clean`
3. Пересоберите: `./gradlew assembleMobileDebug`
4. Проверьте logcat на ошибки

## Дополнительная информация

### Структура сборки

```
app/
├── build/
│   ├── outputs/
│   │   └── apk/
│   │       ├── mobile/
│   │       │   ├── debug/
│   │       │   └── release/
│   │       └── tv/
│   │           ├── debug/
│   │           └── release/
│   └── reports/
│       └── lint-results.html
```

### Полезные команды Gradle

```bash
# Список всех задач
./gradlew tasks

# Зависимости
./gradlew dependencies

# Информация о проекте
./gradlew projects

# Очистка
./gradlew clean

# Сборка всех вариантов
./gradlew assemble
```

### Android Studio плагины (рекомендуется)

- **Rainbow Brackets** - цветовое выделение скобок
- **Key Promoter X** - изучение shortcuts
- **GitToolBox** - расширенные Git функции
- **ADB Idea** - быстрый доступ к ADB командам

## Готово!

Теперь проект настроен и готов к разработке. 

Для дальнейшей информации см.:
- `README.md` - общее описание проекта
- `CONTRIBUTING.md` - как внести вклад
- `app/libs/README.md` - детали интеграции Xray

Удачной разработки! 🚀





