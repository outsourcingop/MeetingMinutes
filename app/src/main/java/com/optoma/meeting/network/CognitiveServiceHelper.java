package com.optoma.meeting.network;

public class CognitiveServiceHelper {
    public static final String BASE_URL =
            "https://eastasia.api.cognitive.microsoft.com/speechtotext/v3.1/";
    private static volatile CognitiveService instance;
    private static final Object lock = new Object();

    // Make the constructor private to prevent direct instantiation
    private CognitiveServiceHelper() {
    }

    public static CognitiveService getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = NetworkServiceHelper.generateRetrofit(BASE_URL).create(CognitiveService.class);
                }
            }
        }
        return instance;
    }
}
