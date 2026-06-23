package com.heyanle.priestess.bot.plugin

import java.io.Closeable
import java.net.URL
import java.net.URLClassLoader

internal data class PluginRuntime(
    val descriptor: PluginDescriptor,
    val classLoader: CloseablePluginClassLoader,
    val instance: Plugin,
    val context: DefaultPluginContext,
)

class CloseablePluginClassLoader(
    urls: Array<URL>,
    parent: ClassLoader,
) : URLClassLoader(urls, parent), Closeable
