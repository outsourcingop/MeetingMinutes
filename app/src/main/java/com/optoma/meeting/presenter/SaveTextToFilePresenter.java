package com.optoma.meeting.presenter;

import static com.optoma.meeting.util.FileUtil.createMeetingMinutesFile;

import android.content.Context;
import android.util.Log;

import com.optoma.meeting.LogTextCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class SaveTextToFilePresenter extends BasicPresenter {

    public interface SaveTextToFileCallback extends ErrorCallback {
        void onTextSaved(Map<Integer, String> text, long timeStamp);
    }

    private final SaveTextToFileCallback mSaveTextToFileCallback;

    public SaveTextToFilePresenter(Context context, LogTextCallback callback,
            SaveTextToFileCallback saveTextToFileCallback) {
        super(context, callback, saveTextToFileCallback);
        TAG = SaveTextToFilePresenter.class.getSimpleName();
        mSaveTextToFileCallback = saveTextToFileCallback;
    }

    public void saveStringsToFile(ArrayList<String> textListToSave) {
        long timestamp = System.currentTimeMillis();
        File outputFile = createMeetingMinutesFile(mContext, timestamp);

        Map<Integer, String> partNumberToSummary = new ConcurrentHashMap<>();

        mCompositeDisposable.add(
                Completable.fromAction(() -> {
                            try {
                                FileOutputStream outputStream = new FileOutputStream(outputFile);

                                StringBuilder toSummary = new StringBuilder();
                                for (String text : textListToSave) {
                                    outputStream.write(text.getBytes());
                                    outputStream.write("\n".getBytes());
                                    toSummary.append("{").append(text).append("}, ");
                                }
                                outputStream.close();
                                // Only one text file to summarize
                                partNumberToSummary.put(0, toSummary.toString());

                            } catch (IOException e) {
                                String errorLog = "errorMessage: IOException";
                                performError(errorLog);
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(() -> {
                            String saveFileLog = "***** Saved audio recoding to " +
                                    outputFile.getPath() + " *****\n";
                            Log.d(TAG, saveFileLog);
                            mLogTextCallback.onLogReceived(saveFileLog);
                            mSaveTextToFileCallback.onTextSaved(partNumberToSummary, timestamp);
                        })
        );
    }
}
