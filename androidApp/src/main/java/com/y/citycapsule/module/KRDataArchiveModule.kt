package com.y.citycapsule.module

import android.content.Context
import android.net.Uri
import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import com.y.citycapsule.KuiklyHostActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URI
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class KRDataArchiveModule : KuiklyRenderBaseModule() {
    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        callback ?: return null
        val host = activity as? KuiklyHostActivity
        if (host == null) {
            callback.invoke(response(STATUS_UNSUPPORTED, "当前页面不支持本地数据文件。"))
            return null
        }
        val store = DataArchiveFileStore(host)
        when (method) {
            METHOD_USAGE -> callback.invoke(store.usage())
            METHOD_CLEAR_TEMP -> callback.invoke(store.clearTemporaryFiles())
            METHOD_EXPORT -> host.exportDataArchive(params.orEmpty(), callback)
            METHOD_SELECT_IMPORT -> host.selectDataArchive(callback)
            METHOD_CREATE_RECOVERY -> callback.invoke(store.createRecovery(params.orEmpty()))
            METHOD_COMMIT_MEDIA -> callback.invoke(store.commitMedia(params.orEmpty()))
            METHOD_DISCARD_IMPORT -> callback.invoke(store.discard(params.orEmpty()))
            else -> callback.invoke(response(STATUS_UNSUPPORTED, "当前数据文件方法不受支持。"))
        }
        return null
    }

    companion object {
        const val MODULE_NAME = "CCDataArchiveModule"
        const val METHOD_USAGE = "storageUsage"
        const val METHOD_CLEAR_TEMP = "clearTemporaryFiles"
        const val METHOD_EXPORT = "exportArchive"
        const val METHOD_SELECT_IMPORT = "selectImport"
        const val METHOD_CREATE_RECOVERY = "createRecovery"
        const val METHOD_COMMIT_MEDIA = "commitImportedMedia"
        const val METHOD_DISCARD_IMPORT = "discardImport"
        const val STATUS_SUCCESS = "success"
        const val STATUS_CANCELLED = "cancelled"
        const val STATUS_FAILURE = "failure"
        const val STATUS_UNSUPPORTED = "unsupported"

        fun response(status: String, message: String = "", block: JSONObject.() -> Unit = {}): String =
            JSONObject().apply {
                put("status", status)
                put("message", message)
                block()
            }.toString()
    }
}

internal class DataArchiveFileStore(private val context: Context) {
    private val stagingRoot = File(context.cacheDir, "data-import")
    private val recoveryRoot = File(context.filesDir, "backups/recovery")
    private val mediaRoot = File(context.filesDir, "images/original")

    fun createExport(request: String): Result<File> = runCatching {
        val output = File(context.cacheDir, "citycapsule-export-${System.currentTimeMillis()}.zip")
        createZip(JSONObject(request), output)
        output
    }

    fun copyExport(file: File, uri: Uri): Result<Unit> = runCatching {
        requireNotNull(context.contentResolver.openOutputStream(uri, "w")).use { output ->
            file.inputStream().use { it.copyTo(output) }
        }
        file.delete()
    }

    fun stageImport(uri: Uri): String {
        val sessionId = UUID.randomUUID().toString()
        val session = File(stagingRoot, sessionId).apply { mkdirs() }
        return runCatching {
            val archive = File(session, "selected.zip")
            requireNotNull(context.contentResolver.openInputStream(uri)).use { input ->
                archive.outputStream().use(input::copyTo)
            }
            unzipSafely(archive, session)
            val payloadFile = File(session, "data/backup.json")
            require(payloadFile.isFile && payloadFile.length() <= MAX_PAYLOAD_BYTES)
            val payload = payloadFile.readText()
            JSONObject(payload) // parse once before crossing the bridge
            KRDataArchiveModule.response(KRDataArchiveModule.STATUS_SUCCESS) {
                put("sessionId", sessionId)
                put("payload", payload)
                put("fileName", queryName(uri) ?: "CityCapsule 备份")
            }
        }.getOrElse {
            session.deleteRecursively()
            KRDataArchiveModule.response(
                KRDataArchiveModule.STATUS_FAILURE,
                "无法读取此备份；文件可能损坏或格式不受支持。"
            )
        }
    }

    fun createRecovery(request: String): String = runCatching {
        recoveryRoot.mkdirs()
        recoveryRoot.listFiles()?.forEach(File::delete)
        val target = File(recoveryRoot, "before-import-${System.currentTimeMillis()}.zip")
        createZip(JSONObject(request), target)
        KRDataArchiveModule.response(KRDataArchiveModule.STATUS_SUCCESS) {
            put("path", target.absolutePath)
        }
    }.getOrElse {
        KRDataArchiveModule.response(
            KRDataArchiveModule.STATUS_FAILURE,
            "无法创建导入前备份，导入已停止。"
        )
    }

    fun commitMedia(request: String): String = runCatching {
        val sessionId = JSONObject(request).getString("sessionId")
        require(SESSION_ID.matches(sessionId))
        val session = File(stagingRoot, sessionId).canonicalFile
        require(session.parentFile == stagingRoot.canonicalFile && session.isDirectory)
        val indexFile = File(session, "media/index.json")
        val index = if (indexFile.isFile) JSONObject(indexFile.readText()) else JSONObject()
        mediaRoot.mkdirs()
        val mapping = JSONObject()
        val created = JSONArray()
        val keys = index.keys()
        while (keys.hasNext()) {
            val oldPath = keys.next()
            val entry = index.optString(oldPath)
            require(entry.startsWith("media/images/") && !entry.contains(".."))
            val source = File(session, entry).canonicalFile
            require(source.path.startsWith(session.path + File.separator) && source.isFile)
            val extension = source.extension.takeIf { it.matches(EXTENSION) } ?: "image"
            val target = File(mediaRoot, "import_${System.currentTimeMillis()}_${created.length()}.$extension")
            source.copyTo(target, overwrite = false)
            val newPath = "file://${target.absolutePath}"
            mapping.put(oldPath, newPath)
            created.put(newPath)
        }
        session.deleteRecursively()
        KRDataArchiveModule.response(KRDataArchiveModule.STATUS_SUCCESS) {
            put("pathMapping", mapping)
            put("createdPaths", created)
        }
    }.getOrElse {
        KRDataArchiveModule.response(
            KRDataArchiveModule.STATUS_FAILURE,
            "照片恢复失败，尚未写入导入数据。"
        )
    }

    fun discard(request: String): String = runCatching {
        val sessionId = JSONObject(request).getString("sessionId")
        require(SESSION_ID.matches(sessionId))
        val session = File(stagingRoot, sessionId).canonicalFile
        require(session.parentFile == stagingRoot.canonicalFile)
        session.deleteRecursively()
        KRDataArchiveModule.response(KRDataArchiveModule.STATUS_SUCCESS)
    }.getOrElse {
        KRDataArchiveModule.response(KRDataArchiveModule.STATUS_FAILURE, "临时导入文件未能清理。")
    }

    fun usage(): String = KRDataArchiveModule.response(KRDataArchiveModule.STATUS_SUCCESS) {
        put("mediaBytes", directoryBytes(mediaRoot).toString())
        put(
            "cacheBytes",
            (
                directoryBytes(stagingRoot) +
                    context.cacheDir.listFiles().orEmpty()
                        .filter { it.name.startsWith("citycapsule-export-") }
                        .sumOf(File::length)
                ).toString()
        )
        put("recoveryBytes", directoryBytes(recoveryRoot).toString())
        put("cacheCount", directoryFiles(stagingRoot).size + context.cacheDir.listFiles().orEmpty().count { it.name.startsWith("citycapsule-export-") })
        put("recoveryCount", directoryFiles(recoveryRoot).size)
    }

    fun clearTemporaryFiles(): String {
        val before = directoryBytes(stagingRoot) +
            context.cacheDir.listFiles().orEmpty()
                .filter { it.name.startsWith("citycapsule-export-") }
                .sumOf(File::length)
        stagingRoot.deleteRecursively()
        context.cacheDir.listFiles().orEmpty()
            .filter { it.name.startsWith("citycapsule-export-") }
            .forEach(File::delete)
        return KRDataArchiveModule.response(KRDataArchiveModule.STATUS_SUCCESS) {
            put("clearedBytes", before.toString())
        }
    }

    private fun createZip(request: JSONObject, output: File) {
        val payload = request.getString("payload")
        require(payload.length <= MAX_PAYLOAD_BYTES)
        val mediaPaths = request.optJSONArray("mediaPaths") ?: JSONArray()
        output.parentFile?.mkdirs()
        ZipOutputStream(output.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("data/backup.json"))
            zip.write(payload.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            val index = JSONObject()
            for (i in 0 until mediaPaths.length()) {
                val oldPath = mediaPaths.optString(i)
                val source = resolveManagedMedia(oldPath) ?: continue
                val entryName = "media/images/${i}_${source.name}"
                index.put(oldPath, entryName)
                zip.putNextEntry(ZipEntry(entryName))
                source.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
            zip.putNextEntry(ZipEntry("media/index.json"))
            zip.write(index.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }

    private fun unzipSafely(archive: File, destination: File) {
        var total = 0L
        var count = 0
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                require(++count <= MAX_ENTRIES && !entry.name.contains(".."))
                val target = File(destination, entry.name).canonicalFile
                require(target.path.startsWith(destination.canonicalPath + File.separator))
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    target.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = zip.read(buffer)
                            if (read < 0) break
                            total += read
                            require(total <= MAX_ARCHIVE_BYTES)
                            output.write(buffer, 0, read)
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    private fun resolveManagedMedia(path: String): File? {
        val uri = runCatching { URI(path) }.getOrNull() ?: return null
        if (uri.scheme != "file") return null
        val file = runCatching { File(uri.path).canonicalFile }.getOrNull() ?: return null
        return file.takeIf { it.parentFile == mediaRoot.canonicalFile && it.isFile }
    }

    private fun queryName(uri: Uri): String? = context.contentResolver.query(
        uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }

    private fun directoryBytes(root: File): Long =
        if (!root.exists()) 0 else root.walkTopDown().filter(File::isFile).sumOf(File::length)

    private fun directoryFiles(root: File): List<File> =
        if (!root.exists()) emptyList() else root.walkTopDown().filter(File::isFile).toList()

    private companion object {
        const val MAX_PAYLOAD_BYTES = 8L * 1024 * 1024
        const val MAX_ARCHIVE_BYTES = 512L * 1024 * 1024
        const val MAX_ENTRIES = 1024
        val SESSION_ID = Regex("[0-9a-f-]{36}")
        val EXTENSION = Regex("[A-Za-z0-9]{1,8}")
    }
}
