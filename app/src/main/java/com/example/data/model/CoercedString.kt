package com.example.data.model

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

data class CoercedString(val value: String) {
    override fun toString(): String = value
}

class CoercedStringAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): CoercedString? {
        if (reader.peek() == JsonReader.Token.NULL) {
            return reader.nextNull()
        }
        val strValue = try {
            val peekedToken = reader.peek()
            if (peekedToken == JsonReader.Token.BEGIN_OBJECT || peekedToken == JsonReader.Token.BEGIN_ARRAY) {
                reader.skipValue()
                null
            } else {
                val value = reader.nextString()
                if (value.endsWith(".0")) value.substring(0, value.length - 2) else value
            }
        } catch (e: Exception) {
            reader.skipValue()
            null
        }
        return strValue?.let { CoercedString(it) }
    }

    @ToJson
    fun toJson(writer: JsonWriter, coerced: CoercedString?) {
        if (coerced == null) {
            writer.nullValue()
        } else {
            writer.value(coerced.value)
        }
    }
}
