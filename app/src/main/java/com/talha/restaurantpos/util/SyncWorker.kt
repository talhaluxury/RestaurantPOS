package com.talha.restaurantpos.util

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.talha.restaurantpos.domain.repository.OrderRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs whenever connectivity returns (see MainActivity/App startup wiring) to flush any
 * bills that were created offline. Safe to call repeatedly / concurrently — orders already
 * marked SYNCED are skipped.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val orderRepository: OrderRepository,
    private val sessionManager: SessionManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val restaurantId = sessionManager.currentRestaurantId() ?: return Result.success()
        return orderRepository.syncPendingOrders(restaurantId).fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }
}
