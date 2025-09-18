package com.example.AgendaLimpio.Data.Api

import android.util.Log
import com.example.AgendaLimpio.BuildConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Date

object ApiClient {

    private const val BASE_URL = BuildConfig.BASE_URL

    private val dateDeserializer = JsonDeserializer { json, _, _ ->
        val dateString = json?.asString
        if (dateString.isNullOrBlank()) {
            return@JsonDeserializer null
        }

        val dateTimeFormatters = listOf(
            DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
        )

        val dateFormatters = listOf(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
        )

        for (formatter in dateTimeFormatters) {
            try {
                val localDateTime = LocalDateTime.parse(dateString, formatter)
                return@JsonDeserializer Date.from(
                    localDateTime.atZone(ZoneId.systemDefault()).toInstant()
                )
            } catch (e: DateTimeParseException) {
                // Continue
            }
        }

        for (formatter in dateFormatters) {
            try {
                val localDate = LocalDate.parse(dateString, formatter)
                return@JsonDeserializer Date.from(
                    localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                )
            } catch (e: DateTimeParseException) {
                // Continue
            }
        }

        Log.e("DateDeserializer", "Could not parse date: '$dateString'")
        return@JsonDeserializer null
    }

    internal val customGson: Gson = GsonBuilder()
        .registerTypeAdapter(Date::class.java, dateDeserializer)
        .create()

    private val retrofit: Retrofit by lazy {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        val httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor(logging)

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(customGson))
            .client(httpClient.build())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}