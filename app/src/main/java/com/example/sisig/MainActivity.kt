package com.example.sisig

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var resetPasswordText: TextView
    private lateinit var createOwnerButton: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize UI
        emailInput = findViewById(R.id.editTextUsername)
        passwordInput = findViewById(R.id.editTextPassword)
        loginButton = findViewById(R.id.buttonLogin)
        resetPasswordText = findViewById(R.id.textViewResetPassword)
        createOwnerButton = findViewById(R.id.createOwnerButton)

        // Clear SharedPreferences to ensure fresh state
        getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE).edit().clear().apply()

        // Check if user is signed in or if setup is needed
        CoroutineScope(Dispatchers.Main).launch {
            if (auth.currentUser == null) {
                // Check if any users exist
                try {
                    val snapshot = db.collection("users").limit(1).get().await()
                    if (snapshot.isEmpty) {
                        startActivity(Intent(this@MainActivity, SetupActivity::class.java))
                        finish()
                        return@launch
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Error checking users: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                // User is signed in, fetch their role and proceed
                try {
                    val uid = auth.currentUser!!.uid
                    val doc = db.collection("users").document(uid).get().await()
                    if (doc.exists()) {
                        val role = doc.getString("role") ?: "Staff"
                        getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE).edit()
                            .putString("userId", uid)
                            .putString("userRole", role)
                            .apply()
                        startActivity(Intent(this@MainActivity, DashboardActivity::class.java))
                        finish()
                    } else {
                        auth.signOut()
                        Toast.makeText(this@MainActivity, "User data not found, please create a new account", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Error fetching user data: ${e.message}", Toast.LENGTH_SHORT).show()
                    auth.signOut()
                }
            }
        }

        // Login button
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val userCredential = auth.signInWithEmailAndPassword(email, password).await()
                    val uid = userCredential.user?.uid ?: return@launch
                    val doc = db.collection("users").document(uid).get().await()
                    if (doc.exists()) {
                        val role = doc.getString("role") ?: "Staff"
                        getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE).edit()
                            .putString("userId", uid)
                            .putString("userRole", role)
                            .apply()
                        Toast.makeText(this@MainActivity, "Login successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@MainActivity, DashboardActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@MainActivity, "User data not found, please create a new account", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Reset password
        resetPasswordText.setOnClickListener {
            startActivity(Intent(this@MainActivity, ResetPasswordActivity::class.java))
        }

        // Create account button
        createOwnerButton.setOnClickListener {
            startActivity(Intent(this@MainActivity, SetupActivity::class.java))
        }
    }
}