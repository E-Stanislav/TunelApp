# 🔧 Руководство по сборке TunelApp

## Системные требования

### Обязательно
- macOS 10.14+ / Windows 10+ / Linux (Ubuntu 18.04+)
- Android Studio Hedgehog (2023.1.1) или новее
- JDK 17
- 8 GB RAM минимум
- 10 GB свободного места

### Рекомендуется
- macOS 13+ / Windows 11
- Android Studio Latest Stable
- 16 GB RAM
- SSD диск

## Пошаговая инструкция

### Шаг 1: Проверка Java
```bash
java -version
# Должно показать: java version "17.x.x"
```

Если нет JDK 17:
```bash
# macOS
brew install openjdk@17
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc

# Linux
sudo apt install openjdk-17-jdk

# Windows - скачайте с oracle.com
```

### Шаг 2: Android Studio
1. Скачайте: https://developer.android.com/studio
2. Установите
3. Первый запуск → Complete Setup
4. SDK Manager → установите:
   - ✅ Android SDK Platform 34
   - ✅ Android SDK Build-Tools 34.0.0
   - ✅ Android SDK Platform-Tools
   - ✅ Android SDK Tools
   - ✅ Android Emulator (опционально)

### Шаг 3: Открыть проект
```bash
cd /Users/stanislave/Documents/Projects/TunelApp
```

В Android Studio:
- File → Open
- Выберите папку TunelApp
- Open

### Шаг 4: Gradle Sync
Дождитесь завершения (показано в статус баре внизу)

Если ошибки:
```bash
# В Terminal внутри Android Studio
./gradlew clean
./gradlew --refresh-dependencies
```

### Шаг 5: Выбор варианта сборки
1. View → Tool Windows → Build Variants
2. Выберите:
   - **mobileDebug** - для смартфона
   - **tvDebug** - для TV

### Шаг 6: Настройка устройства

#### Вариант A: Физическое устройство
1. **На телефоне/планшете:**
   - Настройки → О телефоне
   - Тапните 7 раз по "Номер сборки"
   - Назад → Для разработчиков
   - Включите "Отладка по USB"

2. **Подключите USB кабель**

3. **На телефоне:**
   - Разрешите отладку по USB (диалог)

4. **Проверка:**
   ```bash
   adb devices
   # Должно показать ваше устройство
   ```

#### Вариант B: Эмулятор
1. Tools → Device Manager
2. Create Device
3. Выберите:
   - **Phone**: Pixel 5 (API 34)
   - **TV**: Android TV (1080p, API 34)
4. Next → Download (если нужно) → Finish
5. Запустите эмулятор (▶)

### Шаг 7: Запуск
1. Убедитесь, что устройство выбрано (dropdown вверху)
2. Нажмите зеленую кнопку Run (▶)
3. Или: Run → Run 'app'
4. Или: Shift + F10

**Приложение установится и запустится!**

## Сборка APK вручную

### Debug APK (быстрая сборка)
```bash
cd /Users/stanislave/Documents/Projects/TunelApp

# Смартфон версия
./gradlew assembleMobileDebug

# TV версия
./gradlew assembleTvDebug

# Результат в:
# app/build/outputs/apk/mobile/debug/app-mobile-debug.apk
# app/build/outputs/apk/tv/debug/app-tv-debug.apk
```

### Release APK (для распространения)

#### 1. Создать keystore (один раз)
```bash
keytool -genkey -v -keystore ~/tunelapp.keystore \
  -alias tunelapp \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# Запомните пароли!
```

#### 2. Настроить signing
Создайте файл `keystore.properties` в корне проекта:
```properties
storeFile=/Users/stanislave/tunelapp.keystore
storePassword=ваш_пароль
keyAlias=tunelapp
keyPassword=ваш_пароль
```

Добавьте в `.gitignore`:
```
keystore.properties
*.keystore
```

#### 3. Обновить app/build.gradle.kts
```kotlin
// В начале файла
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    // ... existing config ...
    
    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // ... rest ...
        }
    }
}
```

#### 4. Собрать Release
```bash
./gradlew assembleMobileRelease
# Результат: app/build/outputs/apk/mobile/release/app-mobile-release.apk
```

## Установка APK

### На подключенное устройство
```bash
adb install app/build/outputs/apk/mobile/debug/app-mobile-debug.apk

# Или принудительная переустановка
adb install -r app/build/outputs/apk/mobile/debug/app-mobile-debug.apk
```

### На несколько устройств
```bash
# Список устройств
adb devices

# Установка на конкретное
adb -s DEVICE_ID install app.apk
```

### Передача файла
Просто скопируйте APK на телефон и откройте через файловый менеджер

## Android App Bundle (AAB)

Для Google Play Store:
```bash
./gradlew bundleMobileRelease
# Результат: app/build/outputs/bundle/mobileRelease/app-mobile-release.aab
```

## Проверка качества

### Lint проверка
```bash
./gradlew lint
# Отчет: app/build/reports/lint-results.html
```

### Unit тесты
```bash
./gradlew test
# Отчет: app/build/reports/tests/testMobileDebugUnitTest/index.html
```

### Размер APK
```bash
# Анализ размера
./gradlew :app:analyzeMobileDebugBundle

# Просто размер
ls -lh app/build/outputs/apk/mobile/debug/*.apk
```

## Оптимизация

### Уменьшение размера
1. Используйте Release build (ProGuard включен)
2. Включите shrinkResources:
   ```kotlin
   buildTypes {
       release {
           isMinifyEnabled = true
           isShrinkResources = true
       }
   }
   ```

### Ускорение сборки
В `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m
org.gradle.parallel=true
org.gradle.caching=true
kotlin.incremental=true
```

## Решение проблем

### "SDK location not found"
Создайте `local.properties`:
```properties
sdk.dir=/Users/stanislave/Library/Android/sdk
```

### "Unable to find bundletool"
```bash
./gradlew --stop
./gradlew clean build --refresh-dependencies
```

### "Execution failed for task :app:mergeDebugResources"
```bash
./gradlew clean
# File → Invalidate Caches and Restart
```

### Gradle daemon проблемы
```bash
./gradlew --stop
rm -rf ~/.gradle/caches/
rm -rf .gradle/
./gradlew build
```

### OutOfMemoryError
Увеличьте память в `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m
```

## Continuous Integration

### GitHub Actions пример
```yaml
name: Build
on: [push]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          distribution: 'temurin'
          java-version: '17'
      - name: Build
        run: ./gradlew assembleMobileDebug
      - name: Upload APK
        uses: actions/upload-artifact@v3
        with:
          name: app
          path: app/build/outputs/apk/mobile/debug/*.apk
```

## Полезные команды

```bash
# Список всех задач
./gradlew tasks

# Детальный вывод
./gradlew build --info

# Стек-трейс ошибок
./gradlew build --stacktrace

# Профилирование сборки
./gradlew build --profile

# Очистка
./gradlew clean

# Все варианты
./gradlew assemble

# Только один flavor
./gradlew assembleMobileDebug

# Проверка зависимостей
./gradlew dependencies
```

## Запуск на разных устройствах

### Телефон
```bash
./gradlew installMobileDebug
adb shell am start -n com.tunelapp/.ui.mobile.MainActivity
```

### Планшет
Тот же APK, что и для телефона

### Android TV
```bash
./gradlew installTvDebug
adb shell am start -n com.tunelapp/.ui.tv.TvMainActivity
```

### Wear OS
Не поддерживается (можно добавить flavor)

---

## 📚 Дополнительно

- Android Studio Shortcuts: https://developer.android.com/studio/intro/keyboard-shortcuts
- Gradle User Guide: https://docs.gradle.org/current/userguide/userguide.html
- Android Build Guide: https://developer.android.com/studio/build

## ✅ Готово!

Теперь вы можете собирать TunelApp для любой платформы. Удачной разработки! 🚀



