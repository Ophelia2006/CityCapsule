package com.y.citycapsule.module

import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import com.y.citycapsule.core.storage.StorageProtocol
import com.y.citycapsule.storage.AndroidStorageDispatcher

class KRStorageModule internal constructor(
    private val dispatcher: AndroidStorageDispatcher = AndroidStorageDispatcher()
) : KuiklyRenderBaseModule() {
    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        callback?.invoke(dispatcher.dispatch(method, params).toString())
        return null
    }

    companion object {
        const val MODULE_NAME = StorageProtocol.MODULE_NAME
    }
}

