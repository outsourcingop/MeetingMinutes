package com.optoma.meeting;

import static com.optoma.meeting.util.DebugConfig.TAG_MM;
import static com.optoma.meeting.util.DebugConfig.TAG_WITH_CLASS_NAME;

import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;

public class AiServiceCallbackProxy extends IAiServiceCallback.Stub {

    private static final String TAG = TAG_WITH_CLASS_NAME ? AiServiceCallbackProxy.class.getSimpleName() : TAG_MM;

    private IAiServiceCallback mAiServiceCallback;


    public void setProxy(@Nullable IAiServiceCallback aiService) {
        if (aiService != null) {
            mAiServiceCallback = aiService;
        } else {
            mAiServiceCallback = new DefaultAiServiceCallbackProxy();
        }
    }

    @Override
    public void onStateChanged(String state) {
        try {
            mAiServiceCallback.onStateChanged(state);
        } catch (RemoteException e) {
            Log.w(TAG, "run startAudioProcessing() but " + e);
        }
    }

    @Override
    public void onLogReceived(String text) {
        try {
            mAiServiceCallback.onLogReceived(text);
        } catch (RemoteException e) {
            Log.w(TAG, "run onLogReceived() but " + e);
        }
    }

    @Override
    public void onLiveCaptionReceived(String text) {
        try {
            mAiServiceCallback.onLiveCaptionReceived(text);
        } catch (RemoteException e) {
            Log.w(TAG, "run onLiveCaptionReceived() but " + e);
        }
    }

    @Override
    public void onSummaryAndActionsReceived(String summary) {
        try {
            mAiServiceCallback.onSummaryAndActionsReceived(summary);
        } catch (RemoteException e) {
            Log.w(TAG, "run onSummaryAndActionsReceived() but " + e);
        }
    }

    private static class DefaultAiServiceCallbackProxy extends IAiServiceCallback.Stub {
        @Override
        public void onStateChanged(String state) {
        }

        @Override
        public void onLogReceived(String text) {
        }

        @Override
        public void onLiveCaptionReceived(String text) {
        }

        @Override
        public void onSummaryAndActionsReceived(String summary) {
        }
    }
}
