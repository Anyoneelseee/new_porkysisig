package com.example.sisig

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SetupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var emailInput: EditText
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var createButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize UI
        emailInput = findViewById(R.id.ownerEmailInput)
        usernameInput = findViewById(R.id.ownerUsernameInput)
        passwordInput = findViewById(R.id.ownerPasswordInput)
        createButton = findViewById(R.id.createOwnerButton)

        createButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (username.isEmpty() || password.length < 6) {
                Toast.makeText(this, "Please fill in all fields (password min 6 chars)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val userCredential = auth.createUserWithEmailAndPassword(email, password).await()
                    val uid = userCredential.user?.uid ?: return@launch
                    db.collection("users").document(uid).set(
                        mapOf(
                            "email" to email,
                            "username" to username,
                            "role" to "Owner",
                            "avatarUrl" to null
                        )
                    ).await()
                    getSharedPreferences("SessionPrefs", MODE_PRIVATE).edit()
                        .putString("userId", uid)
                        .putString("userRole", "Owner")
                        .apply()
                    Toast.makeText(this@SetupActivity, "Owner account created", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@SetupActivity, DashboardActivity::class.java))
                    finish()
                } catch (e: FirebaseAuthUserCollisionException) {
                    Toast.makeText(this@SetupActivity, "Email already in use", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@SetupActivity, "Error creating account: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}