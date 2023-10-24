package com.optoma.meeting.network;

import com.optoma.meeting.model.azureopenai.chat.ChatRequest;
import com.optoma.meeting.model.azureopenai.chat.ChatResponse;

import io.reactivex.Single;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface AzureOpenAIService {
 @POST("chat/completions?api-version=2023-07-01-preview")
 Single<Response<ChatResponse>> chatAzureOpenAI(
         @Header("api-key") String apiKey,
         @Header("Content-Type") String contentType,
         @Body ChatRequest chatRequest);
}