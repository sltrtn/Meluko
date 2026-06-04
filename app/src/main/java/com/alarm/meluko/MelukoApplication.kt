package com.alarm.meluko

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * @HiltAndroidApp: Tells Hilt to generate the base component for the whole app.
 * Every ViewModel and Repository we build will ultimately trace back to this.
 * Must be declared in AndroidManifest.xml as android:name=".MelukoApplication"
 */
@HiltAndroidApp
class MelukoApplication : Application()
