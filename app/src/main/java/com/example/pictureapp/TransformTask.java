package com.example.pictureapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RSInvalidStateException;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.support.v8.renderscript.Type;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;

import com.example.pictureapp.MainActivity;
import com.example.pictureapp.ScriptC_image_transforms;

import java.io.File;

/**
 * Created by ddao on 1/26/18.
 */

public class TransformTask {
    private ImageView _target;
    private Context _context;
    private Button _saveButton;
    private Button _undoButton;
    private ImageManager imageManager;
    private Uri transformed_images[] = new Uri[100];
    private int currentIndex = 0;

    private static final String TAG = "TransFormLog";

    public TransformTask(ImageView target,
                         Button saveButton,
                         Button undoButton,
                         Context context) {
        _target = target;
        _context = context;
        _undoButton = undoButton;
        _saveButton = saveButton;
        imageManager = new ImageManager(_context);
    }

    private void delete(Uri uri) {
        File file = new File(uri.toString());
        file.delete();
    }

    public void undo() {
        try {
            // Delete the last one
            delete(transformed_images[currentIndex]);
            currentIndex = currentIndex - 1;
            Bitmap bitmap = MediaStore.Images.Media.
                                getBitmap(_context.getContentResolver(),
                                        transformed_images[currentIndex]);
            _target.setImageBitmap(bitmap);

            if (currentIndex == 0) { _undoButton.setEnabled(false); }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void reset(Uri uri) {
        if (currentIndex > 0) {
            for (int i = 0; i <= currentIndex; i++) {
                delete(transformed_images[i]);
            }
        }
        currentIndex = 0;
        transformed_images[currentIndex] = uri;
    }

    public void saveCurrentImage() {
        if (currentIndex > 0) {
            imageManager.addPhotoToGallery(transformed_images[currentIndex]);
        }
    }

    private void postBackground(Bitmap result) {
        _target.setImageBitmap(result);
        _saveButton.setEnabled(true);
        Uri uri = imageManager.saveImage(result, false);
        currentIndex++;
        transformed_images[currentIndex] = uri;
    }

    public void invokeBlur() {
        new BlurTask().execute();
    }
    public void invokeBulge() {
        new BulgeTask().execute();
    }
    public void invokeFishEye() { new FishEyeTask().execute(); }
    public void invokeSwirl() { new SwirlTask().execute(); }

    private class BulgeTask extends AsyncTask<Void, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(Void... params) {
            Bitmap bitmap =((BitmapDrawable) _target.getDrawable()).getBitmap();
            Bitmap out = bulge(bitmap);
            return out;
        }

        protected void onPostExecute(Bitmap result) {
            postBackground(result);
        }
    }

    private class BlurTask extends AsyncTask<Void, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(Void... params) {
            Bitmap bitmap =((BitmapDrawable) _target.getDrawable()).getBitmap();
            Bitmap out = blur(bitmap, 25.0f);
            return out;
        }

        protected void onPostExecute(Bitmap result) {
            postBackground(result);
        }
    }

    private class FishEyeTask extends AsyncTask<Void, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(Void... params) {
            Bitmap bitmap =((BitmapDrawable) _target.getDrawable()).getBitmap();
            Bitmap out = fisheye(bitmap);
            return out;
        }

        protected void onPostExecute(Bitmap result) {
            postBackground(result);
        }
    }

    private class SwirlTask extends AsyncTask<Void, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(Void... params) {
            Bitmap bitmap =((BitmapDrawable) _target.getDrawable()).getBitmap();
            Bitmap out = swirl(bitmap);
            return out;
        }

        protected void onPostExecute(Bitmap result) {
            postBackground(result);
        }
    }

    private Bitmap fisheye(Bitmap bitmap) {
        //Create renderscript
        RenderScript rs = RenderScript.create(_context);
        //Create allocation from Bitmap
        Allocation allocation = Allocation.createFromBitmap(rs, bitmap);
        //Create allocation with the same type
        Allocation outAllocation = Allocation.createTyped(rs, allocation.getType());
        // Create script
        ScriptC_image_transforms image_transforms = new ScriptC_image_transforms(rs);
        // Set input
        image_transforms.set_height(bitmap.getHeight());
        image_transforms.set_width(bitmap.getWidth());
        image_transforms.bind_input(allocation);

        // Call Fisheye script
        image_transforms.forEach_fisheye(allocation, outAllocation);
        outAllocation.copyTo(bitmap);

        try {
            allocation.destroy();
        } catch (RSInvalidStateException e) {}
        try {
            outAllocation.destroy();
        } catch (RSInvalidStateException e) {}
        try {
            image_transforms.destroy();
        } catch (RSInvalidStateException e) {}
        try {
            rs.destroy();
        } catch (RSInvalidStateException e) {}
        return bitmap;
    }

    private Bitmap swirl(Bitmap bitmap) {
        //Create renderscript
        RenderScript rs = RenderScript.create(_context);
        //Create allocation from Bitmap
        Allocation allocation = Allocation.createFromBitmap(rs, bitmap);
        //Create allocation with the same type
        Allocation outAllocation = Allocation.createTyped(rs, allocation.getType());
        // Create script
        ScriptC_image_transforms image_transforms = new ScriptC_image_transforms(rs);
        // Set input
        image_transforms.set_height(bitmap.getHeight());
        image_transforms.set_width(bitmap.getWidth());
        image_transforms.bind_input(allocation);

        // Call swirl script
        image_transforms.forEach_swirl(allocation, outAllocation);
        outAllocation.copyTo(bitmap);

        try {
            allocation.destroy();
        } catch (RSInvalidStateException e) {}
        try {
            outAllocation.destroy();
        } catch (RSInvalidStateException e) {}
        try {
            image_transforms.destroy();
        } catch (RSInvalidStateException e) {}
        try {
            rs.destroy();
        } catch (RSInvalidStateException e) {}
        return bitmap;
    }

    private Bitmap bulge(Bitmap bitmap) {
        //Create renderscript
        RenderScript rs = RenderScript.create(_context);
        //Create allocation from Bitmap
        Allocation allocation = Allocation.createFromBitmap(rs, bitmap);
        //Create allocation with the same type
        Allocation outAllocation = Allocation.createTyped(rs, allocation.getType());
        // Create script
        ScriptC_image_transforms image_transforms = new ScriptC_image_transforms(rs);
        // Set input
        image_transforms.set_height(bitmap.getHeight());
        image_transforms.set_width(bitmap.getWidth());
        image_transforms.bind_input(allocation);

        // Call Bulge script
        image_transforms.forEach_bulge(allocation, outAllocation);
        outAllocation.copyTo(bitmap);

        try {
            allocation.destroy();
        } catch (RSInvalidStateException e) {}

        try {
            outAllocation.destroy();
        } catch (RSInvalidStateException e) {}

        try {
            image_transforms.destroy();
        } catch (RSInvalidStateException e) {}

        try {
            rs.destroy();
        } catch (RSInvalidStateException e) {}

        return bitmap;
    }

    private Bitmap blur(Bitmap bitmap, float radius) {
        // Reference: https://medium.com/@qhutch/android-simple-and-fast-image-processing-with-renderscript-2fa8316273e1
        //Create renderscript
        RenderScript rs = RenderScript.create(_context);
        //Create allocation from Bitmap
        Allocation allocation = Allocation.createFromBitmap(rs, bitmap);
        //Create allocation with the same type
        Allocation blurredAllocation = Allocation.createTyped(rs, allocation.getType());
        //Create script
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        //Set blur radius (maximum 25.0)
        blurScript.setRadius(radius);
        //Set input for script
        blurScript.setInput(allocation);
        //Call script for output allocation
        blurScript.forEach(blurredAllocation);

        //Copy script result into bitmap
        blurredAllocation.copyTo(bitmap);

        //Destroy everything to free memory
        try {
            allocation.destroy();
        } catch (RSInvalidStateException e) {}
        try {
            blurredAllocation.destroy();
        } catch (RSInvalidStateException e) {}
        try {
            blurScript.destroy();
        } catch (RSInvalidStateException e) {}
        try {
            rs.destroy();
        } catch (RSInvalidStateException e) {}

        return bitmap;
    }
}
