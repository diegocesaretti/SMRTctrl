package com.bwa3d.homehublegacy;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;

import java.io.IOException;
import java.util.List;

@SuppressWarnings("deprecation")
final class MotionDetector implements Camera.PreviewCallback {
    interface Listener { void onMotionDetected(); }

    private final int threshold;
    private final Listener listener;
    private HandlerThread cameraThread;
    private Handler cameraHandler;
    private Camera camera;
    private SurfaceTexture surfaceTexture;
    private int width;
    private int height;
    private int[] previousSamples;
    private int frameCounter;
    private long lastTrigger;

    MotionDetector(int threshold, Listener listener) {
        this.threshold = Math.max(5, Math.min(60, threshold));
        this.listener = listener;
    }

    void start() {
        cameraThread = new HandlerThread("HomeHubMotionCamera");
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());
        cameraHandler.post(new Runnable() {
            @Override public void run() { openCamera(); }
        });
    }

    private void openCamera() {
        try {
            int cameraId = findFrontCamera();
            camera = cameraId >= 0 ? Camera.open(cameraId) : Camera.open();
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = choosePreviewSize(parameters.getSupportedPreviewSizes());
            width = size.width;
            height = size.height;
            parameters.setPreviewSize(width, height);
            parameters.setPreviewFormat(ImageFormat.NV21);
            camera.setParameters(parameters);
            int bitsPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.NV21);
            int bufferSize = width * height * bitsPerPixel / 8;
            camera.addCallbackBuffer(new byte[bufferSize]);
            camera.addCallbackBuffer(new byte[bufferSize]);
            camera.setPreviewCallbackWithBuffer(this);
            surfaceTexture = new SurfaceTexture(11);
            camera.setPreviewTexture(surfaceTexture);
            camera.startPreview();
        } catch (RuntimeException | IOException error) {
            releaseCamera();
        }
    }

    private int findFrontCamera() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int index = 0; index < Camera.getNumberOfCameras(); index++) {
            Camera.getCameraInfo(index, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) return index;
        }
        return -1;
    }

    private Camera.Size choosePreviewSize(List<Camera.Size> sizes) {
        Camera.Size selected = sizes.get(0);
        long selectedPixels = (long) selected.width * selected.height;
        for (Camera.Size candidate : sizes) {
            long pixels = (long) candidate.width * candidate.height;
            if (candidate.width >= 160 && candidate.height >= 120 && pixels < selectedPixels) {
                selected = candidate;
                selectedPixels = pixels;
            }
        }
        return selected;
    }

    @Override public void onPreviewFrame(byte[] data, Camera source) {
        try {
            frameCounter++;
            if (frameCounter % 4 != 0 || data == null) return;
            int step = Math.max(8, width / 40);
            int sampleCount = ((width + step - 1) / step) * ((height + step - 1) / step);
            int[] samples = new int[sampleCount];
            int sampleIndex = 0;
            for (int y = 0; y < height; y += step) {
                int row = y * width;
                for (int x = 0; x < width; x += step) {
                    samples[sampleIndex++] = data[row + x] & 0xFF;
                }
            }
            if (previousSamples != null && previousSamples.length == samples.length) {
                long totalDifference = 0;
                for (int index = 0; index < samples.length; index++) {
                    totalDifference += Math.abs(samples[index] - previousSamples[index]);
                }
                float averageDifference = totalDifference / (float) samples.length;
                long now = System.currentTimeMillis();
                if (averageDifference >= threshold && now - lastTrigger > 1500L) {
                    lastTrigger = now;
                    listener.onMotionDetected();
                }
            }
            previousSamples = samples;
        } finally {
            if (camera != null && data != null) camera.addCallbackBuffer(data);
        }
    }

    void stop() {
        if (cameraHandler != null && Thread.currentThread() != cameraThread) {
            cameraHandler.post(new Runnable() {
                @Override public void run() { releaseCamera(); }
            });
        } else releaseCamera();
    }

    private void releaseCamera() {
        if (camera != null) {
            try {
                camera.setPreviewCallbackWithBuffer(null);
                camera.stopPreview();
            } catch (RuntimeException ignored) { }
            camera.release();
            camera = null;
        }
        if (surfaceTexture != null) {
            surfaceTexture.release();
            surfaceTexture = null;
        }
        if (cameraThread != null) {
            cameraThread.quitSafely();
            cameraThread = null;
            cameraHandler = null;
        }
    }
}
