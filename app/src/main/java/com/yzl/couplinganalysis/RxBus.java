package com.yzl.couplinganalysis;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

/**
 * 利用RxJava实现的一个类似EventBus的工具类
 */
public class RxBus {
    private static final String TAG = RxBus.class.getSimpleName();
    private static RxBus instance;

    public static synchronized RxBus get() {
        if (null == instance) {
            instance = new RxBus();
        }
        return instance;
    }

    private RxBus() {
    }

    private ConcurrentHashMap<Object, List<Subject>> subjectMapper = new ConcurrentHashMap<>();

    /**
     * 注册
     *
     * @param tag
     * @return
     */
    public <T> Observable<T> register(@NonNull Class<T> tag) {
        List<Subject> subjectList = subjectMapper.get(tag);
        if (null == subjectList) {
            subjectList = new ArrayList<>();
            subjectMapper.put(tag, subjectList);
        }
        Subject<T, T> subject = PublishSubject.create();
        subjectList.add(subject);
        return subject;
    }

    /**
     * 解除注册
     *
     * @param tag
     */
    public <T> void unregister(@NonNull Class<T> tag, @NonNull Observable observable) {
        List<Subject> subjects = subjectMapper.get(tag);
        if (null != subjects) {
            subjects.remove(observable);
            if (subjects.isEmpty()) {
                subjectMapper.remove(tag);
            }
        }
    }

    public void unregister(@NonNull Object tag, @NonNull Observable observable) {
        List<Subject> subjects = subjectMapper.get(tag);
        if (null != subjects) {
            subjects.remove((Subject) observable);
            if (subjects != null) {
                subjectMapper.remove(tag);
            }
        }

    }

    /**
     * 发送消息
     *
     * @param event
     */
    public void post(@NonNull Object event) {
        List<Subject> subjectList = subjectMapper.get(event.getClass());
        if (subjectList != null && !subjectList.isEmpty()) {
            for (Subject subject : subjectList) {
                subject.onNext(event);
            }
        }
    }

    /**
     * 清除订阅
     */
    public void clear() {
        if (subjectMapper.isEmpty()) {
            return;
        }
        subjectMapper.clear();
    }

}