package com.jeepchief.photomemorial.model.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jeepchief.photomemorial.util.Converters

@Database(entities = [PhotoEntity::class], version = 1, exportSchema = false)
//@TypeConverters(Converters::class)
@TypeConverters(Converters::class)
abstract class PmDatabase : RoomDatabase() {
    // todo: Will be implement dao.
    abstract fun getPmDAO() : PmDAO

    companion object {
        private var instance: PmDatabase? = null

        @Synchronized
        fun getInstance(context: Context) : PmDatabase {
            instance?.let {
                return it
            } ?: run {
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    PmDatabase::class.java,
                    "PhotoMemorial.db"
                ).build()
                return instance!!
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // TODO("Not yet implemented")
            }
        }
    }
}