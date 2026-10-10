
```kotlin
package com.apz.manager.data.model

import androidx.compose.ui.graphics.Color

enum class RiskLevel(val label: String, val labelFa: String, val color: Color) {
    CLEAN("CLEAN", "پاک", Color(0xFF2E7D32)),
    LOW("LOW", "پایین", Color(0xFF388E3C)),
    MEDIUM("MEDIUM", "متوسط", Color(0xFFF9A825)),
    HIGH("HIGH", "بالا", Color(0xFFE65100)),
    CRITICAL("CRITICAL", "بحرانی", Color(0xFFB71C1C));

    companion object {
        fun from(s: String?): RiskLevel =
            entries.firstOrNull { it.label.equals(s, ignoreCase = true) } ?: CLEAN
    }
}
```
