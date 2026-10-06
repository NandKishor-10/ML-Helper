package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppThemeMode
import com.example.model.FileType
import com.example.model.OpenFileInfo
import com.example.model.ViewMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerTopBar(
    fileInfo: OpenFileInfo?,
    viewMode: ViewMode,
    isDirty: Boolean,
    tabSize: Int,
    themeMode: AppThemeMode,
    isSearchActive: Boolean,
    searchQuery: String,
    searchMatchCount: Int,
    currentMatchIndex: Int,
    wrapLines: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onNextMatch: () -> Unit,
    onPrevMatch: () -> Unit,
    onOpenFilePicker: () -> Unit,
    onSaveFile: () -> Unit,
    onExportFile: () -> Unit,
    onSwitchToReader: () -> Unit,
    onNavigateHome: () -> Unit,
    onOpenTabSizeDialog: () -> Unit,
    onOpenThemeDialog: () -> Unit,
    onToggleWrap: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            TopAppBar(
                navigationIcon = {
                    if (viewMode != ViewMode.HOME) {
                        IconButton(
                            onClick = onNavigateHome,
                            modifier = Modifier.testTag("nav_back_home_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Home"
                            )
                        }
                    }
                },
                title = {
                    if (viewMode == ViewMode.HOME) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "MLViewer",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val typeBadge = fileInfo?.fileType ?: FileType.XML
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (typeBadge) {
                                    FileType.XML -> MaterialTheme.colorScheme.primaryContainer
                                    FileType.HTML -> MaterialTheme.colorScheme.tertiaryContainer
                                    FileType.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = typeBadge.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when (typeBadge) {
                                        FileType.XML -> MaterialTheme.colorScheme.onPrimaryContainer
                                        FileType.HTML -> MaterialTheme.colorScheme.onTertiaryContainer
                                        FileType.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = fileInfo?.fileName ?: "Document",
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isDirty) {
                                        Text(
                                            text = " • edited",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (viewMode == ViewMode.READER) "Reader Mode • Tab: ${tabSize}sp • ${fileInfo?.fileSizeFormatted ?: ""}"
                                    else "Editor Mode • Tab: ${tabSize}sp • ${fileInfo?.fileSizeFormatted ?: ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (viewMode != ViewMode.HOME) {
                        IconButton(
                            onClick = onToggleSearch,
                            modifier = Modifier.testTag("search_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = if (isSearchActive) "Close Search" else "Search Document"
                            )
                        }

                        if (viewMode == ViewMode.EDITOR || isDirty) {
                            IconButton(
                                onClick = onSaveFile,
                                modifier = Modifier.testTag("save_file_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "Save Document",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (viewMode == ViewMode.EDITOR) {
                            IconButton(
                                onClick = onSwitchToReader,
                                modifier = Modifier.testTag("exit_editor_to_reader_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "Switch to Reader"
                                )
                            }
                        }
                    }

                    // Overflow Menu
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("overflow_menu_button")
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Open File…") },
                            leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onOpenFilePicker()
                            },
                            modifier = Modifier.testTag("menu_open_file")
                        )

                        if (viewMode != ViewMode.HOME) {
                            DropdownMenuItem(
                                text = { Text("Save As (Export)…") },
                                leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onExportFile()
                                },
                                modifier = Modifier.testTag("menu_export_file")
                            )
                        }

                        HorizontalDivider()

                        // Tab size preference option (Works in both Viewer Mode & Editor Mode)
                        DropdownMenuItem(
                            text = { Text("Tab Size ($tabSize spaces)") },
                            leadingIcon = { Icon(Icons.Default.SpaceBar, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onOpenTabSizeDialog()
                            },
                            modifier = Modifier.testTag("menu_tab_size")
                        )

                        // Theme preference option (System, Light, Dark)
                        DropdownMenuItem(
                            text = { Text("Theme (${themeMode.label})") },
                            leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onOpenThemeDialog()
                            },
                            modifier = Modifier.testTag("menu_theme_mode")
                        )

                        if (viewMode != ViewMode.HOME) {
                            DropdownMenuItem(
                                text = { Text(if (wrapLines) "Disable Word Wrap" else "Enable Word Wrap") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.WrapText, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onToggleWrap()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Zoom In (+)") },
                                leadingIcon = { Icon(Icons.Default.ZoomIn, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onZoomIn()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Zoom Out (-)") },
                                leadingIcon = { Icon(Icons.Default.ZoomOut, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onZoomOut()
                                }
                            )
                        }
                    }
                }
            )

            // Search bar sub-panel
            AnimatedVisibility(
                visible = isSearchActive && viewMode != ViewMode.HOME,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChanged,
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("search_input_field"),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        "Find in document…",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        if (searchQuery.isNotEmpty()) {
                            Text(
                                text = if (searchMatchCount > 0) "${currentMatchIndex + 1}/$searchMatchCount" else "0/0",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            IconButton(
                                onClick = onPrevMatch,
                                enabled = searchMatchCount > 0,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous Match")
                            }

                            IconButton(
                                onClick = onNextMatch,
                                enabled = searchMatchCount > 0,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next Match")
                            }
                        }
                    }
                }
            }
        }
    }
}
