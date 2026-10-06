package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TabSizeDialog(
    currentTabSize: Int,
    onTabSizeSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(2, 4, 8)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Tab Indentation Size")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Choose number of spaces for indentation:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                options.forEach { size ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onTabSizeSelected(size) }
                            .testTag("tab_size_option_$size"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (currentTabSize == size)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$size Spaces" + if (size == 2) " (Default)" else "",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (currentTabSize == size) FontWeight.Bold else FontWeight.Normal,
                                color = if (currentTabSize == size)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                            RadioButton(
                                selected = (currentTabSize == size),
                                onClick = { onTabSizeSelected(size) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_tab_dialog")
            ) {
                Text("Done")
            }
        }
    )
}
