package com.optoma.meeting.presenter;

import static com.optoma.meeting.BuildConfig.DEFAULT_ENDPOINTS_PROTOCAL;
import static com.optoma.meeting.BuildConfig.STORAGE_ACCOUNT_KEY;
import static com.optoma.meeting.BuildConfig.STORAGE_ACCOUNT_NAME;
import static com.optoma.meeting.util.FileUtil.createMeetingMinutesFile;
import static com.optoma.meeting.util.FileUtil.extractPartNumber;

import android.content.Context;
import android.util.Log;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobContainerPermissions;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.optoma.meeting.LogTextCallback;
import com.optoma.meeting.model.RecognizedPhrase;
import com.optoma.meeting.model.TranscribeBean;
import com.optoma.meeting.model.TranscribeBody;
import com.optoma.meeting.model.TranscribeProperties;
import com.optoma.meeting.model.TranscribeResult;
import com.optoma.meeting.model.TranscribeValue;
import com.optoma.meeting.network.CognitiveServiceHelper;
import com.optoma.meeting.network.NetworkServiceHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;

public class TranscribePresenter extends BasicPresenter {

    public static final int POLLING_FREQ_IN_SEC = 20;
    private static final boolean DEBUG = true;
    private static final String SIMPLE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private String storageConnectionString = DEFAULT_ENDPOINTS_PROTOCAL
            + STORAGE_ACCOUNT_NAME
            + STORAGE_ACCOUNT_KEY;
    private static final String AZURE_STORAGE_CONTAINER_NAME = "audio";
    private static final String AZURE_STORAGE_BLOB_NAME = "audioblob";
    private static final String DEFAULT_PREFER_LOCALE = "zh-tw";
    String mWavContentUrl = "https://" + STORAGE_ACCOUNT_NAME
            + ".blob.core.windows.net/" + AZURE_STORAGE_CONTAINER_NAME + "/" + AZURE_STORAGE_BLOB_NAME;

    private final TranscribeCallback mTranscribeCallback;

    private ExecutorService mExecutorService;

    public TranscribePresenter(Context context, LogTextCallback callback,
            TranscribeCallback transcribeCallback) {
        super(context, callback, transcribeCallback);
        TAG = TranscribePresenter.class.getSimpleName();
        mTranscribeCallback = transcribeCallback;
    }

    public interface TranscribeCallback extends ErrorCallback {
        void onTranscribed(String text, long timeStamp);

        void onAllPartsTranscribed(Map<Integer, String> partNumberToTranscriber, long timeStamp);
    }

    private final Map<String, Integer> mTranscribeIDToPartNumber = new HashMap<>();
    private final Map<Integer, String> mPartNumberToTranscriberForSummary = new HashMap<>();
    private final Map<Integer, String> mPartNumberToTranscriberForView = new HashMap<>();
    private final Map<String, Disposable> mPollingDisposables = new HashMap<>();

    private CloudBlockBlob mCloudBlockBlob;
    private CloudBlobContainer mCloudBlobContainer;
    private TranscribeResult mTranscribeResult;

    public void uploadAudioAndTranscribe(List<String> absolutePathList, String languageString) {
        Log.d(TAG, "uploadFileFromFile: start");
        mLogTextCallback.onLogReceived("uploadFileFromFile: start...");

        mExecutorService = Executors.newFixedThreadPool(absolutePathList.size());
        List<Completable> completables = new ArrayList<>();
        mTranscribeIDToPartNumber.clear();
        mPartNumberToTranscriberForSummary.clear();
        mPartNumberToTranscriberForView.clear();
        mPollingDisposables.clear();

        for (String absolutePath : absolutePathList) {
            Log.d(TAG, "extractPartNumber: " + absolutePath + "\t result" + extractPartNumber(
                    absolutePath));
            Completable completable = Completable.create(emitter -> {
                        try {
                            // Storage init action++
                            // Setup the cloud storage account.
                            CloudStorageAccount account = CloudStorageAccount
                                    .parse(storageConnectionString);

                            // Create a blob service client
                            CloudBlobClient blobClient = account.createCloudBlobClient();

                            // Get a reference to a container
                            // The container name must be lower case
                            // Append a random UUID to the end of the container name so that
                            // this sample can be run more than once in quick succession.
                            CloudBlobContainer container =
                                    blobClient.getContainerReference(
                                            AZURE_STORAGE_CONTAINER_NAME + UUID.randomUUID().toString().replace(
                                                    "-", ""));

                            // Create the container if it does not exist
                            container.createIfNotExists();

                            // Make the container public
                            // Create a permissions object
                            BlobContainerPermissions containerPermissions = new BlobContainerPermissions();

                            // Include public access in the permissions object
                            containerPermissions.setPublicAccess(BlobContainerPublicAccessType.CONTAINER);

                            // Set the permissions on the container
                            container.uploadPermissions(containerPermissions);

                            // Get a reference to a blob in the container
                            CloudBlockBlob blob = container.getBlockBlobReference(AZURE_STORAGE_BLOB_NAME);

                            // Upload file to the blob
                            blob.uploadFromFile(absolutePath);
                            // Storage init action--

                            Log.d(TAG, "Upload complete " + blob.getUri());
                            mLogTextCallback.onLogReceived("Upload complete " + blob.getUri());

                            mWavContentUrl = blob.getUri().toString();
                            createSpeechToText(languageString, extractPartNumber(absolutePath));

                            // Storage post action++
                            // Delete the blobs
                            mCloudBlockBlob = blob;

                            // Delete the container
                            mCloudBlobContainer = container;

                        } catch (URISyntaxException | StorageException | InvalidKeyException |
                                 IOException e) {
                            e.printStackTrace();
                            emitter.onError(e);
                        }
                        emitter.onComplete();
                    })
                    .subscribeOn(Schedulers.from(mExecutorService));

            completables.add(completable);
        }

        Completable.merge(completables)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    Log.d(TAG, "*** executorService.shutdown().");
                    mLogTextCallback.onLogReceived("*** executorService.shutdown().");
                    // Handle completion on the main thread
                    mExecutorService.shutdown();
                }, throwable -> {
                    Log.w(TAG, "fail to uploadAudio, %s", throwable);
                    mExecutorService.shutdown();
                });
    }

    /**
     * Create and initiate a speech-to-text operation for a specific language using the audio file part.
     *
     * @param languageString The target language for the speech recognition.
     * @param filePartNumber The part number of the audio file (starting from 0).
     */
    private void createSpeechToText(String languageString, int filePartNumber) {
        mLogTextCallback.onLogReceived(
                "createSpeechToText: " + languageString + "\tfilePartNumber: " + filePartNumber);

        TranscribeProperties properties = new TranscribeProperties();
        properties.diarizationEnabled = true;
        properties.wordLevelTimestampsEnabled = true;
        properties.punctuationMode = "DictatedAndAutomatic";
        properties.profanityFilterMode = "Masked";
        properties.timeToLive = "PT1H";

        if (DEBUG) {
            Log.d(TAG, "createTranscription: mWavContentUrl: " + mWavContentUrl);
        }

        TranscribeBody body = new TranscribeBody(Arrays.asList(mWavContentUrl),
                properties, languageString, "test");
        mCompositeDisposable.add(
                CognitiveServiceHelper.getInstance()
                        .createTranscription(body)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .timeout(10, TimeUnit.SECONDS)
                        .subscribe(response -> {
                            Log.d(TAG, "onResponse: " + response.code());
                            if (response.isSuccessful()) {
                                TranscribeBean result = response.body();
                                Log.d(TAG, "onResponse: " + result.status);
                                Log.d(TAG, "onResponse: " + result.links.files);
                                String[] info = result.links.files.split("/");
                                String transcriptionID = info[info.length - 2];

                                if (response.code() == 201) {
                                    Log.d(TAG,
                                            "createTranscription success: " + transcriptionID + ", filePartNumber: " + filePartNumber);
                                    mLogTextCallback.onLogReceived(
                                            "createTranscription success: " + transcriptionID + ", filePartNumber: " + filePartNumber);
                                    mTranscribeIDToPartNumber.put(transcriptionID, filePartNumber);
                                    startPolling(transcriptionID);
                                }
                            } else {
                                ResponseBody errorBody = response.errorBody();
                                if (errorBody != null) {
                                    String errorLog = "errorMessage: " +
                                            NetworkServiceHelper.generateErrorToastContent(errorBody);
                                    performError(errorLog);
                                }
                            }
                        }, throwable -> Log.w(TAG, "fail to getUserDrinkList, %s", throwable))
        );
    }

    private void pollingStatus(String transcriptionID) {
        mCompositeDisposable.add(
                CognitiveServiceHelper.getInstance()
                        .getTranscriptionStatus(transcriptionID)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .timeout(10, TimeUnit.SECONDS)
                        .subscribe(response -> {
                            Log.d(TAG, "onResponse: " + response.code());
                            if (response.isSuccessful()) {
                                TranscribeBean result = response.body();
                                Log.d(TAG, "polling Status onResponse: " + result.status);

                                int partNumber = mTranscribeIDToPartNumber.get(transcriptionID);
                                String resultLog;
                                if ("Running".equalsIgnoreCase(result.status)) {
                                    resultLog = "Transcribe " + partNumber + " is Running...";
                                    Log.d(TAG, resultLog);
                                    mLogTextCallback.onLogReceived(resultLog);
                                } else if ("Succeeded".equalsIgnoreCase(result.status)) {
                                    resultLog = "***** Transcribe " + partNumber + " is Ready! " +
                                            "*****\n";
                                    Log.d(TAG, resultLog);
                                    mLogTextCallback.onLogReceived(resultLog);
                                    stopPolling(transcriptionID);
                                    getTranscriber(transcriptionID);
                                } else {
                                    resultLog = "Failed to get transcription with unknown reason";
                                    Log.d(TAG, resultLog);
                                    mLogTextCallback.onLogReceived(resultLog);
                                }
                            } else {
                                ResponseBody errorBody = response.errorBody();
                                if (errorBody != null) {
                                    String errorLog = "errorMessage: " +
                                            NetworkServiceHelper.generateErrorToastContent(errorBody);
                                    performError(errorLog);
                                }
                            }
                        }, throwable -> Log.w(TAG, "fail to getTranscriptionStatus, %s", throwable))
        );
    }

    public void getTranscriber(String transcriptionID) {
        mCompositeDisposable.add(
                CognitiveServiceHelper.getInstance()
                        .getTranscriptionFiles(transcriptionID)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .timeout(10, TimeUnit.SECONDS)
                        .subscribe(response -> {
                            Log.d(TAG, "onResponse: " + response.code());
                            if (response.isSuccessful()) {
                                List<TranscribeValue> valueList = response.body().values;
                                // TODO error handling
                                for (int i = 0; i < valueList.size(); i++) {
                                    if ("Transcription".equalsIgnoreCase(valueList.get(i).kind)) {
                                        String contentUrl = valueList.get(
                                                i).links.contentUrl; //smaller one
                                        Log.d(TAG,
                                                "getTranscriptionFiles onResponse: contentUrl = " + contentUrl);
                                        int filePartNumber = mTranscribeIDToPartNumber.get(
                                                transcriptionID);
                                        getTranscribedFilesFromUrl(contentUrl, filePartNumber);
                                    }
                                }

                            } else {
                                ResponseBody errorBody = response.errorBody();
                                if (errorBody != null) {
                                    String errorLog = "errorMessage: " +
                                            NetworkServiceHelper.generateErrorToastContent(errorBody);
                                    performError(errorLog);
                                }
                            }
                        }, throwable -> Log.w(TAG, "fail to getUserDrinkList, %s", throwable))
        );
    }

    private void getTranscribedFilesFromUrl(String contentUrl, int filePartNumber) {
        mCompositeDisposable.add(
                CognitiveServiceHelper.getInstance()
                        .getTranscriptionFilesFromUrl(contentUrl)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .timeout(10, TimeUnit.SECONDS)
                        .subscribe(response -> {
                            Log.d(TAG, "onResponse: " + response.code());
                            if (response.isSuccessful()) {
                                mTranscribeResult = response.body();
                                Log.d(TAG,
                                        "getTranscriptionFilesFromUrl onResponse: contentUrl = " +
                                                mTranscribeResult.combinedRecognizedPhrases.get(
                                                        0).lexical);
                                deleteCloudFileAfterTranscribeEnd();

                                SimpleDateFormat sdf = new SimpleDateFormat(SIMPLE_DATE_FORMAT);
                                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                                long timeInMillis = 0;

                                try {
                                    Date date = sdf.parse(mTranscribeResult.timestamp);
                                    timeInMillis = date.getTime();
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                                postProcessTranscriptionData(timeInMillis, filePartNumber);
                            } else {
                                ResponseBody errorBody = response.errorBody();
                                if (errorBody != null) {
                                    String errorLog = "errorMessage: " +
                                            NetworkServiceHelper.generateErrorToastContent(errorBody);
                                    performError(errorLog);
                                }
                            }
                        }, throwable -> Log.w(TAG, "fail to getUserDrinkList, %s", throwable))
        );
    }

    private void postProcessTranscriptionData(long timestamp, int filePartNumber) {
        Log.d(TAG, "postProcessTranscriptionData# t=" + timestamp + ", filePartNumber=" + filePartNumber);
        StringBuilder sbTranscriptionForSummary = new StringBuilder();
        StringBuilder sbTranscriptionForView = new StringBuilder();

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        for (RecognizedPhrase phrase : mTranscribeResult.recognizedPhrases) {
            // string for the next steps to summarize
            String transcriptionForSummary = "{speaker " + phrase.speaker +
                    ":" +
                    phrase.nBest.get(0).display +
                    "}, ";
            sbTranscriptionForSummary.append(transcriptionForSummary);

            // string for log appearance and saving file.
            // here we transform minutes into milliseconds
            long baseOffset = filePartNumber * mEachSegmentDuration * 60 * 1000L;
            // According to definition on https://learn.microsoft.com/zh-tw/azure/ai-services/speech-service/batch-transcription-get?pivots=rest-api
            // , 1 offsetInTicks = 100ns. Here we want the offsetInTicks to milliseconds.
            long offset = phrase.offsetInTicks.longValue() / 10000L;
            long finalOffset = baseOffset + offset;
            String dateText = sdf.format(new Date(finalOffset));

            String transcriptionForView = dateText + " {speaker " + phrase.speaker +
                    ":" +
                    phrase.nBest.get(0).display +
                    "}\n";
            sbTranscriptionForView.append(transcriptionForView);
        }

        mPartNumberToTranscriberForSummary.put(filePartNumber,
                sbTranscriptionForSummary.toString());
        mPartNumberToTranscriberForView.put(filePartNumber, sbTranscriptionForView.toString());

        // Get all results from server and start to process next step.
        if (mPartNumberToTranscriberForSummary.size() == mTranscribeIDToPartNumber.size()) {
            storeMeetingMinutesToFile(timestamp);
        }
        mLogTextCallback.onLogReceived("Transcription:\n" + sbTranscriptionForView.toString());
    }

    private void storeMeetingMinutesToFile(long timestamp) {
        Log.d(TAG, "storeMeetingMinutesToFile#");

        File outputFile = createMeetingMinutesFile(mContext, timestamp);

        mCompositeDisposable.add(
                Completable.fromAction(() -> {
                            try {
                                FileOutputStream outputStream = new FileOutputStream(outputFile);

                                for (int i = 0; i < mPartNumberToTranscriberForView.size(); i++) {
                                    outputStream.write(mPartNumberToTranscriberForView.get(i).getBytes());
                                }

                                outputStream.close();

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(() -> {
                            String saveFileLog = "***** Saved meeting minutes to "
                                    + outputFile.getPath() + " *****\n";
                            Log.d(TAG, saveFileLog);
                            mLogTextCallback.onLogReceived(saveFileLog);
                            mTranscribeCallback.onAllPartsTranscribed(
                                    mPartNumberToTranscriberForSummary, timestamp);
                        })
        );
    }

    private void deleteCloudFileAfterTranscribeEnd() {
        mCompositeDisposable.add(
                Completable.create(emitter -> {
                            try {
                                mCloudBlockBlob.deleteIfExists();
                                mCloudBlobContainer.deleteIfExists();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            emitter.onComplete();
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(() -> {
                            // Handle completion on the main thread
                        }, throwable -> {
                            // Handle error on the main thread
                        }));
    }

    private void startPolling(String transcriptionID) {
        // If the timer is already running for this transcriptionID, cancel it first
        stopPolling(transcriptionID);

        // Create a timer that triggers every 30 seconds
        Disposable disposable = Observable.interval(0, POLLING_FREQ_IN_SEC, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(interval -> {
                    // Execute polling operations each time it triggers
                    pollingStatus(transcriptionID);
                });

        mPollingDisposables.put(transcriptionID, disposable);
        mCompositeDisposable.add(disposable);
    }

    private void stopPolling(String transcriptionID) {
        Disposable disposable = mPollingDisposables.get(transcriptionID);
        // Cancel the timer to stop polling
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
            mPollingDisposables.remove(transcriptionID);
        }
    }
}
