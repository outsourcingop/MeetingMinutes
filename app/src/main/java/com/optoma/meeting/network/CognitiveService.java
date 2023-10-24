package com.optoma.meeting.network;

import com.optoma.meeting.BuildConfig;
import com.optoma.meeting.model.TranscribeBean;
import com.optoma.meeting.model.TranscribeBody;
import com.optoma.meeting.model.TranscribeResult;

import io.reactivex.Single;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Url;

/**
 * Manual:
 * https://learn.microsoft.com/en-us/azure/cognitive-services/speech-service/batch-transcription-get?pivots=rest-api
 */
public interface CognitiveService {
    @Headers({"Content-Type: application/json",
            "Ocp-Apim-Subscription-Key: " + BuildConfig.SPEECH_SUBSCRPTION_KEY})
    @POST("transcriptions")
    Single<Response<TranscribeBean>> createTranscription(@Body TranscribeBody transcriptionBody);

    @Headers("Ocp-Apim-Subscription-Key: " + BuildConfig.SPEECH_SUBSCRPTION_KEY)
    @GET("transcriptions/{id}")
    Single<Response<TranscribeBean>> getTranscriptionStatus(@Path("id") String id);

    @Headers("Ocp-Apim-Subscription-Key: " + BuildConfig.SPEECH_SUBSCRPTION_KEY)
    @GET("transcriptions/{files}/files")
    Single<Response<TranscribeBean>> getTranscriptionFiles(@Path("files") String files);

    @GET
    Single<Response<TranscribeResult>> getTranscriptionFilesFromUrl(@Url String url);
}
