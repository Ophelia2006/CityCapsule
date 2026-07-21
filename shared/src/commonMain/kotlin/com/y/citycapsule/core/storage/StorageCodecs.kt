package com.y.citycapsule.core.storage

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

object StorageCodecs {
    val STRING: StorageCodec<String> = object : StorageCodec<String> {
        override val valueType: StorageValueType = StorageValueType.STRING

        override fun encode(value: String): String = value

        override fun decode(encoded: String): String = encoded
    }

    val BOOLEAN: StorageCodec<Boolean> = object : StorageCodec<Boolean> {
        override val valueType: StorageValueType = StorageValueType.BOOLEAN

        override fun encode(value: Boolean): String = value.toString()

        override fun decode(encoded: String): Boolean? = when (encoded) {
            "true" -> true
            "false" -> false
            else -> null
        }
    }

    val LONG: StorageCodec<Long> = object : StorageCodec<Long> {
        override val valueType: StorageValueType = StorageValueType.LONG

        override fun encode(value: Long): String = value.toString()

        override fun decode(encoded: String): Long? = encoded.toLongOrNull()
    }

    val DOUBLE: StorageCodec<Double> = object : StorageCodec<Double> {
        override val valueType: StorageValueType = StorageValueType.DOUBLE

        override fun encode(value: Double): String = value.toString()

        override fun decode(encoded: String): Double? = encoded.toDoubleOrNull()?.takeIf {
            it.isFinite()
        }
    }

    val JSON_OBJECT: StorageCodec<JSONObject> = object : StorageCodec<JSONObject> {
        override val valueType: StorageValueType = StorageValueType.JSON_OBJECT

        override fun encode(value: JSONObject): String = value.toString()

        override fun decode(encoded: String): JSONObject? = try {
            JSONObject(encoded)
        } catch (_: Throwable) {
            null
        }
    }
}

