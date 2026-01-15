package com.example.app_truyen.API;

import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public interface CloudinaryService {
    @Multipart
    @POST("image/upload")
    Call<CloudinaryResponse> uploadImage(
            @Part("upload_preset") RequestBody uploadPreset,
            @Part MultipartBody.Part file
    );
}

