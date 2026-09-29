package com.example.chaskirider.data.remote

import com.example.chaskirider.domain.text.TextProvider
import com.example.chaskirider.domain.text.TextKey
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.CancellationException


suspend fun <T> firebaseResult(texts: TextProvider, block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) { throw e
    } catch (e: Exception) {
        val message = when (e) {
            is FirebaseFirestoreException -> when (e.code) {
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    texts.get(TextKey.TEXT_NO_SE_PUDO_CONSULTAR_TU_PERFIL_EN)
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    texts.get(TextKey.TEXT_FIREBASE_NO_PERMITE_CONSULTAR_TU_PERFIL_REVISA)
                else -> texts.get(TextKey.TEXT_NO_SE_PUDO_CARGAR_TU_PERFIL_DE)
            }
            is FirebaseAuthInvalidCredentialsException, is FirebaseAuthInvalidUserException -> texts.get(TextKey.TEXT_CORREO_O_CONTRASENA_INCORRECTOS)
            is FirebaseAuthUserCollisionException -> texts.get(TextKey.TEXT_LA_CREDENCIAL_YA_PERTENECE_A_OTRA_CUENTA)
            is FirebaseAuthRecentLoginRequiredException -> texts.get(TextKey.TEXT_POR_SEGURIDAD_CIERRA_SESION_Y_VUELVE_A)
            is FirebaseAuthWeakPasswordException -> texts.get(TextKey.TEXT_LA_CONTRASENA_NO_CUMPLE_LA_POLITICA_DE)
            is FirebaseFunctionsException -> when (e.code) {
                FirebaseFunctionsException.Code.NOT_FOUND, FirebaseFunctionsException.Code.UNIMPLEMENTED ->
                    texts.get(TextKey.TEXT_FALTA_DESPLEGAR_EL_SERVICIO_DE_REGISTRO_EN)
                FirebaseFunctionsException.Code.UNAVAILABLE -> texts.get(TextKey.TEXT_NO_SE_PUDO_CONECTAR_CON_EL_SERVIDOR)
                else -> texts.resolveError(e.message, TextKey.TEXT_NO_SE_PUDO_GUARDAR_LA_INFORMACION)
            }
            else -> texts.resolveError(e.message, TextKey.TEXT_NO_SE_PUDO_COMPLETAR_LA_OPERACION_REINTENTA)
        }
        Result.failure(IllegalStateException(message, e))
    }
