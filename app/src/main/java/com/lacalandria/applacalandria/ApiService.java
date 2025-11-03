package com.lacalandria.applacalandria;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface ApiService {
    @Headers("Content-Type: application/json")
    @POST("save_control_cerco.php")
    Call<ApiResponse> sendControlCerco(@Body ControlCercoRecord record);

    // Nuevo: obtener configuración de alarma desde servidor
    @GET("get_alarm.php")
    Call<ApiResponse> getAlarmConfig();
}

