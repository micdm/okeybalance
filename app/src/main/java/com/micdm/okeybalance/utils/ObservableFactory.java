package com.micdm.okeybalance.utils;

import android.util.Pair;
import android.view.animation.Animation;

import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

public class ObservableFactory {

    public static Pair<Observable<Object>, Observable<Object>> getForAnimation(Animation animation) {
        Subject<Object, Object> start = PublishSubject.create();
        Subject<Object, Object> finish = PublishSubject.create();
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                start.onNext(null);
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                finish.onNext(null);
            }
            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        return new Pair<>(start, finish);
    }
}
