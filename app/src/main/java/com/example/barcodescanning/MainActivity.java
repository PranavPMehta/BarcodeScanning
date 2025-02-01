package com.example.barcodescanning;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

import static com.example.barcodescanning.ScannedDetails.netBarcodeNos;

public class MainActivity extends AppCompatActivity {


    private SurfaceView surfaceView;
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private static final int REQUEST_CAMERA_PERMISSION = 201;
    private ToneGenerator toneGen1;
    private TextView barcodeText;
    private String barcodeData;
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String RADIO_BUTTON_STATE = "radioButtonState";
    public static RadioButton allowDuplicates,disAllowDuplicates;
    private RadioGroup radioGroup;

    private Button btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(MainActivity.this, ScannedDetails.class);

        toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        surfaceView = findViewById(R.id.surface_view);
        barcodeText = findViewById(R.id.barcode_text);
        btnNext=findViewById(R.id.btnNext);
        radioGroup=findViewById(R.id.radioGroup);
        allowDuplicates=findViewById(R.id.allowDuplicate);
        disAllowDuplicates=findViewById(R.id.disAllowDuplicate);
        initialiseDetectorsAndSources();

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int savedState = preferences.getInt(RADIO_BUTTON_STATE, R.id.allowDuplicate);

        // Set the previously saved radio button to be selected
        radioGroup.check(savedState);

        // Set an OnCheckedChangeListener to save the selected state
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            // Save the selected radio button's ID to SharedPreferences
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt(RADIO_BUTTON_STATE, checkedId);
            editor.apply();
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (barcodeText.getText().toString().equals("Barcode Text")) {
                    Toast.makeText(MainActivity.this, "Please scan the barcode and then click next", Toast.LENGTH_SHORT).show();
                } else {
                    intent.putExtra("barcodeNetWeight",barcodeText.getText().toString());
                    startActivity(intent);
                    finish();
                }
            }
        });
    }

    private void initialiseDetectorsAndSources() {

        //Toast.makeText(getApplicationContext(), "Barcode scanner started", Toast.LENGTH_SHORT).show();

        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.ALL_FORMATS)
                .build();

        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(1920, 1080)
                .setAutoFocusEnabled(true) //you should add this feature
                .build();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        cameraSource.start(surfaceView.getHolder());
                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this, new
                                String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });


        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
                // Toast.makeText(getApplicationContext(), "To prevent memory leaks barcode scanner has been stopped", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if (barcodes.size() != 0) {


                    barcodeText.post(new Runnable() {

                        @Override
                        public void run() {
//                            System.out.println("Barcode Value " + barcodes.size());
//                            System.out.println("Barcode Value " + barcodes.valueAt(0).displayValue);
//                            System.out.println("Barcode Value " + barcodes.valueAt(0).rawValue);
//                            System.out.println("Barcode Value " + barcodes.valueAt(0).contactInfo);
                            if (barcodes.valueAt(0).email != null) {
                                barcodeText.removeCallbacks(null);
                                barcodeData = barcodes.valueAt(0).email.address;
                                barcodeData=barcodeData.trim();
                                barcodeText.setText(barcodeData);

                                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
                                btnNext.setEnabled(true);
                            } else if (barcodes.valueAt(0).displayValue != null) {
                                barcodeData = barcodes.valueAt(0).displayValue;
                                barcodeData=barcodeData.trim();
                                barcodeText.setText(barcodeData);
                                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
                                btnNext.setEnabled(true);
                            }
                            if(disAllowDuplicates.isChecked() && netBarcodeNos.contains(barcodeData)){
                                Toast.makeText(getApplicationContext(), "This is duplicate barcode, Re-Scan.", Toast.LENGTH_SHORT).show();
                                btnNext.setEnabled(false);
                            }
                        }
                    });
                }
            }
        });
    }


    @Override
    protected void onPause() {
        super.onPause();
        getSupportActionBar().hide();
        cameraSource.release();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSupportActionBar().hide();
        initialiseDetectorsAndSources();
    }

}