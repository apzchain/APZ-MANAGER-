```kotlin
package com.apz.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.apz.manager.data.model.RiskLevel

@Composable
fun RiskBadge(level: RiskLevel, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(level.color)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "${level.labelFa} (${level.label})",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
```
