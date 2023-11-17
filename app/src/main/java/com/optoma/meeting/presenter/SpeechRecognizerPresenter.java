package com.optoma.meeting.presenter;

import static com.optoma.meeting.BuildConfig.SPEECH_SUBSCRPTION_KEY;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.optoma.meeting.LogTextCallback;
import com.optoma.meeting.R;
import com.optoma.meeting.util.MicrophoneStream;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SpeechRecognizerPresenter extends BasicPresenter {

    private interface OnTaskCompletedListener<T> {
        void onCompleted(T taskResult);
    }

    public interface SpeechRecognizerCallback extends ErrorCallback {
        void onSpeechRecognitionCompleted(ArrayList<String> texts);
    }

    private final SpeechRecognizerCallback mSpeechRecognizerCallback;
    private final String mSpeechRegion;
    private final int mMaxWordsInLine;
    private final CopyOnWriteArrayList<String> mCopyOnWriteTexts;
    private final ExecutorService mExecutorService;

    private SpeechConfig mSpeechConfig;
    private MicrophoneStream mMicrophoneStream;
    private AudioConfig mAudioInput;
    private SpeechRecognizer mSpeechRecognizer;

    private boolean mStartContinuousRecognition;


    public SpeechRecognizerPresenter(Context context, LogTextCallback callback,
            SpeechRecognizerCallback speechRecognizerCallback) {
        super(context, callback, speechRecognizerCallback);
        TAG = SaveTextToFilePresenter.class.getSimpleName();
        mSpeechRecognizerCallback = speechRecognizerCallback;
        mSpeechRegion = context.getResources().getString(R.string.speech_region);
        mMaxWordsInLine = context.getResources().getInteger(R.integer.max_words_in_line);
        mCopyOnWriteTexts = new CopyOnWriteArrayList<>();
        mExecutorService = Executors.newCachedThreadPool();
    }

    public void startContinuousRecognitionAsync(String currentLanguage) {
        Log.d(TAG, "startContinuousRecognitionAsync# currentLanguage=" + currentLanguage);
        final ArrayList<String> tmpTexts = new ArrayList<>();
        mCopyOnWriteTexts.clear();

        mSpeechConfig = SpeechConfig.fromSubscription(SPEECH_SUBSCRPTION_KEY, mSpeechRegion);
        mSpeechConfig.setSpeechRecognitionLanguage(currentLanguage);
        mMicrophoneStream = new MicrophoneStream();
        mAudioInput = AudioConfig.fromStreamInput(mMicrophoneStream);
        mSpeechRecognizer = new SpeechRecognizer(mSpeechConfig, mAudioInput);

        mSpeechRecognizer.recognizing.addEventListener((o, speechRecognitionResultEventArgs) -> {
            String s = speechRecognitionResultEventArgs.getResult().getText();
            Log.d(TAG, "Intermediate result received: " + s);
            tmpTexts.add(s);
            String delimiter = decideDelimiter(tmpTexts);
            mLogTextCallback.onLogReceived(TextUtils.join(delimiter, tmpTexts));
//            tmpTexts.remove(tmpTexts.size() - 1);
        });

        mSpeechRecognizer.recognized.addEventListener((o, speechRecognitionResultEventArgs) -> {
            String s = speechRecognitionResultEventArgs.getResult().getText();
            mCopyOnWriteTexts.add(s);
            Log.d(TAG, "Final result received: " + s);
        });

        Future<Void> startRecognitionTask = mSpeechRecognizer.startContinuousRecognitionAsync();
        Log.d(TAG, "startContinuousRecognitionAsync#");
        setOnTaskCompletedListener(startRecognitionTask, result -> {
            Log.d(TAG, "startContinuousRecognitionAsync# Continuous recognition started.");
            setContinuousRecognition(true);
        });
    }

    private void speechRecognitionCompleted() {
        ArrayList<String> texts = new ArrayList<>(mCopyOnWriteTexts);
        // Swipe texts
        mCopyOnWriteTexts.clear();
        // Start next steps
        mSpeechRecognizerCallback.onSpeechRecognitionCompleted(texts);
        setContinuousRecognition(false);
    }

    public void stopContinuousRecognitionAsync() {
        if (!mStartContinuousRecognition || mSpeechRecognizer == null) {
            return;
        }
        Future<Void> task = mSpeechRecognizer.stopContinuousRecognitionAsync();
        Log.d(TAG, "stopContinuousRecognitionAsync#");
        setOnTaskCompletedListener(task, result -> {
            Log.d(TAG, "stopContinuousRecognitionAsync# Continuous recognition stopped.");
            mMicrophoneStream.close();
            mAudioInput.close();
            mSpeechConfig.close();
            mSpeechRecognizer.close();
            // start the text processing in the service
            speechRecognitionCompleted();
        });
    }

    private String decideDelimiter(ArrayList<String> tmpTexts) {
        int length = 0;
        for (String s : tmpTexts) {
            length += s.length();
        }
        return length >= mMaxWordsInLine ? " \n" : " ";
    }

    private void setContinuousRecognition(boolean startContinuousRecognition) {
        mStartContinuousRecognition = startContinuousRecognition;
    }

    public boolean isContinuousRecognition() {
        return mStartContinuousRecognition;
    }

    private <T> void setOnTaskCompletedListener(Future<T> task, OnTaskCompletedListener<T> l) {
        mExecutorService.submit(() -> {
            T result = task.get();
            l.onCompleted(result);
            return null;
        });
    }

    @Override
    public void destroy() {
        super.destroy();
        stopContinuousRecognitionAsync();
    }
}
