package com.optoma.meeting;

import static com.optoma.meeting.AiServiceProxy.KEY_LANGUAGE;
import static com.optoma.meeting.AiServiceProxy.KEY_TEXT;
import static com.optoma.meeting.util.DebugConfig.TAG_MM;
import static com.optoma.meeting.util.DebugConfig.TAG_WITH_CLASS_NAME;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

public class SpeechRecognizerHelper {

    private static final String TAG = TAG_WITH_CLASS_NAME ? "MainActivity" : TAG_MM;

    private final Context mContext;
    private final AiServiceProxy mAiServiceProxy;
    private final ArrayList<String> mTextResults;
    private final SpeechRecognizer mSpeechRecognizer;

    private String mCurrentLanguage;
    private boolean mAudioRecoding;

    private long mSpeechRecognizerStartListeningTime = 0;
    private boolean mSuccess = false;

    SpeechRecognizerHelper(Context context, AiServiceProxy aiServiceProxy) {
        mContext = context.getApplicationContext();
        mAiServiceProxy = aiServiceProxy;
        mTextResults = new ArrayList<>();
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(mContext);
    }

    @SuppressLint("MissingPermission")
    public void startAudioRecording(String currentLanguage, LogTextCallback callback) {
        Log.d(TAG, "startAudioRecording# currentLanguage=" + currentLanguage);
        if (!SpeechRecognizer.isRecognitionAvailable(mContext)) {
            Log.w(TAG, "Speech recognizer not available");
            return;
        }
        mCurrentLanguage = currentLanguage;
        mTextResults.clear();
        mSpeechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d(TAG, "onReadyForSpeech# params=" + params.size());
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d(TAG, "onBeginningOfSpeech#");
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                Log.d(TAG, "onBufferReceived# buffer=" + buffer.length);
            }

            @Override
            public void onEndOfSpeech() {
                Log.d(TAG, "onEndOfSpeech#");
            }

            @Override
            public void onError(int error) {
                Log.d(TAG, "onError# error=" + error);

                // Sometime onError will get called after onResults so we keep a boolean to ignore error also
                if (mSuccess) {
                    Log.w(TAG, "Already success, ignoring error");
                    return;
                }

                long duration = System.currentTimeMillis() - mSpeechRecognizerStartListeningTime;
                if (duration < 500 && error == SpeechRecognizer.ERROR_NO_MATCH) {
                    Log.d(TAG, "Doesn't seem like the system tried to listen at all. duration = " + duration + "ms. This might be a bug with onError and startListening methods of SpeechRecognizer");
                    Log.d(TAG, "Going to ignore the error");
                    return;
                }
                // -- actual error handing code goes here.
                if (mAudioRecoding) {
                    startAudioRecording(currentLanguage, callback);
                }
            }

            @Override
            public void onResults(Bundle results) {
                Log.d(TAG, "onResults# results=" + results.size());
                mSuccess = true;

                StringBuilder sb = new StringBuilder();
                ArrayList<String> data =
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                // show the results on UI
                for (String s : data) {
                    Log.d(TAG, "result=" + s);
                    sb.append(s);
                }
                callback.onLogReceived(sb.toString());
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                Log.d(TAG, "onPartialResults# partialResults=" + partialResults.size());
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                Log.d(TAG, "onEvent# eventType=" + eventType + ", params=" + params.size());
            }
        });
        mSpeechRecognizerStartListeningTime = System.currentTimeMillis();
        mSpeechRecognizer.startListening(createRecognizerIntent(currentLanguage));
        mAudioRecoding = true;
        mSuccess = false;
    }

    private Intent createRecognizerIntent(String currentLanguage) {
        final Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                mContext.getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1000);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Your Prompt");
        return intent;
    }

    private void startTextProcessing(String currentLanguage, ArrayList<String> text) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_LANGUAGE, currentLanguage);
        bundle.putStringArrayList(KEY_TEXT, text);
        // Send the params to the service
        mAiServiceProxy.startTextProcessing(bundle);
    }

    public boolean isAudioRecoding() {
        return mAudioRecoding;
    }

    public void stopAudioRecording() {
        if (mAudioRecoding) {
            mSpeechRecognizer.stopListening();
        }
        // start the text processing in the service
        startTextProcessing(mCurrentLanguage, mTextResults);
        mTextResults.clear();
        mAudioRecoding = false;
    }

    public void destroySpeechRecognizer() {
        stopAudioRecording();
        mSpeechRecognizer.destroy();
    }
}
