package com.y.citycapsule.core.capsule

import com.y.citycapsule.core.storage.StorageCallback

interface CapsuleRepository {
    fun getPublished(callback: StorageCallback<List<CityCapsule>>)
    fun getPublishedForPlace(placeId: String, callback: StorageCallback<List<CityCapsule>>)
    fun getById(capsuleId: String, callback: StorageCallback<CityCapsule>)
    fun publish(draft: CapsuleDraft, callback: StorageCallback<CityCapsule>)
    fun delete(capsuleId: String, callback: StorageCallback<Unit>)
    fun getDraft(callback: StorageCallback<CapsuleDraft>)
    fun saveDraft(draft: CapsuleDraft, callback: StorageCallback<Unit>)
    fun clearDraft(callback: StorageCallback<Unit>)
}

fun interface CapsuleClock { fun nowEpochMs(): Long }
fun interface CapsuleIdGenerator { fun newId(): String }
