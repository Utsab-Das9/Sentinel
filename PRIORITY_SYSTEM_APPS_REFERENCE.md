# Priority System Apps - Quick Reference

## Complete List of Included Apps

### Google Apps (14 apps)
| App Name | Package Name |
|----------|--------------|
| YouTube | com.google.android.youtube |
| Play Store | com.android.vending |
| Google Photos | com.google.android.apps.photos |
| Maps | com.google.android.apps.maps |
| Gmail | com.google.android.gm |
| Google | com.google.android.googlequicksearchbox |
| Messages | com.google.android.apps.messaging |
| Chrome | com.android.chrome |
| Files by Google | com.google.android.apps.docs |
| GPay | com.google.android.apps.nbu.paisa.user |
| Android Auto | com.google.android.projection.gearhead |
| Lens | com.google.ar.lens |
| Digital Wellbeing | com.google.android.apps.wellbeing |

### Samsung Apps (16 apps)
| App Name | Package Name |
|----------|--------------|
| Calendar | com.samsung.android.calendar |
| Camera | com.sec.android.app.camera |
| Settings | com.android.settings, com.samsung.android.app.settings.bixby |
| Phone | com.samsung.android.dialer |
| Messages | com.samsung.android.messaging |
| My Files | com.sec.android.app.myfiles |
| Phone Manager | com.samsung.android.app.telephonyui |
| Theme Store | com.samsung.android.themestore |
| Internet | com.samsung.android.app.sbrowseredge |
| Contacts | com.samsung.android.contacts |
| Clock | com.samsung.android.app.clockpackage |
| Videos | com.samsung.android.video |
| Weather | com.samsung.android.weather |
| Game Assistant | com.samsung.android.game.gametools |
| Assistant | com.samsung.android.app.assistant |

### Generic Android Apps (11 apps)
| App Name | Package Name |
|----------|--------------|
| Calculator | com.android.calculator2 |
| Calendar | com.android.calendar |
| Camera | com.android.camera, com.android.camera2 |
| Clock | com.android.deskclock |
| Settings | com.android.settings |
| Phone | com.android.dialer |
| Contacts | com.android.contacts |
| Messages | com.android.mms |
| Downloads | com.android.providers.downloads.ui |
| Files | com.android.documentsui |
| Gallery | com.android.gallery3d |

### Xiaomi/MIUI Apps (3 apps)
| App Name | Package Name |
|----------|--------------|
| Calculator | com.miui.calculator |
| Gallery | com.miui.gallery |
| Compass | com.miui.compass |

### OnePlus Apps (2 apps)
| App Name | Package Name |
|----------|--------------|
| Calculator | com.oneplus.calculator |
| Camera | com.oneplus.camera |

### Oppo/ColorOS Apps (3 apps)
| App Name | Package Name |
|----------|--------------|
| Compass | com.coloros.compass |
| Calculator | com.coloros.calculator |
| Camera | com.oppo.camera |

### Huawei Apps (2 apps)
| App Name | Package Name |
|----------|--------------|
| Calculator | com.huawei.calculator |
| Camera | com.huawei.camera |

### Vivo Apps (2 apps)
| App Name | Package Name |
|----------|--------------|
| Calculator | com.vivo.calculator |
| Camera | com.vivo.camera |

## Total Count
**60+ Priority System Apps** across all major Android manufacturers

## How to Add More Apps

Edit `InstalledAppsRepository.kt` and add to the `PRIORITY_SYSTEM_APPS` set:

```kotlin
private val PRIORITY_SYSTEM_APPS = setOf(
    // ... existing apps ...
    "com.example.newapp",  // New App Name
)
```

## Verification Commands

### Check if app is installed
```bash
adb shell pm list packages | grep <package_name>
```

### Get app details
```bash
adb shell dumpsys package <package_name>
```

### List all system apps
```bash
adb shell pm list packages -s
```

## Common Package Name Patterns

- **Google**: `com.google.android.*`
- **Samsung**: `com.samsung.android.*`, `com.sec.android.*`
- **Xiaomi**: `com.miui.*`
- **OnePlus**: `com.oneplus.*`
- **Oppo**: `com.coloros.*`, `com.oppo.*`
- **Huawei**: `com.huawei.*`
- **Vivo**: `com.vivo.*`
- **Generic Android**: `com.android.*`

## Notes

- Apps are only included if installed on the device
- Missing apps are gracefully skipped (no crashes)
- All apps get full PRS analysis and privacy monitoring
- Apps appear in Dashboard, Apps, and App Insights sections
- Manufacturer-specific apps only appear on respective devices
