package com.example.ok.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class FileUtil {
    private static final String TAG = "FileUtil";

    public static File getFileFromUri(Context context, Uri uri) throws IOException {
        String fileName = getFileName(context, uri);
        File tempFile = createTempFile(context, fileName);
        copyFile(context, uri, tempFile);
        return tempFile;
    }

    private static String getFileName(Context context, Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting filename from uri", e);
            }
        }

        if (result == null) {
            result = uri.getLastPathSegment();
        }

        // Ensure we have a valid filename
        if (result == null) {
            String extension = getFileExtension(context, uri);
            result = UUID.randomUUID().toString() + "." + (extension != null ? extension : "jpg");
        }

        return result;
    }

    private static String getFileExtension(Context context, Uri uri) {
        ContentResolver contentResolver = context.getContentResolver();
        String mimeType = contentResolver.getType(uri);

        if (mimeType == null) {
            return "jpg";  // Default to jpg if we can't determine type
        }

        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
    }

    private static File createTempFile(Context context, String fileName) throws IOException {
        File cacheDir = context.getCacheDir();
        return new File(cacheDir, fileName);
    }

    private static void copyFile(Context context, Uri uri, File destFile) throws IOException {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(destFile)) {

            if (inputStream == null) {
                throw new IOException("Could not open input stream for URI: " + uri);
            }

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        }
    }
}