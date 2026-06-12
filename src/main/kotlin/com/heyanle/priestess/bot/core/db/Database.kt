package com.heyanle.priestess.bot.core.db

import com.heyanle.priestess.bot.core.lifecycle.LifecycleAware

interface AppDatabase : LifecycleAware {
    suspend fun open()
    suspend fun close()
}
