package com.example.ok.utils;

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

public class FileUtils {

    private static final String TAG = "FileUtils";

    public static File createFileFromUri(Context context, Uri uri) {
        try {
            // Get the file name and ensure it has proper extension
            String fileName = getFileName(context, uri);
            if (fileName == null) {
                fileName = "image_" + System.currentTimeMillis() + ".jpg";
            }

            // Ensure proper extension
            fileName = ensureImageExtension(fileName, context, uri);

            Log.d(TAG, "Creating file with name: " + fileName);

            // Create temporary file in cache directory
            File tempFile = new File(context.getCacheDir(), fileName);

            // Copy content from URI to temporary file
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                FileOutputStream outputStream = new FileOutputStream(tempFile);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                inputStream.close();
                outputStream.close();

                Log.d(TAG, "File created successfully: " + tempFile.getAbsolutePath());
                Log.d(TAG, "File size: " + tempFile.length() + " bytes");
                Log.d(TAG, "File name: " + tempFile.getName());

                return tempFile;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error creating file from URI", e);
        }

        return null;
    }

    private static String ensureImageExtension(String fileName, Context context, Uri uri) {
        // Check if already has image extension
        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg") ||
                lowerFileName.endsWith(".png") || lowerFileName.endsWith(".webp")) {
            return fileName;
        }

        // Try to get MIME type and determine extension
        String mimeType = context.getContentResolver().getType(uri);
        Log.d(TAG, "MIME type from ContentResolver: " + mimeType);

        if (mimeType != null) {
            if (mimeType.equals("image/jpeg") || mimeType.equals("image/jpg")) {
                return fileName + ".jpg";
            } else if (mimeType.equals("image/png")) {
                return fileName + ".png";
            } else if (mimeType.equals("image/webp")) {
                return fileName + ".webp";
            }
        }

        // Default to .jpg if can't determine
        return fileName + ".jpg";
    }

    private static String getFileName(Context context, Uri uri) {
        String fileName = null;

        if (uri.getScheme().equals("content")) {
            ContentResolver contentResolver = context.getContentResolver();
            Cursor cursor = contentResolver.query(uri, null, null, null, null);

            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (nameIndex >= 0) {
                            fileName = cursor.getString(nameIndex);
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        }

        if (fileName == null) {
            fileName = uri.getPath();
            if (fileName != null) {
                int cut = fileName.lastIndexOf('/');
                if (cut != -1) {
                    fileName = fileName.substring(cut + 1);
                }
            }
        }

        return fileName;
    }

    public static boolean isValidImageFile(File file) {
        if (file == null || !file.exists()) {
            return false;
        }

        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".jpg") ||
                fileName.endsWith(".jpeg") ||
                fileName.endsWith(".png") ||
                fileName.endsWith(".webp");
    }

    public static String getMimeType(File file) {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/*"; // Default
    }
}