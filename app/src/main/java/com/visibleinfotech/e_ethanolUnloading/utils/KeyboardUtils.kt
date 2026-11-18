package com.visibleinfotech.e_ethanolUnloading.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

object KeyboardUtils {

    @Composable
    fun hideKeyboard(): () -> Unit {

        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current

        return {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
        }
    }
}