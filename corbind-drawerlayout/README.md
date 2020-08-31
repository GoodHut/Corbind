﻿
# corbind-drawerlayout

To add androidx drawerlayout bindings, import `corbind-drawerlayout` module:

```groovy
implementation 'ru.ldralighieri.corbind:corbind-drawerlayout:1.4.0'
```

## List of extensions

Component | Extension | Description
--|---|--
**DrawerLayout** | `drawerOpens` | Called when a drawer has settled in a completely open or close state.


## Example

```kotlin
drawer.drawerOpens() // Flow<Boolean>
    .onEach { isOpen ->
      tv_message = "Drawer completely ${ if (isOpen) "open" else "close"}"
    }
    .launchIn(scope)
```

More examples in source code
