# Notigrab

NotiGrab is a privacy-focused Android application built with Kotlin and Jetpack Compose that provides a centralized, local notification vault.

Instead of allowing selected app notifications to remain visible in the Android notification panel, NotiGrab uses Android's NotificationListenerService to intercept notifications, store them locally, and remove them from the system notification shade. Users can then access their notification history directly inside NotiGrab.



Current Features

🔔 Notification interception using NotificationListenerService

📱 Select which installed apps NotiGrab should capture notifications from

🔍 Searchable installed-app manager

🗃️ Local SQLite notification storage

🔖 Save notifications for permanent retention

⭐ Account-based favorite system

📌 Favorite accounts are automatically prioritized at the top

🔐 Favorites are scoped independently to each application

🖼️ Sender profile pictures / avatars

🗑️ Swipe-to-delete notifications

🧹 Clear All for normal unsaved notifications

🛡️ Saved and favorite notifications are protected from Clear All

⏳ Automatic deletion of normal unsaved notifications after 48 hours

🕐 12-hour notification timestamps

🌙 Modern dark-mode UI built with Jetpack Compose

🎨 Custom application icon and opening animation

🔄 Separate Inbox and Saved sections

⚡ Real-time UI updates when notifications are saved or deleted

🔒 Privacy-focused local storage

📋 Per-app notification capture controls

🔀 Favorite accounts remain independent between different apps



What We Upgraded in v2.0.0

📱 Added installed-app manager with searchable app list

🎯 Added per-app notification capture toggles

⭐ Added account-based favorite system

🔀 Made favorites app-specific so the same account can have different favorite status across different apps

📌 Added automatic favorite priority so favorite accounts remain at the top

🖼️ Added sender profile pictures / avatars

🧹 Added Clear All for normal unsaved notifications

🛡️ Protected saved and favorite notifications from Clear All

⏳ Improved automatic 48-hour cleanup to protect favorite notifications

🔄 Improved Inbox and Saved notification separation

🗑️ Added swipe-to-delete notification management

🎨 Added custom NotiGrab app icon

✨ Added opening/loading animation

🌙 Improved dark-mode readability and UI styling

🔤 Improved username and notification text visibility

🔧 Fixed saved notifications incorrectly appearing in Inbox

🔧 Fixed app search input behavior

🔧 Improved notification refresh and state updates



Planned Features

🔍 Notification search

🏷️ Filtering by application

🔐 PIN/biometric app lock

⚙️ Custom notification capture rules

📊 Notification statistics

📤 Export and backup

🎨 Advanced UI animations and customization

