# 🚀 Быстрый старт TunelApp

## 30 секунд до первого запуска

### 1. Открыть проект
```bash
cd /Users/stanislave/Documents/Projects/TunelApp
open -a "Android Studio" .
```

### 2. Дождаться синхронизации Gradle
Android Studio автоматически скачает зависимости (1-2 минуты)

### 3. Выбрать Build Variant
View → Tool Windows → Build Variants → **mobileDebug**

### 4. Запустить
Нажмите зеленую кнопку Run (▶) или `Shift+F10`

**Готово!** Приложение запустится на эмуляторе или подключенном устройстве.

---

## ⚠️ Важно

### VPN не будет работать полностью
Приложение работает в **демо-режиме** без реальной библиотеки Xray:
- ✅ UI полностью работает
- ✅ Импорт VLESS URL работает
- ✅ База данных работает
- ✅ VPN Service запускается
- ❌ Трафик не маршрутизируется (требуется Xray)

### Для полной функциональности VPN
1. Получите libXray.aar (см. `app/libs/README.md`)
2. Поместите в `app/libs/`
3. Раскомментируйте в `app/build.gradle.kts`:
   ```kotlin
   implementation(files("libs/libxray.aar"))
   ```
4. Пересоберите проект

---

## 📱 Тестирование функций

### Импорт сервера
1. Скопируйте в буфер тестовый VLESS URL:
   ```
   vless://550e8400-e29b-41d4-a716-446655440000@example.com:443?type=tcp&security=none#TestServer
   ```
2. В приложении нажмите кнопку "+" (FAB)
3. Сервер добавится в список

### Подключение (демо)
1. Выберите сервер из списка
2. Нажмите большую круглую кнопку
3. Разрешите VPN доступ
4. Статус изменится на "CONNECTED" (но трафик не будет маршрутизироваться)

### Для Android TV
1. Build Variants → **tvDebug**
2. Создайте TV эмулятор (Android TV 1080p, API 34)
3. Run
4. Навигация через клавиши-стрелки или D-pad

---

## 🛠 Варианты сборки

### Debug (для разработки)
```bash
./gradlew assembleMobileDebug
# APK: app/build/outputs/apk/mobile/debug/
```

### Release (для распространения)
```bash
# Требуется настроить signing (см. SETUP.md)
./gradlew assembleMobileRelease
```

### TV версия
```bash
./gradlew assembleTvDebug
./gradlew assembleTvRelease
```

---

## 📖 Полная документация

- **README.md** - Детальное описание проекта
- **SETUP.md** - Полная инструкция по настройке
- **CONTRIBUTING.md** - Для разработчиков
- **PROJECT_STRUCTURE.md** - Архитектура проекта

---

## 🐛 Проблемы?

### Gradle sync failed
```bash
./gradlew clean
# File → Invalidate Caches and Restart в Android Studio
```

### VPN permission denied
Нормально при первом запуске - разрешите в диалоге

### Не видно устройства
```bash
adb devices  # Проверьте подключение
adb kill-server && adb start-server  # Перезапустите ADB
```

---

## ✅ Что дальше?

1. Изучите код в `app/src/main/java/com/tunelapp/`
2. Попробуйте изменить UI в Compose
3. Добавьте новые функции
4. Интегрируйте Xray для полной функциональности

**Приятной разработки!** 🎉



