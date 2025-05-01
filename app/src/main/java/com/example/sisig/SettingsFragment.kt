package com.example.sisig

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.sisig.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SettingsFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var avatarImage: ImageView
    private lateinit var emailInput: EditText
    private lateinit var usernameInput: EditText
    private lateinit var preferredNameInput: EditText
    private lateinit var saveChangesButton: Button
    private lateinit var changeAvatar: TextView
    private lateinit var db: AppDatabase

    private val PICK_IMAGE_REQUEST = 1000
    private val avatarFileName = "avatar_image.jpg"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)

        // Initialize Database
        db = AppDatabase.getInstance(requireContext())

        // Initialize UI elements
        avatarImage = view.findViewById(R.id.avatarImage)
        emailInput = view.findViewById(R.id.emailInput)
        usernameInput = view.findViewById(R.id.usernameInput)
        preferredNameInput = view.findViewById(R.id.preferredNameInput)
        saveChangesButton = view.findViewById(R.id.saveChangesButton)
        changeAvatar = view.findViewById(R.id.changeAvatar)

        // Initialize Delete Data Button
        val deleteDataButton = view.findViewById<Button>(R.id.deleteDataButton)
        deleteDataButton.setOnClickListener {
            confirmAndDeleteData()
        }

        // Load saved data into input fields and avatar
        loadUserDetails()

        // Handle Change Avatar click
        changeAvatar.setOnClickListener {
            openImagePicker()
        }

        // Save Changes button click listener
        saveChangesButton.setOnClickListener {
            saveUserDetails()
        }

        return view
    }

    private fun saveUserDetails() {
        val email = emailInput.text.toString().trim()
        val username = usernameInput.text.toString().trim()
        val preferredName = preferredNameInput.text.toString().trim()

        // Validate input
        if (email.isNotEmpty() && username.isNotEmpty() && preferredName.isNotEmpty()) {
            with(sharedPreferences.edit()) {
                putString("email", email)
                putString("username", username)
                putString("preferredName", preferredName)
                apply()
            }
            Toast.makeText(requireContext(), "Changes saved", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserDetails() {
        val email = sharedPreferences.getString("email", "")
        val username = sharedPreferences.getString("username", "")
        val preferredName = sharedPreferences.getString("preferredName", "")

        emailInput.setText(email)
        usernameInput.setText(username)
        preferredNameInput.setText(preferredName)

        // Load avatar from internal storage
        loadAvatarImage()
    }

    private fun loadAvatarImage() {
        val avatarFile = File(requireContext().filesDir, avatarFileName)
        if (avatarFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(avatarFile.absolutePath)
            avatarImage.setImageBitmap(bitmap)
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
                saveImageToInternalStorage(selectedImageUri)
            } else {
                Toast.makeText(requireContext(), "Failed to select image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveImageToInternalStorage(imageUri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val avatarFile = File(requireContext().filesDir, avatarFileName)
            val outputStream = FileOutputStream(avatarFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.close()

            avatarImage.setImageBitmap(bitmap)
            Toast.makeText(requireContext(), "Avatar updated", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Log.e("SettingsFragment", "Failed to save avatar: ${e.message}", e)
            Toast.makeText(requireContext(), "Failed to save avatar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmAndDeleteData() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete All Data")
            .setMessage("Are you sure you want to delete all saved user data and app data? This action cannot be undone.")
            .setPositiveButton("Yes") { _, _ -> deleteAllData() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAllData() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Clear SharedPreferences
                requireContext().getSharedPreferences("UserPreferences", Context.MODE_PRIVATE).edit().clear().apply()
                requireContext().getSharedPreferences("ReportPrefs", Context.MODE_PRIVATE).edit().clear().apply()
                requireContext().getSharedPreferences("InventoryPrefs", Context.MODE_PRIVATE).edit().clear().apply()

                // Delete avatar file from internal storage
                val avatarFile = File(requireContext().filesDir, avatarFileName)
                if (avatarFile.exists()) {
                    avatarFile.delete()
                }

                // Clear Room database
                db.orderDao.deleteAll()
                db.orderItemDao.deleteAll()
                db.productDao.deleteAll()
                db.stockDao.deleteAll()
                db.allOrderDao.deleteAll()
                db.productStockDao.deleteAll()
                db.notificationDao.clearAll()

                withContext(Dispatchers.Main) {
                    // Clear UI fields
                    emailInput.text.clear()
                    usernameInput.text.clear()
                    preferredNameInput.text.clear()
                    avatarImage.setImageResource(R.drawable.circle_background)
                    Toast.makeText(requireContext(), "All data deleted successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("SettingsFragment", "Error deleting data: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error deleting data: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    companion object {
        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }
}