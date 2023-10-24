package com.optoma.meeting.presenter;

import static com.optoma.meeting.util.FileUtil.createNewAudioFilePath;

import android.content.Context;
import android.util.Log;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.SessionState;
import com.optoma.meeting.LogTextCallback;
import com.optoma.meeting.util.AudioUtil;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SplitFilePresenter extends BasicPresenter {
    private static final String TAG = SplitFilePresenter.class.getSimpleName();

    private static final boolean DEBUG = true;
    private final Context mContext;
    private final LogTextCallback mLogTextCallback;

    private SplitFileCallback mSplitFileCallback;

    public interface SplitFileCallback {
        void onFileSplit(List<String> newFileAbsolutePathList);
    }

    public SplitFilePresenter(Context context, LogTextCallback callback,
            SplitFileCallback splitFileCallback) {
        this.mContext = context;
        this.mLogTextCallback = callback;
        this.mSplitFileCallback = splitFileCallback;
    }

    public void startSplitFile(String inputAudioFilePath) {
        // TODO remove sleep and use other to check whether file is ready
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "startSplitFile +++");
        mLogTextCallback.onLogReceived("startSplitFile +++");

        if (DEBUG) {
            Log.d(TAG, "inputAudioFilePath=" + inputAudioFilePath);
            mLogTextCallback.onLogReceived("inputAudioFilePath:" + inputAudioFilePath);
        }

        if (inputAudioFilePath != null) {
            calculateAndSplit(inputAudioFilePath);
        } else {
            Log.e(TAG, "File path error!");
            mLogTextCallback.onLogReceived("File path error!");
        }
    }

    private void calculateAndSplit(String inputFilePath) {
        // 1. get duration
        // 2. calculate split number
        FFmpegKit.executeAsync("-i " + inputFilePath, originalSession -> {
            String output = originalSession.getAllLogsAsString();
            String[] lines = output.split(System.lineSeparator());
            for (String line : lines) {
                if (line.contains("Duration:")) {
                    String durationLine = line.trim();
                    int startIndex = durationLine.indexOf("Duration:") + 10;
                    int endIndex = durationLine.indexOf(",");
                    String duration = durationLine.substring(startIndex, endIndex).trim();
                    Log.d(TAG, "Duration: " + duration);

                    int splitNumber = AudioUtil.calculateSegments(duration);
                    Log.d(TAG, "Split to : " + splitNumber + " files.");

                    splitAndSaveFiles(splitNumber, inputFilePath);
                }
            }
        });
    }

    private void splitAndSaveFiles(int splitNumber, String inputFilePath) {
        if (splitNumber <= 0) {
            Log.e(TAG, "SplitAndSaveFiles error.  splitNumber = " + splitNumber);
            return;
        }
        List<String> newFileAbsolutePathList = new CopyOnWriteArrayList<>();
        for (int i = 0; i < splitNumber; i++) {
            String newFileAbsolutePath = createNewAudioFilePath(inputFilePath, i);

            String startTime = (i == 0) ? "00:00:00" : "00:" + i * 10 + ":00";
            String duration = "00:10:00";
            Log.d(TAG, "splitAndSaveFiles, file" + i + "startTime" + "\t" + duration);

            String command = "-y -ss " + startTime +
                    " -i " + inputFilePath +
                    " -t " + duration +
                    " -ar 8000 -ac 1 -c:a pcm_s16le "
                    + newFileAbsolutePath;

            FFmpegKit.executeAsync(command, session -> {
                SessionState state = session.getState();
                ReturnCode returnCode = session.getReturnCode();
                // CALLED WHEN SESSION IS EXECUTED
                Log.d(TAG, String.format(
                        "FFmpeg process exited with state %s and rc %s.%s",
                        state, returnCode,
                        session.getFailStackTrace()));

                // File should be ready after check return code
                if (ReturnCode.isSuccess(returnCode)) {
                    newFileAbsolutePathList.add(newFileAbsolutePath);
                    if (newFileAbsolutePathList.size() == splitNumber) {
                        mSplitFileCallback.onFileSplit(newFileAbsolutePathList);
                        Log.d(TAG, "endSplitFile ---");
                        mLogTextCallback.onLogReceived("endSplitFile ---");
                    }
                }
            });
        }
    }
}
