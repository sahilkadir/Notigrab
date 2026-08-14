package com.nyralite.notigrab

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class StoredNotification(
    val id: Long,
    val appPackage: String,
    val accountId: String,
    val accountName: String,
    val message: String,
    val time: Long,
    val saved: Boolean,
    val favorite: Boolean,
    val avatar: ByteArray?
)

data class CaptureApp(
    val packageName: String,
    val appName: String,
    val enabled: Boolean
)

class NotificationDatabase(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "notigrab.db"
        private const val DATABASE_VERSION = 4

        private const val TABLE_NOTIFICATIONS = "notifications"
        private const val TABLE_FAVORITES = "favorite_accounts"
        private const val TABLE_APPS = "capture_apps"

        private const val COLUMN_ID = "id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_MESSAGE = "message"
        private const val COLUMN_TIME = "time"
        private const val COLUMN_SAVED = "saved"
        private const val COLUMN_FAVORITE = "favorite"
        private const val COLUMN_APP_PACKAGE = "app_package"
        private const val COLUMN_ACCOUNT_ID = "account_id"
        private const val COLUMN_ACCOUNT_NAME = "account_name"
        private const val COLUMN_AVATAR = "avatar"

        private const val COLUMN_PACKAGE_NAME = "package_name"
        private const val COLUMN_APP_NAME = "app_name"
        private const val COLUMN_ENABLED = "enabled"
        private const val COLUMN_FAVORITED_AT = "favorited_at"
    }

    override fun onCreate(db: SQLiteDatabase) {

        db.execSQL(
            """
            CREATE TABLE $TABLE_NOTIFICATIONS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT,
                $COLUMN_MESSAGE TEXT,
                $COLUMN_TIME INTEGER,
                $COLUMN_SAVED INTEGER DEFAULT 0,
                $COLUMN_FAVORITE INTEGER DEFAULT 0,
                $COLUMN_APP_PACKAGE TEXT DEFAULT '',
                $COLUMN_ACCOUNT_ID TEXT DEFAULT '',
                $COLUMN_ACCOUNT_NAME TEXT DEFAULT '',
                $COLUMN_AVATAR BLOB
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_FAVORITES (
                $COLUMN_APP_PACKAGE TEXT NOT NULL,
                $COLUMN_ACCOUNT_ID TEXT NOT NULL,
                $COLUMN_ACCOUNT_NAME TEXT,
                $COLUMN_AVATAR BLOB,
                $COLUMN_FAVORITED_AT INTEGER,
                PRIMARY KEY (
                    $COLUMN_APP_PACKAGE,
                    $COLUMN_ACCOUNT_ID
                )
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_APPS (
                $COLUMN_PACKAGE_NAME TEXT PRIMARY KEY,
                $COLUMN_APP_NAME TEXT,
                $COLUMN_ENABLED INTEGER DEFAULT 0
            )
            """.trimIndent()
        )

        val values = ContentValues().apply {
            put(
                COLUMN_PACKAGE_NAME,
                "com.instapro2.android"
            )

            put(
                COLUMN_APP_NAME,
                "InstaPro2"
            )

            put(
                COLUMN_ENABLED,
                1
            )
        }

        db.insertWithOnConflict(
            TABLE_APPS,
            null,
            values,
            SQLiteDatabase.CONFLICT_IGNORE
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {

        if (oldVersion < 2) {

            db.execSQL(
                """
                ALTER TABLE $TABLE_NOTIFICATIONS
                ADD COLUMN $COLUMN_SAVED INTEGER DEFAULT 0
                """.trimIndent()
            )
        }

        if (oldVersion < 3) {

            db.execSQL(
                """
                ALTER TABLE $TABLE_NOTIFICATIONS
                ADD COLUMN $COLUMN_FAVORITE INTEGER DEFAULT 0
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_APPS (
                    $COLUMN_PACKAGE_NAME TEXT PRIMARY KEY,
                    $COLUMN_APP_NAME TEXT,
                    $COLUMN_ENABLED INTEGER DEFAULT 0
                )
                """.trimIndent()
            )

            val values = ContentValues().apply {

                put(
                    COLUMN_PACKAGE_NAME,
                    "com.instapro2.android"
                )

                put(
                    COLUMN_APP_NAME,
                    "InstaPro2"
                )

                put(
                    COLUMN_ENABLED,
                    1
                )
            }

            db.insertWithOnConflict(
                TABLE_APPS,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
            )
        }

        if (oldVersion < 4) {

            db.execSQL(
                """
                ALTER TABLE $TABLE_NOTIFICATIONS
                ADD COLUMN $COLUMN_APP_PACKAGE TEXT DEFAULT ''
                """.trimIndent()
            )

            db.execSQL(
                """
                ALTER TABLE $TABLE_NOTIFICATIONS
                ADD COLUMN $COLUMN_ACCOUNT_ID TEXT DEFAULT ''
                """.trimIndent()
            )

            db.execSQL(
                """
                ALTER TABLE $TABLE_NOTIFICATIONS
                ADD COLUMN $COLUMN_ACCOUNT_NAME TEXT DEFAULT ''
                """.trimIndent()
            )

            db.execSQL(
                """
                ALTER TABLE $TABLE_NOTIFICATIONS
                ADD COLUMN $COLUMN_AVATAR BLOB
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_FAVORITES (
                    $COLUMN_APP_PACKAGE TEXT NOT NULL,
                    $COLUMN_ACCOUNT_ID TEXT NOT NULL,
                    $COLUMN_ACCOUNT_NAME TEXT,
                    $COLUMN_AVATAR BLOB,
                    $COLUMN_FAVORITED_AT INTEGER,
                    PRIMARY KEY (
                        $COLUMN_APP_PACKAGE,
                        $COLUMN_ACCOUNT_ID
                    )
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                UPDATE $TABLE_NOTIFICATIONS
                SET
                    $COLUMN_ACCOUNT_NAME = $COLUMN_TITLE,
                    $COLUMN_ACCOUNT_ID = LOWER(TRIM($COLUMN_TITLE))
                WHERE
                    $COLUMN_ACCOUNT_NAME = ''
                """.trimIndent()
            )
        }
    }

    fun insertNotification(
        appPackage: String,
        accountId: String,
        accountName: String,
        message: String,
        time: Long,
        avatar: ByteArray?
    ): Long {

        val favorite =
            isFavoriteAccount(
                appPackage,
                accountId
            )

        val values = ContentValues().apply {

            put(
                COLUMN_TITLE,
                accountName
            )

            put(
                COLUMN_MESSAGE,
                message
            )

            put(
                COLUMN_TIME,
                time
            )

            put(
                COLUMN_SAVED,
                0
            )

            put(
                COLUMN_FAVORITE,
                if (favorite) 1 else 0
            )

            put(
                COLUMN_APP_PACKAGE,
                appPackage
            )

            put(
                COLUMN_ACCOUNT_ID,
                accountId
            )

            put(
                COLUMN_ACCOUNT_NAME,
                accountName
            )

            if (avatar != null) {
                put(
                    COLUMN_AVATAR,
                    avatar
                )
            }
        }

        val id =
            writableDatabase.insert(
                TABLE_NOTIFICATIONS,
                null,
                values
            )

        if (
            id != -1L &&
            avatar != null
        ) {

            updateFavoriteAvatar(
                appPackage,
                accountId,
                avatar
            )
        }

        return id
    }

    fun getAllNotifications():
            List<StoredNotification> {

        return getNotifications(
            "$COLUMN_SAVED = 0",
            null
        )
    }

    fun getSavedNotifications():
            List<StoredNotification> {

        return getNotifications(
            "$COLUMN_SAVED = 1",
            null
        )
    }

    private fun getNotifications(
        selection: String?,
        selectionArgs: Array<String>?
    ): List<StoredNotification> {

        val notifications =
            mutableListOf<StoredNotification>()

        val cursor =
            readableDatabase.query(
                TABLE_NOTIFICATIONS,
                null,
                selection,
                selectionArgs,
                null,
                null,
                "$COLUMN_FAVORITE DESC, $COLUMN_TIME DESC"
            )

        cursor.use {

            while (it.moveToNext()) {

                val avatarIndex =
                    it.getColumnIndex(
                        COLUMN_AVATAR
                    )

                val avatar =
                    if (
                        avatarIndex >= 0 &&
                        !it.isNull(avatarIndex)
                    ) {

                        it.getBlob(
                            avatarIndex
                        )

                    } else {

                        null
                    }

                notifications.add(
                    StoredNotification(

                        id =
                            it.getLong(
                                it.getColumnIndexOrThrow(
                                    COLUMN_ID
                                )
                            ),

                        appPackage =
                            it.getString(
                                it.getColumnIndexOrThrow(
                                    COLUMN_APP_PACKAGE
                                )
                            ) ?: "",

                        accountId =
                            it.getString(
                                it.getColumnIndexOrThrow(
                                    COLUMN_ACCOUNT_ID
                                )
                            ) ?: "",

                        accountName =
                            it.getString(
                                it.getColumnIndexOrThrow(
                                    COLUMN_ACCOUNT_NAME
                                )
                            ) ?: "",

                        message =
                            it.getString(
                                it.getColumnIndexOrThrow(
                                    COLUMN_MESSAGE
                                )
                            ) ?: "",

                        time =
                            it.getLong(
                                it.getColumnIndexOrThrow(
                                    COLUMN_TIME
                                )
                            ),

                        saved =
                            it.getInt(
                                it.getColumnIndexOrThrow(
                                    COLUMN_SAVED
                                )
                            ) == 1,

                        favorite =
                            it.getInt(
                                it.getColumnIndexOrThrow(
                                    COLUMN_FAVORITE
                                )
                            ) == 1,

                        avatar =
                            avatar
                    )
                )
            }
        }

        return notifications
    }

    fun setSaved(
        id: Long,
        saved: Boolean
    ) {

        val values =
            ContentValues().apply {

                put(
                    COLUMN_SAVED,
                    if (saved) 1 else 0
                )
            }

        writableDatabase.update(
            TABLE_NOTIFICATIONS,
            values,
            "$COLUMN_ID = ?",
            arrayOf(
                id.toString()
            )
        )
    }

    fun toggleFavoriteAccount(
        appPackage: String,
        accountId: String,
        accountName: String,
        avatar: ByteArray?
    ): Boolean {

        val db =
            writableDatabase

        val currentlyFavorite =
            isFavoriteAccount(
                appPackage,
                accountId
            )

        if (currentlyFavorite) {

            db.delete(
                TABLE_FAVORITES,
                "$COLUMN_APP_PACKAGE = ? AND $COLUMN_ACCOUNT_ID = ?",
                arrayOf(
                    appPackage,
                    accountId
                )
            )

            val values =
                ContentValues().apply {

                    put(
                        COLUMN_FAVORITE,
                        0
                    )
                }

            db.update(
                TABLE_NOTIFICATIONS,
                values,
                "$COLUMN_APP_PACKAGE = ? AND $COLUMN_ACCOUNT_ID = ?",
                arrayOf(
                    appPackage,
                    accountId
                )
            )

            return false

        } else {

            val values =
                ContentValues().apply {

                    put(
                        COLUMN_APP_PACKAGE,
                        appPackage
                    )

                    put(
                        COLUMN_ACCOUNT_ID,
                        accountId
                    )

                    put(
                        COLUMN_ACCOUNT_NAME,
                        accountName
                    )

                    if (avatar != null) {

                        put(
                            COLUMN_AVATAR,
                            avatar
                        )
                    }

                    put(
                        COLUMN_FAVORITED_AT,
                        System.currentTimeMillis()
                    )
                }

            db.insertWithOnConflict(
                TABLE_FAVORITES,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )

            val notificationValues =
                ContentValues().apply {

                    put(
                        COLUMN_FAVORITE,
                        1
                    )
                }

            db.update(
                TABLE_NOTIFICATIONS,
                notificationValues,
                "$COLUMN_APP_PACKAGE = ? AND $COLUMN_ACCOUNT_ID = ?",
                arrayOf(
                    appPackage,
                    accountId
                )
            )

            return true
        }
    }

    fun isFavoriteAccount(
        appPackage: String,
        accountId: String
    ): Boolean {

        val cursor =
            readableDatabase.query(
                TABLE_FAVORITES,
                arrayOf(
                    COLUMN_APP_PACKAGE
                ),
                "$COLUMN_APP_PACKAGE = ? AND $COLUMN_ACCOUNT_ID = ?",
                arrayOf(
                    appPackage,
                    accountId
                ),
                null,
                null,
                null,
                "1"
            )

        cursor.use {
            return it.moveToFirst()
        }
    }

    private fun updateFavoriteAvatar(
        appPackage: String,
        accountId: String,
        avatar: ByteArray
    ) {

        val values =
            ContentValues().apply {

                put(
                    COLUMN_AVATAR,
                    avatar
                )
            }

        writableDatabase.update(
            TABLE_FAVORITES,
            values,
            "$COLUMN_APP_PACKAGE = ? AND $COLUMN_ACCOUNT_ID = ?",
            arrayOf(
                appPackage,
                accountId
            )
        )

        writableDatabase.update(
            TABLE_NOTIFICATIONS,
            values,
            "$COLUMN_APP_PACKAGE = ? AND $COLUMN_ACCOUNT_ID = ?",
            arrayOf(
                appPackage,
                accountId
            )
        )
    }

    fun deleteNotification(
        id: Long
    ) {

        writableDatabase.delete(
            TABLE_NOTIFICATIONS,
            "$COLUMN_ID = ?",
            arrayOf(
                id.toString()
            )
        )
    }

    fun clearInbox() {

        writableDatabase.delete(
            TABLE_NOTIFICATIONS,
            "$COLUMN_SAVED = 0",
            null
        )
    }

    fun clearNormalUnsaved() {

        clearInbox()
    }

    fun clearAll() {

        clearInbox()
    }

    fun deleteExpiredNotifications() {

        val twoDaysAgo =
            System.currentTimeMillis() -
                    (48L * 60L * 60L * 1000L)

        writableDatabase.delete(
            TABLE_NOTIFICATIONS,
            """
            $COLUMN_SAVED = 0
            AND $COLUMN_FAVORITE = 0
            AND $COLUMN_TIME < ?
            """.trimIndent(),
            arrayOf(
                twoDaysAgo.toString()
            )
        )
    }

    fun saveApp(
        packageName: String,
        appName: String,
        enabled: Boolean
    ) {

        val values =
            ContentValues().apply {

                put(
                    COLUMN_PACKAGE_NAME,
                    packageName
                )

                put(
                    COLUMN_APP_NAME,
                    appName
                )

                put(
                    COLUMN_ENABLED,
                    if (enabled) 1 else 0
                )
            }

        writableDatabase.insertWithOnConflict(
            TABLE_APPS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun setAppEnabled(
        packageName: String,
        enabled: Boolean
    ) {

        val values =
            ContentValues().apply {

                put(
                    COLUMN_ENABLED,
                    if (enabled) 1 else 0
                )
            }

        writableDatabase.update(
            TABLE_APPS,
            values,
            "$COLUMN_PACKAGE_NAME = ?",
            arrayOf(
                packageName
            )
        )
    }

    fun isAppEnabled(
        packageName: String
    ): Boolean {

        val cursor =
            readableDatabase.query(
                TABLE_APPS,
                arrayOf(
                    COLUMN_ENABLED
                ),
                "$COLUMN_PACKAGE_NAME = ?",
                arrayOf(
                    packageName
                ),
                null,
                null,
                null
            )

        cursor.use {

            if (it.moveToFirst()) {

                return it.getInt(
                    it.getColumnIndexOrThrow(
                        COLUMN_ENABLED
                    )
                ) == 1
            }
        }

        return false
    }

    fun getCaptureApps():
            List<CaptureApp> {

        val apps =
            mutableListOf<CaptureApp>()

        val cursor =
            readableDatabase.query(
                TABLE_APPS,
                null,
                null,
                null,
                null,
                null,
                "$COLUMN_APP_NAME COLLATE NOCASE ASC"
            )

        cursor.use {

            while (it.moveToNext()) {

                apps.add(
                    CaptureApp(

                        packageName =
                            it.getString(
                                it.getColumnIndexOrThrow(
                                    COLUMN_PACKAGE_NAME
                                )
                            ),

                        appName =
                            it.getString(
                                it.getColumnIndexOrThrow(
                                    COLUMN_APP_NAME
                                )
                            ) ?: "",

                        enabled =
                            it.getInt(
                                it.getColumnIndexOrThrow(
                                    COLUMN_ENABLED
                                )
                            ) == 1
                    )
                )
            }
        }

        return apps
    }
}