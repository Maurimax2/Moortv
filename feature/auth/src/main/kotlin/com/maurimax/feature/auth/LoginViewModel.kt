package com.maurimax.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maurimax.core.data.AuthRepository
import com.maurimax.core.data.Graph
import com.maurimax.core.data.LoginFailure
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
    val error: String? = null,
    val account: Account? = null,
    val credentials: Credentials? = null,
) {
    val canSubmit: Boolean
        get() = username.isNotBlank() && password.isNotBlank() && !signingIn

    val signedIn: Boolean get() = account != null && credentials != null
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
        // Returning customers should not have to type anything.
        authRepository.savedCredentials()?.let { saved ->
            _uiState.update { it.copy(username = saved.username, password = saved.password) }
            signIn()
        }
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
                    )
                }

                is LoginResult.Failure -> _uiState.update {
                    it.copy(signingIn = false, error = result.reason.message())
                }
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _uiState.value = LoginUiState()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { LoginViewModel(Graph.authRepository) }
        }
    }
}

/**
 * Customer-facing wording. A customer cannot fix a server outage, so the
 * unreachable case says what to do rather than showing them a stack trace.
 */
internal fun LoginFailure.message(): String = when (this) {
    LoginFailure.BadCredentials ->
        "That username and password did not match. Check them and try again."

    is LoginFailure.Inactive ->
        "This account is $status. Contact support to renew it."

    is LoginFailure.Unreachable ->
        "Could not reach MAURIMAX. Check your internet connection and try again."
}
