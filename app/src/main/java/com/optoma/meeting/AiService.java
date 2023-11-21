package com.optoma.meeting;

import static com.optoma.meeting.AiServiceProxy.KEY_AUDIO_FILE_PATH;
import static com.optoma.meeting.AiServiceProxy.KEY_CALLBACK;
import static com.optoma.meeting.AiServiceProxy.KEY_LANGUAGE;
import static com.optoma.meeting.util.DebugConfig.TAG_MM;
import static com.optoma.meeting.util.DebugConfig.TAG_WITH_CLASS_NAME;
import static com.optoma.meeting.util.FileUtil.deleteCache;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.optoma.meeting.presenter.SaveTextToFilePresenter;
import com.optoma.meeting.presenter.SpeechRecognizerPresenter;
import com.optoma.meeting.presenter.SplitFilePresenter;
import com.optoma.meeting.presenter.SummaryPresenter;
import com.optoma.meeting.presenter.TranscribePresenter;
import com.optoma.meeting.state.ProcessState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AiService extends Service {

    private static final String TAG = TAG_WITH_CLASS_NAME ? "AiService" : TAG_MM;

    private final Executor mExecutors = Executors.newSingleThreadExecutor();

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
            setState(ProcessState.START_SPLIT);
            // Put the heavy things to the background thread.
            mExecutors.execute(() ->
                    mSplitFilePresenter.startSplitFile(params.getString(KEY_AUDIO_FILE_PATH)));
        }

        @Override
        public void startAudioRecognition(Bundle params) {
            Log.d(TAG, "AIDL.Stub#startAudioRecognition params=" + params.size());
            mCurrentLanguage = params.getString(KEY_LANGUAGE);
            setState(ProcessState.START_AUDIO_RECOGNITION);
            mSpeechRecognizerPresenter.startContinuousRecognitionAsync(mCurrentLanguage);
        }

        @Override
        public void stopAudioRecognition(Bundle params) {
            Log.d(TAG, "AIDL.Stub#stopAudioRecognition params=" + params.size());
            mSpeechRecognizerPresenter.stopContinuousRecognitionAsync();
        }

        @Override
        public boolean isAudioRecognizing() {
            return mSpeechRecognizerPresenter.isContinuousRecognition();
        }
    };

    private final LogTextCallback mLogTextCallbackWrapper = new LogTextCallback() {
        @Override
        public void onLogReceived(String text) {
            mAiServiceCallback.onLogReceived(text);
        }
    };

    private final AiServiceCallbackProxy mAiServiceCallback = new AiServiceCallbackProxy();

    private SplitFilePresenter mSplitFilePresenter;
    private TranscribePresenter mTranscribePresenter;
    private SpeechRecognizerPresenter mSpeechRecognizerPresenter;
    private SaveTextToFilePresenter mSaveTextToFilePresenter;
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
                new SplitFilePresenter.SplitFileCallback() {
                    @Override
                    public void onFileSplit(List<String> newFileAbsolutePathList) {
                        setState(ProcessState.END_SPLIT);
                        for (String path : newFileAbsolutePathList) {
                            Log.d(TAG, "new file: " + path + "\n");
                        }
                        // transcribe
                        setState(ProcessState.START_TRANSCRIBE);
                        mTranscribePresenter.uploadAudioAndTranscribe(newFileAbsolutePathList,
                                mCurrentLanguage);
                    }

                    @Override
                    public void onError(String error) {
                        Log.d(TAG, "onFileSplitError# error=" + error);
                        setState(ProcessState.END_SPLIT);
                        setState(ProcessState.IDLE);
                    }
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
                        setState(ProcessState.END_TRANSCRIBE);
                        setState(ProcessState.START_SUMMARY);
                        mSummaryPresenter.processMultipleConversations(mCurrentLanguage,
                                partNumberToTranscriber, timeStamp);
                    }

                    @Override
                    public void onError(String error) {
                        Log.d(TAG, "onTranscribedError# error=" + error);
                        setState(ProcessState.END_TRANSCRIBE);
                        setState(ProcessState.IDLE);
                    }
                });

        mSpeechRecognizerPresenter = new SpeechRecognizerPresenter(this, mLogTextCallbackWrapper,
                new SpeechRecognizerPresenter.SpeechRecognizerCallback() {
                    @Override
                    public void onSpeechRecognitionCompleted(ArrayList<String> texts) {
                        setState(ProcessState.STOP_AUDIO_RECOGNITION);
                        setState(ProcessState.START_TEXT_SAVING);
                        mSaveTextToFilePresenter.saveStringsToFile(texts);
                    }

                    @Override
                    public void onError(String error) {
                        Log.d(TAG, "onSpeechRecognitionError# error=" + error);
                        setState(ProcessState.STOP_AUDIO_RECOGNITION);
                        setState(ProcessState.IDLE);
                    }
                });

        mSaveTextToFilePresenter = new SaveTextToFilePresenter(this, mLogTextCallbackWrapper,
                new SaveTextToFilePresenter.SaveTextToFileCallback() {
                    @Override
                    public void onTextSaved(Map<Integer, String> partNumberToTranscriber,
                            long timeStamp) {
                        Log.d(TAG, "onTextSaved#");
                        setState(ProcessState.END_TEXT_SAVING);
                        setState(ProcessState.START_SUMMARY);
                        mSummaryPresenter.processMultipleConversations(mCurrentLanguage,
                                partNumberToTranscriber, timeStamp);
                    }

                    @Override
                    public void onError(String error) {
                        Log.d(TAG, "onTextSavedError# error=" + error);
                        setState(ProcessState.END_TEXT_SAVING);
                        setState(ProcessState.IDLE);
                    }
                });

        mSummaryPresenter = new SummaryPresenter(this, mLogTextCallbackWrapper,
                new SummaryPresenter.SummaryCallback() {
                    @Override
                    public void onSummarized() {
                        Log.d(TAG, "onSummarized#");
                        setState(ProcessState.END_SUMMARY);
                        setState(ProcessState.IDLE);
                    }

                    @Override
                    public void onError(String error) {
                        Log.d(TAG, "onSummarizedError# error=" + error);
                        setState(ProcessState.END_SUMMARY);
                        setState(ProcessState.IDLE);
                    }
                });
    }

    private void setState(ProcessState state) {
        mAiServiceCallback.onStateChanged(state.name());
        if (state == ProcessState.IDLE) {
            deleteCache(this);
        }
    }

    private void destroyPresenter() {
        mSplitFilePresenter.destroy();
        mTranscribePresenter.destroy();
        mSpeechRecognizerPresenter.destroy();
        mSaveTextToFilePresenter.destroy();
        mSummaryPresenter.destroy();
    }
}
