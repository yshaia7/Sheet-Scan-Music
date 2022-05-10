package com.ishaia.global;

import android.app.Application;
import android.util.Log;

import com.google.gson.Gson;
import com.ishaia.api.ApiService;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * That class is api for create the client side
 * here we will build the http request that contain the
 * photo and parameters of the song.
 *
 * we use okhttp3 and retrofit
 */
public class App extends Application {
    private static final String TAG = App.class.getSimpleName();
    public static final String BASE_URL = "https://www.noteappproject.info/";
    private static App INSTANCE;

    private ApiService apiService;

    public static App get() { return INSTANCE; }

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
        /* define the time out of read write and connect via connection with the server*/
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(180, TimeUnit.SECONDS)
                .connectTimeout(180, TimeUnit.SECONDS);

        /** we add Interceptor for the call that send args to the server
        * we will customize the request using it
        */
        httpClient.addInterceptor(chain -> {
            Request original = chain.request();

            // Customize the request
            Request request = original.newBuilder()
                    .method(original.method(), original.body())
                    .build();

            Response response = chain.proceed(request);

            Log.e("apiname", request.url().toString());
            Log.e("RequestArgs: ", request.url().queryParameterNames().toString());
            Log.e("encodedValue", new Gson().toJson(response.request()));

            // Customize or return the response
            return response;
        });

        /** here we build new client and point them to the root of the server
         * root == url that server are listening on it
         */
        OkHttpClient OkHttpClient = httpClient.build();

        Retrofit retrofit = new Retrofit.Builder()
                .client(OkHttpClient)
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        //ApiService
        apiService = retrofit.create(ApiService.class);

    }

    public ApiService getApiService() {
        return apiService;
    }

}