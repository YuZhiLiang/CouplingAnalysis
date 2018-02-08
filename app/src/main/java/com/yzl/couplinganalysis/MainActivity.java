package com.yzl.couplinganalysis;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextPaint;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    TextView mLogView;
    ImageView mResult;
    TextPaint mTextPaint = new TextPaint();

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLogView = findViewById(R.id.log);
        mResult = findViewById(R.id.result);

        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        } else {
            startCreadCouplingImage();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        startCreadCouplingImage();
    }

    /**
     * 开始进行绘制引用图的操作
     */
    private void startCreadCouplingImage() {
        initTools();
        Observable.create(new Observable.OnSubscribe<File>() {
            @Override
            public void call(Subscriber<? super File> subscriber) {
                subscriber.onStart();
                File externalStorageDirectory = Environment.getExternalStorageDirectory();
                File community = new File(externalStorageDirectory, "community");
                if (community.exists()) {
                    subscriber.onNext(community);
                    subscriber.onCompleted();
                } else {
                    subscriber.onError(new FileNotFoundException("文件" + community.getName() + "不存在"));
                }
            }
        }).map(new Func1<File, List<ReferenceObject>>() {
            @Override
            public List<ReferenceObject> call(File file) {
                ArrayList<File> files = forFileDirectory(file);
                return file2ReferenceObject(files);
            }
        }).map(new Func1<List<ReferenceObject>, Object>() {
            @Override
            public Object call(List<ReferenceObject> referenceObjects) {
                for (ReferenceObject referenceObject : referenceObjects) {
                    printlnLog(referenceObject.getMainClassName() + " 导入了" + referenceObject.referenceObjectSize() + "个文件");
                }
                return new Object();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Object>() {
                    @Override
                    public void onCompleted() {
                        printlnLog("测试完成");
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        printlnLog("出现错误了 " + e.toString());
                    }

                    @Override
                    public void onNext(Object o) {

                    }
                });
        /*new Thread("测试线程") {
            @Override
            public void run() {
                super.run();
                File externalStorageDirectory = Environment.getExternalStorageDirectory();
                File community = new File(externalStorageDirectory, "community");
                if (community.exists()) {
                    printlnLog("文件存在");
                    ArrayList<File> files = forFileDirectory(community);
                    printlnLog("迭代完毕 文件总数为" + files.size());
                    printlnLog("===================================");
                    printlnLog("开始绘制图片");
                    creadImage();
                } else {
                    printlnLog("文件不存在");
                }
            }
        }.start();*/
    }

    /**
     * 文件转引用
     *
     * @param files
     * @return
     */
    private List<ReferenceObject> file2ReferenceObject(ArrayList<File> files) {
        ArrayList<ReferenceObject> referenceObjects = new ArrayList<>();
        double averageAngle = 360 / files.size();
        ReferenceObject.AVERAGE_ANGLE = averageAngle;
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            ReferenceObject referenceObject = new ReferenceObject(file);
            referenceObject.setAngleOfRotation(averageAngle * i);
            referenceObjects.add(referenceObject);
            printlnLog("生成对象 " + file.getName());
        }
        Associatedreference(referenceObjects);
        return referenceObjects;
    }

    /**
     * 关联引用
     *
     * @param referenceObjects
     */
    private void Associatedreference(ArrayList<ReferenceObject> referenceObjects) {
        for (int i = 0; i < referenceObjects.size(); i++) {
            ReferenceObject referenceObject = referenceObjects.get(i);
            File mainFile = referenceObject.getFile();
            printlnLog("关联引用" + mainFile.getName());
            try {
                getMainClassNameAndImportClass(referenceObject, mainFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < referenceObjects.size(); ++i) {
            ReferenceObject referenceObject = referenceObjects.get(i);
            if (i < referenceObjects.size()) {
                for (int f = 0; f < i; f++) {
                    ReferenceObject referenceObject1 = referenceObjects.get(f);
                    if (referenceObject.containsImport(referenceObject1.getMainClassName())
                            || referenceObject1.containsImport(referenceObject.getMainClassName())) {
                        referenceObject.addReferenceObject(referenceObject1);
                    }
                }
            }
        }
    }

    private void getMainClassNameAndImportClass(ReferenceObject referenceObject, File mainFile) throws IOException {
        BufferedReader bf = new BufferedReader(new InputStreamReader(new FileInputStream(mainFile)));
        String mainClass = null;
        String tempClass = null;
        while ((tempClass = bf.readLine()) != null) {
            if (tempClass.startsWith("package") && tempClass.contains(";") && tempClass.contains(" ")) {
                mainClass = tempClass
                        .replace("package", "")
                        .replace(";", "." + mainFile.getName().replace(".java", ""))
                        .trim();
                referenceObject.setMianClassName(mainClass);
                break;
            }
        }

        while ((tempClass = bf.readLine()) != null) {
            if (tempClass.startsWith("import") && tempClass.contains(";") && tempClass.contains(" ")) {
                String anImport = tempClass.replace("import", "")
                        .replace(";", "")
                        .trim();
                referenceObject.addReferenceObjectClassName(anImport);
            }
        }

        bf.close();
    }

    /**
     * 初始化工具
     */
    private void initTools() {
        mTextPaint.setColor(Color.BLACK);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(26);
    }

    int weiht = 15000;
    int halfWeiht = weiht / 2;

    /*private void creadImage() {
        double i = 360f / mFiles.size();
        final Bitmap bitmap = Bitmap.createBitmap(weiht, weiht, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, weiht, weiht, paint);

        double r = 0;
        double tr = i;
        for (int i1 = 0; i1 < mFiles.size(); i1++) {
            canvas.rotate((float) tr, halfWeiht, halfWeiht);
            canvas.drawText(mFiles.get(i1).getName().replace(".java", ""), 7000 + halfWeiht, halfWeiht, mTextPaint);
        }

        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        File community = new File(externalStorageDirectory, "rec.png");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(community);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        try {
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(community));
        sendBroadcast(intent);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mResult.setImageBitmap(bitmap);
            }
        });
    }*/

    /**
     * 获取到所有文件
     *
     * @param directory
     * @return
     */
    private ArrayList<File> forFileDirectory(File directory) {
        ArrayList<File> mFiles = new ArrayList<>();
        if (directory.isDirectory()) {
            printlnLog("开始迭代文件夹：" + directory.getName());
            File[] files = directory.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    if (dir.isDirectory()) {
                        return true;
                    }
                    if (name.endsWith(".java")) {
                        return true;
                    }
                    return false;
                }
            });

            if (files != null && files.length > 0) {
                for (File file : files) {
                    ArrayList<File> files1 = forFileDirectory(file);
                    if (files1 != null && !files1.isEmpty()) {
                        mFiles.addAll(files1);
                    }
                }
            } else {
                printlnLog("文件夹为空：" + directory.getName());
            }
        } else {
            mFiles.add(directory);
        }
        return mFiles;
    }

    void printlnLog(String log) {
        Observable.just(log)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        mLogView.append("#" + s + System.getProperty("line.separator"));
                    }
                });
    }
}
