package com.y.citycapsule.platform

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import com.y.citycapsule.module.DataArchiveFileStore
import com.y.citycapsule.module.KRDataArchiveModule
import java.io.File

/** Android Storage Access Framework transport for shared backup requests. */
internal class AndroidArchiveHost(private val activity: AppCompatActivity) {
    private var pendingCallback: KuiklyRenderCallback? = null
    private var pendingExportFile: File? = null

    private val archiveCreator = activity.registerForActivityResult(
        ActivityResultContracts.CreateDocument()
    ) { uri ->
        val callback = pendingCallback ?: return@registerForActivityResult
        val file = pendingExportFile
        pendingCallback = null
        pendingExportFile = null
        if (uri == null || file == null) {
            file?.delete()
            callback.invoke(KRDataArchiveModule.response(KRDataArchiveModule.STATUS_CANCELLED))
        } else {
            DataArchiveFileStore(activity).copyExport(file, uri).fold(
                onSuccess = {
                    callback.invoke(KRDataArchiveModule.response(KRDataArchiveModule.STATUS_SUCCESS) {
                        put("path", uri.toString())
                    })
                },
                onFailure = {
                    callback.invoke(KRDataArchiveModule.response(KRDataArchiveModule.STATUS_FAILURE, "无法写入所选位置。"))
                }
            )
        }
    }

    private val archivePicker = activity.registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val callback = pendingCallback ?: return@registerForActivityResult
        pendingCallback = null
        callback.invoke(
            if (uri == null) KRDataArchiveModule.response(KRDataArchiveModule.STATUS_CANCELLED)
            else DataArchiveFileStore(activity).stageImport(uri)
        )
    }

    fun export(request: String, callback: KuiklyRenderCallback) {
        if (rejectConcurrent(callback)) return
        DataArchiveFileStore(activity).createExport(request).fold(
            onSuccess = { file ->
                pendingCallback = callback
                pendingExportFile = file
                archiveCreator.launch("citycapsule-backup-${System.currentTimeMillis()}.zip")
            },
            onFailure = {
                callback.invoke(KRDataArchiveModule.response(KRDataArchiveModule.STATUS_FAILURE, "无法创建备份文件。"))
            }
        )
    }

    fun select(callback: KuiklyRenderCallback) {
        if (rejectConcurrent(callback)) return
        pendingCallback = callback
        archivePicker.launch(arrayOf("application/zip", "application/octet-stream"))
    }

    fun dispose() {
        pendingCallback = null
        pendingExportFile?.delete()
        pendingExportFile = null
    }

    private fun rejectConcurrent(callback: KuiklyRenderCallback): Boolean {
        if (pendingCallback == null) return false
        callback.invoke(KRDataArchiveModule.response(KRDataArchiveModule.STATUS_FAILURE, "已有文件操作正在进行。"))
        return true
    }
}
