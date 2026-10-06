package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardTab
import androidx.compose.material.icons.filled.SubdirectoryArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EditorContentView(
    content: String,
    onContentChanged: (String) -> Unit,
    fontSizeSp: Float,
    wrapLines: Boolean,
    tabSize: Int,
    modifier: Modifier = Modifier
) {
    // Maintain TextFieldValue to preserve cursor position during tab insertion & auto-indent
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = content, selection = TextRange(content.length)))
    }

    // Keep textFieldValue in sync if content was modified externally (e.g. reload sample)
    LaunchedEffect(content) {
        if (content != textFieldValue.text) {
            val newSelection = textFieldValue.selection.let { sel ->
                val end = sel.end.coerceIn(0, content.length)
                val start = sel.start.coerceIn(0, end)
                TextRange(start, end)
            }
            textFieldValue = textFieldValue.copy(text = content, selection = newSelection)
        }
    }

    val lines = remember(textFieldValue.text) { textFieldValue.text.lines() }
    val totalDigits = remember(lines.size) { lines.size.toString().length.coerceAtLeast(2) }
    val lineNumberWidth = remember(totalDigits, fontSizeSp) {
        (totalDigits * fontSizeSp * 0.7f).coerceAtLeast(36f).dp
    }

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    // Helper function to insert text at current cursor location
    fun insertAtCursor(toInsert: String) {
        val currentText = textFieldValue.text
        val selection = textFieldValue.selection
        val start = selection.min.coerceIn(0, currentText.length)
        val end = selection.max.coerceIn(0, currentText.length)

        val newText = currentText.substring(0, start) + toInsert + currentText.substring(end)
        val newCursor = start + toInsert.length
        val newValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
        textFieldValue = newValue
        onContentChanged(newText)
    }

    // Smart auto-indent on new line
    fun insertNewLineWithAutoIndent() {
        val currentText = textFieldValue.text
        val cursor = textFieldValue.selection.start.coerceIn(0, currentText.length)

        // Find the leading whitespace of the current line
        val lastNewLine = currentText.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0))
        val lineStart = if (lastNewLine == -1) 0 else lastNewLine + 1
        val currentLinePrefix = currentText.substring(lineStart, cursor)
        val indent = currentLinePrefix.takeWhile { it == ' ' || it == '\t' }

        insertAtCursor("\n$indent")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Handy Quick Coding Bar for Tab indentation & tag shortcuts
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tab key button inserts the configured number of spaces at current cursor position
                Button(
                    onClick = {
                        val spaces = " ".repeat(tabSize.coerceAtLeast(1))
                        insertAtCursor(spaces)
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("quick_tab_insert_button")
                ) {
                    Icon(
                        Icons.Default.KeyboardTab,
                        contentDescription = "Insert Tab",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tab ($tabSize sp)", style = MaterialTheme.typography.labelSmall)
                }

                // Auto-indent newline button
                OutlinedButton(
                    onClick = { insertNewLineWithAutoIndent() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("quick_newline_indent_button")
                ) {
                    Icon(
                        Icons.Default.SubdirectoryArrowLeft,
                        contentDescription = "New Line with Indent",
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Quick markup tokens inserted at cursor
                listOf("<", ">", "</", "=\"\"", "\"", "/", "?>").forEach { token ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("quick_insert_$token")
                    ) {
                        TextButton(
                            onClick = { insertAtCursor(token) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                token,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Code Editor text area with scrollbars
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScrollState)
                    .padding(top = 8.dp, bottom = 180.dp)
                    .testTag("editor_scroll_container")
            ) {
                Row(
                    modifier = if (!wrapLines) {
                        Modifier
                            .horizontalScroll(horizontalScrollState)
                            .widthIn(min = 1600.dp)
                    } else {
                        Modifier.fillMaxWidth()
                    }
                ) {
                    // Line numbers column
                    Column(
                        modifier = Modifier
                            .width(lineNumberWidth)
                            .padding(end = 12.dp, top = 2.dp)
                    ) {
                        lines.forEachIndexed { index, _ ->
                            Text(
                                text = (index + 1).toString(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                fontSize = (fontSizeSp - 2f).coerceAtLeast(9f).sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.End,
                                lineHeight = (fontSizeSp * 1.35f).sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Editable input with TextFieldValue
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            // Intercept physical or software tab key ('\t') and expand to configured spaces!
                            val oldText = textFieldValue.text
                            val newText = newValue.text

                            if (newText.length == oldText.length + 1 && newText.contains('\t')) {
                                val cursor = newValue.selection.start.coerceIn(0, newText.length)
                                if (cursor > 0 && newText[cursor - 1] == '\t') {
                                    val spaces = " ".repeat(tabSize.coerceAtLeast(1))
                                    val expandedText = newText.substring(0, cursor - 1) + spaces + newText.substring(cursor)
                                    val updatedCursor = cursor - 1 + spaces.length
                                    val finalVal = TextFieldValue(text = expandedText, selection = TextRange(updatedCursor))
                                    textFieldValue = finalVal
                                    onContentChanged(expandedText)
                                    return@BasicTextField
                                }
                            }

                            textFieldValue = newValue
                            if (newValue.text != content) {
                                onContentChanged(newValue.text)
                            }
                        },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = fontSizeSp.sp,
                            lineHeight = (fontSizeSp * 1.35f).sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = if (wrapLines) {
                            Modifier
                                .weight(1f, fill = false)
                                .fillMaxWidth()
                                .padding(end = 16.dp)
                                .testTag("code_editor_text_field")
                        } else {
                            Modifier
                                .wrapContentWidth()
                                .padding(end = 64.dp)
                                .testTag("code_editor_text_field")
                        }
                    )
                }
            }

            // Right scrollbar for vertical scrolling
            VerticalScrollbar(
                scrollState = verticalScrollState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 2.dp, top = 8.dp, bottom = 8.dp)
            )

            // Bottom scrollbar for horizontal scrolling (when text wrap is disabled)
            if (!wrapLines) {
                HorizontalScrollbar(
                    scrollState = horizontalScrollState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp, start = 8.dp, end = 8.dp)
                )
            }
        }
    }
}
