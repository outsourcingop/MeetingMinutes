package com.optoma.meeting.network;

public class AzureOpenAIServiceHelper {
    public static final String BASE_URL =
            "https://devop.openai.azure.com/openai/deployments/devp/";

    private static volatile AzureOpenAIService instance;
    private static final Object lock = new Object();

    // Make the constructor private to prevent direct instantiation
    private AzureOpenAIServiceHelper() {
    }

    public static AzureOpenAIService getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = NetworkServiceHelper.generateRetrofit(BASE_URL).create(AzureOpenAIService.class);
                }
            }
        }
        return instance;
    }
}
