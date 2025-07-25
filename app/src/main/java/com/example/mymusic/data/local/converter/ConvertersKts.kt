package com.example.mymusic.data.local.converter

import android.util.Log
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.time.LocalDate

object ConvertersKts {
    private val gson = Gson()

    @TypeConverter
    @JvmStatic
    fun fromList(list: List<String>?): String? =
        if (list.isNullOrEmpty()) null else list.joinToString(",")


    @TypeConverter
    @JvmStatic
    fun toList(value: String?): List<String> =
        if (value.isNullOrBlank()) emptyList() else value.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    @TypeConverter
    @JvmStatic
    fun fromListList(list: List<List<String>>?): String =
        gson.toJson(list)

    @TypeConverter
    @JvmStatic
    fun toListList(data: String?): List<List<String>> {
        Log.d("Converters", "toListList called with data: $data")
        if (data.isNullOrBlank()) return emptyList()
        return try {
            gson.fromJson<List<List<String>>>(data, object : TypeToken<List<List<String>>>() {}.type)
        } catch (_: Exception) {
            // 예전 포맷(콤마 분리된 문자열) 호환
            data.split(",").map { listOf(it.trim(), "") }
        }
    }

    @TypeConverter
    @JvmStatic
    fun fromMap(map: Map<LocalDate, Int>?): String? {
        if (map == null) return null
        val stringKeyMap = map.mapKeys { (k, _) -> k.toString() }
        return gson.toJson(stringKeyMap)
    }

    @TypeConverter
    @JvmStatic
    fun toMap(data: String?): Map<LocalDate, Int> {
        if (data.isNullOrBlank()) return emptyMap()
        val type: Type = object : TypeToken<Map<String, Int>>() {}.type
        val stringKeyMap: Map<String, Int> = gson.fromJson(data, type)
        return stringKeyMap.mapKeys { (k, _) -> LocalDate.parse(k) }
    }

    @TypeConverter
    @JvmStatic
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    @JvmStatic
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)
}
