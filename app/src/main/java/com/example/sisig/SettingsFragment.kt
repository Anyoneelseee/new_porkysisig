package com.example.sisig

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.sisig.data.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import android.util.Log

class SettingsFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var roomDb: AppDatabase
    private lateinit var avatarImage: ImageView
    private lateinit var emailInput: EditText
    private lateinit var usernameInput: EditText
    private lateinit var changeAvatar: TextView
    private lateinit var deleteDataButton: Button
    private lateinit var manageUsersButton: Button
    private val PICK_IMAGE_REQUEST = 1000

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // Initialize Firebase and Room
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        roomDb = AppDatabase.getInstance(requireContext())

        // Initialize UI elements
        avatarImage = view.findViewById(R.id.avatarImage)
        emailInput = view.findViewById(R.id.emailInput)
        usernameInput = view.findViewById(R.id.usernameInput)
        changeAvatar = view.findViewById(R.id.changeAvatar)
        deleteDataButton = view.findViewById(R.id.deleteDataButton)
        manageUsersButton = view.findViewById(R.id.manageUsersButton)

        // Check user role and restrict access
        val userRole = requireContext().getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
            .getString("userRole", "")
        if (userRole != "Owner") {
            manageUsersButton.visibility = View.GONE
            deleteDataButton.visibility = View.GONE
            Toast.makeText(context, "Restricted to Owner", Toast.LENGTH_SHORT).show()
            return view
        }

        // Load user details
        loadUserDetails()

        // Handle button clicks
        changeAvatar.setOnClickListener { openImagePicker() }
        deleteDataButton.setOnClickListener { confirmAndDeleteData() }
        manageUsersButton.setOnClickListener { showManageUsersDialog() }

        return view
    }

    private fun loadUserDetails() {
        val user = auth.currentUser ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val doc = db.collection("users").document(user.uid).get().await()
                emailInput.setText(doc.getString("email"))
                usernameInput.setText(doc.getString("username"))
                Log.d("SettingsFragment", "User role: ${doc.getString("role")}")
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == PICK_IMAGE_REQUEST) {
            val selectedImageUri = data?.data
            if (selectedImageUri != null) {
                saveImageToStorage(selectedImageUri)
            } else {
                Toast.makeText(context, "Failed to select image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveImageToStorage(imageUri: Uri) {
        val user = auth.currentUser ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val ref = storage.getReference("avatars/${user.uid}.jpg")
                ref.putFile(imageUri).await()
                val url = ref.downloadUrl.await().toString()
                db.collection("users").document(user.uid).update("avatarUrl", url).await()
                Toast.makeText(context, "Avatar updated", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save avatar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmAndDeleteData() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete All Data")
            .setMessage("Are you sure you want to delete all app data? This action cannot be undone.")
            .setPositiveButton("Yes") { _, _ -> deleteAllData() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAllData() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            var deletionSuccessful = true
            try {
                // Delete Room database tables
                roomDb.allOrderDao.deleteAll()
                roomDb.notificationDao.clearAll()
                roomDb.productStockDao.deleteAll()
                roomDb.stockDao.deleteAll()
                roomDb.productDao.deleteAll()
                roomDb.orderItemDao.deleteAll()
                roomDb.orderDao.deleteAll()
                Log.d("SettingsFragment", "Room database tables cleared")

                // Delete Firestore users collection
                try {
                    deleteCollection(db.collection("users"))
                    Log.d("SettingsFragment", "Firestore users collection deleted")
                } catch (e: Exception) {
                    deletionSuccessful = false
                    Log.e("SettingsFragment", "Failed to delete Firestore users: ${e.message}")
                }

                // Delete avatar from storage
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    try {
                        storage.getReference("avatars/$uid.jpg").delete().await()
                        Log.d("SettingsFragment", "Avatar deleted for UID: $uid")
                    } catch (e: Exception) {
                        Log.d("SettingsFragment", "No avatar to delete: ${e.message}")
                    }
                }

                // Delete current Firebase Auth user
                try {
                    auth.currentUser?.delete()?.await()
                    Log.d("SettingsFragment", "Firebase Auth user deleted")
                } catch (e: Exception) {
                    deletionSuccessful = false
                    Log.e("SettingsFragment", "Failed to delete Firebase Auth user: ${e.message}")
                }

            } catch (e: Exception) {
                deletionSuccessful = false
                Log.e("SettingsFragment", "Unexpected deletion error: ${e.message}", e)
            }

            // Always attempt redirection
            withContext(Dispatchers.Main) {
                try {
                    // Clear SharedPreferences
                    requireContext().getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE).edit().clear().apply()
                    requireContext().getSharedPreferences("ReportPrefs", Context.MODE_PRIVATE).edit().clear().apply()
                    requireContext().getSharedPreferences("InventoryPrefs", Context.MODE_PRIVATE).edit().clear().apply()

                    // Clear UI
                    emailInput.text.clear()
                    usernameInput.text.clear()
                    avatarImage.setImageResource(R.drawable.circle_background)

                    // Show success Toast
                    Toast.makeText(
                        context,
                        if (deletionSuccessful) "All data deleted successfully" else "Some data could not be deleted",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Sign out and redirect to MainActivity
                    auth.signOut()
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    Log.d("SettingsFragment", "Attempting to start MainActivity")
                    try {
                        requireActivity().startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("SettingsFragment", "Primary startActivity failed: ${e.message}")
                        context?.startActivity(intent)
                    }
                    delay(100)
                    requireActivity().finish()
                    Log.d("SettingsFragment", "Current activity finished")
                } catch (e: Exception) {
                    Log.e("SettingsFragment", "Redirection error: ${e.message}", e)
                    Toast.makeText(context, "Error redirecting to login: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun deleteCollection(collection: com.google.firebase.firestore.CollectionReference) {
        val batchSize = 100
        val querySnapshot = collection.limit(batchSize.toLong()).get().await()
        if (querySnapshot.isEmpty) {
            Log.d("SettingsFragment", "Collection ${collection.path} is empty")
            return
        }

        val batch = collection.firestore.batch()
        for (document in querySnapshot.documents) {
            batch.delete(document.reference)
            Log.d("SettingsFragment", "Deleting document: ${document.reference.path}")
        }
        batch.commit().await()
        Log.d("SettingsFragment", "Batch committed for ${collection.path}")

        deleteCollection(collection)
    }

    private fun showManageUsersDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_manage_users, null)
        val emailInput = dialogView.findViewById<EditText>(R.id.staffEmailInput)
        val usernameInput = dialogView.findViewById<EditText>(R.id.staffUsernameInput)
        val passwordInput = dialogView.findViewById<EditText>(R.id.staffPasswordInput)
        val addButton = dialogView.findViewById<Button>(R.id.addStaffButton)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Add Staff Account")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        addButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(context, "Invalid email format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (username.isEmpty() || password.length < 6) {
                Toast.makeText(context, "Please fill in all fields (password min 6 chars)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val userCredential = auth.createUserWithEmailAndPassword(email, password).await()
                    val uid = userCredential.user?.uid ?: return@launch
                    db.collection("users").document(uid).set(
                        mapOf(
                            "email" to email,
                            "username" to username,
                            "role" to "Staff",
                            "avatarUrl" to null
                        )
                    ).await()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Staff account created", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                } catch (e: FirebaseAuthUserCollisionException) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Email already in use", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error creating account: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        dialog.show()
    }

    companion object {
        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }
}