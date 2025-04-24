package com.example.sisig

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class NotificationsActivity : Fragment() {

    private lateinit var noNotificationLayout: View
    private lateinit var notificationsList: RecyclerView
    private val notifications = mutableListOf<String>() // Mutable list for notifications
    private lateinit var notificationsAdapter: NotificationsAdapter

    companion object {
        fun newInstance(): NotificationsActivity {
            return NotificationsActivity()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notification, container, false)

        // Initialize views
        noNotificationLayout = view.findViewById(R.id.no_notification_layout)
        notificationsList = view.findViewById(R.id.notifications_list)

        // Add hardcoded notifications for demonstration
        notifications.addAll(
            listOf(
                "You earned 5000 today!",
                "New stock successfully added.",
                "Meat is out of stock"

            )
        )

        // Set up RecyclerView and Adapter
        notificationsAdapter = NotificationsAdapter(notifications) {
            // This is the callback function to update notification visibility
            updateNotificationVisibility()
        }
        notificationsList.layoutManager = LinearLayoutManager(context)
        notificationsList.adapter = notificationsAdapter

        // Check if there are notifications
        updateNotificationVisibility()

        return view
    }

    private fun updateNotificationVisibility() {
        if (notifications.isEmpty()) {
            // Show no notifications layout
            noNotificationLayout.visibility = View.VISIBLE
            notificationsList.visibility = View.GONE
        } else {
            // Show notifications list
            noNotificationLayout.visibility = View.GONE
            notificationsList.visibility = View.VISIBLE
        }
    }
}



