package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun SaveConfirmationDialog(
    fileName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Save changes?")
        },
        text = {
            Text(text = "Do you want to save modifications made to \"$fileName\"?")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_save_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun UnsavedExitDialog(
    onDiscard: () -> Unit,
    onKeepEditing: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        icon = {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        },
        title = {
            Text(text = "Discard unsaved changes?")
        },
        text = {
            Text(text = "You have modified the file. If you leave edit mode now, your unsaved edits will be lost.")
        },
        confirmButton = {
            Button(
                onClick = onDiscard,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.testTag("confirm_discard_button")
            ) {
                Text("Discard")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onKeepEditing,
                modifier = Modifier.testTag("keep_editing_button")
            ) {
                Text("Keep Editing")
            }
        }
    )
}
