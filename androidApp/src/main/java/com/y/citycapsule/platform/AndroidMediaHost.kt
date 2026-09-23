package com.y.citycapsule.platform

import android.content.ActivityNotFoundException
import android.webkit.MimeTypeMap
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import com.y.citycapsule.module.KRMediaModule
import java.io.File

/** Android-only picker, camera and managed-file transport for the shared media capability. */
internal class AndroidMediaHost(private val activity: AppCompatActivity) {
    private var pendingImageLimit = 0
    private var pendingImageCallback: KuiklyRenderCallback? = null
    private var pendingCameraCallback: KuiklyRenderCallback? = null
    private var pendingCameraFile: File? = null

    private val imagePicker = activity.registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val callback = pendingImageCallback ?: return@registerForActivityResult
        pendingImageCallback = null
        val selected = uris.take(pendingImageLimit)
        pendingImageLimit = 0
        if (selected.isEmpty()) {
            callback.invoke(KRMediaModule.response(KRMediaModule.STATUS_CANCELLED))
            return@registerForActivityResult
        }
        val createdFiles = mutableListOf<File>()
        runCatching {
            selected.mapIndexed { index, uri ->
                val mime = activity.contentResolver.getType(uri).orEmpty()
                val extension = MimeTypeMap.getSingleton()
                    .getExtensionFromMimeType(mime)
                    ?.takeIf(String::isNotBlank)
                    ?: "jpg"
                val directory = File(activity.filesDir, MANAGED_IMAGE_DIRECTORY).apply {
                    check(mkdirs() || isDirectory)
                }
                val target = File(directory, "capsule_${System.currentTimeMillis()}_${index}.$extension")
                createdFiles += target
                requireNotNull(activity.contentResolver.openInputStream(uri)).use { input ->
                    target.outputStream().use(input::copyTo)
                }
                "file://${target.absolutePath}"
            }
        }.fold(
            onSuccess = { paths -> callback.invoke(KRMediaModule.response(KRMediaModule.STATUS_SUCCESS, paths = paths)) },
            onFailure = {
                createdFiles.forEach(File::delete)
                callback.invoke(KRMediaModule.response(KRMediaModule.STATUS_FAILURE, "无法复制所选照片，请重试。"))
            }
        )
    }

    private val cameraCapture = activity.registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { captured ->
        val callback = pendingCameraCallback ?: return@registerForActivityResult
        val target = pendingCameraFile
        pendingCameraCallback = null
        pendingCameraFile = null
        if (!captured || target == null || !target.isFile || target.length() <= 0L) {
            target?.delete()
            callback.invoke(KRMediaModule.response(KRMediaModule.STATUS_CANCELLED))
        } else {
            callback.invoke(
                KRMediaModule.response(
                    KRMediaModule.STATUS_SUCCESS,
                    paths = listOf("file://${target.absolutePath}")
                )
            )
        }
    }

    fun pickImages(maxCount: Int, callback: KuiklyRenderCallback) {
        if (hasPendingMediaOperation()) {
            callback.invoke(KRMediaModule.response(KRMediaModule.STATUS_FAILURE, "已有照片选择操作正在进行。"))
            return
        }
        // The shared editor owns the business limit. This clamp only protects the platform contract.
        pendingImageLimit = maxCount.coerceIn(MIN_PICK_COUNT, MAX_PLATFORM_PICK_COUNT)
        pendingImageCallback = callback
        imagePicker.launch(arrayOf("image/*"))
    }

    fun captureImage(callback: KuiklyRenderCallback) {
        if (hasPendingMediaOperation()) {
            callback.invoke(KRMediaModule.response(KRMediaModule.STATUS_FAILURE, "已有照片操作正在进行。"))
            return
        }
        val target = runCatching {
            val directory = File(activity.filesDir, MANAGED_IMAGE_DIRECTORY).apply {
                check(mkdirs() || isDirectory)
            }
            File.createTempFile("camera_", ".jpg", directory)
        }.getOrElse {
            callback.invoke(KRMediaModule.response(KRMediaModule.STATUS_FAILURE, "无法创建拍照目标文件。"))
            return
        }
        val uri = runCatching {
            FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", target)
        }.getOrElse {
            target.delete()
            callback.invoke(KRMediaModule.response(KRMediaModule.STATUS_FAILURE, "无法准备系统相机。"))
            return
        }
        pendingCameraFile = target
        pendingCameraCallback = callback
        try {
            cameraCapture.launch(uri)
        } catch (_: ActivityNotFoundException) {
            finishCameraLaunchFailure(KRMediaModule.STATUS_UNSUPPORTED, "当前设备没有可用的系统相机。")
        } catch (_: Throwable) {
            finishCameraLaunchFailure(KRMediaModule.STATUS_FAILURE, "无法打开系统相机。")
        }
    }

    fun dispose() {
        pendingImageLimit = 0
        pendingImageCallback = null
        pendingCameraCallback = null
        pendingCameraFile?.delete()
        pendingCameraFile = null
    }

    private fun hasPendingMediaOperation() = pendingImageCallback != null || pendingCameraCallback != null

    private fun finishCameraLaunchFailure(status: String, message: String) {
        val callback = pendingCameraCallback
        pendingCameraCallback = null
        pendingCameraFile?.delete()
        pendingCameraFile = null
        callback?.invoke(KRMediaModule.response(status, message))
    }

    private companion object {
        const val MANAGED_IMAGE_DIRECTORY = "images/original"
        const val MIN_PICK_COUNT = 1
        const val MAX_PLATFORM_PICK_COUNT = 9
    }
}
