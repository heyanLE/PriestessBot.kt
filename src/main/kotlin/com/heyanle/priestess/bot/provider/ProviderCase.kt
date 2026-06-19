package com.heyanle.priestess.bot.provider

class ProviderCase(
    private val controller: ProviderController,
) {
    fun getByName(name: String): ChatProvider? = controller.getByName(name)
    fun getAll(): List<ChatProvider> = controller.getAll()
    fun getMetaList(): List<ProviderMetadata> = controller.getMetaList()
    suspend fun testAll(): Map<String, Boolean> = controller.testAll()
}
