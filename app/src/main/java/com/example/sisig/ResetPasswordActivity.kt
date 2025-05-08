package com.example.sisig

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var editTextEmail: EditText
    private lateinit var buttonReset: Button
    private lateinit var textViewBackToLogin: TextView
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)
        supportActionBar?.hide()

        editTextEmail = findViewById(R.id.editTextEmail)
        buttonReset = findViewById(R.id.buttonResetPassword)
        textViewBackToLogin = findViewById(R.id.textViewBackToLogin)

        firebaseAuth = FirebaseAuth.getInstance()

        buttonReset.setOnClickListener {
            resetPassword()
        }

        textViewBackToLogin.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun resetPassword() {
        val email = editTextEmail.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Send password reset email
                firebaseAuth.sendPasswordResetEmail(email).await()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ResetPasswordActivity, "Password reset email sent. Check your inbox.", Toast.LENGTH_SHORT).show()
                    // Navigate back to MainActivity
                    val intent = Intent(this@ResetPasswordActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMessage = when {
                        e.message?.contains("no user record", true) == true -> "Email not registered"
                        e.message?.contains("network", true) == true -> "Network error, please try again"
                        else -> "Error: ${e.message}"
                    }
                    Toast.makeText(this@ResetPasswordActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}