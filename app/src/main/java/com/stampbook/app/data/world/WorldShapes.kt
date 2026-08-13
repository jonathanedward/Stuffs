package com.stampbook.app.data.world

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Reads the packed country outlines off disk, once, on a background thread.
 * 238 outlines, about 27,000 points, 68 KB.
 */
object WorldShapes {

    private const val ASSET = "world.sbw"

    private val mutex = Mutex()
    @Volatile private var cached: List<CountryShape>? = null

    suspend fun load(context: Context): List<CountryShape> {
        cached?.let { return it }
        return mutex.withLock {
            cached ?: withContext(Dispatchers.IO) {
                context.applicationContext.assets.open(ASSET).use { WorldAsset.parse(it.readBytes()) }
            }.also { cached = it }
        }
    }
}
