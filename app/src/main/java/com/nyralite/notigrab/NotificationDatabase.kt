package com.nyralite.notigrab

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class StoredNotification(
    val id: Long,
    val title: String,
    val message: String,
    val time: Long,
    val saved: Boolean
)

class NotificationDatabase(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {

        private const val DATABASE_NAME =
            "notigrab.db"

        private const val DATABASE_VERSION =
            2

        private const val TABLE_NAME =
            "notifications"

        private const val COLUMN_ID =
            "id"

        private const val COLUMN_TITLE =
            "title"

        private const val COLUMN_MESSAGE =
            "message"

        private const val COLUMN_TIME =
            "time"

        private const val COLUMN_SAVED =
            "saved"
    }

    override fun onCreate(
        db: SQLiteDatabase
    ) {

        db.execSQL(
            """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT,
                $COLUMN_MESSAGE TEXT,
                $COLUMN_TIME INTEGER,
                $COLUMN_SAVED INTEGER DEFAULT 0
            )
            """.trimIndent()
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
                ALTER TABLE $TABLE_NAME
                ADD COLUMN $COLUMN_SAVED INTEGER DEFAULT 0
                """.trimIndent()
            )
        }
    }

    fun insertNotification(
        title: String,
        message: String,
        time: Long
    ): Long {

        val values =
            ContentValues().apply {

                put(
                    COLUMN_TITLE,
                    title
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
            }

        return writableDatabase.insert(
            TABLE_NAME,
            null,
            values
        )
    }

    fun getAllNotifications():
            List<StoredNotification> {

        return getNotifications(
            "$COLUMN_SAVED = 0"
        )
    }

    fun getSavedNotifications():
            List<StoredNotification> {

        return getNotifications(
            "$COLUMN_SAVED = 1"
        )
    }

    private fun getNotifications(
        selection: String?
    ): List<StoredNotification> {

        val notifications =
            mutableListOf<StoredNotification>()

        val cursor =
            readableDatabase.query(

                TABLE_NAME,

                null,

                selection,

                null,

                null,

                null,

                "$COLUMN_TIME DESC"
            )

        cursor.use {

            while (it.moveToNext()) {

                notifications.add(

                    StoredNotification(

                        id =
                            it.getLong(
                                it.getColumnIndexOrThrow(
                                    COLUMN_ID
                                )
                            ),

                        title =
                            it.getString(
                                it.getColumnIndexOrThrow(
                                    COLUMN_TITLE
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
                            ) == 1
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

            TABLE_NAME,

            values,

            "$COLUMN_ID = ?",

            arrayOf(
                id.toString()
            )
        )
    }

    fun deleteNotification(
        id: Long
    ) {

        writableDatabase.delete(

            TABLE_NAME,

            "$COLUMN_ID = ?",

            arrayOf(
                id.toString()
            )
        )
    }

    fun clearAll() {

        writableDatabase.delete(

            TABLE_NAME,

            null,

            null
        )
    }

    fun deleteExpiredNotifications() {

        val twoDaysAgo =

            System.currentTimeMillis() -
                    (48L * 60L * 60L * 1000L)

        writableDatabase.delete(

            TABLE_NAME,

            "$COLUMN_SAVED = 0 AND $COLUMN_TIME < ?",

            arrayOf(
                twoDaysAgo.toString()
            )
        )
    }
}