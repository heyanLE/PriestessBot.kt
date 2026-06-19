package com.heyanle.priestess.bot.core.db

interface AppDatabase {
    suspend fun open()
    suspend fun close()
}
