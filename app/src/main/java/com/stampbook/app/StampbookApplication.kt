package com.stampbook.app

import android.app.Application
import android.content.Context
import com.stampbook.app.data.StampbookRepository
import com.stampbook.app.data.local.StampbookDatabase

/**
 * Hand-rolled service locator. The graph is two DAOs and a repository; a DI
 * framework would be more machinery than the app has moving parts.
 */
class AppContainer(context: Context) {
    private val database = StampbookDatabase.get(context)
    val repository = StampbookRepository(database.tripDao(), database.stampDao())
}

class StampbookApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
