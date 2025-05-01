package com.example.sisig

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sisig.data.Notification

class NotificationsAdapter(
    private val notifications: MutableList<Notification>,
    private val onRemoveClicked: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.notif_item, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount(): Int {
        return notifications.size
    }

    fun updateNotifications(newNotifications: List<Notification>) {
        notifications.clear()
        notifications.addAll(newNotifications)
        notifyDataSetChanged()
    }

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val notificationText: TextView = itemView.findViewById(R.id.notification_text)
        private val removeButton: Button = itemView.findViewById(R.id.remove_notification)

        fun bind(notification: Notification) {
            notificationText.text = notification.message

            removeButton.setOnClickListener {
                onRemoveClicked(notification)
            }
        }
    }
}