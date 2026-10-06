package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppThemeMode
import com.example.model.ViewMode
import com.example.util.SyntaxHighlighter
import com.example.viewmodel.ViewerViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches MLViewer`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MLViewer", appName)
    }

    @Test
    fun `test initial state defaults to HOME screen with tabSize 2 and SYSTEM theme`() {
        val viewModel = ViewerViewModel()
        val state = viewModel.uiState.value

        assertEquals(ViewMode.HOME, state.viewMode)
        assertEquals(2, state.tabSize)
        assertEquals(AppThemeMode.SYSTEM, state.themeMode)
        assertFalse(state.isDirty)
    }

    @Test
    fun `test switching to editor mode and back`() {
        val viewModel = ViewerViewModel()
        viewModel.switchToEditMode()
        assertEquals(ViewMode.EDITOR, viewModel.uiState.value.viewMode)

        viewModel.switchToReaderMode()
        assertEquals(ViewMode.READER, viewModel.uiState.value.viewMode)

        viewModel.goHome()
        assertEquals(ViewMode.HOME, viewModel.uiState.value.viewMode)
    }

    @Test
    fun `test tab size configuration`() {
        val viewModel = ViewerViewModel()
        assertEquals(2, viewModel.uiState.value.tabSize)
        assertEquals("  ", viewModel.insertTab())

        viewModel.setTabSize(4)
        assertEquals(4, viewModel.uiState.value.tabSize)
        assertEquals("    ", viewModel.insertTab())
    }

    @Test
    fun `test theme mode configuration`() {
        val viewModel = ViewerViewModel()
        assertEquals(AppThemeMode.SYSTEM, viewModel.uiState.value.themeMode)

        viewModel.setThemeMode(AppThemeMode.DARK)
        assertEquals(AppThemeMode.DARK, viewModel.uiState.value.themeMode)

        viewModel.setThemeMode(AppThemeMode.LIGHT)
        assertEquals(AppThemeMode.LIGHT, viewModel.uiState.value.themeMode)
    }

    @Test
    fun `test editing marks state as dirty and save confirmation is requested`() {
        val viewModel = ViewerViewModel()
        viewModel.onContentChanged("<modified/>")
        assertTrue(viewModel.uiState.value.isDirty)

        viewModel.requestSave()
        assertTrue(viewModel.uiState.value.showSaveConfirmationDialog)

        viewModel.dismissSaveConfirmation()
        assertFalse(viewModel.uiState.value.showSaveConfirmationDialog)
    }

    @Test
    fun `test syntax highlighting produces non-empty annotated string`() {
        val tagText = "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">"
        val highlighted = SyntaxHighlighter.highlightLine(tagText, isDark = true, tabSize = 2)
        assertEquals(tagText, highlighted.text)
        assertTrue(highlighted.spanStyles.isNotEmpty())
    }
}
