package com.example.chaskirider.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class DeviceConnectivity(context: Context) {
    private val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java)
    fun isOnline(): Boolean = manager.getNetworkCapabilities(manager.activeNetwork)?.let {
        it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    } == true

    val online = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(isOnline()) }
            override fun onLost(network: Network) { trySend(isOnline()) }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) { trySend(isOnline()) }
        }
        manager.registerDefaultNetworkCallback(callback)
        trySend(isOnline())
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
