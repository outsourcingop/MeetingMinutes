package com.optoma.meeting.presenter;

import android.content.Context;
import android.util.Log;

import com.optoma.meeting.LogTextCallback;
import com.optoma.meeting.R;

import io.reactivex.disposables.CompositeDisposable;

public class BasicPresenter {

    protected static String TAG = BasicPresenter.class.getSimpleName();

    protected final Context mContext;
    protected final LogTextCallback mLogTextCallback;
    protected final ErrorCallback mErrorCallback;
    protected final int mEachSegmentDuration;

    protected CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    public interface ErrorCallback {
        void onError(String error);
    }

    public BasicPresenter(Context context, LogTextCallback callback, ErrorCallback errorCallback) {
        mContext = context;
        mLogTextCallback = callback;
        mErrorCallback = errorCallback;
        mEachSegmentDuration = context.getResources().getInteger(R.integer.each_segment_duration);
    }

    public void destroy() {
        if (!mCompositeDisposable.isDisposed()) {
            mCompositeDisposable.dispose();
        }
    }

    final void performError(String errorLog) {
        Log.w(TAG, errorLog);
        mLogTextCallback.onLogReceived(errorLog);
        mErrorCallback.onError(errorLog);
    }
}