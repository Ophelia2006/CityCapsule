package com.y.citycapsule.core.track

import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager

sealed interface TrackFileResult { data class Success(val path: String) : TrackFileResult; data class Failure(val message: String) : TrackFileResult; data object Unsupported : TrackFileResult }
sealed interface TrackReadResult { data class Success(val points: List<TrackPoint>) : TrackReadResult; data class Failure(val message: String) : TrackReadResult; data object Unsupported : TrackReadResult }
interface TrackFileCapability { fun writeChunk(sessionStartedAt: Long, chunkIndex: Int, points: List<TrackPoint>, callback: (TrackFileResult) -> Unit); fun readChunks(paths: List<String>, callback: (TrackReadResult) -> Unit) }
class KuiklyTrackFiles(private val pager: Pager) : TrackFileCapability {
    override fun writeChunk(sessionStartedAt: Long, chunkIndex: Int, points: List<TrackPoint>, callback: (TrackFileResult) -> Unit) {
        val body = JSONObject().apply { put("sessionStartedAt", sessionStartedAt.toString()); put("chunkIndex", chunkIndex); put("points", JSONArray().apply { points.forEach { p -> put(JSONObject().apply { put("latitude", p.latitude); put("longitude", p.longitude); p.accuracyMeters?.let { put("accuracyMeters", it) }; put("recordedAtEpochMs", p.recordedAtEpochMs.toString()) }) } }) }
        try { pager.acquireModule<TrackModule>(MODULE).write(body) { json -> when (json?.optString("status")) { "success" -> callback(TrackFileResult.Success(json.optString("path"))); "unsupported" -> callback(TrackFileResult.Unsupported); else -> callback(TrackFileResult.Failure(json?.optString("message").orEmpty())) } } } catch (_: Throwable) { callback(TrackFileResult.Unsupported) }
    }
    override fun readChunks(paths: List<String>, callback: (TrackReadResult) -> Unit) { val body=JSONObject().apply{put("paths",JSONArray().apply{paths.forEach(::put)})}; try{pager.acquireModule<TrackModule>(MODULE).read(body){j->if(j?.optString("status")!="success")return@read callback(TrackReadResult.Failure(j?.optString("message").orEmpty())); val a=j.optJSONArray("points")?:return@read callback(TrackReadResult.Failure("missing points")); val points=mutableListOf<TrackPoint>(); for(i in 0 until a.length()){val p=a.optJSONObject(i)?:continue; points+=TrackPoint(p.optDouble("latitude"),p.optDouble("longitude"),if(p.has("accuracyMeters"))p.optDouble("accuracyMeters")else null,p.optString("recordedAtEpochMs").toLongOrNull()?:continue)}; callback(TrackReadResult.Success(points))}}catch(_:Throwable){callback(TrackReadResult.Unsupported)} }
    companion object { const val MODULE = "CCTrackModule"; const val METHOD = "writeChunk"; const val READ = "readChunks" }
}
internal class TrackModule : Module() { override fun moduleName() = KuiklyTrackFiles.MODULE; fun write(body: JSONObject, callback: (JSONObject?) -> Unit) = asyncToNativeMethod(KuiklyTrackFiles.METHOD, body, callback); fun read(body:JSONObject,callback:(JSONObject?)->Unit)=asyncToNativeMethod(KuiklyTrackFiles.READ,body,callback) }
