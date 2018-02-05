package com.example.pictureapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;

// import android.renderscript.Allocation;
// import android.renderscript.Element;
// import android.renderscript.RSInvalidStateException;
// import android.renderscript.RenderScript;
// import android.renderscript.ScriptIntrinsicBlur;
// import android.renderscript.Type;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v8.renderscript.Script;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.support.v8.renderscript.Type;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.RSInvalidStateException;
import android.widget.Toast;

import com.example.pictureapp.ScriptC_image_transforms;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity {
    /*
        Reference: https://androidkennel.org/android-camera-access-tutorial/
        Reference: http://codetheory.in/android-pick-select-image-from-gallery-with-intents/
     */
    private int PICK_IMAGE_REQUEST = 1;
    private TransformTask transformTask;
    private Button takePictureButton;
    private Button saveButton;
    private Button undoButton;
    private ImageManager imageManager;
    private ImageView imageView;
    private Bitmap editedImage;
    private Uri file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // This allows URI exposure when saving images to Gallery
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        saveButton = (Button) findViewById(R.id.save_button);
        saveButton.setEnabled(false);
        undoButton = (Button)findViewById(R.id.undo_button);
        undoButton.setEnabled(false);
        takePictureButton = (Button) findViewById(R.id.take_picture_button);
        imageView = (ImageView) findViewById(R.id.imageView);
        transformTask = new TransformTask(imageView, saveButton,
                                            undoButton, MainActivity.this);
        imageManager = new ImageManager(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            takePictureButton.setEnabled(false);
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE }, 0);
        }

        imageView.setOnTouchListener(new SimpleSwipeListener(MainActivity.this) {
            @Override
            public void onSwipeDown() {
                if (imageView.getDrawable() == null) {
                    Toast.makeText(MainActivity.this,
                                "Please take a new image or load an existing image!",
                                Toast.LENGTH_SHORT).show();
                } else {
                    transformTask.invokeBlur();
                    undoButton.setEnabled(true);
                    Toast.makeText(MainActivity.this, "Down",
                            Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onSwipeLeft() {
                if (imageView.getDrawable() == null) {
                    Toast.makeText(MainActivity.this,
                            "Please take a new image or load an existing image!",
                            Toast.LENGTH_SHORT).show();
                } else {
                    transformTask.invokeBulge();
                    undoButton.setEnabled(true);
                    Toast.makeText(MainActivity.this, "Left", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onSwipeUp() {
                if (imageView.getDrawable() == null) {
                    Toast.makeText(MainActivity.this,
                            "Please take a new image or load an existing image!",
                            Toast.LENGTH_SHORT).show();
                } else {
                    transformTask.invokeFishEye();
                    undoButton.setEnabled(true);
                    Toast.makeText(MainActivity.this, "Up", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onSwipeRight() {
                if (imageView.getDrawable() == null) {
                    Toast.makeText(MainActivity.this,
                            "Please take a new image or load an existing image!",
                            Toast.LENGTH_SHORT).show();
                } else {
                    transformTask.invokeSwirl();
                    undoButton.setEnabled(true);
                    Toast.makeText(MainActivity.this, "Right", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void SaveImageClick(View view) {
        // Make current bitmap public to Gallery
        transformTask.saveCurrentImage();
        // Disable Save button after save
        saveButton.setEnabled(false);
        // Show message
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                takePictureButton.setEnabled(true);
            }
        }
    }

    public void undo(View view) {
        transformTask.undo();
    }

    public void takePicture(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        file = Uri.fromFile(imageManager.getOutputMediaFile());
        intent.putExtra(MediaStore.EXTRA_OUTPUT, file);
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 100) {
            if (resultCode == RESULT_OK) {
                // Set image to the view
                imageView.setImageURI(file);
                // Add to Gallery
                imageManager.addPhotoToGallery(file);
                // Disable Save button
                saveButton.setEnabled(false);
                // Reset image transform
                transformTask.reset(file);
            }
        }

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            file = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), file);
                imageView.setImageBitmap(bitmap);
                saveButton.setEnabled(false);
                transformTask.reset(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void loadPicture(View view) {
        Intent intent = new Intent();
        // Show only images, no videos or anything else
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        // Always show the chooser (if there are multiple options available)
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }
}






