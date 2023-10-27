package com.optoma.meeting;

import static com.optoma.meeting.AiServiceProxy.KEY_AUDIO_FILE_PATH;
import static com.optoma.meeting.AiServiceProxy.KEY_CALLBACK;
import static com.optoma.meeting.AiServiceProxy.KEY_LANGUAGE;
import static com.optoma.meeting.AiServiceProxy.KEY_TEXT;
import static com.optoma.meeting.util.DebugConfig.TAG_MM;
import static com.optoma.meeting.util.DebugConfig.TAG_WITH_CLASS_NAME;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.optoma.meeting.presenter.SaveTextToFilePresenter;
import com.optoma.meeting.presenter.SplitFilePresenter;
import com.optoma.meeting.presenter.SummaryPresenter;
import com.optoma.meeting.presenter.TranscribePresenter;
import com.optoma.meeting.state.ProcessState;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AiService extends Service {

    private static final String TAG = TAG_WITH_CLASS_NAME ? "AiService" : TAG_MM;

    private final Executor mExecutors = Executors.newFixedThreadPool(3);

    private final IAiService.Stub mAiService = new IAiService.Stub() {
        @Override
        public void initialize(Bundle params) {
            Log.d(TAG, "AIDL.Stub#initialize params=" + params.size());
            mAiServiceCallback.setProxy((IAiServiceCallback) params.getBinder(KEY_CALLBACK));
        }

        @Override
        public void startAudioProcessing(Bundle params) {
            Log.d(TAG, "AIDL.Stub#startAudioProcessing params=" + params.size());
            mCurrentLanguage = params.getString(KEY_LANGUAGE);
            mAiServiceCallback.onStateChanged(ProcessState.START_SPLIT.name());
            // Put the heavy things to the background thread.
            mExecutors.execute(() ->
                    mSplitFilePresenter.startSplitFile(params.getString(KEY_AUDIO_FILE_PATH)));
        }

        @Override
        public void startTextProcessing(Bundle params) {
            Log.d(TAG, "AIDL.Stub#startTextProcessing params=" + params.size());
            mCurrentLanguage = params.getString(KEY_LANGUAGE);
            ArrayList<String> textListToSave = params.getStringArrayList(KEY_TEXT);
            mAiServiceCallback.onStateChanged(ProcessState.START_TEXT_SAVING.name());
            // Put the heavy things to the background thread.
            mExecutors.execute(() ->
                    mSaveTextToFilePresenter.saveStringsToFile(textListToSave));
        }
    };

    private final LogTextCallback mLogTextCallbackWrapper = new LogTextCallback() {
        @Override
        public void onLogReceived(String text) {
            mAiServiceCallback.onLogReceived(text);
        }
    };

    private final AiServiceCallbackProxy mAiServiceCallback = new AiServiceCallbackProxy();

    private SaveTextToFilePresenter mSaveTextToFilePresenter;
    private SplitFilePresenter mSplitFilePresenter;
    private TranscribePresenter mTranscribePresenter;
    private SummaryPresenter mSummaryPresenter;

    private String mCurrentLanguage;

    @Override
    public void onCreate() {
        super.onCreate();
        setupPresenter();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mAiService;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        destroyPresenter();
    }

    private void setupPresenter() {
        mSplitFilePresenter = new SplitFilePresenter(this, mLogTextCallbackWrapper,
                newFileAbsolutePathList -> {
                    mAiServiceCallback.onStateChanged(ProcessState.END_SPLIT.name());
                    for (String path : newFileAbsolutePathList) {
                        mLogTextCallbackWrapper.onLogReceived("new file: " + path + "\n");
                    }
                    // transcribe
                    mAiServiceCallback.onStateChanged(ProcessState.START_TRANSCRIBE.name());
                    mTranscribePresenter.uploadAudioAndTranscribe(newFileAbsolutePathList,
                            mCurrentLanguage);
                });

        mTranscribePresenter = new TranscribePresenter(this, mLogTextCallbackWrapper,
                new TranscribePresenter.TranscribeCallback() {
                    @Override
                    public void onTranscribed(String transcribeResult, long timeStamp) {
                        Log.d(TAG, "onTranscribed -> getAndStoreSummary");
                    }

                    @Override
                    public void onAllPartsTranscribed(Map<Integer, String> partNumberToTranscriber,
                            long timeStamp) {
                        Log.d(TAG, "onAllPartsTranscribed -> getAndStoreSummary");
                        mAiServiceCallback.onStateChanged(ProcessState.END_TRANSCRIBE.name());
                        mAiServiceCallback.onStateChanged(ProcessState.START_SUMMARY.name());
                        mSummaryPresenter.processMultipleConversations(mCurrentLanguage,
                                partNumberToTranscriber, timeStamp);
                    }

                    @Override
                    public void onError(String error) {
                        Log.d(TAG, "onError# error=" + error);
                        mAiServiceCallback.onStateChanged(ProcessState.END_TRANSCRIBE.name());
                        mAiServiceCallback.onStateChanged(ProcessState.IDLE.name());
                    }
                });

        mSummaryPresenter = new SummaryPresenter(this, mLogTextCallbackWrapper,
                new SummaryPresenter.SummaryCallback() {
                    @Override
                    public void onSummarized() {
                        Log.d(TAG, "onSummarized#");
                        mAiServiceCallback.onStateChanged(ProcessState.END_SUMMARY.name());
                        mAiServiceCallback.onStateChanged(ProcessState.IDLE.name());
                    }

                    @Override
                    public void onError(String error) {
                        Log.d(TAG, "onError# error=" + error);
                        mAiServiceCallback.onStateChanged(ProcessState.END_SUMMARY.name());
                        mAiServiceCallback.onStateChanged(ProcessState.IDLE.name());
                    }
                });

        mSaveTextToFilePresenter = new SaveTextToFilePresenter(this, mLogTextCallbackWrapper,
                (Map<Integer, String> partNumberToTranscriber, long timeStamp) -> {
                    Log.d(TAG, "onTextSaved#");
                    mAiServiceCallback.onStateChanged(ProcessState.END_TEXT_SAVING.name());
                    mSummaryPresenter.processMultipleConversations(mCurrentLanguage,
                            partNumberToTranscriber, timeStamp);
                });
    }

    private void destroyPresenter() {
        mSplitFilePresenter.destroy();
        mTranscribePresenter.destroy();
        mSummaryPresenter.destroy();
        mSaveTextToFilePresenter.destroy();
    }
}
