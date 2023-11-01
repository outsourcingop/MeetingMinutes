package com.optoma.meeting.presenter;

import static com.optoma.meeting.util.FileUtil.createNewAudioFilePath;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.SessionState;
import com.optoma.meeting.LogTextCallback;
import com.optoma.meeting.R;
import com.optoma.meeting.util.AudioUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SplitFilePresenter extends BasicPresenter {

    private static final boolean DEBUG = true;

    private final SplitFileCallback mSplitFileCallback;

    public interface SplitFileCallback extends ErrorCallback {
        void onFileSplit(List<String> newFileAbsolutePathList);
    }

    public SplitFilePresenter(Context context, LogTextCallback callback,
            SplitFileCallback splitFileCallback) {
        super(context, callback, splitFileCallback);
        TAG = SplitFilePresenter.class.getSimpleName();
        mSplitFileCallback = splitFileCallback;
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
                    String stringDuration = durationLine.substring(startIndex, endIndex).trim();
                    @SuppressLint("SimpleDateFormat")
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
                    try {
                        Calendar duration = Calendar.getInstance();
                        duration.setTime(sdf.parse(stringDuration));
                        Log.d(TAG, "duration=" + calendarToString(duration));

                        splitAndSaveFiles(duration, inputFilePath);
                    } catch (ParseException | NullPointerException e) {
                        String errorLog = "errorMessage: " + e.getMessage();
                        performError(errorLog);
                    }
                }
            }
        });
    }

    private void splitAndSaveFiles(Calendar duration, String inputFilePath) {
        // Split the large file into some of the small files.
        int splitNumber = AudioUtil.calculateSegments(duration, mEachSegmentDuration);
        if (splitNumber <= 0) {
            String errorLog = "errorMessage: SplitAndSaveFiles error. splitNumber=" + splitNumber;
            performError(errorLog);
            return;
        }
        Log.d(TAG, "Split to : " + splitNumber + " files.");

        // Create a calendar that saves each segment duration by minutes.
        Calendar eachSegmentDuration = Calendar.getInstance();
        eachSegmentDuration.set(0, 0, 0, 0, mEachSegmentDuration, 0);

        List<String> newFileAbsolutePathList = new CopyOnWriteArrayList<>();
        for (int i = 0; i < splitNumber; i++) {
            String newFileAbsolutePath = createNewAudioFilePath(inputFilePath, i);

            Calendar startTime = Calendar.getInstance();
            startTime.set(0, 0, 0, 0, i * mEachSegmentDuration, 0);

            // The format is like "00:23:45"
            String stringStartTime = calendarToString(startTime);
            Log.d(TAG, "splitAndSaveFiles# filePartNumber=" + i + ", startTime=" + stringStartTime);

            // The format is like "00:08:00"
            String stringEachDuration = calendarToString(eachSegmentDuration);

            String command = "-y -ss " + stringStartTime +
                    " -i " + inputFilePath +
                    " -t " + stringEachDuration +
                    " -ar 8000 -ac 1 -c:a pcm_s16le "
                    + newFileAbsolutePath;

            final int fileIndex = i;
            FFmpegKit.executeAsync(command, session -> {
                SessionState state = session.getState();
                ReturnCode returnCode = session.getReturnCode();
                // CALLED WHEN SESSION IS EXECUTED
                Log.d(TAG, String.format(
                        "FFmpeg process the filePartNumber=%d exited with state %s and rc %s.%s",
                        fileIndex, state, returnCode,
                        session.getFailStackTrace()));

                // File should be ready after check return code
                if (ReturnCode.isSuccess(returnCode)) {
                    newFileAbsolutePathList.add(newFileAbsolutePath);
                    if (newFileAbsolutePathList.size() == splitNumber) {
                        String endLog = "endSplitFile ---";
                        Log.d(TAG, endLog);
                        mLogTextCallback.onLogReceived(endLog);
                        mSplitFileCallback.onFileSplit(newFileAbsolutePathList);
                    }
                }
            });
        }
    }

    @SuppressLint("DefaultLocale")
    private String calendarToString(Calendar calendar) {
        return String.format("%02d:%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
    }
}
