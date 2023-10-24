// IAiServiceCallback.aidl
package com.optoma.meeting;

// Declare any non-default types here with import statements

interface IAiServiceCallback {

    void onStateChanged(in String state) = 1;

    void onLogReceived(in String text) = 2;

    void onSummaryAndActionsReceived(in String summary) = 3;
}