package com.hasu.livetv

import android.app.Application
import com.hasu.livetv.data.LiveTvRepository
import com.hasu.livetv.data.LocalLiveTvRepository

class HasuLiveTvApplication : Application() {
    lateinit var repo: LiveTvRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repo = LocalLiveTvRepository(
            context = this,
            // Only the Admin edition seeds the initial catalog when the cloud
            // collection is empty. Mobile/TV only consume the shared catalog.
            seedCloudIfEmpty = BuildConfig.EDITION == "admin"
        )
    }
}
