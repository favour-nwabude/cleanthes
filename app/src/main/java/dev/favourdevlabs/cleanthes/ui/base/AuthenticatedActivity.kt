package dev.favourdevlabs.cleanthes.ui.base

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import dev.favourdevlabs.cleanthes.ui.auth.LoginActivity
import dev.favourdevlabs.cleanthes.ui.auth.SessionManager

abstract class AuthenticatedActivity : AppCompatActivity() {

    override fun onStart() {
        super.onStart()
        if (!SessionManager.isUnlocked()) {
            redirectToLogin()
        }
    }

    override fun onResume() {
        super.onResume()
        // Only refresh — never make redirect decisions here
        if (SessionManager.isUnlocked()) {
            SessionManager.refreshSession()
        }
    }

    protected fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }
}

