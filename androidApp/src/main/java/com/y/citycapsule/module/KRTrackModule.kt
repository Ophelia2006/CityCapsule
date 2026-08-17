package com.y.citycapsule.module

import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import org.json.JSONObject
import java.io.File

class KRTrackModule : KuiklyRenderBaseModule() {
    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        if (callback == null) return null
        if (method == METHOD_READ_CHUNKS) { read(params, callback); return null }
        if (method != METHOD_WRITE_CHUNK) { callback.invoke(response("unsupported")); return null }
        val result = runCatching {
            val body = JSONObject(params ?: "{}")
            val session = body.optString("sessionStartedAt").toLongOrNull() ?: error("invalid session")
            val index = body.optInt("chunkIndex", -1).takeIf { it >= 0 } ?: error("invalid index")
            val points = body.optJSONArray("points") ?: error("missing points")
            require(points.length() in 1..MAX_POINTS)
            val root = File(requireNotNull(activity).filesDir, "tracks/$session").canonicalFile
            root.mkdirs()
            val target = File(root, "chunk_$index.json").canonicalFile
            require(target.parentFile == root)
            val temp = File(root, "chunk_$index.tmp")
            temp.writeText(points.toString(), Charsets.UTF_8)
            require(temp.renameTo(target) || run { temp.copyTo(target, overwrite = true); temp.delete() })
            "file://${target.absolutePath}"
        }
        callback.invoke(result.fold({ response("success", path = it) }, { response("failure", "轨迹分片写入失败。") }))
        return null
    }
    private fun response(status: String, message: String = "", path: String = "") = JSONObject().apply { put("status", status); put("message", message); if (path.isNotEmpty()) put("path", path) }.toString()
    private fun read(params:String?,callback:KuiklyRenderCallback){val result=runCatching{val root=File(requireNotNull(activity).filesDir,"tracks").canonicalFile;val a=JSONObject(params?:"{}").getJSONArray("paths");val points=org.json.JSONArray();for(i in 0 until a.length()){val f=File(java.net.URI(a.getString(i))).canonicalFile;require(f.path.startsWith(root.path+File.separator));val chunk=org.json.JSONArray(f.readText());for(j in 0 until chunk.length())points.put(chunk.getJSONObject(j))};JSONObject().apply{put("status","success");put("points",points)}.toString()};callback.invoke(result.getOrElse{response("failure","轨迹读取失败。")})}
    companion object { const val MODULE_NAME = "CCTrackModule"; private const val METHOD_WRITE_CHUNK = "writeChunk"; private const val METHOD_READ_CHUNKS="readChunks"; private const val MAX_POINTS = 50 }
}
