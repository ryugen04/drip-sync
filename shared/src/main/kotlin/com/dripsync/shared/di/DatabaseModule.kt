package com.dripsync.shared.di

import android.content.Context
import androidx.room.Room
import com.dripsync.shared.data.local.DripSyncDatabase
import com.dripsync.shared.data.local.HydrationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(
        @ApplicationContext context: Context
    ): DripSyncDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            DripSyncDatabase::class.java,
            DripSyncDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    fun provideHydrationDao(database: DripSyncDatabase): HydrationDao {
        return database.hydrationDao()
    }
}
