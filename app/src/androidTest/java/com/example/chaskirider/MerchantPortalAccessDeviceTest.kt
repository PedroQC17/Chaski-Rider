package com.example.chaskirider

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Assume.assumeNotNull
import org.junit.Test

class MerchantPortalAccessDeviceTest {
    @Test fun actualCallableRequiresOperatorRole() = runBlocking {
        val user = FirebaseAuth.getInstance().currentUser
        assumeNotNull(user)
        val operator = user!!.getIdToken(true).await().claims["merchantChatOperator"] == true
        try {
            val result = FirebaseFunctions.getInstance("us-central1").getHttpsCallable("merchantChat")
                .call(mapOf("action" to "list")).await().data as Map<*, *>
            assertTrue(operator)
            val orders = result["orders"] as List<*>
            assertTrue(orders.all { row -> (row as Map<*, *>).keys.none { it in listOf("pickup", "destination", "bankInfo") } })
        } catch (error: FirebaseFunctionsException) {
            assertFalse(operator)
            assertEquals(FirebaseFunctionsException.Code.PERMISSION_DENIED, error.code)
            assertTrue(error.message.orEmpty().contains("Esta cuenta no está habilitada"))
        }
    }
}
