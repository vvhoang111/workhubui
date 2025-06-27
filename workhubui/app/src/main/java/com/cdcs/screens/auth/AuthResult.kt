package com.cdcs.screens.auth

sealed class AuthResult {
    data object Idle : AuthResult()
    data object Loading : AuthResult()
    data class Success(val message: String? = null) : AuthResult()
    data class Error(val errorMessage: String) : AuthResult()
}
