package dev.favourdevlabs.cleanthes.ui.base

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import dev.favourdevlabs.cleanthes.ui.auth.LoginActivity
import dev.favourdevlabs.cleanthes.ui.auth.SessionManager
import javax.inject.Inject

@AndroidEntryPoint
abstract class AuthenticatedActivity : AppCompatActivity() {

    @Inject lateinit var sessionManager: SessionManager

    override fun onStart() {
        super.onStart()
        if (!sessionManager.isUnlocked()) {
            redirectToLogin()
        }
    }

    override fun onResume() {
        super.onResume()
        if (sessionManager.isUnlocked()) {
            sessionManager.refreshSession()
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
