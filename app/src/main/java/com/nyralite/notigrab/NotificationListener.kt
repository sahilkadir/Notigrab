package com.nyralite.notigrab

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationListener :
    NotificationListenerService() {

    private lateinit var database:
            NotificationDatabase

    override fun onCreate() {

        super.onCreate()

        database =
            NotificationDatabase(
                applicationContext
            )

        database.deleteExpiredNotifications()

        Log.d(
            "NOTIGRAB",
            "Notification service created"
        )
    }

    override fun onListenerConnected() {

        super.onListenerConnected()

        database.deleteExpiredNotifications()

        Log.d(
            "NOTIGRAB",
            "Notification listener connected"
        )
    }

    override fun onNotificationPosted(
        sbn: StatusBarNotification
    ) {

        if (
            sbn.packageName !=
            "com.instapro2.android"
        ) {

            return
        }

        try {

            val extras =
                sbn.notification.extras

            val title =
                extras.getString(
                    Notification.EXTRA_TITLE
                )
                    ?: "InstaPro2"

            val message =
                extras.getCharSequence(
                    Notification.EXTRA_TEXT
                )
                    ?.toString()
                    ?: ""

            Log.d(
                "NOTIGRAB",
                "InstaPro2 notification received"
            )

            Log.d(
                "NOTIGRAB",
                "Title: $title"
            )

            Log.d(
                "NOTIGRAB",
                "Message: $message"
            )

            val databaseId =
                database.insertNotification(

                    title =
                        title,

                    message =
                        message,

                    time =
                        System.currentTimeMillis()
                )

            if (databaseId != -1L) {

                cancelNotification(
                    sbn.key
                )

                Log.d(
                    "NOTIGRAB",
                    "Notification saved and removed"
                )
            }

        } catch (e: Exception) {

            Log.e(
                "NOTIGRAB",
                "Error processing notification",
                e
            )
        }
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification
    ) {

        Log.d(
            "NOTIGRAB",
            "Notification removed: " +
                    sbn.packageName
        )
    }

    override fun onDestroy() {

        if (
            ::database.isInitialized
        ) {

            database.close()
        }

        super.onDestroy()
    }
}