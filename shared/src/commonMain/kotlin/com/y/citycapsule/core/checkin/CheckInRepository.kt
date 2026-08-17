package com.y.citycapsule.core.checkin

import com.tencent.kuikly.core.datetime.DateTime
import com.y.citycapsule.core.storage.*

class CheckInRepository(private val storage: KeyValueStore) {
    fun get(callback: StorageCallback<CheckInCatalog>) = storage.get(AppStorageKeys.Roaming.CHECK_INS, callback)
    fun prepare(session: Long, callback: StorageCallback<CheckInCatalog>) = get { r -> val v=(r as? StorageResult.Success)?.value?.takeIf{it.sessionStartedAtEpochMs==session}?:CheckInCatalog(sessionStartedAtEpochMs=session); storage.put(AppStorageKeys.Roaming.CHECK_INS,v){callback(if(it is StorageResult.Success)StorageResult.Success(v) else failure())} }
    fun add(session: Long, placeId: String, method: CheckInMethod, distance: Double?, callback: StorageCallback<CheckInCatalog>) = prepare(session) { r -> if(r !is StorageResult.Success)return@prepare callback(failure()); val c=r.value; if(c.checkIns.any{it.placeId==placeId})return@prepare callback(StorageResult.Success(c)); val v=c.copy(checkIns=c.checkIns+CheckIn(placeId,DateTime.currentTimestamp(),method,distance)); storage.put(AppStorageKeys.Roaming.CHECK_INS,v){callback(if(it is StorageResult.Success)StorageResult.Success(v) else failure())} }
    private fun failure()=StorageResult.Failure(StorageError(StorageErrorCode.INVALID_REQUEST,"Check-in write failed."))
}
