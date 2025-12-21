package com.dripsync.shared.di

import com.dripsync.shared.data.repository.HydrationRepository
import com.dripsync.shared.data.repository.HydrationRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Singleton
    @Binds
    abstract fun bindHydrationRepository(
        impl: HydrationRepositoryImpl
    ): HydrationRepository
}
