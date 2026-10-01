package com.hasu.livetv

import android.app.Application
import com.hasu.livetv.data.DemoRepository
import com.hasu.livetv.data.LiveTvRepository

class HasuLiveTvApplication : Application() {
    lateinit var repo: LiveTvRepository
        private set
    override fun onCreate() { super.onCreate(); repo = DemoRepository() }
}
