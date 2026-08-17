package com.y.citycapsule.core.checkin

import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.y.citycapsule.core.storage.StorageCodec
import com.y.citycapsule.core.storage.StorageValueType

object CheckInCodec : StorageCodec<CheckInCatalog> {
    override val valueType = StorageValueType.JSON_OBJECT
    override fun encode(v: CheckInCatalog) = JSONObject().apply { put("schemaVersion",1); put("sessionStartedAtEpochMs",v.sessionStartedAtEpochMs); put("checkIns",JSONArray().apply { v.checkIns.forEach { c -> put(JSONObject().apply { put("placeId",c.placeId); put("checkedInAtEpochMs",c.checkedInAtEpochMs); put("method",c.method.wireValue); c.distanceMeters?.let { put("distanceMeters",it) } }) } }) }.toString()
    override fun decode(e: String): CheckInCatalog? { return try { val j=JSONObject(e); if(j.optInt("schemaVersion",-1)!=1)return null; val a=j.optJSONArray("checkIns")?:return null; val list=mutableListOf<CheckIn>(); for(i in 0 until a.length()){val c=a.optJSONObject(i)?:return null; list+=CheckIn(c.optString("placeId").takeIf(String::isNotBlank)?:return null,c.optString("checkedInAtEpochMs").toLongOrNull()?:return null,CheckInMethod.fromWireValue(c.optString("method"))?:return null,if(c.has("distanceMeters"))c.optDouble("distanceMeters") else null)}; CheckInCatalog(sessionStartedAtEpochMs=j.optString("sessionStartedAtEpochMs").toLongOrNull()?:return null,checkIns=list) }catch(_:Throwable){null} }
}
