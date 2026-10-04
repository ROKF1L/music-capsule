package com.capsule.music

import android.app.Application
import androidx.room.Room
import com.capsule.music.data.AppDatabase

class CapsuleApp : Application() {
    companion object {
        lateinit var database: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "music_capsule.db"
        ).fallbackToDestructiveMigration().build()
    }
}
