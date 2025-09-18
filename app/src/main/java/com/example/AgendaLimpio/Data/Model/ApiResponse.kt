package com.example.AgendaLimpio.Data.Model

import com.google.gson.annotations.SerializedName

data class ApiResponse(
    @SerializedName("Table")
    val table: List<UserData>?,

   /* @SerializedName("logo")
    val logotipoUrl: String? *///revisar

)