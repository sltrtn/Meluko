package com.alarm.meluko.di

import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module — a class that teaches Hilt HOW to build objects it can't
 * construct automatically (e.g. third-party singletons like FirebaseAuth).
 *
 * @InstallIn(SingletonComponent::class) → this binding lives for the entire
 * app lifetime. One FirebaseAuth instance, shared everywhere.
 *
 * Why not just call FirebaseAuth.getInstance() inside the ViewModel directly?
 * Because then the ViewModel is tightly coupled to Firebase — you can't test it
 * in isolation. With injection, you swap in a fake for tests.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
}
