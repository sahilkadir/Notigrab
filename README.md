# Notigrab
NotiGrab is a privacy-focused Android application built with Kotlin and Jetpack Compose that provides a centralized, local notification vault.

Instead of allowing selected app notifications to remain visible in the Android notification panel, NotiGrab uses Android's NotificationListenerService to intercept notifications, store them locally, and remove them from the system notification shade. Users can then access their notification history directly inside NotiGrab.

Current Features
🔔 Notification interception using NotificationListenerService

📱 Currently supports InstaPro2 with plans for multi-app support

🗃️ Local SQLite notification storage

🔖 Save notifications for permanent retention

🗑️ Swipe-to-delete notifications

⏳ Automatic deletion of unsaved notifications after 48 hours

🕐 12-hour notification timestamps

🌙 Modern dark-mode UI built with Jetpack Compose

🔐 Privacy-focused local storage

🔄 Separate Inbox and Saved sections

⚡ Real-time UI updates when notifications are saved or deleted

Planned Features

📋 Installed-app manager with search and capture toggles

⭐ Favorite notifications pinned to the top

🖼️ Sender profile pictures

🔍 Notification search

🏷️ Filtering by application

🔐 PIN/biometric app lock

⚙️ Custom notification capture rules

📊 Notification statistics

📤 Export and backup

🎨 Advanced UI animations and customization
