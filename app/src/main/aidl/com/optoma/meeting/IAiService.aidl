// IAiService.aidl
package com.optoma.meeting;

// Declare any non-default types here with import statements

interface IAiService {

    void initialize(in Bundle params) = 1;

    // for the audio file processing
    void startAudioProcessing(in Bundle params) = 2;

    // for the live audio processing
    void startAudioRecognition(in Bundle params) = 3;

    // for the live audio processing
    void stopAudioRecognition(in Bundle params) = 4;

    // for the live audio processing
    boolean isAudioRecognizing() = 5;
}