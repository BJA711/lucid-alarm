package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AlarmEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [AlarmEntity::class], version = 1, exportSchema = false)
abstract class AlarmDatabase : RoomDatabase() {

    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile
        private var INSTANCE: AlarmDatabase? = null

        fun getInstance(context: Context): AlarmDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AlarmDatabase::class.java,
                    "alarm_clock_db"
                )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate with default alarm (7:00 AM weekdays) for great first launch experience
                        CoroutineScope(Dispatchers.IO).launch {
                            getInstance(context).alarmDao().insertAlarm(
                                AlarmEntity(
                                    hour = 7,
                                    minute = 0,
                                    isEnabled = true,
                                    label = "Wake Up",
                                    repeatDays = AlarmEntity.WEEKDAYS_MASK,
                                    ringtoneName = "Gentle Chime",
                                    ringtoneUri = "builtin_gentle",
                                    isVibrate = true,
                                    snoozeDurationMinutes = 10
                                )
                            )
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
