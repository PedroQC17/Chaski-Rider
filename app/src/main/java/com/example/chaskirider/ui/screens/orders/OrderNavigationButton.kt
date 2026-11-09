package com.example.chaskirider.ui.screens.orders

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import com.example.chaskirider.R
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.chaskirider.domain.orders.OfferSnapshot
import com.example.chaskirider.domain.orders.RoutePoint

@Composable
internal fun OrderNavigationButton(snapshot: OfferSnapshot?, enabled: Boolean) {
    if (snapshot?.batch == null || snapshot.isCompleted) return
    val target = if (snapshot.pickedUp) snapshot.batch.stops.firstOrNull()?.point else snapshot.merchant
    if (target == null || !target.latitude.isFinite() || !target.longitude.isFinite() ||
        target.latitude !in -90.0..90.0 || target.longitude !in -180.0..180.0) return
    val context = LocalContext.current
    var failed by remember(target) { mutableStateOf(false) }
    FloatingActionButton(onClick = {
        if (enabled) {
            failed = !openOrderNavigation(context, target)
            if (failed) android.widget.Toast.makeText(context, "No se pudo abrir la navegación", android.widget.Toast.LENGTH_SHORT).show()
        }
    }) {
        Icon(painterResource(R.drawable.ic_navigation), contentDescription = if (snapshot.pickedUp) "Navegar al cliente" else "Navegar al local")
    }
}

internal fun orderNavigationIntent(target: RoutePoint) =
    Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${target.latitude},${target.longitude}"))
        .setPackage("com.google.android.apps.maps")

private fun openOrderNavigation(context: Context, target: RoutePoint): Boolean {
    return try {
        context.startActivity(orderNavigationIntent(target))
        true
    } catch (_: ActivityNotFoundException) {
        try {
            val url = Uri.parse("https://www.google.com/maps/dir/").buildUpon()
                .appendQueryParameter("api", "1")
                .appendQueryParameter("destination", "${target.latitude},${target.longitude}")
                .appendQueryParameter("dir_action", "navigate").build()
            context.startActivity(Intent(Intent.ACTION_VIEW, url))
            true
        } catch (_: ActivityNotFoundException) { false }
        catch (_: SecurityException) { false }
    } catch (_: SecurityException) { false }
}
