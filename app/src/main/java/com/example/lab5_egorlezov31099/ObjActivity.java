package com.example.lab5_egorlezov31099;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ObjActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private TextureView textureView;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;


    private final TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            if (ContextCompat.checkSelfPermission(ObjActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                ActivityCompat.requestPermissions(ObjActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            }
        }


        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    public void goHome(View view) {
        startActivity(new Intent(ObjActivity.this, MainActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_obj);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            openCamera();
        }
        numObjectsTextView = findViewById(R.id.textView);
        textureView = findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(surfaceTextureListener);
    }

    private void openCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String cameraId = cameraManager.getCameraIdList()[0];

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                return;
            }

            cameraManager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private TextView numObjectsTextView;
    private void createCameraPreview() {
        try {
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            assert surfaceTexture != null;
            surfaceTexture.setDefaultBufferSize(textureView.getWidth(), textureView.getHeight());
            Surface surface = new Surface(surfaceTexture);

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);

            LocalModel localModel =
                    new LocalModel.Builder()
                            .setAssetFilePath("mobilenet_v1_1.0_224_quantized_1_metadata_1.tflite")
                            .build();

            CustomObjectDetectorOptions customObjectDetectorOptions =
                    new CustomObjectDetectorOptions
                            .Builder(localModel)
                            .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
                            .enableMultipleObjects()
                            .enableClassification()
                            .setClassificationConfidenceThreshold(0.5f)
                            .setMaxPerObjectLabelCount(5)
                            .build();
            ObjectDetector objectDetector = ObjectDetection.getClient(customObjectDetectorOptions);

            cameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (cameraDevice == null) {
                        return;
                    }

                    cameraCaptureSession = session;
                    try {
                        cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                            @Override
                            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                                super.onCaptureCompleted(session, request, result);

                                Bitmap bitmap = textureView.getBitmap();

                                InputImage image = InputImage.fromBitmap(bitmap, 0);

                                objectDetector.process(image)
                                        .addOnSuccessListener(detectedObjects -> {
                                            ViewGroup frameLayoutContainer = findViewById(R.id.frameLayoutContainer);

                                            removePreviousLabelsAndOverlays(frameLayoutContainer);

                                            Set<Integer> drawnObjectIds = new HashSet<>();

                                            for (DetectedObject detectedObject : detectedObjects) {
                                                Rect boundingBox = detectedObject.getBoundingBox();
                                                RectF rectF = new RectF(boundingBox);

                                                int objectId = detectedObject.hashCode();

                                                if (drawnObjectIds.contains(objectId)) {
                                                    continue;
                                                }

                                                List<DetectedObject.Label> labelsOne = detectedObject.getLabels();
                                                for (DetectedObject.Label labelabelsFirst : labelsOne) {
                                                    Log.d("Detected Objects", "labelText: " + labelabelsFirst.getText());
                                                    Log.d("Detected Objects", "confidence: " + labelabelsFirst.getConfidence());
                                                    if (labelabelsFirst.getText().equals("sunglass") || labelabelsFirst.getText().equals("glasses") || labelabelsFirst.getText().equals("sunglasses")) {
                                                        drawnObjectIds.add(objectId);
                                                        BoxOverlay boxOverlay = new BoxOverlay(textureView.getContext(), rectF);
                                                        boxOverlay.setObjectId(objectId);
                                                        boxOverlay.setTag("overlay");
                                                        frameLayoutContainer.addView(boxOverlay);
                                                        frameLayoutContainer.postInvalidate();

                                                        TextView labelTextView = new TextView(textureView.getContext());
                                                        String confidenceText = String.format(Locale.getDefault(), "%s (%.2f)", labelabelsFirst.getText(), labelabelsFirst.getConfidence());
                                                        labelTextView.setText(confidenceText);
                                                        labelTextView.setTextColor(Color.WHITE);
                                                        labelTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                                                        labelTextView.setPadding(8, 8, 8, 8);
                                                        FrameLayout.LayoutParams labelLayoutParams = new FrameLayout.LayoutParams(
                                                                FrameLayout.LayoutParams.WRAP_CONTENT,
                                                                FrameLayout.LayoutParams.WRAP_CONTENT
                                                        );
                                                        labelLayoutParams.leftMargin = (int) rectF.left;
                                                        labelLayoutParams.topMargin = (int) rectF.top;
                                                        labelTextView.setLayoutParams(labelLayoutParams);
                                                        labelTextView.setTag("label");
                                                        frameLayoutContainer.addView(labelTextView);
                                                    }
                                                }
                                            }

                                            frameLayoutContainer.postInvalidate();
                                            int numObjects = drawnObjectIds.size();
                                            runOnUiThread(() -> {
                                                numObjectsTextView.setText("Glasses detected: " + numObjects);
                                            });
                                        })
                                        .addOnFailureListener(e -> {
                                            e.printStackTrace();
                                        });
                            }
                        }, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e("Camera2", "Camera configuration failed");
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void removePreviousLabelsAndOverlays(ViewGroup frameLayoutContainer) {
        int childCount = frameLayoutContainer.getChildCount();
        List<View> viewsToRemove = new ArrayList<>();

        for (int i = childCount - 1; i >= 0; i--) {
            View childView = frameLayoutContainer.getChildAt(i);
            Object tag = childView.getTag();
            if (tag != null && (tag.equals("label") || tag.equals("overlay"))) {
                viewsToRemove.add(childView);
            }
        }

        for (View view : viewsToRemove) {
            frameLayoutContainer.removeView(view);
        }

        frameLayoutContainer.invalidate();
    }

    private void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        super.onPause();
    }
}