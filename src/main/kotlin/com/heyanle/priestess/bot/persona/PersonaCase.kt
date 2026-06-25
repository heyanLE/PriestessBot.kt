package com.heyanle.priestess.bot.persona

class PersonaCase(
    private val controller: PersonaController,
) {
    fun list(workspaceId: String): List<Persona> = controller.list(workspaceId)

    fun get(id: String): Persona? = controller.get(id)

    fun upsert(request: PersonaUpsertRequest): Persona = controller.upsert(request)

    fun delete(id: String): Boolean = controller.delete(id)

    fun resolve(workspaceId: String, agentName: String): Persona? = controller.resolve(workspaceId, agentName)
}
