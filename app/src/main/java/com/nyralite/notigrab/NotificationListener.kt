package com.nyralite.notigrab

import android.app.Notification
import android.graphics.Bitmap
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.io.ByteArrayOutputStream
import java.util.Locale

class NotificationListener : NotificationListenerService() {

    private lateinit var database: NotificationDatabase

    override fun onCreate() {
        super.onCreate()

        database = NotificationDatabase(
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
        try {

            val packageName =
                sbn.packageName

            if (!database.isAppEnabled(packageName)) {
                return
            }

            val notification =
                sbn.notification

            val extras =
                notification.extras

            val title =
                extras.getCharSequence(
                    Notification.EXTRA_TITLE
                )
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            val message =
                extras.getCharSequence(
                    Notification.EXTRA_TEXT
                )
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            val subText =
                extras.getCharSequence(
                    Notification.EXTRA_SUB_TEXT
                )
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            val accountName =
                extractAccountName(
                    title = title,
                    subText = subText,
                    packageName = packageName
                )

            if (accountName.isBlank()) {
                return
            }

            val accountId =
                accountName
                    .trim()
                    .lowercase(
                        Locale.getDefault()
                    )

            val avatar =
                extractAvatar(
                    notification
                )

            val databaseId =
                database.insertNotification(
                    appPackage = packageName,
                    accountId = accountId,
                    accountName = accountName,
                    message = message,
                    time = System.currentTimeMillis(),
                    avatar = avatar
                )

            if (databaseId != -1L) {

                cancelNotification(
                    sbn.key
                )

                Log.d(
                    "NOTIGRAB",
                    "Notification captured"
                )

                Log.d(
                    "NOTIGRAB",
                    "Package: $packageName"
                )

                Log.d(
                    "NOTIGRAB",
                    "Account: $accountName"
                )

                Log.d(
                    "NOTIGRAB",
                    "Message: $message"
                )

                Log.d(
                    "NOTIGRAB",
                    "Avatar available: ${avatar != null}"
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

    private fun extractAccountName(
        title: String,
        subText: String,
        packageName: String
    ): String {

        if (
            title.isNotBlank() &&
            !looksLikeApplicationName(
                title,
                packageName
            )
        ) {
            return title
        }

        if (subText.isNotBlank()) {
            return subText
        }

        if (title.isNotBlank()) {
            return title
        }

        return packageName
    }

    private fun looksLikeApplicationName(
        title: String,
        packageName: String
    ): Boolean {

        return try {

            val applicationInfo =
                packageManager.getApplicationInfo(
                    packageName,
                    0
                )

            val applicationName =
                packageManager
                    .getApplicationLabel(
                        applicationInfo
                    )
                    .toString()

            title.equals(
                applicationName,
                ignoreCase = true
            )

        } catch (e: Exception) {

            false
        }
    }

    private fun extractAvatar(
        notification: Notification
    ): ByteArray? {

        return try {

            val extras =
                notification.extras

            val bitmap: Bitmap? =
                try {

                    @Suppress("DEPRECATION")
                    extras.getParcelable(
                        Notification.EXTRA_LARGE_ICON
                    ) as? Bitmap

                } catch (e: Exception) {

                    null
                }

            if (bitmap != null) {

                bitmapToByteArray(
                    bitmap
                )

            } else {

                null
            }

        } catch (e: Exception) {

            Log.d(
                "NOTIGRAB",
                "Avatar extraction failed",
                e
            )

            null
        }
    }

    private fun bitmapToByteArray(
        bitmap: Bitmap
    ): ByteArray? {

        return try {

            val output =
                ByteArrayOutputStream()

            bitmap.compress(
                Bitmap.CompressFormat.PNG,
                100,
                output
            )

            output.toByteArray()

        } catch (e: Exception) {

            null
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