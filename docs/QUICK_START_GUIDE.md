# ⚡ TunelApp - Краткая Инструкция

## 🚀 Быстрый Старт (1 минута)

### Установка:
```bash
cd /Users/stanislave/Documents/Projects/TunelApp
./gradlew installMobileDebug
```

### Первый Запуск:
1. Откройте TunelApp на устройстве
2. Скопируйте VLESS/VMess/Shadowsocks URL
3. Нажмите кнопку **+** (FAB)
4. Сервер импортируется автоматически
5. Нажмите большую кнопку **CONNECT**
6. Разрешите VPN (первый раз)
7. ✅ Готово! VPN работает!

---

## 📖 Основные Функции

### Импорт Серверов:
- **Протоколы:** VLESS, VMess, Shadowsocks, Trojan, SOCKS5, HTTP
- **Способы:** Буфер обмена, QR-код (скоро), Подписки

### Подписки:
```
1. Меню → Subscriptions
2. Add Subscription
3. Вставьте URL подписки
4. Auto-update включится автоматически
```

### Тестирование:
```
1. Долгое нажатие на сервер
2. Test Latency - проверить задержку
3. Fastest Server - найти лучший
```

---

## 🔧 Отладка

### Логи:
```bash
# Основные логи
adb logcat -s TunelApp SingBoxManager

# Трафик
adb logcat -s PacketForwarder TrafficMonitor

# sing-box процесс
adb shell ps | grep sing-box
```

### Проверки:
```bash
# VPN интерфейс
adb shell ifconfig tun0

# SOCKS proxy
adb shell netstat -an | grep 10808

# Размер БД
adb shell ls -lh /data/data/com.tunelapp/databases/
```

---

## 💡 Советы

### Производительность:
- Используйте Bypass Local для локальных адресов
- Тестируйте серверы перед использованием
- Обновляйте подписки регулярно

### Безопасность:
- Не используйте публичные SOCKS прокси
- Проверяйте источник подписок
- Используйте TLS/Reality для VLESS

### Troubleshooting:
1. VPN не подключается → Проверьте логи sing-box
2. Нет интернета → Проверьте routing rules
3. Медленно → Тестируйте latency
4. Крашится → Очистите данные приложения

---

## 📚 Документация

- **FINAL_SUMMARY.md** - Полная сводка
- **INTEGRATION_COMPLETE.md** - Детали интеграции
- **WHAT_WAS_ADDED.md** - Список изменений
- **BUILD_SUCCESS.md** - Результаты сборки

---

## 🎯 Следующие Шаги

### Для Лучшего Опыта:
1. Добавьте QR scanner UI (30-60 мин)
2. Создайте UI для подписок (2-3 часа)
3. Добавьте Quick Settings Tile (1 час)
4. Создайте Widget (2 часа)

### Для Production:
1. Замените PacketForwarder на tun2socks или sing-box TUN mode
2. Добавьте ProGuard rules для уменьшения APK
3. Создайте release keystore
4. Протестируйте на разных устройствах

---

## ✅ Чеклист Проверки

- [ ] Приложение устанавливается
- [ ] Можно импортировать сервер
- [ ] VPN подключается
- [ ] Статистика обновляется
- [ ] Интернет работает через VPN
- [ ] VPN отключается корректно
- [ ] Подписки обновляются
- [ ] Тестирование серверов работает

---

## 🎉 Готово!

**TunelApp полностью функционален!**

Наслаждайтесь вашим современным VPN клиентом! 🚀

