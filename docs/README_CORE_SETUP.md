# ⚡ Быстрая Настройка VPN Ядра

## 🎯 Проблема

sing-box **НЕ предоставляет** готовые `.aar` файлы для Android.  
Версия файла которую мы пытались скачать **не существует** (404 ошибка).

## ✅ Решение

Используйте новый скрипт `setup_vpn_core.sh` который предлагает 3 варианта:

### Вариант 1: NekoBox libcore (РЕКОМЕНДУЕТСЯ) ⭐

```bash
./setup_vpn_core.sh
# Выберите опцию 1

# Скрипт автоматически:
# 1. Склонирует NekoBox
# 2. Найдет libcore.aar
# 3. Скопирует в app/libs/
# 4. Покажет следующие шаги
```

**Время:** 5-10 минут  
**Результат:** Готовый к использованию libcore.aar

---

## 📝 После Получения libcore.aar

### Шаг 1: Обновить build.gradle.kts

```kotlin
// app/build.gradle.kts

dependencies {
    // ... другие зависимости
    
    // Добавить эту строку:
    implementation(files("libs/libcore.aar"))
}
```

### Шаг 2: Sync Gradle

В Android Studio нажмите **Sync Now**

### Шаг 3: Обновить XrayManager.kt

```kotlin
package com.tunelapp.core

import io.nekohasekai.libbox.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class XrayManager(private val context: Context) {
    
    private var boxService: BoxService? = null
    
    suspend fun start(server: ProxyServer): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val config = ProxyConfig.generateXrayConfig(server)
            
            boxService = Libbox.newStandaloneBox(
                BoxConfig.Builder()
                    .setConfiguration(config)
                    .build()
            )
            
            boxService?.start()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun stop(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            boxService?.stop()
            boxService = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun isRunning(): Boolean {
        return boxService != null
    }
}
```

### Шаг 4: Тестировать!

```bash
# Запустить приложение
./gradlew installMobileDebug

# Проверить логи
adb logcat -s TunelApp XrayManager
```

---

## 🚀 Быстрый Запуск (30 секунд)

```bash
# 1. Запустить скрипт
./setup_vpn_core.sh

# 2. Выбрать опцию 1 (NekoBox)

# 3. Дождаться завершения (~5-10 мин)

# 4. Открыть Android Studio

# 5. В app/build.gradle.kts добавить:
#    implementation(files("libs/libcore.aar"))

# 6. Sync Gradle

# 7. ГОТОВО! 🎉
```

---

## 📚 Дополнительная Документация

- **CORE_INTEGRATION_OPTIONS.md** - Подробные опции интеграции
- **NEXT_STEPS.md** - Следующие шаги после интеграции
- **WHAT_WAS_ADDED.md** - Что уже реализовано

---

## ❓ FAQ

**Q: Почему не работает download_singbox.sh?**  
A: sing-box не публикует готовые .aar файлы. Используйте `setup_vpn_core.sh`

**Q: Сколько времени займет интеграция?**  
A: 10-15 минут с автоматическим скриптом

**Q: Нужен ли Android NDK?**  
A: Нет, если используете готовый libcore из NekoBox

**Q: Можно ли использовать без ядра?**  
A: Нет, ядро критично важно для работы VPN

---

## 🎉 Успех!

После интеграции libcore ваше приложение сможет:
- ✅ Подключаться к VPN
- ✅ Маршрутизировать трафик
- ✅ Работать со всеми протоколами
- ✅ Показывать статистику

**Время до рабочего VPN: 1-2 часа максимум!**

