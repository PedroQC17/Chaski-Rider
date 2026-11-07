package com.example.chaskirider.data.orders

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.chaskirider.domain.orders.OrderSyncRepository
import com.example.chaskirider.domain.orders.OrderSyncState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class RoomOrderSyncRepository(
    private val context: Context,
    private val dao: DemoOrderDao,
    private val connectivity: DeviceConnectivity
) : OrderSyncRepository {
    override fun observe(uid: String) = combine(dao.observePendingCount(uid),
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow("order-sync-$uid"), connectivity.online
    ) { count, work, online ->
        val active = work.any { it.state in listOf(WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED) }
        OrderSyncState(online, count, work.any { it.state == WorkInfo.State.RUNNING },
            count > 0 && !active && work.any { it.state == WorkInfo.State.FAILED })
    }.distinctUntilChanged()

    override fun retry(uid: String) {
        check(FirebaseAuth.getInstance().currentUser?.uid == uid)
        OrderSyncWorker.schedule(context, uid, force = true)
    }
}
