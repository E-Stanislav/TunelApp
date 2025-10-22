# 🔧 Опции Интеграции VPN Ядра

## ⚠️ Важное Открытие

**sing-box НЕ предоставляет готовые .aar файлы!**

Актуальная версия: `v1.12.10`  
Доступные файлы: только `.tar.gz` с бинарниками для Android

---

## 3 Реальных Варианта Интеграции

### Вариант 1: Использовать Libcore от SagerNet (РЕКОМЕНДУЕТСЯ) ⭐

**Что это:**  
SagerNet предоставляет готовую библиотеку `libcore` которая включает sing-box и готова к использованию.

**Репозиторий:** https://github.com/SagerNet/sing-box-for-android

**Шаги:**
```bash
# Клонировать репозиторий
git clone https://github.com/SagerNet/sing-box-for-android.git
cd sing-box-for-android

# Собрать libcore (требует Android NDK)
./gradlew :libcore:assembleRelease

# Скопировать AAR
cp libcore/build/outputs/aar/libcore-release.aar \
   /Users/stanislave/Documents/Projects/TunelApp/app/libs/libcore.aar
```

**В build.gradle.kts:**
```kotlin
implementation(files("libs/libcore.aar"))
```

**Время:** 30-60 минут (если есть Android NDK)

---

### Вариант 2: Взять готовый AAR из NekoBox

**Что это:**  
NekoBox уже использует sing-box и включает готовый AAR в свой проект.

**Репозиторий:** https://github.com/MatsuriDayo/NekoBoxForAndroid

**Шаги:**
```bash
# Клонировать NekoBox
git clone https://github.com/MatsuriDayo/NekoBoxForAndroid.git
cd NekoBoxForAndroid

# Найти libcore
find . -name "*.aar" | grep libcore

# Скопировать к себе
cp <путь_к_libcore.aar> /Users/stanislave/Documents/Projects/TunelApp/app/libs/
```

**Время:** 10-15 минут

---

### Вариант 3: v2rayNG с Xray-core (Альтернатива)

**Что это:**  
v2rayNG использует Xray-core и включает готовые библиотеки.

**Репозиторий:** https://github.com/2dust/v2rayNG

**Шаги:**
```bash
# Клонировать v2rayNG
git clone https://github.com/2dust/v2rayNG.git
cd v2rayNG

# Найти библиотеки
ls V2rayNG/app/libs/

# Скопировать нужные
cp V2rayNG/app/libs/*.aar \
   /Users/stanislave/Documents/Projects/TunelApp/app/libs/
```

**Время:** 10-15 минут

---

## 🎯 Рекомендация

**Используйте Вариант 2 (NekoBox)** - самый быстрый способ:

1. Склонировать NekoBox
2. Найти и скопировать libcore.aar
3. Добавить в свой проект
4. Готово!

---

## 📝 После Получения AAR

### Шаг 1: Добавить в проект

```kotlin
// app/build.gradle.kts
dependencies {
    // ... остальные зависимости
    
    // Libcore from SagerNet/NekoBox
    implementation(files("libs/libcore.aar"))
}
```

### Шаг 2: Обновить XrayManager.kt

```kotlin
package com.tunelapp.core

import io.nekohasekai.libbox.* // Импорт из libcore

class XrayManager(private val context: Context) {
    
    private var box: BoxService? = null
    
    suspend fun start(server: ProxyServer): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Генерируем конфиг
            val config = ProxyConfig.generateXrayConfig(server)
            
            // Запускаем sing-box
            box = Libbox.newStandaloneBox(
                BoxConfig.Builder()
                    .setConfiguration(config)
                    .build()
            )
            
            box?.start()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun stop(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            box?.stop()
            box = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### Шаг 3: Обновить TunelVpnService.kt

```kotlin
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    // ... получить сервер
    
    // Установить VPN
    val builder = Builder()
        .setSession("TunelApp")
        .setMtu(1500)
        .addAddress("10.0.0.2", 24)
        .addRoute("0.0.0.0", 0)
        .addDnsServer("8.8.8.8")
    
    val tunFd = builder.establish()?.detachFd() ?: return START_NOT_STICKY
    
    // Запустить libcore
    xrayManager.start(server)
    
    // libcore сам обрабатывает packet forwarding!
    
    return START_STICKY
}
```

---

## 🚀 Быстрый Старт (Рекомендуется)

```bash
# 1. Клонировать NekoBox
cd ~/Downloads
git clone https://github.com/MatsuriDayo/NekoBoxForAndroid.git

# 2. Найти libcore.aar
cd NekoBoxForAndroid
find . -name "libcore*.aar"

# 3. Скопировать в свой проект
cp <путь>/libcore.aar /Users/stanislave/Documents/Projects/TunelApp/app/libs/

# 4. Открыть Android Studio и раскомментировать в build.gradle.kts:
# implementation(files("libs/libcore.aar"))

# 5. Gradle Sync

# 6. Готово!
```

---

## ⏱️ Время на Интеграцию

- **Вариант 1** (собрать libcore): 30-60 мин + требует NDK
- **Вариант 2** (взять из NekoBox): **10-15 мин** ⭐
- **Вариант 3** (v2rayNG/Xray): 10-15 мин

---

## 📚 Документация

### Libcore API
- https://github.com/SagerNet/sing-box-for-android
- https://github.com/MatsuriDayo/NekoBoxForAndroid

### Примеры Использования
Посмотреть как NekoBox использует libcore:
```
NekoBoxForAndroid/app/src/main/java/io/nekohasekai/sagernet/
```

---

## ✅ Следующие Шаги

1. [ ] Выбрать вариант (рекомендую #2)
2. [ ] Получить libcore.aar
3. [ ] Добавить в build.gradle.kts
4. [ ] Обновить XrayManager.kt
5. [ ] Обновить TunelVpnService.kt
6. [ ] Тестировать!

---

**Время до рабочего VPN: 1-2 часа** (после получения библиотеки)

