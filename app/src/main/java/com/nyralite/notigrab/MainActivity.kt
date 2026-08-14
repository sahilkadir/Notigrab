package com.nyralite.notigrab

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.nyralite.notigrab.ui.theme.NotigrabTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var database: NotificationDatabase

    private var notifications by mutableStateOf(
        emptyList<StoredNotification>()
    )

    private var installedApps by mutableStateOf(
        emptyList<CaptureApp>()
    )

    private var appIcons by mutableStateOf(
        emptyMap<String, Bitmap>()
    )

    private var showSavedOnly by mutableStateOf(false)

    private var showSystemApps by mutableStateOf(false)

    private var notificationAccessGranted by mutableStateOf(false)

    private val databaseExecutor: ExecutorService =
        Executors.newSingleThreadExecutor()

    private val mainHandler =
        Handler(Looper.getMainLooper())

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        val splashScreen =
            installSplashScreen()

        super.onCreate(savedInstanceState)

        database =
            NotificationDatabase(this)

        checkNotificationAccess()

        setContent {

            NotigrabTheme(
                darkTheme = true
            ) {

                NotiGrabApp(
                    notifications =
                        notifications,

                    showSavedOnly =
                        showSavedOnly,

                    showSystemApps =
                        showSystemApps,

                    notificationAccessGranted =
                        notificationAccessGranted,

                    installedApps =
                        installedApps,

                    appIcons =
                        appIcons,

                    onSavedFilterChange = {
                            savedOnly ->

                        showSavedOnly =
                            savedOnly

                        showSystemApps =
                            false

                        loadNotifications()
                    },

                    onSystemAppsClick = {

                        showSystemApps =
                            true

                        loadInstalledApps()
                    },

                    onBackFromSystemApps = {

                        showSystemApps =
                            false
                    },

                    onAppEnabledChange = {
                            app,
                            enabled ->

                        databaseExecutor.execute {

                            database.saveApp(
                                packageName =
                                    app.packageName,

                                appName =
                                    app.appName,

                                enabled =
                                    enabled
                            )

                            loadInstalledApps()
                        }
                    },

                    onSave = {
                            notification ->

                        databaseExecutor.execute {

                            database.setSaved(
                                id =
                                    notification.id,

                                saved =
                                    !notification.saved
                            )

                            loadNotifications()
                        }
                    },

                    onFavorite = {
                            notification ->

                        databaseExecutor.execute {

                            database.toggleFavoriteAccount(
                                appPackage =
                                    notification.appPackage,

                                accountId =
                                    notification.accountId,

                                accountName =
                                    notification.accountName,

                                avatar =
                                    notification.avatar
                            )

                            loadNotifications()
                        }
                    },

                    onDelete = {
                            id ->

                        databaseExecutor.execute {

                            database.deleteNotification(
                                id
                            )

                            loadNotifications()
                        }
                    },

                    onClearInbox = {

                        databaseExecutor.execute {

                            database.clearInbox()

                            loadNotifications()
                        }
                    },

                    onOpenSettings = {

                        openNotificationAccessSettings()
                    }
                )
            }
        }

        loadNotifications()

        loadInstalledApps()
    }

    override fun onResume() {

        super.onResume()

        checkNotificationAccess()

        if (showSystemApps) {

            loadInstalledApps()
        }
    }

    override fun onDestroy() {

        databaseExecutor.shutdownNow()

        mainHandler.removeCallbacksAndMessages(null)

        super.onDestroy()
    }

    private fun checkNotificationAccess() {

        notificationAccessGranted =
            NotificationManagerCompat
                .getEnabledListenerPackages(this)
                .contains(packageName)
    }

    private fun loadNotifications() {

        databaseExecutor.execute {

            try {

                database.deleteExpiredNotifications()

                val result =
                    if (showSavedOnly) {

                        database.getSavedNotifications()

                    } else {

                        database.getAllNotifications()
                    }

                mainHandler.post {

                    notifications =
                        result
                }

            } catch (e: Exception) {

                e.printStackTrace()
            }
        }
    }

    private fun loadInstalledApps() {

        databaseExecutor.execute {

            try {

                val savedApps =
                    database.getCaptureApps()

                val savedByPackage =
                    savedApps.associateBy {
                        it.packageName
                    }

                val launcherIntent =
                    Intent(
                        Intent.ACTION_MAIN
                    ).apply {

                        addCategory(
                            Intent.CATEGORY_LAUNCHER
                        )
                    }

                val packageManager =
                    packageManager

                val launcherApps =
                    packageManager.queryIntentActivities(
                        launcherIntent,
                        0
                    )

                val loadedIcons =
                    mutableMapOf<String, Bitmap>()

                val result =
                    launcherApps
                        .map { resolveInfo ->

                            val appInfo =
                                resolveInfo
                                    .activityInfo
                                    .applicationInfo

                            val packageName =
                                appInfo.packageName

                            try {

                                loadedIcons[
                                    packageName
                                ] =
                                    drawableToBitmap(
                                        packageManager
                                            .getApplicationIcon(
                                                appInfo
                                            )
                                    )

                            } catch (_: Exception) {
                            }

                            val appName =
                                packageManager
                                    .getApplicationLabel(
                                        appInfo
                                    )
                                    .toString()

                            val saved =
                                savedByPackage[
                                    packageName
                                ]

                            CaptureApp(

                                packageName =
                                    packageName,

                                appName =
                                    appName,

                                enabled =
                                    saved?.enabled
                                        ?: (
                                                packageName ==
                                                        "com.instapro2.android"
                                                )
                            )
                        }
                        .filter {

                            it.packageName !=
                                    packageName
                        }
                        .distinctBy {

                            it.packageName
                        }
                        .sortedBy {

                            it.appName.lowercase(
                                Locale.getDefault()
                            )
                        }

                val instaPackage =
                    "com.instapro2.android"

                val hasInsta =
                    result.any {

                        it.packageName ==
                                instaPackage
                    }

                val finalResult =
                    if (hasInsta) {

                        result

                    } else {

                        try {

                            loadedIcons[
                                instaPackage
                            ] =
                                drawableToBitmap(
                                    packageManager
                                        .getApplicationIcon(
                                            instaPackage
                                        )
                                )

                        } catch (_: Exception) {
                        }

                        val saved =
                            savedByPackage[
                                instaPackage
                            ]

                        result +
                                CaptureApp(

                                    packageName =
                                        instaPackage,

                                    appName =
                                        "InstaPro2",

                                    enabled =
                                        saved?.enabled
                                            ?: true
                                )
                    }

                mainHandler.post {

                    installedApps =
                        finalResult

                    appIcons =
                        loadedIcons
                }

            } catch (e: Exception) {

                e.printStackTrace()
            }
        }
    }

    private fun drawableToBitmap(
        drawable: Drawable
    ): Bitmap {

        val width =
            if (drawable.intrinsicWidth > 0)
                drawable.intrinsicWidth
            else
                96

        val height =
            if (drawable.intrinsicHeight > 0)
                drawable.intrinsicHeight
            else
                96

        val bitmap =
            Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
            )

        val canvas =
            Canvas(bitmap)

        drawable.setBounds(
            0,
            0,
            canvas.width,
            canvas.height
        )

        drawable.draw(canvas)

        return bitmap
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

@Composable
fun NotiGrabSplash() {

    Box(

        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color(0xFF090A0F)
                ),

        contentAlignment =
            Alignment.Center
    ) {

        Column(

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Surface(

                modifier =
                    Modifier.size(82.dp),

                shape =
                    RoundedCornerShape(24.dp),

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
                            Color(0xFFB9A7FF),

                        modifier =
                            Modifier.size(44.dp)
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )

            Text(

                text =
                    "NotiGrab",

                color =
                    Color.White,

                fontSize =
                    28.sp,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            Text(

                text =
                    "Private inbox",

                color =
                    Color(0xFF9294A3),

                fontSize =
                    14.sp
            )

            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )

            CircularProgressIndicator(

                modifier =
                    Modifier.size(24.dp),

                color =
                    Color(0xFFB9A7FF),

                strokeWidth =
                    2.dp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotiGrabApp(

    notifications:
    List<StoredNotification>,

    showSavedOnly:
    Boolean,

    showSystemApps:
    Boolean,

    notificationAccessGranted:
    Boolean,

    installedApps:
    List<CaptureApp>,

    appIcons:
    Map<String, Bitmap>,

    onSavedFilterChange:
        (Boolean) -> Unit,

    onSystemAppsClick:
        () -> Unit,

    onBackFromSystemApps:
        () -> Unit,

    onAppEnabledChange:
        (CaptureApp, Boolean) -> Unit,

    onSave:
        (StoredNotification) -> Unit,

    onFavorite:
        (StoredNotification) -> Unit,

    onDelete:
        (Long) -> Unit,

    onClearInbox:
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
                                        if (showSystemApps)
                                            Icons.Default.Apps
                                        else
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
                                    if (showSystemApps)
                                        "System Apps"
                                    else
                                        "NotiGrab",

                                color =
                                    Color.White,

                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(

                                text =
                                    if (showSystemApps)
                                        "Notification sources"
                                    else
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

                navigationIcon = {

                    if (showSystemApps) {

                        IconButton(

                            onClick =
                                onBackFromSystemApps
                        ) {

                            Icon(

                                imageVector =
                                    Icons.Default.ArrowBack,

                                contentDescription =
                                    "Back",

                                tint =
                                    Color.White
                            )
                        }
                    }
                },

                actions = {

                    if (!showSystemApps) {

                        IconButton(

                            onClick =
                                onSystemAppsClick
                        ) {

                            Icon(

                                imageVector =
                                    Icons.Default.Apps,

                                contentDescription =
                                    "System Apps",

                                tint =
                                    Color(0xFFB9A7FF)
                            )
                        }

                        IconButton(

                            onClick =
                                onOpenSettings
                        ) {

                            Icon(

                                imageVector =
                                    Icons.Default.Settings,

                                contentDescription =
                                    "Notification access",

                                tint =
                                    Color(0xFFB9A7FF)
                            )
                        }
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

        if (showSystemApps) {

            SystemAppsScreen(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),

                apps =
                    installedApps,

                appIcons =
                    appIcons,

                onAppEnabledChange =
                    onAppEnabledChange
            )

        } else {

            MainInboxScreen(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),

                notifications =
                    notifications,

                showSavedOnly =
                    showSavedOnly,

                notificationAccessGranted =
                    notificationAccessGranted,

                onSavedFilterChange =
                    onSavedFilterChange,

                onSave =
                    onSave,

                onFavorite =
                    onFavorite,

                onDelete =
                    onDelete,

                onClearInbox =
                    onClearInbox
            )
        }
    }
}

@Composable
fun SystemAppsScreen(

    modifier:
    Modifier,

    apps:
    List<CaptureApp>,

    appIcons:
    Map<String, Bitmap>,

    onAppEnabledChange:
        (CaptureApp, Boolean) -> Unit
) {

    var searchQuery by remember {

        mutableStateOf("")
    }

    val filteredApps =
        apps.filter {

            it.appName.contains(

                searchQuery,

                ignoreCase =
                    true
            )
        }

    Column(

        modifier =
            modifier.padding(
                horizontal = 16.dp
            )
    ) {

        Spacer(
            modifier =
                Modifier.height(14.dp)
        )

        Card(

            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(20.dp),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color(0xFF13151D)
                )
        ) {

            Column(

                modifier =
                    Modifier.padding(18.dp)
            ) {

                Text(

                    text =
                        "System Apps",

                    color =
                        Color.White,

                    fontWeight =
                        FontWeight.Bold,

                    fontSize =
                        18.sp
                )

                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )

                Text(

                    text =
                        "Select which apps NotiGrab should capture notifications from.",

                    color =
                        Color(0xFF9294A3),

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )

                Spacer(
                    modifier =
                        Modifier.height(14.dp)
                )

                OutlinedTextField(

                    value =
                        searchQuery,

                    onValueChange = {

                        searchQuery =
                            it
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    singleLine =
                        true,

                    placeholder = {

                        Text(
                            "Search apps"
                        )
                    },

                    shape =
                        RoundedCornerShape(14.dp)
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(14.dp)
        )

        if (filteredApps.isEmpty()) {

            Box(

                modifier =
                    Modifier.fillMaxSize(),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(

                    text =
                        if (searchQuery.isBlank())
                            "No apps found"
                        else
                            "No matching apps",

                    color =
                        Color(0xFF858795)
                )
            }

        } else {

            LazyColumn(

                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                items(

                    items =
                        filteredApps,

                    key = {

                        it.packageName
                    }

                ) { app ->

                    SystemAppRow(

                        app =
                            app,

                        icon =
                            appIcons[
                                app.packageName
                            ],

                        onEnabledChange =
                            {
                                    enabled ->

                                onAppEnabledChange(
                                    app,
                                    enabled
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

@Composable
fun SystemAppRow(

    app:
    CaptureApp,

    icon:
    Bitmap?,

    onEnabledChange:
        (Boolean) -> Unit
) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color(0xFF13151D)
            )
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Surface(

                modifier =
                    Modifier.size(46.dp),

                shape =
                    RoundedCornerShape(14.dp),

                color =
                    if (app.enabled)
                        Color(0xFF201C31)
                    else
                        Color(0xFF1A1B22)
            ) {

                Box(

                    contentAlignment =
                        Alignment.Center
                ) {

                    if (icon != null) {

                        Image(

                            bitmap =
                                icon.asImageBitmap(),

                            contentDescription =
                                app.appName,

                            modifier =
                                Modifier
                                    .size(38.dp)
                                    .clip(
                                        RoundedCornerShape(
                                            10.dp
                                        )
                                    )
                        )

                    } else {

                        Icon(

                            imageVector =
                                Icons.Default.Apps,

                            contentDescription =
                                null,

                            tint =
                                if (app.enabled)
                                    Color(0xFFB9A7FF)
                                else
                                    Color(0xFF6F717C)
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.width(14.dp)
            )

            Column(

                modifier =
                    Modifier.weight(1f)
            ) {

                Text(

                    text =
                        app.appName,

                    color =
                        Color.White,

                    fontWeight =
                        FontWeight.SemiBold
                )

                Spacer(
                    modifier =
                        Modifier.height(3.dp)
                )

                Text(

                    text =
                        if (app.enabled)
                            "Notifications will be captured"
                        else
                            "Notifications are ignored",

                    color =
                        Color(0xFF858795),

                    style =
                        MaterialTheme
                            .typography
                            .labelSmall
                )
            }

            Switch(

                checked =
                    app.enabled,

                onCheckedChange =
                    onEnabledChange
            )
        }
    }
}

@Composable
fun MainInboxScreen(

    modifier:
    Modifier,

    notifications:
    List<StoredNotification>,

    showSavedOnly:
    Boolean,

    notificationAccessGranted:
    Boolean,

    onSavedFilterChange:
        (Boolean) -> Unit,

    onSave:
        (StoredNotification) -> Unit,

    onFavorite:
        (StoredNotification) -> Unit,

    onDelete:
        (Long) -> Unit,

    onClearInbox:
        () -> Unit
) {

    Column(

        modifier =
            modifier.padding(
                horizontal = 16.dp
            )
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

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            FilterChip(

                selected =
                    !showSavedOnly,

                onClick = {

                    onSavedFilterChange(
                        false
                    )
                },

                label = {

                    Text(
                        "INBOX"
                    )
                }
            )

            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )

            FilterChip(

                selected =
                    showSavedOnly,

                onClick = {

                    onSavedFilterChange(
                        true
                    )
                },

                label = {

                    Text(
                        "SAVED"
                    )
                }
            )

            Spacer(
                modifier =
                    Modifier.weight(1f)
            )

            if (!showSavedOnly) {

                TextButton(

                    onClick =
                        onClearInbox
                ) {

                    Text(

                        text =
                            "CLEAR ALL",

                        color =
                            Color(0xFFFF7777),

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
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

                        onFavorite = {

                            onFavorite(
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

@Composable
fun AccessStatus(

    connected:
    Boolean
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

                    color =
                        Color.White,

                    fontWeight =
                        FontWeight.Bold
                )

                Text(

                    text =
                        if (connected)
                            "Selected app notifications are being captured"
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

    onFavorite:
        () -> Unit,

    onDelete:
        () -> Unit
) {

    val dismissState =
        rememberSwipeToDismissBoxState(

            confirmValueChange = {

                    value ->

                if (
                    value ==
                    SwipeToDismissBoxValue.EndToStart
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
                        if (notification.favorite)
                            Color(0xFF181522)
                        else
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
                            if (notification.favorite)
                                Color(0xFF29213D)
                            else
                                Color(0xFF201C31)
                    ) {

                        Box(

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Text(

                                text =
                                    notification
                                        .accountName
                                        .firstOrNull()
                                        ?.uppercase()
                                        ?: "N",

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
                                notification.accountName,

                            color =
                                Color.White,

                            fontWeight =
                                FontWeight.Bold,

                            fontSize =
                                16.sp
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
                            onFavorite
                    ) {

                        Icon(

                            imageVector =
                                if (notification.favorite)
                                    Icons.Default.Favorite
                                else
                                    Icons.Default.FavoriteBorder,

                            contentDescription =
                                if (notification.favorite)
                                    "Remove favorite"
                                else
                                    "Favorite",

                            tint =
                                if (notification.favorite)
                                    Color(0xFFFF6B8A)
                                else
                                    Color(0xFF777987)
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
                                    Icons.Default.BookmarkBorder,

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

                if (notification.favorite) {

                    Text(

                        text =
                            "Favorite account",

                        style =
                            MaterialTheme
                                .typography
                                .labelSmall,

                        color =
                            Color(0xFFFF6B8A),

                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(5.dp)
                    )
                }

                if (notification.saved) {

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

                } else {

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

                color =
                    Color.White,

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
                        "New selected-app notifications will appear here.",

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

    timestamp:
    Long
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
                notification.get(Calendar.YEAR) &&
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