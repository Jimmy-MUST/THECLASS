package com.example.theclass;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class TeachableMachine {

    private static final int MODEL_INPUT_SIZE = 224;

    private Interpreter tflite;
    private List<String> rawLabels;
    private Context context;

    private static TeachableMachine instance;

    public static synchronized TeachableMachine getInstance(Context context) {
        if (instance == null) {
            instance = new TeachableMachine(context.getApplicationContext());
        }
        return instance;
    }

    private TeachableMachine(Context context) {
        this.context = context.getApplicationContext();
        loadModel();
    }

    private synchronized void loadModel() {
        try {
            if (tflite != null) {
                tflite.close();
                tflite = null;
            }
            tflite = new Interpreter(loadModelFile());
            rawLabels = loadLabels();
        } catch (IOException e) {
            tflite = null;
            rawLabels = null;
        }
    }

    private synchronized boolean ensureModelLoaded() {
        if (tflite == null || rawLabels == null) {
            loadModel();
        }
        return tflite != null && rawLabels != null;
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        FileChannel fileChannel = new java.io.FileInputStream(
                context.getAssets().openFd("model_unquant.tflite").getFileDescriptor()
        ).getChannel();
        long startOffset = context.getAssets().openFd("model_unquant.tflite").getStartOffset();
        long declaredLength = context.getAssets().openFd("model_unquant.tflite").getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private List<String> loadLabels() throws IOException {
        List<String> labelList = new ArrayList<>();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open("labels.txt"))
        );
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line.trim());
        }
        reader.close();
        return labelList;
    }

    private String cleanLabel(String rawLabel) {
        if (rawLabel == null) return null;
        String cleaned = rawLabel;
        cleaned = cleaned.replaceFirst("^\\d+\\s+", "");
        cleaned = cleaned.replaceFirst("^\\d+", "");
        return cleaned.trim();
    }

    public RecognitionResult recognize(Bitmap photo) {
        if (!ensureModelLoaded()) {
            return new RecognitionResult(null, ", Model not loaded");
        }

        Rect faceRect = Face.FaceDetector.detectFace(photo);
        if (faceRect == null || faceRect.width() <= 0) {
            return new RecognitionResult(null, "No face detected");
        }

        Bitmap faceBitmap = Face.cropFace(photo, faceRect);
        if (faceBitmap == null) {
            return new RecognitionResult(null, "Face cropping failed");
        }

        Bitmap inputBitmap = Bitmap.createScaledBitmap(faceBitmap, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, true);

        ByteBuffer inputBuffer = convertBitmapToByteBuffer(inputBitmap);

        float[][] output = new float[1][rawLabels.size()];
        tflite.run(inputBuffer, output);

        float[] probabilities = output[0];
        int bestIndex = 0;
        float bestConfidence = probabilities[0];
        for (int i = 1; i < probabilities.length; i++) {
            if (probabilities[i] > bestConfidence) {
                bestConfidence = probabilities[i];
                bestIndex = i;
            }
        }

        String rawUsername = rawLabels.get(bestIndex);
        String cleanedUsername = cleanLabel(rawUsername);

        if (cleanedUsername.equalsIgnoreCase("other") ||
                cleanedUsername.equalsIgnoreCase("error")) {
            return new RecognitionResult(null, "X");
        }

        float threshold = 0.90f;
        if (bestConfidence < threshold) {
            return new RecognitionResult(null, "Fail");
        }

        return new RecognitionResult(cleanedUsername, "success");
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * MODEL_INPUT_SIZE * MODEL_INPUT_SIZE * 3);
        byteBuffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[MODEL_INPUT_SIZE * MODEL_INPUT_SIZE];
        bitmap.getPixels(pixels, 0, MODEL_INPUT_SIZE, 0, 0, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE);

        for (int pixel : pixels) {
            float r = (((pixel >> 16) & 0xFF) / 255.0f) * 2.0f - 1.0f;
            float g = (((pixel >> 8) & 0xFF) / 255.0f) * 2.0f - 1.0f;
            float b = ((pixel & 0xFF) / 255.0f) * 2.0f - 1.0f;

            byteBuffer.putFloat(r);
            byteBuffer.putFloat(g);
            byteBuffer.putFloat(b);
        }

        return byteBuffer;
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
    }

    public static class RecognitionResult {
        public final String username;
        public final String message;

        public RecognitionResult(String username, String message) {
            this.username = username;
            this.message = message;
        }

        public boolean isSuccess() {
            return username != null;
        }
    }
}