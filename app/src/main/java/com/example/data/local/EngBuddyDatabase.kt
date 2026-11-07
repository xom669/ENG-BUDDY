package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserProfileEntity::class,
        SubjectEntity::class,
        TimetableSlotEntity::class,
        VaultDocumentEntity::class,
        DocumentPageEntity::class,
        AttendanceLogEntity::class,
        OfflineSyncQueueEntity::class,
        CalendarEventEntity::class,
        GmailSenderRuleEntity::class,
        SubjectOverlapPrefEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class EngBuddyDatabase : RoomDatabase() {
    abstract fun dao(): EngBuddyDao

    companion object {
        @Volatile
        private var INSTANCE: EngBuddyDatabase? = null

        fun getInstance(context: Context): EngBuddyDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    EngBuddyDatabase::class.java,
                    "engbuddy_offline_first.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
