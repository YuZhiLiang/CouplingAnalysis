package com.yzl.couplinganalysis;

import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by 04816381 on 2018-02-08.
 */

public class ReferenceObject {
    private File mFile;//检测文件
    private double mAngleOfRotation;//旋转角度
    public static double AVERAGE_ANGLE = 0;//平均角度
    private ArrayList<ReferenceObject> mReferenceObjects = new ArrayList<>();//引用对象
    private String mMainClassName;
    private ArrayList<String> mReferenceClassName = new ArrayList<>();//引用对象类名

    public ReferenceObject(File file) {
        mFile = file;
    }

    public double getAngleOfRotation() {
        return mAngleOfRotation;
    }

    public void setAngleOfRotation(double angleOfRotation) {
        mAngleOfRotation = angleOfRotation;
    }

    public void addReferenceObject(ReferenceObject referenceObject) {
        if (!mReferenceObjects.contains(referenceObject)) {
            mReferenceObjects.add(referenceObject);
        }
    }

    public void addReferenceObjectClassName(String s) {
        if (!TextUtils.isEmpty(s) && !mReferenceClassName.contains(s)) {
            mReferenceClassName.add(s);
        }
    }

    public int referenceObjectSize() {
        return mReferenceObjects.size();
    }

    public boolean containsImport(String s) {
        if (TextUtils.isEmpty(s)) {
            return false;
        } else {
            return mReferenceClassName.contains(s);
        }
    }

    public File getFile() {
        return mFile;
    }

    public String getMainClassName() {
        return mMainClassName;
    }

    public void setMianClassName(String fileClassName) {
        mMainClassName = fileClassName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj instanceof ReferenceObject && ((ReferenceObject) obj).getFile() != null) {
            return ((ReferenceObject) obj).getFile().equals(mFile);
        }
        return super.equals(obj);
    }
}
