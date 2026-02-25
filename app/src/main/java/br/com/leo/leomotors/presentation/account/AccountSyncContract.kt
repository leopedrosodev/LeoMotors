package br.com.leo.leomotors.presentation.account

sealed interface AccountSyncUiEvent {
    data class SubmitGoogleIdToken(val idToken: String) : AccountSyncUiEvent
    data object SignOut : AccountSyncUiEvent
    data object Upload : AccountSyncUiEvent
    data object Download : AccountSyncUiEvent
    data object SyncNow : AccountSyncUiEvent
    data class SetStatusMessage(val message: String?) : AccountSyncUiEvent
}

data class AccountSyncUiState(
    val userEmail: String? = null,
    val busy: Boolean = false,
    val statusMessage: String? = null
)
