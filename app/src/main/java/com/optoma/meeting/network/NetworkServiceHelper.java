package com.optoma.meeting.network;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class NetworkServiceHelper {

   public static Retrofit generateRetrofit(String baseUrl) {
      return generateRetrofit(baseUrl, generateOkHttpClient());
   }

   public static Retrofit generateRetrofit(String baseUrl, OkHttpClient client) {
      return new Retrofit.Builder()
              .baseUrl(baseUrl)
              .client(client)
              .addConverterFactory(GsonConverterFactory.create())
              .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
              .build();
   }

   private static OkHttpClient generateOkHttpClient() {
      // Create a custom interceptor
      Interceptor customInterceptor = new Interceptor() {
         @Override
         public Response intercept(Chain chain) throws IOException {
            // You can modify the request or response here as needed
            return chain.proceed(chain.request());
         }
      };

      // Create a logging interceptor
      HttpLoggingInterceptor loggingInterceptor =
              new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);

      // Build OkHttpClient with the network interceptors
      return new OkHttpClient.Builder()
              .addNetworkInterceptor(customInterceptor)
              .addNetworkInterceptor(loggingInterceptor)
              .build();
   }

   public static String generateErrorToastContent(ResponseBody errorResponse) {
      int code = -1;
      String message = "";
      try {
         JSONObject root = new JSONObject(errorResponse.string());
         code = root.getInt("code");
         message = root.getString("message");
      } catch (JSONException | IOException e) {
         e.printStackTrace();
      }

      return "code = " + code + ", message = " + message;
   }
}
