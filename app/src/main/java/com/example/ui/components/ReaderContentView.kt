package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.SyntaxHighlighter

@Composable
fun ReaderContentView(
    lines: List<String>,
    listState: LazyListState,
    isDark: Boolean,
    fontSizeSp: Float,
    wrapLines: Boolean,
    tabSize: Int,
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    val totalDigits = remember(lines.size) {
        lines.size.toString().length.coerceAtLeast(2)
    }

    val lineNumberWidth = remember(totalDigits, fontSizeSp) {
        (totalDigits * fontSizeSp * 0.7f).coerceAtLeast(36f).dp
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (wrapLines) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("reader_lazy_column"),
                contentPadding = PaddingValues(bottom = 96.dp, top = 8.dp, end = 8.dp)
            ) {
                itemsIndexed(
                    items = lines,
                    key = { index, _ -> index }
                ) { index, line ->
                    LineRow(
                        lineNumber = index + 1,
                        line = line,
                        isDark = isDark,
                        fontSizeSp = fontSizeSp,
                        wrapLines = true,
                        tabSize = tabSize,
                        searchQuery = searchQuery,
                        lineNumberWidth = lineNumberWidth
                    )
                }
            }

            // Right vertical scrollbar when text-wrap is enabled
            LazyListVerticalScrollbar(
                listState = listState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 2.dp, top = 8.dp, bottom = 96.dp)
            )
        } else {
            val hScrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(hScrollState)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(min = 1600.dp)
                        .testTag("reader_lazy_column_unwrapped"),
                    contentPadding = PaddingValues(bottom = 96.dp, top = 8.dp, end = 120.dp)
                ) {
                    itemsIndexed(
                        items = lines,
                        key = { index, _ -> index }
                    ) { index, line ->
                        LineRow(
                            lineNumber = index + 1,
                            line = line,
                            isDark = isDark,
                            fontSizeSp = fontSizeSp,
                            wrapLines = false,
                            tabSize = tabSize,
                            searchQuery = searchQuery,
                            lineNumberWidth = lineNumberWidth
                        )
                    }
                }
            }

            // Right vertical scrollbar
            LazyListVerticalScrollbar(
                listState = listState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 2.dp, top = 8.dp, bottom = 96.dp)
            )

            // Bottom horizontal scrollbar when text-wrap is disabled
            HorizontalScrollbar(
                scrollState = hScrollState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp, start = 8.dp, end = 8.dp)
            )
        }
    }
}

@Composable
private fun LineRow(
    lineNumber: Int,
    line: String,
    isDark: Boolean,
    fontSizeSp: Float,
    wrapLines: Boolean,
    tabSize: Int,
    searchQuery: String,
    lineNumberWidth: androidx.compose.ui.unit.Dp
) {
    val highlightedText = remember(line, isDark, tabSize, searchQuery) {
        SyntaxHighlighter.highlightLine(line, isDark, tabSize, searchQuery)
    }

    Row(
        modifier = if (wrapLines) {
            Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)
        } else {
            Modifier
                .wrapContentWidth()
                .padding(vertical = 1.dp)
        },
        verticalAlignment = Alignment.Top
    ) {
        // Line number gutter
        Text(
            text = lineNumber.toString(),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            fontSize = (fontSizeSp - 2f).coerceAtLeast(9f).sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(lineNumberWidth)
                .padding(end = 12.dp, top = 2.dp)
        )

        // Line content with syntax highlights
        Text(
            text = highlightedText,
            fontSize = fontSizeSp.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = (fontSizeSp * 1.35f).sp,
            softWrap = wrapLines,
            modifier = if (wrapLines) {
                Modifier.weight(1f, fill = false)
            } else {
                Modifier.wrapContentWidth()
            }
        )
    }
}
