# TunelApp - VLESS VPN для Android

Современное нативное Android приложение для VLESS VPN с поддержкой смартфонов и Android TV.

## Особенности

### 🚀 Основные возможности
- ✅ Поддержка протокола VLESS
- ✅ Импорт конфигурации через буфер обмена
- ✅ Современный Material Design 3 UI
- ✅ Поддержка Android TV с оптимизацией для D-pad
- ✅ Хранение нескольких серверов
- ✅ Автоматическое переподключение
- ✅ Статистика трафика (в разработке)

### 📱 Платформы
- **Смартфоны**: Android 5.0+ (API 21+)
- **Android TV**: Leanback интерфейс с упрощенной навигацией

### 🎨 Дизайн
- Material Design 3 с Dynamic Colors
- Адаптивная темная/светлая тема
- Интуитивный и современный интерфейс
- Анимированные переходы состояний

## Архитектура

### Технологический стек
- **Язык**: Kotlin 100%
- **UI**: Jetpack Compose
- **Архитектура**: MVVM
- **База данных**: Room
- **Асинхронность**: Kotlin Coroutines & Flow
- **DI**: Manual (можно добавить Hilt/Koin)
- **VPN Core**: Xray-core (требуется интеграция)

### Структура проекта
```
TunelApp/
├── app/src/main/java/com/tunelapp/
│   ├── core/              # VPN логика и Xray интеграция
│   ├── data/              # Модели данных и БД
│   ├── parser/            # VLESS URL парсер
│   ├── service/           # VPN сервис
│   ├── ui/
│   │   ├── mobile/        # UI для смартфонов
│   │   ├── tv/            # UI для Android TV
│   │   └── theme/         # Тема Material Design 3
│   └── viewmodel/         # ViewModels
├── app/src/tv/            # TV-специфичные ресурсы
└── app/libs/              # Xray библиотека (требуется)
```

## Установка и сборка

### Требования
- Android Studio Hedgehog или новее
- JDK 17
- Android SDK 34
- Gradle 8.2+

### Сборка проекта

1. **Клонировать репозиторий**
   ```bash
   cd /Users/stanislave/Documents/Projects/TunelApp
   ```

2. **Интеграция Xray-core** (важно!)
   
   Приложение требует библиотеку Xray для работы VPN. См. `app/libs/README.md` для инструкций.
   
   Быстрый старт:
   - Скачайте libXray.aar
   - Поместите в `app/libs/`
   - Раскомментируйте зависимость в `app/build.gradle.kts`

3. **Открыть в Android Studio**
   ```bash
   open -a "Android Studio" .
   ```

4. **Выбрать Build Variant**
   - `mobileDebug` - для смартфонов (отладка)
   - `mobileRelease` - для смартфонов (релиз)
   - `tvDebug` - для Android TV (отладка)
   - `tvRelease` - для Android TV (релиз)

5. **Собрать и запустить**
   ```bash
   # Для смартфона
   ./gradlew assembleMobileDebug
   
   # Для TV
   ./gradlew assembleTvDebug
   ```

### Подписание APK

Для релизной версии создайте keystore:
```bash
keytool -genkey -v -keystore tunelapp.keystore -alias tunelapp -keyalg RSA -keysize 2048 -validity 10000
```

Добавьте в `app/build.gradle.kts`:
```kotlin
signingConfigs {
    create("release") {
        storeFile = file("tunelapp.keystore")
        storePassword = "your_password"
        keyAlias = "tunelapp"
        keyPassword = "your_password"
    }
}
```

## Использование

### Импорт сервера

1. **Скопируйте VLESS URL** в буфер обмена:
   ```
   vless://uuid@server.com:443?encryption=none&security=tls&type=ws&host=server.com&path=/path#MyServer
   ```

2. **В приложении**:
   - Нажмите кнопку "+" (FAB) на смартфоне
   - Или "IMPORT FROM CLIPBOARD" на TV
   - Сервер автоматически распарсится и сохранится

### Подключение

1. Выберите сервер из списка
2. Нажмите большую кнопку подключения
3. Разрешите VPN доступ (первый раз)
4. Дождитесь статуса "CONNECTED"

### Формат VLESS URL

Поддерживаемый формат:
```
vless://UUID@ADDRESS:PORT?param1=value1&param2=value2#NAME
```

Поддерживаемые параметры:
- `type`: tcp, ws, grpc, http, quic
- `security`: none, tls, reality
- `encryption`: none (обычно)
- `flow`: xtls-rprx-vision и др.
- `sni`: Server Name Indication
- `fp`: TLS fingerprint
- `alpn`: Application-Layer Protocol Negotiation
- `path`: WebSocket path
- `host`: WebSocket/HTTP host
- `serviceName`: gRPC service name

## Разработка

### Добавление новых функций

#### Добавление нового транспорта
1. Обновите `VlessServer.kt` с новыми полями
2. Добавьте парсинг в `VlessParser.kt`
3. Обновите `XrayConfig.kt` для генерации конфига

#### Добавление статистики
1. Реализуйте `XrayJNI.getStats()` в `XrayManager.kt`
2. Обновите `TrafficStats` в `VlessServer.kt`
3. Добавьте периодическое обновление в `MainViewModel.kt`
4. Обновите UI в `MainScreen.kt`

### Тестирование

```bash
# Unit тесты
./gradlew test

# Инструментальные тесты
./gradlew connectedAndroidTest

# Lint проверка
./gradlew lint
```

### Отладка VPN

Логи можно просмотреть через Logcat:
```bash
adb logcat -s TunelVpnService XrayManager MainViewModel
```

## TODO

- [ ] Интеграция реального Xray-core
- [ ] Реализация packet forwarding через SOCKS proxy
- [ ] Статистика трафика в реальном времени
- [ ] Split tunneling (выборочная маршрутизация)
- [ ] Автоматический выбор оптимального сервера
- [ ] Тесты скорости
- [ ] Экспорт/импорт конфигураций
- [ ] Резервное копирование в облако
- [ ] Виджет для быстрого подключения
- [ ] Shortcuts для Android
- [ ] Уведомления о состоянии соединения

## Известные проблемы

1. **Xray не интегрирован**: Требуется добавить libXray.aar для полной функциональности
2. **Packet forwarding**: Stub реализация, требуется tun2socks интеграция
3. **Статистика**: Показывает 0, пока не реализована связь с Xray

## Безопасность

- ✅ VPN разрешения корректно запрашиваются
- ✅ Данные сервера хранятся локально в зашифрованной БД
- ✅ Не собирается телеметрия
- ⚠️ UUID хранится в открытом виде (можно зашифровать)
- ⚠️ Не реализован SSL Pinning

## Лицензия

Этот проект создан для образовательных целей. Используйте на свой риск.

## Ресурсы

- [Xray Documentation](https://xtls.github.io/)
- [Android VPN Guide](https://developer.android.com/guide/topics/connectivity/vpn)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)

## Авторы

Разработано для демонстрации современной Android разработки с Kotlin и Jetpack Compose.

## Поддержка

Если у вас возникли вопросы или проблемы:
1. Проверьте раздел "Известные проблемы"
2. Убедитесь, что Xray библиотека интегрирована
3. Проверьте логи через adb logcat
4. Убедитесь в правильности формата VLESS URL

---

**Важно**: Это приложение требует интеграции Xray-core для полной функциональности. Текущая версия содержит stub реализации для демонстрации архитектуры и UI/UX.
