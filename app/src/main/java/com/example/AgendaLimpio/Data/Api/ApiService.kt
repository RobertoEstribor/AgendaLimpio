package com.example.AgendaLimpio.Data.Api

import com.example.AgendaLimpio.Data.Model.ApiResponse
import com.example.AgendaLimpio.Data.Model.ApiResponsePedidos
import com.example.AgendaLimpio.Data.Model.ApiResponseTrabajos
import com.example.AgendaLimpio.Data.Model.PedidoActualizadoRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.Response

interface ApiService {

    // Cambiamos Response<String> por Response<ApiResponse>
    @GET("api/empresa/{company_code}")
    suspend fun getCompanyData(
        @Path("company_code") companyCode: String
    ): Response<String>

    // Cambiamos Response<String> por Response<ApiResponsePedidos>
    @GET("api/pedidos/{usuario}/{nempresa}/{fechaSincro}")
    suspend fun getPedidos(
        @Path("usuario") usuario: String,
        @Path("nempresa") nempresa: String,
        @Path("fechaSincro") fechaSincro: String
    ): Response<String>

    // Cambiamos Response<String> por Response<ApiResponseTrabajos>
    @GET("api/trabajos/{usuario}/{nempresa}")
    suspend fun getTrabajos(
        @Path("usuario") usuario: String,
        @Path("nempresa") nempresa: String
    ): Response<String>

    @POST("api/pedidos/actualizarCompleto")
    suspend fun actualizarPedidoCompleto(@Body pedido: PedidoActualizadoRequest): Response<ResponseBody>

    @Multipart
    @POST("api/pedidos/subirFoto")
    suspend fun subirFoto(
        @Part("pedidoId") pedidoId: RequestBody,
        @Part("tipoFoto") tipoFoto: RequestBody,
        @Part("codEmpresa") codEmpresa: RequestBody,
        @Part foto: MultipartBody.Part
    ): Response<Unit>
}