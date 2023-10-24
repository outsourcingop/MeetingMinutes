package com.optoma.meeting.state;

import androidx.annotation.NonNull;

public enum ProcessState {
    IDLE(true),
    START_SPLIT(false),
    END_SPLIT(false),
    START_TRANSCRIBE(false),
    END_TRANSCRIBE(false),
    START_SUMMARY(false),
    END_SUMMARY(false),
    START_TEXT_PROCESSING(false);

    public final boolean interactWithUi;

    ProcessState(boolean interactWithUi) {
        this.interactWithUi = interactWithUi;
    }

    @NonNull
    @Override
    public String toString() {
        return "[" + name() + ", interactWithUi=" + interactWithUi + "]";
    }
}
