package com.example.theclass;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class Face {


    private static final String CASCADE_FILE = "haarcascade_frontalface_default.xml";

    private static CascadeClassifier cascadeClassifier;
    private static boolean isOpenCVLoaded = false;


    public static void initOpenCV(Context context) {
        if (isOpenCVLoaded) return;


       if (!OpenCVLoader.initDebug()) {
            return;
        }

        isOpenCVLoaded = true;

        loadCascadeClassifier(context);
    }


    private static void loadCascadeClassifier(Context context) {
            File cascadeFile = copyCascadeFileFromAssets(context);
            if (cascadeFile != null) {
                cascadeClassifier = new CascadeClassifier(cascadeFile.getAbsolutePath());
                if (cascadeClassifier.empty()) {
                    cascadeClassifier = null;
                }
            }
    }

    private static File copyCascadeFileFromAssets(Context context) {
        File cascadeFile = new File(context.getCacheDir(), CASCADE_FILE);
        try {
            InputStream is = context.getAssets().open(CASCADE_FILE);
            FileOutputStream os = new FileOutputStream(cascadeFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
            return cascadeFile;
        } catch (IOException e) {

            return null;
        }
    }

    public static class FaceDetector {

        public static Rect detectFace(Bitmap bitmap) {
            if (!isOpenCVLoaded || cascadeClassifier == null) {

                return null;
            }
            try {
                Mat srcMat = new Mat();
                Utils.bitmapToMat(bitmap, srcMat);

                Mat grayMat = new Mat();
                Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_RGBA2GRAY);

                Mat equalizedMat = new Mat();
                Imgproc.equalizeHist(grayMat, equalizedMat);

                MatOfRect faces = new MatOfRect();
                cascadeClassifier.detectMultiScale(
                        equalizedMat,
                        faces,
                        1.1,
                        3,
                        0,
                        new Size(60, 60),
                        new Size(500, 500)
                );

                // 5. 获取第一个检测到的人脸
                org.opencv.core.Rect[] facesArray = faces.toArray();

                // 释放内存
                srcMat.release();
                grayMat.release();
                equalizedMat.release();
                faces.release();

                if (facesArray.length > 0) {
                    org.opencv.core.Rect face = facesArray[0];


                    int expand = (int)(face.width * 0.1);
                    int left = Math.max(0, face.x - expand);
                    int top = Math.max(0, face.y - expand);
                    int right = Math.min(bitmap.getWidth(), face.x + face.width + expand);
                    int bottom = Math.min(bitmap.getHeight(), face.y + face.height + expand);

                    return new Rect(left, top, right, bottom);
                } else {

                    return null;
                }

            } catch (Exception e) {

                return null;
            }
        }
    }


    public static Bitmap cropFace(Bitmap bitmap, Rect faceRect) {
        if (faceRect == null || faceRect.width() <= 0 || faceRect.height() <= 0) {
            return null;
        }

        try {
            return Bitmap.createBitmap(bitmap,
                    faceRect.left, faceRect.top,
                    faceRect.width(), faceRect.height());
        } catch (Exception e) {
            return null;
        }
    }

}