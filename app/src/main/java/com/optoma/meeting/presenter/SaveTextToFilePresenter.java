package com.optoma.meeting.presenter;

import static com.optoma.meeting.util.FileUtil.createMeetingMinutesFile;

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
    private final LogTextCallback mLogTextCallback;

    public SaveTextToFilePresenter(Context context, LogTextCallback callback) {
        this.mContext = context;
        this.mLogTextCallback = callback;
    }

    public void saveStringsToFile(ArrayList<String> textListToSave) {
        File outputFile = createMeetingMinutesFile(mContext, System.currentTimeMillis());

        mCompositeDisposable.add(
                Completable.fromAction(() -> {
                            mLogTextCallback.onLogReceived(
                                    "Start saveStringsToFile, fileName = " + outputFile.getName());

                            try {
                                FileOutputStream outputStream = new FileOutputStream(outputFile);

                                for (String text : textListToSave) {
                                    outputStream.write(text.getBytes());
                                    outputStream.write("\n".getBytes());
                                }

                                mLogTextCallback.onLogReceived(
                                        "End saveStringsToFile, fileName = " + outputFile.getName());
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
