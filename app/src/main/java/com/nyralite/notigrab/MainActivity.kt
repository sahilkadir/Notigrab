package com.nyralite.notigrab

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.nyralite.notigrab.ui.theme.NotigrabTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var database: NotificationDatabase

    private var notifications by mutableStateOf(
        emptyList<StoredNotification>()
    )

    private var showSavedOnly by mutableStateOf(false)

    private var notificationAccessGranted by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        database = NotificationDatabase(this)

        database.deleteExpiredNotifications()

        checkNotificationAccess()

        loadNotifications()

        setContent {

            NotigrabTheme(
                darkTheme = true
            ) {

                NotiGrabApp(

                    notifications = notifications,

                    showSavedOnly = showSavedOnly,

                    notificationAccessGranted =
                        notificationAccessGranted,

                    onSavedFilterChange = { savedOnly ->

                        showSavedOnly = savedOnly

                        notifications =
                            if (savedOnly) {

                                database.getSavedNotifications()

                            } else {

                                database.getAllNotifications()
                            }
                    },

                    onSave = { notification ->

                        database.setSaved(
                            notification.id,
                            !notification.saved
                        )

                        loadNotifications()
                    },

                    onDelete = { id ->

                        database.deleteNotification(id)

                        loadNotifications()
                    },

                    onClear = {

                        database.clearAll()

                        loadNotifications()
                    },

                    onOpenSettings = {

                        openNotificationAccessSettings()
                    }
                )
            }
        }
    }

    override fun onResume() {

        super.onResume()

        database.deleteExpiredNotifications()

        checkNotificationAccess()

        loadNotifications()
    }

    private fun checkNotificationAccess() {

        notificationAccessGranted =
            NotificationManagerCompat
                .getEnabledListenerPackages(this)
                .contains(packageName)
    }

    private fun loadNotifications() {

        notifications =
            if (showSavedOnly) {

                database.getSavedNotifications()

            } else {

                database.getAllNotifications()
            }
    }

    private fun openNotificationAccessSettings() {

        try {

            startActivity(
                Intent(
                    Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                )
            )

        } catch (e: Exception) {

            startActivity(
                Intent(
                    Settings.ACTION_SETTINGS
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotiGrabApp(

    notifications: List<StoredNotification>,

    showSavedOnly: Boolean,

    notificationAccessGranted: Boolean,

    onSavedFilterChange:
        (Boolean) -> Unit,

    onSave:
        (StoredNotification) -> Unit,

    onDelete:
        (Long) -> Unit,

    onClear:
        () -> Unit,

    onOpenSettings:
        () -> Unit
) {

    Scaffold(

        containerColor =
            Color(0xFF090A0F),

        topBar = {

            TopAppBar(

                title = {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Surface(

                            modifier =
                                Modifier.size(42.dp),

                            shape =
                                RoundedCornerShape(14.dp),

                            color =
                                Color(0xFF171923)
                        ) {

                            Box(
                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Icon(

                                    imageVector =
                                        Icons.Default.Inbox,

                                    contentDescription =
                                        null,

                                    tint =
                                        Color(0xFFB9A7FF)
                                )
                            }
                        }

                        Spacer(
                            modifier =
                                Modifier.width(12.dp)
                        )

                        Column {

                            Text(

                                text =
                                    "NotiGrab",

                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(

                                text =
                                    "Private inbox",

                                style =
                                    MaterialTheme
                                        .typography
                                        .labelSmall,

                                color =
                                    Color(0xFF9294A3)
                            )
                        }
                    }
                },

                actions = {

                    IconButton(

                        onClick =
                            onOpenSettings
                    ) {

                        Icon(

                            imageVector =
                                Icons.Default.Settings,

                            contentDescription =
                                "Settings",

                            tint =
                                Color(0xFFB9A7FF)
                        )
                    }
                },

                colors =
                    TopAppBarDefaults
                        .topAppBarColors(

                            containerColor =
                                Color(0xFF090A0F)
                        )
            )
        }

    ) { padding ->

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
        ) {

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            AccessStatus(

                connected =
                    notificationAccessGranted
            )

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                FilterChip(

                    selected =
                        !showSavedOnly,

                    onClick = {

                        onSavedFilterChange(false)
                    },

                    label = {

                        Text("INBOX")
                    }
                )

                FilterChip(

                    selected =
                        showSavedOnly,

                    onClick = {

                        onSavedFilterChange(true)
                    },

                    label = {

                        Text("SAVED")
                    }
                )
            }

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            if (notifications.isEmpty()) {

                EmptyInbox(

                    savedOnly =
                        showSavedOnly
                )

            } else {

                LazyColumn(

                    modifier =
                        Modifier.fillMaxSize(),

                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    items(

                        items =
                            notifications,

                        key = {

                            it.id
                        }

                    ) { notification ->

                        NotificationCard(

                            notification =
                                notification,

                            onSave = {

                                onSave(
                                    notification
                                )
                            },

                            onDelete = {

                                onDelete(
                                    notification.id
                                )
                            }
                        )
                    }

                    item {

                        Spacer(
                            modifier =
                                Modifier.height(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AccessStatus(
    connected: Boolean
) {

    val background =

        if (connected)

            Color(0xFF101A18)

        else

            Color(0xFF1D1416)

    val statusColor =

        if (connected)

            Color(0xFF65D6A5)

        else

            Color(0xFFFF7777)

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(

                containerColor =
                    background
            )
    ) {

        Row(

            modifier =
                Modifier.padding(16.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Surface(

                modifier =
                    Modifier.size(10.dp),

                shape =
                    RoundedCornerShape(50),

                color =
                    statusColor
            ) {}

            Spacer(
                modifier =
                    Modifier.width(12.dp)
            )

            Column {

                Text(

                    text =

                        if (connected)

                            "Capture active"

                        else

                            "Capture disconnected",

                    fontWeight =
                        FontWeight.Bold
                )

                Text(

                    text =

                        if (connected)

                            "InstaPro2 notifications are being captured"

                        else

                            "Notification access is disabled",

                    style =
                        MaterialTheme
                            .typography
                            .labelSmall,

                    color =
                        Color(0xFF9294A3)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCard(

    notification:
    StoredNotification,

    onSave:
        () -> Unit,

    onDelete:
        () -> Unit
) {

    val dismissState =

        rememberSwipeToDismissBoxState(

            confirmValueChange = { value ->

                if (
                    value ==
                    SwipeToDismissBoxValue
                        .EndToStart
                ) {

                    onDelete()

                    true

                } else {

                    false
                }
            }
        )

    SwipeToDismissBox(

        state =
            dismissState,

        enableDismissFromStartToEnd =
            false,

        backgroundContent = {

            Box(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(
                            RoundedCornerShape(22.dp)
                        )
                        .background(
                            Color(0xFF301519)
                        )
                        .padding(
                            end = 22.dp
                        ),

                contentAlignment =
                    Alignment.CenterEnd
            ) {

                Icon(

                    imageVector =
                        Icons.Default.Delete,

                    contentDescription =
                        "Delete",

                    tint =
                        Color(0xFFFF7777)
                )
            }
        }

    ) {

        Card(

            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(22.dp),

            colors =
                CardDefaults.cardColors(

                    containerColor =
                        Color(0xFF13151D)
                )
        ) {

            Column(

                modifier =
                    Modifier.padding(17.dp)
            ) {

                Row(

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Surface(

                        modifier =
                            Modifier.size(44.dp),

                        shape =
                            RoundedCornerShape(15.dp),

                        color =
                            Color(0xFF201C31)
                    ) {

                        Box(

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Text(

                                text =
                                    notification.title
                                        .firstOrNull()
                                        ?.uppercase()
                                        ?: "I",

                                color =
                                    Color(0xFFC4B6FF),

                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.width(12.dp)
                    )

                    Column(

                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(

                            text =
                                notification.title,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(

                            text =
                                formatNotificationTime(
                                    notification.time
                                ),

                            style =
                                MaterialTheme
                                    .typography
                                    .labelSmall,

                            color =
                                Color(0xFF858795)
                        )
                    }

                    IconButton(

                        onClick =
                            onSave
                    ) {

                        Icon(

                            imageVector =

                                if (notification.saved)

                                    Icons.Default.Bookmark

                                else

                                    Icons.Default
                                        .BookmarkBorder,

                            contentDescription =

                                if (notification.saved)

                                    "Unsave"

                                else

                                    "Save",

                            tint =

                                if (notification.saved)

                                    Color(0xFFB9A7FF)

                                else

                                    Color(0xFF777987)
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(14.dp)
                )

                Text(

                    text =
                        notification.message,

                    style =
                        MaterialTheme
                            .typography
                            .bodyLarge,

                    color =
                        Color(0xFFD8D9E1)
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                if (!notification.saved) {

                    Row(

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(

                            imageVector =
                                Icons.Default.Lock,

                            contentDescription =
                                null,

                            modifier =
                                Modifier.size(14.dp),

                            tint =
                                Color(0xFF70727F)
                        )

                        Spacer(
                            modifier =
                                Modifier.width(6.dp)
                        )

                        Text(

                            text =
                                "Auto-deletes after 2 days",

                            style =
                                MaterialTheme
                                    .typography
                                    .labelSmall,

                            color =
                                Color(0xFF70727F)
                        )
                    }

                } else {

                    Text(

                        text =
                            "Saved permanently",

                        style =
                            MaterialTheme
                                .typography
                                .labelSmall,

                        color =
                            Color(0xFFB9A7FF)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyInbox(

    savedOnly:
    Boolean
) {

    Box(

        modifier =
            Modifier.fillMaxSize(),

        contentAlignment =
            Alignment.Center
    ) {

        Column(

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Surface(

                modifier =
                    Modifier.size(76.dp),

                shape =
                    RoundedCornerShape(26.dp),

                color =
                    Color(0xFF141620)
            ) {

                Box(

                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(

                        imageVector =

                            if (savedOnly)

                                Icons.Default.Bookmark

                            else

                                Icons.Default.Inbox,

                        contentDescription =
                            null,

                        modifier =
                            Modifier.size(32.dp),

                        tint =
                            Color(0xFF8E7CE6)
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            Text(

                text =

                    if (savedOnly)

                        "Nothing saved yet"

                    else

                        "Your inbox is empty",

                style =
                    MaterialTheme
                        .typography
                        .titleMedium,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            Text(

                text =

                    if (savedOnly)

                        "Tap the bookmark to keep a notification."

                    else

                        "New InstaPro2 notifications will appear here.",

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,

                color =
                    Color(0xFF858795)
            )
        }
    }
}

fun formatNotificationTime(
    timestamp: Long
): String {

    val now =
        Calendar.getInstance()

    val notification =
        Calendar.getInstance().apply {

            timeInMillis =
                timestamp
        }

    val sameDay =

        now.get(Calendar.YEAR) ==
                notification.get(Calendar.YEAR)

                &&

                now.get(Calendar.DAY_OF_YEAR) ==
                notification.get(Calendar.DAY_OF_YEAR)

    return if (sameDay) {

        SimpleDateFormat(

            "h:mm a",

            Locale.getDefault()

        ).format(
            Date(timestamp)
        )

    } else {

        SimpleDateFormat(

            "dd MMM, h:mm a",

            Locale.getDefault()

        ).format(
            Date(timestamp)
        )
    }
}