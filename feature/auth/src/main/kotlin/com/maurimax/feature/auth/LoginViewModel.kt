package com.maurimax.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maurimax.core.data.AuthRepository
import com.maurimax.core.data.Graph
import com.maurimax.core.data.PortalFailure
import com.maurimax.core.data.LoginResult
import com.maurimax.core.model.Account
import com.maurimax.core.model.Credentials
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val signingIn: Boolean = false,
    val error: PortalFailure? = null,
    val account: Account? = null,
    val credentials: Credentials? = null,
    /** Every account saved on this device, most recently used first. */
    val accounts: List<Credentials> = emptyList(),
    /** True once the customer has asked for the form rather than the list. */
    val addingAccount: Boolean = false,
) {
    val canSubmit: Boolean
        get() = username.isNotBlank() && password.isNotBlank() && !signingIn

    val signedIn: Boolean get() = account != null && credentials != null

    /**
     * Which of the two sign-in screens to show. A customer who has used this
     * box before should be picking a line, not typing one — the form is one
     * step away behind "add an account".
     */
    val showingPicker: Boolean get() = accounts.isNotEmpty() && !addingAccount
}

/**
 * Sign-in for the fixed portal. There is no host field: the server is compiled
 * into the build, so a customer enters a username and password and nothing else.
 */
class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(accounts = authRepository.savedAccounts()) }

        // Returning customers should not have to type anything.
        authRepository.savedCredentials()?.let { saved ->
            _uiState.update { it.copy(username = saved.username, password = saved.password) }
            signIn()
        }
    }

    /** Signs in as an account already on the device, without a password prompt. */
    fun useAccount(credentials: Credentials) {
        _uiState.update {
            it.copy(
                username = credentials.username,
                password = credentials.password,
                error = null,
                addingAccount = false,
            )
        }
        signIn()
    }

    /** Opens the empty form so another line can be added alongside the others. */
    fun addAccount() =
        _uiState.update { it.copy(username = "", password = "", error = null, addingAccount = true) }

    /** Back from the empty form to the list, when there is a list to go back to. */
    fun cancelAdd() =
        _uiState.update { it.copy(username = "", password = "", error = null, addingAccount = false) }

    /**
     * Removes an account and everything it saved. Not a sign-out: this is the
     * customer saying the line is finished with, so its continue-watching and
     * favourites go too rather than surfacing under whoever types that username
     * next.
     */
    fun forgetAccount(credentials: Credentials) {
        Graph.forgetAccount(credentials.username)
        val left = authRepository.savedAccounts()
        _uiState.update { it.copy(accounts = left, addingAccount = left.isEmpty()) }
    }

    fun onUsernameChange(value: String) =
        _uiState.update { it.copy(username = value.trim(), error = null) }

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(password = value, error = null) }

    fun signIn() {
        val state = _uiState.value
        if (state.username.isBlank() || state.password.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(signingIn = true, error = null) }

            when (val result = authRepository.signIn(state.username, state.password)) {
                is LoginResult.Success -> _uiState.update {
                    it.copy(
                        signingIn = false,
                        account = result.account,
                        credentials = Credentials(state.username, state.password),
                        accounts = authRepository.savedAccounts(),
                        addingAccount = false,
                    )
                }

                is LoginResult.Failure -> _uiState.update {
                    it.copy(signingIn = false, error = result.reason)
                }
            }
        }
    }

    /**
     * Back to the account list. The password stays on the device, because this
     * is a switch between the household's lines far more often than it is
     * someone leaving.
     */
    fun signOut() {
        authRepository.signOut()
        _uiState.value = LoginUiState(accounts = authRepository.savedAccounts())
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { LoginViewModel(Graph.authRepository) }
        }
    }
}
