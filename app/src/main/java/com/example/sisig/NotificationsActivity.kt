package com.example.sisig

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sisig.data.AppDatabase
import com.example.sisig.data.Notification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationsActivity : Fragment() {

    private lateinit var noNotificationLayout: View
    private lateinit var notificationsList: RecyclerView
    private lateinit var notificationsAdapter: NotificationsAdapter
    private lateinit var db: AppDatabase

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

        noNotificationLayout = view.findViewById(R.id.no_notification_layout)
        notificationsList = view.findViewById(R.id.notifications_list)
        db = AppDatabase.getInstance(requireContext())

        notificationsAdapter = NotificationsAdapter(mutableListOf()) { notification ->
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val deletedRows = db.notificationDao.deleteById(notification.id)
                withContext(Dispatchers.Main) {
                    if (deletedRows > 0) {
                        Log.d("NotificationsActivity", "Deleted notification with ID: ${notification.id}")
                    } else {
                        Log.e("NotificationsActivity", "Failed to delete notification with ID: ${notification.id}")
                    }
                }
            }
        }
        notificationsList.layoutManager = LinearLayoutManager(context)
        notificationsList.adapter = notificationsAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            db.notificationDao.getAllNotifications().collect { notifications ->
                notificationsAdapter.updateNotifications(notifications)
                updateNotificationVisibility(notifications)
            }
        }

        return view
    }

    private fun updateNotificationVisibility(notifications: List<Notification>) {
        if (notifications.isEmpty()) {
            noNotificationLayout.visibility = View.VISIBLE
            notificationsList.visibility = View.GONE
        } else {
            noNotificationLayout.visibility = View.GONE
            notificationsList.visibility = View.VISIBLE
        }
    }
}