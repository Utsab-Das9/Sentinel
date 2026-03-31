package com.privacynudge.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.privacynudge.data.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for authentication screens.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application)

    // Login State
    private val _loginIdentity = MutableStateFlow("")
    val loginIdentity: StateFlow<String> = _loginIdentity.asStateFlow()

    private val _loginPassword = MutableStateFlow("")
    val loginPassword: StateFlow<String> = _loginPassword.asStateFlow()

    // Signup State
    private val _signupName = MutableStateFlow("")
    val signupName: StateFlow<String> = _signupName.asStateFlow()

    private val _signupDob = MutableStateFlow("")
    val signupDob: StateFlow<String> = _signupDob.asStateFlow()

    private val _signupPhone = MutableStateFlow("")
    val signupPhone: StateFlow<String> = _signupPhone.asStateFlow()

    private val _signupGmail = MutableStateFlow("")
    val signupGmail: StateFlow<String> = _signupGmail.asStateFlow()

    private val _signupPassword = MutableStateFlow("")
    val signupPassword: StateFlow<String> = _signupPassword.asStateFlow()

    // UI state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun onLoginIdentityChange(value: String) { _loginIdentity.value = value }
    fun onLoginPasswordChange(value: String) { _loginPassword.value = value }

    fun onSignupNameChange(value: String) { _signupName.value = value }
    fun onSignupDobChange(value: String) { _signupDob.value = value }
    fun onSignupPhoneChange(value: String) { _signupPhone.value = value }
    fun onSignupGmailChange(value: String) { _signupGmail.value = value }
    fun onSignupPasswordChange(value: String) { _signupPassword.value = value }

    fun clearError() { _errorMessage.value = null }

    /**
     * Perform login.
     */
    fun login(onSuccess: () -> Unit) {
        if (_loginIdentity.value.isBlank() || _loginPassword.value.isBlank()) {
            _errorMessage.value = "Please fill in all fields"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val success = authRepository.login(_loginIdentity.value, _loginPassword.value)
            _isLoading.value = false
            if (success) {
                onSuccess()
            } else {
                _errorMessage.value = "Invalid credentials"
            }
        }
    }

    /**
     * User profile details for the Profile screen.
     */
    val userProfile: Flow<com.privacynudge.data.UserProfile> = authRepository.userProfile

    /**
     * Perform registration.
     */
    fun signup(onSuccess: () -> Unit) {
        val name = _signupName.value
        val dob = _signupDob.value
        val phone = _signupPhone.value
        val gmail = _signupGmail.value
        val password = _signupPassword.value

        if (name.isBlank() || dob.isBlank() || phone.isBlank() || gmail.isBlank() || password.isBlank()) {
            _errorMessage.value = "Please fill in all fields"
            return
        }

        if (!android.util.Patterns.PHONE.matcher(phone).matches()) {
            _errorMessage.value = "Invalid phone number"
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(gmail).matches()) {
            _errorMessage.value = "Invalid Gmail address"
            return
        }

        if (password.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val success = authRepository.registerUser(name, dob, phone, gmail, password)
            _isLoading.value = false
            if (success) {
                onSuccess()
            } else {
                _errorMessage.value = "Phone or Gmail already registered"
            }
        }
    }
}
