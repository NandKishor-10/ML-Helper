package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ViewMode

@Composable
fun EditorStatusBar(
    lineCount: Int,
    charCount: Int,
    viewMode: ViewMode,
    isDirty: Boolean,
    fontSizeSp: Float,
    tabSize: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$lineCount lines  |  $charCount chars  |  ${fontSizeSp.toInt()}sp  |  Tab: ${tabSize}sp",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = when {
                    viewMode == ViewMode.EDITOR && isDirty -> "● Unsaved"
                    viewMode == ViewMode.EDITOR -> "Editor Active"
                    else -> "Reader View"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (isDirty) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}
