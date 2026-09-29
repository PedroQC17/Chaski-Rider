package com.example.chaskirider.data.remote

import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.CancellationException


suspend fun <T> firebaseResult(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) { throw e
    } catch (e: Exception) {
        val message = when (e) {
            is FirebaseFirestoreException -> when (e.code) {
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "No se pudo consultar tu perfil en Firestore. Comprueba la conexión y que Cloud Firestore esté habilitado en Firebase."
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Firebase no permite consultar tu perfil. Revisa que la API de Firestore esté habilitada y las reglas publicadas."
                else -> "No se pudo cargar tu perfil de Firestore. Reintenta."
            }
            is FirebaseAuthInvalidCredentialsException, is FirebaseAuthInvalidUserException -> "Correo o contraseña incorrectos"
            is FirebaseAuthUserCollisionException -> "La credencial ya pertenece a otra cuenta. Ingresa con su método original."
            is FirebaseAuthRecentLoginRequiredException -> "Por seguridad, cierra sesión y vuelve a ingresar antes de configurar la contraseña."
            is FirebaseAuthWeakPasswordException -> "La contraseña no cumple la política de seguridad."
            is FirebaseFunctionsException -> when (e.code) {
                FirebaseFunctionsException.Code.NOT_FOUND, FirebaseFunctionsException.Code.UNIMPLEMENTED ->
                    "Falta desplegar el servicio de registro en Firebase. Tu sesión sigue activa."
                FirebaseFunctionsException.Code.UNAVAILABLE -> "No se pudo conectar con el servidor. Reintenta."
                else -> e.message ?: "No se pudo guardar la información"
            }
            else -> e.message ?: "No se pudo completar la operación. Reintenta."
        }
        Result.failure(IllegalStateException(message, e))
    }
