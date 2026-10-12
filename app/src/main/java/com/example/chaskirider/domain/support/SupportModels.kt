package com.example.chaskirider.domain.support

import kotlinx.coroutines.flow.Flow

enum class SupportCategory(val title: String) {
    ORDER("Un pedido"), ACCOUNT("Mi cuenta"), PAYMENTS("Ganancias y pagos"), APP("Uso de la app")
}

enum class SupportTopic(val category: SupportCategory, val title: String, val guidance: String) {
    LOCAL_CLOSED(SupportCategory.ORDER, "El local está cerrado",
        "Verifica que estés en el local indicado. Antes del recojo puedes cancelar desde el pedido, indicando el motivo. Si ya tienes el producto, solicita ayuda; el pedido debe seguir activo."),
    ORDER_NOT_READY(SupportCategory.ORDER, "El pedido no está listo",
        "Al llegar al local, usa Reportar: Pedido no está listo. El pedido seguirá activo mientras esperas. No confirmes el recojo antes de recibir el producto."),
    CUSTOMER_UNREACHABLE(SupportCategory.ORDER, "El cliente no responde",
        "Verifica el punto de entrega y los datos del pedido. Si no puedes entregar, abre un ticket. No confirmes una entrega que no realizaste ni canceles un pedido que ya recogiste."),
    ADDRESS_PROBLEM(SupportCategory.ORDER, "Problema con la dirección",
        "Compara la dirección con el punto de entrega. Describe la diferencia en el ticket. Mantén el pedido activo hasta resolver el problema."),
    ORDER_OTHER(SupportCategory.ORDER, "Otro problema con el pedido",
        "Describe lo ocurrido y en qué paso estás. Si ya recogiste el producto, el pedido debe permanecer activo; abrir un ticket no cancela ni completa la entrega."),
    REGISTRATION(SupportCategory.ACCOUNT, "Registro y documentos",
        "Consulta el estado de tu registro y las observaciones. Puedes corregir los documentos observados cuando tu cuenta permita editar. Soporte no aprueba automáticamente un registro."),
    ACCESS(SupportCategory.ACCOUNT, "Acceso a mi cuenta",
        "Usa el mismo correo de tu cuenta. Si configuraste una contraseña, puedes recuperar el acceso desde la pantalla de ingreso. Nunca compartas tu contraseña en un ticket."),
    PAYMENT(SupportCategory.PAYMENTS, "Tengo una duda sobre mis ganancias",
        "Describe el importe y el pedido al que te refieres, si corresponde. Los pedidos actuales son de prueba y no generan depósitos reales."),
    LOCATION(SupportCategory.APP, "Mi ubicación no se actualiza",
        "Activa la ubicación del dispositivo y revisa el permiso de ubicación de Chaski Rider. Para avanzar un pedido debes estar a menos de 100 m del local o del destino correspondiente."),
    APP_OTHER(SupportCategory.APP, "Otro problema con la app",
        "Describe qué estabas haciendo, qué esperabas y qué ocurrió. No incluyas contraseñas ni datos bancarios completos.")
}

data class SupportOrderContext(val orderId: String, val merchantName: String, val pickupAddress: String,
    val deliveryAddress: String, val pickedUp: Boolean, val problemId: String? = null)

data class SupportDraft(val id: String, val topic: SupportTopic? = null, val body: String = "",
    val order: SupportOrderContext? = null)

enum class SupportTicketStatus { OPEN, CLOSED }
enum class SupportSender { RIDER, SUPPORT }
data class SupportMessage(val id: String, val sender: SupportSender, val body: String,
    val createdAt: Long, val pending: Boolean = false)
data class SupportTicket(val id: String, val topic: SupportTopic, val createdAt: Long,
    val status: SupportTicketStatus = SupportTicketStatus.OPEN, val order: SupportOrderContext? = null,
    val messages: List<SupportMessage> = emptyList(), val pending: Boolean = false)

interface SupportDraftStore {
    suspend fun load(uid: String): SupportDraft?
    suspend fun save(uid: String, draft: SupportDraft)
    suspend fun clear(uid: String, draftId: String)
}

interface SupportRepository {
    fun observe(uid: String): Flow<List<SupportTicket>>
    fun observeMessages(uid: String, ticketId: String): Flow<List<SupportMessage>> = kotlinx.coroutines.flow.flowOf(emptyList())
    suspend fun create(uid: String, draft: SupportDraft): String
    suspend fun reply(uid: String, ticketId: String, messageId: String, body: String)
}

object SupportValidation {
    fun validBody(body: String) = body.trim().length in 10..2000
    fun validReply(body: String) = body.trim().length in 1..2000
}
