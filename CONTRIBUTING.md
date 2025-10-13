# Contributing to TunelApp

Спасибо за интерес к проекту TunelApp! Мы рады любому вкладу в развитие проекта.

## Как внести вклад

### Сообщение об ошибках

Если вы нашли ошибку:
1. Проверьте, не была ли она уже сообщена в Issues
2. Создайте новый Issue с подробным описанием
3. Включите шаги для воспроизведения
4. Добавьте логи (adb logcat)
5. Укажите версию Android и модель устройства

### Предложение улучшений

Для предложения новых функций:
1. Создайте Issue с меткой "enhancement"
2. Опишите предлагаемую функцию
3. Объясните, почему она будет полезна
4. При возможности, предложите реализацию

### Pull Requests

1. **Fork проекта**
2. **Создайте ветку** для вашей функции:
   ```bash
   git checkout -b feature/amazing-feature
   ```
3. **Следуйте code style**:
   - Kotlin coding conventions
   - Material Design guidelines
   - MVVM архитектура
4. **Пишите чистый код**:
   - Документируйте публичные методы
   - Добавляйте комментарии для сложной логики
   - Следуйте принципам SOLID
5. **Тестируйте**:
   - Добавьте unit тесты для новой логики
   - Проверьте на разных версиях Android
   - Протестируйте на TV, если меняете UI
6. **Commit**:
   ```bash
   git commit -m "Add: amazing feature"
   ```
7. **Push**:
   ```bash
   git push origin feature/amazing-feature
   ```
8. **Создайте Pull Request**

### Code Style

- **Kotlin**: Следуйте официальным [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- **Compose**: Используйте Material Design 3 компоненты
- **Naming**: 
  - Classes: PascalCase
  - Functions/Variables: camelCase
  - Constants: UPPER_SNAKE_CASE
- **Formatting**: Используйте Android Studio auto-format (Ctrl+Alt+L)

### Приоритетные задачи

Особенно приветствуются вклады в:
1. ✅ Интеграция реального Xray-core
2. ✅ Packet forwarding через tun2socks
3. ✅ Статистика трафика
4. ✅ Split tunneling
5. ✅ Unit и UI тесты

### Структура коммитов

Используйте семантические коммиты:
- `Add:` - новая функция
- `Fix:` - исправление ошибки
- `Update:` - обновление существующего
- `Remove:` - удаление кода
- `Refactor:` - рефакторинг
- `Docs:` - документация
- `Test:` - тесты

Пример:
```
Add: WebSocket transport support in VlessParser

- Implemented WebSocket parameter parsing
- Added ws settings to XrayConfig
- Updated UI to show transport type
```

### Тестирование

Перед отправкой PR убедитесь:
- [ ] Код компилируется без ошибок
- [ ] Все существующие тесты проходят
- [ ] Новый код покрыт тестами
- [ ] Lint проверка успешна
- [ ] Приложение работает на Android 5.0+
- [ ] TV версия работает с D-pad

### Лицензия

Внося вклад, вы соглашаетесь, что ваш код будет распространяться под той же лицензией, что и проект.

### Вопросы?

Если у вас есть вопросы:
- Создайте Issue с меткой "question"
- Проверьте существующую документацию
- Изучите примеры кода в проекте

Спасибо за ваш вклад! 🚀



