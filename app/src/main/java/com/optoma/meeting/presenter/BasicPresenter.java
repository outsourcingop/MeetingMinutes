package com.optoma.meeting.presenter;

import io.reactivex.disposables.CompositeDisposable;
public class BasicPresenter {
   protected CompositeDisposable mCompositeDisposable = new CompositeDisposable();

   public void destroy() {
      if (!mCompositeDisposable.isDisposed()) {
         mCompositeDisposable.dispose();
      }
   }
}