package br.com.tec.tecmotors.presentation.app

sealed interface AppUiEvent {
    data object ToggleTheme : AppUiEvent
    data object DismissIntro : AppUiEvent
    data class SelectTab(val index: Int) : AppUiEvent
    data class SetAccountDialogVisible(val visible: Boolean) : AppUiEvent
}

data class AppUiState(
    val darkThemeEnabled: Boolean = true,
    val showIntro: Boolean = true,
    val selectedTabIndex: Int = 0,
    val showAccountDialog: Boolean = false
)
