package com.example.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.AppThemeMode
import com.example.model.FileType
import com.example.model.ViewMode
import com.example.ui.components.*
import com.example.viewmodel.UiEvent
import com.example.viewmodel.ViewerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun MainViewerScreen(
    viewModel: ViewerViewModel,
    onFinishApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val recentFiles by viewModel.recentFiles.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val readerListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val systemDark = isSystemInDarkTheme()
    val isDark = when (uiState.themeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.openFileFromUri(context, uri)
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            uiState.failedUri?.let { failedUri ->
                viewModel.openFileFromUri(context, failedUri)
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Storage permission denied. You can still open files via the document picker.")
            }
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            if (uiState.fileInfo?.fileType == FileType.HTML) "text/html" else "application/xml"
        )
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.saveContentToExportUri(context, uri)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.ScrollToLine -> {
                    coroutineScope.launch {
                        readerListState.animateScrollToItem(event.lineNumber.coerceAtLeast(0))
                    }
                }
            }
        }
    }

    BackHandler {
        viewModel.handleBackPress(onExit = onFinishApp)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_viewer_scaffold"),
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            ViewerTopBar(
                fileInfo = uiState.fileInfo,
                viewMode = uiState.viewMode,
                isDirty = uiState.isDirty,
                tabSize = uiState.tabSize,
                themeMode = uiState.themeMode,
                isSearchActive = uiState.isSearchActive,
                searchQuery = uiState.searchQuery,
                searchMatchCount = uiState.searchMatchCount,
                currentMatchIndex = uiState.currentMatchIndex,
                wrapLines = uiState.wrapLines,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onToggleSearch = viewModel::toggleSearch,
                onNextMatch = viewModel::nextSearchMatch,
                onPrevMatch = viewModel::prevSearchMatch,
                onOpenFilePicker = {
                    openDocumentLauncher.launch(
                        arrayOf(
                            "text/xml",
                            "application/xml",
                            "text/html",
                            "application/xhtml+xml",
                            "text/plain",
                            "*/*"
                        )
                    )
                },
                onSaveFile = viewModel::requestSave,
                onExportFile = {
                    val defaultName = uiState.fileInfo?.fileName ?: "document.xml"
                    createDocumentLauncher.launch(defaultName)
                },
                onSwitchToReader = viewModel::switchToReaderMode,
                onNavigateHome = viewModel::goHome,
                onOpenTabSizeDialog = viewModel::showTabSizeDialog,
                onOpenThemeDialog = viewModel::showThemeDialog,
                onToggleWrap = viewModel::toggleWrap,
                onZoomIn = viewModel::zoomIn,
                onZoomOut = viewModel::zoomOut
            )
        },
        bottomBar = {
            if (uiState.viewMode != ViewMode.HOME && uiState.errorMessage == null) {
                EditorStatusBar(
                    lineCount = uiState.lineCount,
                    charCount = uiState.rawContent.length,
                    viewMode = uiState.viewMode,
                    isDirty = uiState.isDirty,
                    fontSizeSp = uiState.fontSizeSp,
                    tabSize = uiState.tabSize,
                    modifier = Modifier.navigationBarsPadding()
                )
            } else if (uiState.viewMode == ViewMode.HOME) {
                // Spacer for bottom navigation bar padding on homescreen
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        },
        floatingActionButton = {
            when {
                uiState.viewMode == ViewMode.HOME && recentFiles.isNotEmpty() && uiState.errorMessage == null -> {
                    FloatingActionButton(
                        onClick = {
                            openDocumentLauncher.launch(
                                arrayOf(
                                    "text/xml",
                                    "application/xml",
                                    "text/html",
                                    "application/xhtml+xml",
                                    "text/plain",
                                    "*/*"
                                )
                            )
                        },
                        modifier = Modifier
                            .navigationBarsPadding()
                            .testTag("home_open_file_fab")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Open New File")
                    }
                }
                uiState.viewMode == ViewMode.READER && uiState.errorMessage == null -> {
                    ExtendedFloatingActionButton(
                        text = { Text("Edit") },
                        icon = { Icon(Icons.Default.Edit, contentDescription = "Edit File") },
                        onClick = { viewModel.switchToEditMode() },
                        modifier = Modifier
                            .navigationBarsPadding()
                            .testTag("edit_file_fab")
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding()
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.testTag("loading_indicator"))
                    }
                }
                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .navigationBarsPadding(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (uiState.isPermissionError) Icons.Default.Lock else Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (uiState.isPermissionError) "Access Permission Needed" else "Unable to Open File",
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = uiState.errorMessage ?: "Unknown error occurred.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    openDocumentLauncher.launch(
                                        arrayOf(
                                            "text/xml",
                                            "application/xml",
                                            "text/html",
                                            "application/xhtml+xml",
                                            "text/plain",
                                            "*/*"
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .testTag("select_with_file_picker_button")
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Select File via Document Picker")
                            }

                            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 && uiState.isPermissionError) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = {
                                        storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                    },
                                    modifier = Modifier.fillMaxWidth(0.85f)
                                ) {
                                    Text("Request Storage Permission")
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            TextButton(
                                onClick = viewModel::clearErrorAndGoHome,
                                modifier = Modifier.testTag("back_to_home_button")
                            ) {
                                Text("Back to Home")
                            }
                        }
                    }
                }
                uiState.viewMode == ViewMode.HOME -> {
                    HomeScreenContent(
                        recentFiles = recentFiles,
                        onOpenFilePicker = {
                            openDocumentLauncher.launch(
                                arrayOf(
                                    "text/xml",
                                    "application/xml",
                                    "text/html",
                                    "application/xhtml+xml",
                                    "text/plain",
                                    "*/*"
                                )
                            )
                        },
                        onOpenRecentFile = { uriString ->
                            viewModel.openFileFromUri(context, Uri.parse(uriString))
                        },
                        onDeleteRecentFile = { uriString ->
                            viewModel.removeRecentFile(uriString)
                        },
                        onClearAllHistory = {
                            viewModel.clearAllRecentFiles()
                        }
                    )
                }
                uiState.viewMode == ViewMode.READER -> {
                    ReaderContentView(
                        lines = uiState.lines,
                        listState = readerListState,
                        isDark = isDark,
                        fontSizeSp = uiState.fontSizeSp,
                        wrapLines = uiState.wrapLines,
                        tabSize = uiState.tabSize,
                        searchQuery = uiState.searchQuery
                    )
                }
                uiState.viewMode == ViewMode.EDITOR -> {
                    EditorContentView(
                        content = uiState.rawContent,
                        onContentChanged = viewModel::onContentChanged,
                        fontSizeSp = uiState.fontSizeSp,
                        wrapLines = uiState.wrapLines,
                        tabSize = uiState.tabSize
                    )
                }
            }
        }
    }

    if (uiState.showSaveConfirmationDialog) {
        SaveConfirmationDialog(
            fileName = uiState.fileInfo?.fileName ?: "File",
            onConfirm = { viewModel.confirmSave(context) },
            onDismiss = viewModel::dismissSaveConfirmation
        )
    }

    if (uiState.showUnsavedExitDialog) {
        UnsavedExitDialog(
            onDiscard = viewModel::discardChangesAndSwitchToReader,
            onKeepEditing = viewModel::dismissUnsavedExitDialog
        )
    }

    if (uiState.showTabSizeDialog) {
        TabSizeDialog(
            currentTabSize = uiState.tabSize,
            onTabSizeSelected = viewModel::setTabSize,
            onDismiss = viewModel::dismissTabSizeDialog
        )
    }

    if (uiState.showThemeDialog) {
        ThemeSelectionDialog(
            currentThemeMode = uiState.themeMode,
            onThemeSelected = viewModel::setThemeMode,
            onDismiss = viewModel::dismissThemeDialog
        )
    }
}
