package com.filmy.app

import android.app.Application
import com.filmy.app.data.Config

/**
 * Application entry. Initializes [Config] exactly once, before any Activity
 * or ViewModel can reach for the API base (SharedPreferences is not safe to
 * read through an uninitialized object).
 */
class FilmyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Config.init(this)
    }
}
