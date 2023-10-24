package com.optoma.meeting.presenter;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.optoma.meeting.LogTextCallback;
import com.optoma.meeting.MainActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class SaveTextToFilePresenter extends BasicPresenter {
    private static final String TAG = SaveTextToFilePresenter.class.getSimpleName();

    private final Context mContext;

    private LogTextCallback mLogTextCallback;
    public SaveTextToFilePresenter(Context context, LogTextCallback callback) {
        this.mContext = context;
        this.mLogTextCallback = callback;
    }

    public void saveStringsToFile(ArrayList<String> textListToSave) {
        mCompositeDisposable.add(
                Completable.fromAction(() -> {
                            File downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                            long timestamp = System.currentTimeMillis();

                            String fileName = "Meeting_minutes_" + timestamp + ".txt";
                            mLogTextCallback.onLogReceived("Start saveStringsToFile, fileName = " + fileName);
                            File outputFile = new File(downloadsDirectory, fileName);

                            try {
                                FileOutputStream outputStream = new FileOutputStream(outputFile);

                                for (String text : textListToSave) {
                                    outputStream.write(text.getBytes());
                                    outputStream.write("\n".getBytes());
                                }

                                mLogTextCallback.onLogReceived("End saveStringsToFile, fileName = " + fileName);
                                outputStream.close();

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(() -> Log.d(TAG, "Saved meeting minutes to file"))
        );
    }
}
