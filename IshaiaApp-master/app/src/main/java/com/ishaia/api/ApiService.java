package com.ishaia.api;


import  okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * Api class for create post request and send it to the server
 * request type is Multipart and the request contain photo and
 * parameters about the song
 */
public interface ApiService {
    @Multipart
    @POST("upload_image")
    Call<ResponseBody> uploadImgWithArgs(@Part("type") RequestBody type,
                                         @Part MultipartBody.Part photo,
                                         @Part("song_speed") RequestBody _songSpeed,
                                         @Part("scale_type") RequestBody _scaleType,
                                         @Part("amount_scale") RequestBody _amountScale);

}
