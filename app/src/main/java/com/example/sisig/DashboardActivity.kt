package com.example.sisig

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.sisig.HomeFragment
import com.example.sisig.InventoryFragment
import com.example.sisig.MainActivity
import com.example.sisig.NotificationsFragment
import com.example.sisig.R
import com.example.sisig.ReportFragment
import com.example.sisig.SettingsFragment
import com.example.sisig.ViewPagerAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DashboardActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var header: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        supportActionBar?.hide()

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        header = findViewById(R.id.viewpager_header)
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        viewPager = findViewById(R.id.view_pager)

        // Set up the menu icon click listener
        val menuIcon: ImageView = findViewById(R.id.menu_icon)
        menuIcon.setOnClickListener { showPopupMenu(menuIcon) }

        // Check user role and setup UI
        CoroutineScope(Dispatchers.Main).launch {
            setupRoleBasedUI()
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateHeaderText(position)
                bottomNavigationView.menu.getItem(position).isChecked = true
            }
        })

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_home -> {
                    viewPager.currentItem = 0
                    true
                }
                R.id.action_inventory -> {
                    viewPager.currentItem = 1
                    true
                }
                R.id.action_reports -> {
                    viewPager.currentItem = 2
                    true
                }
                R.id.action_notifications -> {
                    viewPager.currentItem = 3
                    true
                }
                R.id.action_settings -> {
                    viewPager.currentItem = 4
                    true
                }
                else -> false
            }
        }
    }

    private suspend fun setupRoleBasedUI() {
        val user = auth.currentUser ?: run {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        try {
            val doc = db.collection("users").document(user.uid).get().await()
            if (doc.exists()) {
                val role = doc.getString("role") ?: ""
                getSharedPreferences("SessionPrefs", MODE_PRIVATE).edit()
                    .putString("userRole", role)
                    .putString("userId", user.uid)
                    .apply()

                val adapter = ViewPagerAdapter(this)
                if (role != "Owner") {
                    // Restrict to Home and Inventory for Staff
                    bottomNavigationView.menu.findItem(R.id.action_reports).isVisible = false
                    bottomNavigationView.menu.findItem(R.id.action_notifications).isVisible = false
                    bottomNavigationView.menu.findItem(R.id.action_settings).isVisible = false
                    adapter.addFragment(HomeFragment.newInstance(), "Home")
                    adapter.addFragment(InventoryFragment.newInstance(), "Inventory")
                } else {
                    // Full access for Owner
                    adapter.addFragment(HomeFragment.newInstance(), "Home")
                    adapter.addFragment(InventoryFragment.newInstance(), "Inventory")
                    adapter.addFragment(ReportFragment.newInstance(), "Report")
                    adapter.addFragment(NotificationsFragment.newInstance(), "Notifications")
                    adapter.addFragment(SettingsFragment.newInstance(), "Settings")
                }
                viewPager.adapter = adapter
            } else {
                Toast.makeText(this, "User data not found, logging out", Toast.LENGTH_SHORT).show()
                logout()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading user role: ${e.message}", Toast.LENGTH_SHORT).show()
            logout()
        }
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.inflate(R.menu.menu_menu)
        popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.about -> {
                    true
                }
                R.id.logout -> {
                    confirmLogout()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun updateHeaderText(position: Int) {
        val headerText = when (position) {
            0 -> "Home"
            1 -> "Inventory"
            2 -> "Report"
            3 -> "Notifications"
            4 -> "Settings"
            else -> "Home"
        }
        header.text = headerText
    }

    private fun confirmLogout() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirm Logout")
        builder.setMessage("Are you sure you want to logout?")
        builder.setPositiveButton("Yes") { dialog, _ ->
            logout()
            dialog.dismiss()
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun logout() {
        auth.signOut()
        getSharedPreferences("SessionPrefs", MODE_PRIVATE).edit().clear().apply()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupBottomNavigationView(bottomNavigationView: BottomNavigationView) {
        for (i in 0 until bottomNavigationView.menu.size()) {
            val menuItem = bottomNavigationView.menu.getItem(i)
            val view = LayoutInflater.from(this).inflate(R.layout.nav_item, null)
            val iconResId = when (menuItem.itemId) {
                R.id.action_home -> R.drawable.ic_home
                R.id.action_settings -> R.drawable.ic_settings
                R.id.action_inventory -> R.drawable.ic_inventory
                R.id.action_notifications -> R.drawable.ic_notification
                R.id.action_reports -> R.drawable.ic_report
                else -> R.drawable.ic_home
            }
            view.findViewById<ImageView>(R.id.icon).setImageResource(iconResId)
            view.findViewById<TextView>(R.id.title).text = menuItem.title
            val menuView = bottomNavigationView.getChildAt(0) as ViewGroup
            menuView.getChildAt(i).setBackgroundColor(ContextCompat.getColor(this, R.color.red))
        }
    }
}