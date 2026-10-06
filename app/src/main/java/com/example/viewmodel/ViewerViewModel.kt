package com.example.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.model.AppThemeMode
import com.example.model.FileType
import com.example.model.OpenFileInfo
import com.example.model.ViewMode
import com.example.util.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ViewerUiState(
    val fileInfo: OpenFileInfo? = null,
    val rawContent: String = "",
    val originalSavedContent: String = "",
    val lines: List<String> = emptyList(),
    val viewMode: ViewMode = ViewMode.HOME,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val failedUri: Uri? = null,
    val isPermissionError: Boolean = false,
    val showSaveConfirmationDialog: Boolean = false,
    val showUnsavedExitDialog: Boolean = false,
    val showTabSizeDialog: Boolean = false,
    val showThemeDialog: Boolean = false,
    val tabSize: Int = 2, // Default: tab = 2 spaces
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val searchMatchCount: Int = 0,
    val currentMatchIndex: Int = 0,
    val searchMatchLines: List<Int> = emptyList(),
    val fontSizeSp: Float = 14f,
    val wrapLines: Boolean = true
) {
    val isDirty: Boolean
        get() = rawContent != originalSavedContent

    val lineCount: Int
        get() = lines.size
}

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    data class ScrollToLine(val lineNumber: Int) : UiEvent()
}

class ViewerViewModel(
    private val recentFilesRepository: RecentFilesRepository? = null,
    private val preferencesRepository: PreferencesRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    val recentFiles: StateFlow<List<RecentFileEntity>> = recentFilesRepository?.recentFiles
        ?.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        ) ?: MutableStateFlow(emptyList())

    init {
        preferencesRepository?.let { repo ->
            viewModelScope.launch {
                repo.userPreferencesFlow.collect { prefs ->
                    _uiState.update {
                        it.copy(
                            tabSize = prefs.tabSize,
                            themeMode = prefs.themeMode,
                            wrapLines = prefs.wrapLines,
                            fontSizeSp = prefs.fontSizeSp
                        )
                    }
                }
            }
        }
    }

    fun openFileFromUri(context: Context, uri: Uri, autoSwitchToEditor: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    failedUri = null,
                    isPermissionError = false
                )
            }

            FileHelper.tryTakePersistableUriPermission(context, uri)

            val result = FileHelper.readFile(context, uri)
            result.onSuccess { (info, content) ->
                val splitLines = withContext(Dispatchers.Default) { content.lines() }
                _uiState.update {
                    it.copy(
                        fileInfo = info,
                        rawContent = content,
                        originalSavedContent = content,
                        lines = splitLines,
                        viewMode = if (autoSwitchToEditor) ViewMode.EDITOR else ViewMode.READER,
                        isLoading = false,
                        errorMessage = null,
                        failedUri = null,
                        isPermissionError = false,
                        searchQuery = "",
                        isSearchActive = false,
                        searchMatchLines = emptyList(),
                        searchMatchCount = 0
                    )
                }

                recentFilesRepository?.recordFileOpened(
                    uriString = uri.toString(),
                    fileName = info.fileName,
                    fileSizeFormatted = info.fileSizeFormatted,
                    fileType = info.fileType.label
                )

                _events.emit(UiEvent.ShowSnackbar("Opened ${info.fileName}"))
            }.onFailure { err ->
                val errText = err.localizedMessage ?: err.message ?: "Unknown error"
                val isPerm = err is SecurityException ||
                    errText.contains("Permission Denial", ignoreCase = true) ||
                    errText.contains("requires that you obtain access", ignoreCase = true) ||
                    errText.contains("permission", ignoreCase = true)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        failedUri = uri,
                        isPermissionError = isPerm,
                        errorMessage = if (isPerm) {
                            "Permission required to open this file. Android requires files from Downloads/Storage to be selected via the system document picker."
                        } else {
                            "Could not load file: $errText"
                        }
                    )
                }
            }
        }
    }

    fun clearErrorAndGoHome() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                failedUri = null,
                isPermissionError = false,
                viewMode = ViewMode.HOME
            )
        }
    }

    fun switchToEditMode() {
        _uiState.update { it.copy(viewMode = ViewMode.EDITOR) }
    }

    fun switchToReaderMode() {
        _uiState.update { it.copy(viewMode = ViewMode.READER) }
        val content = _uiState.value.rawContent
        viewModelScope.launch {
            val splitLines = withContext(Dispatchers.Default) { content.lines() }
            _uiState.update { it.copy(lines = splitLines) }
        }
    }

    fun goHome() {
        _uiState.update {
            it.copy(
                viewMode = ViewMode.HOME,
                errorMessage = null,
                failedUri = null,
                isPermissionError = false
            )
        }
    }

    fun onContentChanged(newContent: String) {
        _uiState.update { it.copy(rawContent = newContent) }
    }

    fun requestSave() {
        _uiState.update { it.copy(showSaveConfirmationDialog = true) }
    }

    fun dismissSaveConfirmation() {
        _uiState.update { it.copy(showSaveConfirmationDialog = false) }
    }

    fun confirmSave(context: Context) {
        val currentState = _uiState.value
        val uri = currentState.fileInfo?.uri
        _uiState.update { it.copy(showSaveConfirmationDialog = false) }

        if (uri == null) {
            viewModelScope.launch {
                _events.emit(UiEvent.ShowSnackbar("Cannot save sample in-place. Use Save As or open an existing file."))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = FileHelper.saveFile(context, uri, currentState.rawContent)
            result.onSuccess {
                val newLines = withContext(Dispatchers.Default) { currentState.rawContent.lines() }
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        originalSavedContent = currentState.rawContent,
                        lines = newLines,
                        viewMode = ViewMode.READER // Exit editor mode and open the same file in viewer mode!
                    )
                }
                _events.emit(UiEvent.ShowSnackbar("Saved successfully!"))
            }.onFailure { err ->
                _uiState.update { it.copy(isSaving = false) }
                _events.emit(UiEvent.ShowSnackbar("Save failed: ${err.localizedMessage}"))
            }
        }
    }

    fun saveContentToExportUri(context: Context, destinationUri: Uri) {
        val currentState = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = FileHelper.saveFile(context, destinationUri, currentState.rawContent)
            result.onSuccess {
                val newInfo = FileHelper.getFileInfo(context, destinationUri)
                val newLines = withContext(Dispatchers.Default) { currentState.rawContent.lines() }
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        fileInfo = newInfo,
                        originalSavedContent = currentState.rawContent,
                        lines = newLines,
                        viewMode = ViewMode.READER // Exit editor mode and open the saved file in viewer mode!
                    )
                }
                recentFilesRepository?.recordFileOpened(
                    uriString = destinationUri.toString(),
                    fileName = newInfo.fileName,
                    fileSizeFormatted = newInfo.fileSizeFormatted,
                    fileType = newInfo.fileType.label
                )
                _events.emit(UiEvent.ShowSnackbar("Saved to ${newInfo.fileName}!"))
            }.onFailure { err ->
                _uiState.update { it.copy(isSaving = false) }
                _events.emit(UiEvent.ShowSnackbar("Export failed: ${err.localizedMessage}"))
            }
        }
    }

    fun setTabSize(spaces: Int) {
        _uiState.update { it.copy(tabSize = spaces, showTabSizeDialog = false) }
        viewModelScope.launch {
            preferencesRepository?.updateTabSize(spaces)
        }
    }

    fun showTabSizeDialog() {
        _uiState.update { it.copy(showTabSizeDialog = true) }
    }

    fun dismissTabSizeDialog() {
        _uiState.update { it.copy(showTabSizeDialog = false) }
    }

    fun setThemeMode(themeMode: AppThemeMode) {
        _uiState.update { it.copy(themeMode = themeMode, showThemeDialog = false) }
        viewModelScope.launch {
            preferencesRepository?.updateThemeMode(themeMode)
        }
    }

    fun showThemeDialog() {
        _uiState.update { it.copy(showThemeDialog = true) }
    }

    fun dismissThemeDialog() {
        _uiState.update { it.copy(showThemeDialog = false) }
    }

    fun insertTab(): String {
        return " ".repeat(_uiState.value.tabSize)
    }

    fun removeRecentFile(uriString: String) {
        viewModelScope.launch {
            recentFilesRepository?.removeRecentFile(uriString)
        }
    }

    fun clearAllRecentFiles() {
        viewModelScope.launch {
            recentFilesRepository?.clearHistory()
        }
    }

    fun toggleSearch() {
        _uiState.update {
            val nextState = !it.isSearchActive
            if (!nextState) {
                it.copy(isSearchActive = false, searchQuery = "", searchMatchLines = emptyList(), searchMatchCount = 0)
            } else {
                it.copy(isSearchActive = true)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        performSearch(query)
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) {
            _uiState.update {
                it.copy(searchMatchLines = emptyList(), searchMatchCount = 0, currentMatchIndex = 0)
            }
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            val lines = _uiState.value.lines
            val matchingLineIndices = mutableListOf<Int>()
            var totalCount = 0

            lines.forEachIndexed { index, line ->
                var findIndex = line.indexOf(query, 0, ignoreCase = true)
                var foundInLine = false
                while (findIndex >= 0) {
                    totalCount++
                    foundInLine = true
                    findIndex = line.indexOf(query, findIndex + query.length, ignoreCase = true)
                }
                if (foundInLine) {
                    matchingLineIndices.add(index)
                }
            }

            _uiState.update {
                it.copy(
                    searchMatchLines = matchingLineIndices,
                    searchMatchCount = totalCount,
                    currentMatchIndex = if (matchingLineIndices.isNotEmpty()) 0 else -1
                )
            }

            if (matchingLineIndices.isNotEmpty()) {
                _events.emit(UiEvent.ScrollToLine(matchingLineIndices.first()))
            }
        }
    }

    fun nextSearchMatch() {
        val matches = _uiState.value.searchMatchLines
        if (matches.isEmpty()) return
        val nextIdx = (_uiState.value.currentMatchIndex + 1) % matches.size
        _uiState.update { it.copy(currentMatchIndex = nextIdx) }
        viewModelScope.launch {
            _events.emit(UiEvent.ScrollToLine(matches[nextIdx]))
        }
    }

    fun prevSearchMatch() {
        val matches = _uiState.value.searchMatchLines
        if (matches.isEmpty()) return
        val prevIdx = if (_uiState.value.currentMatchIndex <= 0) matches.size - 1 else _uiState.value.currentMatchIndex - 1
        _uiState.update { it.copy(currentMatchIndex = prevIdx) }
        viewModelScope.launch {
            _events.emit(UiEvent.ScrollToLine(matches[prevIdx]))
        }
    }

    fun zoomIn() {
        val newSize = (_uiState.value.fontSizeSp + 1f).coerceAtMost(28f)
        _uiState.update { it.copy(fontSizeSp = newSize) }
        viewModelScope.launch {
            preferencesRepository?.updateFontSize(newSize)
        }
    }

    fun zoomOut() {
        val newSize = (_uiState.value.fontSizeSp - 1f).coerceAtLeast(10f)
        _uiState.update { it.copy(fontSizeSp = newSize) }
        viewModelScope.launch {
            preferencesRepository?.updateFontSize(newSize)
        }
    }

    fun toggleWrap() {
        val newWrap = !_uiState.value.wrapLines
        _uiState.update { it.copy(wrapLines = newWrap) }
        viewModelScope.launch {
            preferencesRepository?.updateWrapLines(newWrap)
        }
    }

    fun handleBackPress(onExit: () -> Unit) {
        val state = _uiState.value
        if (state.errorMessage != null) {
            clearErrorAndGoHome()
            return
        }
        if (state.isSearchActive) {
            toggleSearch()
            return
        }
        if (state.viewMode == ViewMode.EDITOR) {
            if (state.isDirty) {
                _uiState.update { it.copy(showUnsavedExitDialog = true) }
            } else {
                switchToReaderMode()
            }
            return
        }
        if (state.viewMode == ViewMode.READER) {
            goHome()
            return
        }
        onExit()
    }

    fun dismissUnsavedExitDialog() {
        _uiState.update { it.copy(showUnsavedExitDialog = false) }
    }

    fun discardChangesAndSwitchToReader() {
        val original = _uiState.value.originalSavedContent
        _uiState.update {
            it.copy(
                rawContent = original,
                lines = original.lines(),
                viewMode = ViewMode.READER,
                showUnsavedExitDialog = false
            )
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.getDatabase(context)
            val recentRepo = RecentFilesRepository(db.recentFilesDao())
            val prefsRepo = PreferencesRepository(context.userPreferencesDataStore)
            return ViewerViewModel(recentRepo, prefsRepo) as T
        }
    }
}
