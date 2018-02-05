package com.example.pictureapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by ddao on 2/5/18.
 */

public class ImageManager {
    Context _context;

    public ImageManager(Context context) {
        _context = context;
    }

    public Uri saveImage(Bitmap bitmap, boolean save) {
        // Save current bitmap public to Gallery
        File new_file = getOutputMediaFile();
        try {
            new_file.createNewFile();
            FileOutputStream ostream = new FileOutputStream(new_file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, ostream);
            ostream.close();
            // Add the photo to gallery after save
            Uri uri = Uri.fromFile(new_file);
            if (save) { addPhotoToGallery(uri); }
            return uri;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public File getOutputMediaFile(){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "PictureInteractive");
        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator +
                "IMG_"+ timeStamp + ".jpg");
    }

    public void addPhotoToGallery(Uri uri) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(uri); //your file uri
        _context.sendBroadcast(mediaScanIntent);
    }

}
