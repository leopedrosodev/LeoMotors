package br.com.tec.tecmotors.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.tec.tecmotors.data.local.migration.LegacyImportManager
import br.com.tec.tecmotors.domain.usecase.EnsureDefaultVehiclesUseCase
import br.com.tec.tecmotors.domain.usecase.ObserveDarkThemeUseCase
import br.com.tec.tecmotors.domain.usecase.SetDarkThemeUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(
    private val observeDarkThemeUseCase: ObserveDarkThemeUseCase,
    private val setDarkThemeUseCase: SetDarkThemeUseCase,
    private val ensureDefaultVehiclesUseCase: EnsureDefaultVehiclesUseCase,
    private val legacyImportManager: LegacyImportManager
) : ViewModel() {
    private val localState = MutableStateFlow(AppUiState())

    val uiState: StateFlow<AppUiState> = combine(
        observeDarkThemeUseCase(),
        localState
    ) { darkTheme, state ->
        state.copy(darkThemeEnabled = darkTheme)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppUiState()
    )

    init {
        viewModelScope.launch {
            legacyImportManager.importIfNeeded()
            ensureDefaultVehiclesUseCase()
        }

        viewModelScope.launch {
            delay(2200)
            localState.update { it.copy(showIntro = false) }
        }
    }

    fun onEvent(event: AppUiEvent) {
        when (event) {
            is AppUiEvent.ToggleTheme -> {
                viewModelScope.launch {
                    val current = uiState.value.darkThemeEnabled
                    setDarkThemeUseCase(!current)
                }
            }

            is AppUiEvent.DismissIntro -> {
                localState.update { it.copy(showIntro = false) }
            }

            is AppUiEvent.SelectTab -> {
                localState.update { it.copy(selectedTabIndex = event.index) }
            }

            is AppUiEvent.SetAccountDialogVisible -> {
                localState.update { it.copy(showAccountDialog = event.visible) }
            }
        }
    }
}
