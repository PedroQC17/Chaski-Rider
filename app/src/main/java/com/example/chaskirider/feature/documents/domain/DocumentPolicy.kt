package com.example.chaskirider.feature.documents.domain

object DocumentPolicy {
    const val MAX_FILE_BYTES = 10L * 1024 * 1024
    val mimeTypes = arrayOf("application/pdf", "image/jpeg", "image/png")
    val types = listOf("dniFront", "dniBack", "bankStatement", "driverLicense", "soat")

    fun field(docType: String): String = when (docType) {
        "dniFront" -> "dniFrontUrl"
        "dniBack" -> "dniBackUrl"
        "bankStatement" -> "bankStatementUrl"
        "driverLicense" -> "driverLicenseUrl"
        "soat" -> "soatUrl"
        else -> error("Documento desconocido: $docType")
    }
}
