﻿
# corbind-core

To add androidx core bindings, import `corbind-core` module:

```groovy
implementation 'ru.ldralighieri.corbind:corbind-core:1.4.0'
```

## List of extensions

Component | Extension | Description
--|---|--
**NestedScrollView** | `scrollChangeEvents` | Called when the scroll position of a view changes.


## Example

```kotlin
scrollView.scrollChangeEvents() // Flow<ViewScrollChangeEvent>
    .onEach { /* handle scroll change events */ }
    .launchIn(scope)
```

More examples in source code
