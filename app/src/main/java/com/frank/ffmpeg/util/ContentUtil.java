package com.frank.ffmpeg.util;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ContentUtil {

    private static final String TAG = "ImprovedContentUtil";

    /**
     * Modern approach to get a usable file path from URI
     * For Android 10+ with scoped storage, this may copy the file to app's cache directory
     */
    public static String getPath(Context context, Uri uri) {
        if (uri == null) return null;

        // Try different approaches based on URI scheme and Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return getPathForModernAndroid(context, uri);
        } else {
            return getPathForLegacyAndroid(context, uri);
        }
    }

    /**
     * Handle path resolution for Android 10+ (API 29+) with scoped storage
     */
    @TargetApi(Build.VERSION_CODES.Q)
    private static String getPathForModernAndroid(Context context, Uri uri) {
        String scheme = uri.getScheme();

        if ("file".equalsIgnoreCase(scheme)) {
            return uri.getPath();
        }

        if ("content".equalsIgnoreCase(scheme)) {
            // Try to get real path first
            String realPath = getRealPathFromURI(context, uri);
            if (realPath != null && new File(realPath).exists()) {
                return realPath;
            }

            // If real path doesn't work, copy to cache directory
            return copyFileToCache(context, uri);
        }

        return null;
    }

    /**
     * Handle path resolution for older Android versions
     */
    private static String getPathForLegacyAndroid(Context context, Uri uri) {
        if (uri == null) return null;

        String scheme = uri.getScheme();

        if ("file".equalsIgnoreCase(scheme)) {
            return uri.getPath();
        }

        if ("content".equalsIgnoreCase(scheme)) {
            // Try document provider first
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
                    DocumentsContract.isDocumentUri(context, uri)) {
                return getPathFromDocumentProvider(context, uri);
            }

            // Fall back to content resolver
            return getDataColumn(context, uri, null, null);
        }

        return null;
    }

    /**
     * Get real path from URI using various methods
     */
    private static String getRealPathFromURI(Context context, Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
                DocumentsContract.isDocumentUri(context, uri)) {
            return getPathFromDocumentProvider(context, uri);
        }

        return getDataColumn(context, uri, null, null);
    }

    /**
     * Handle document provider URIs (improved version)
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static String getPathFromDocumentProvider(Context context, Uri uri) {
        try {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                String docId = DocumentsContract.getDocumentId(uri);
                String[] split = docId.split(":");
                String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // For Android 10+, use MediaStore API
                        return getPathFromMediaStore(context, uri);
                    } else {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }
                } else {
                    // Handle SD card and other storage
                    return getPathFromSecondaryStorage(type, split[1]);
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                return getPathFromDownloadsProvider(context, uri);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                return getPathFromMediaProvider(context, uri);
            }
            // Google Drive and other cloud providers
            else if (isGoogleDriveDocument(uri) || isCloudDocument(uri)) {
                return copyFileToCache(context, uri);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting path from document provider", e);
        }

        return null;
    }

    /**
     * Improved downloads provider handling
     */
    private static String getPathFromDownloadsProvider(Context context, Uri uri) {
        try {
            String id = DocumentsContract.getDocumentId(uri);

            // Handle "raw:" prefixed IDs
            if (id.startsWith("raw:")) {
                return id.replaceFirst("raw:", "");
            }

            // Try different download URIs
            String[] downloadUris = {
                    "content://downloads/public_downloads",
                    "content://downloads/my_downloads",
                    "content://downloads/all_downloads"
            };

            for (String baseUri : downloadUris) {
                try {
                    Uri contentUri = ContentUris.withAppendedId(Uri.parse(baseUri), Long.valueOf(id));
                    String path = getDataColumn(context, contentUri, null, null);
                    if (path != null) {
                        return path;
                    }
                } catch (NumberFormatException e) {
                    // Continue to next URI
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting path from downloads provider", e);
        }

        return null;
    }

    /**
     * Get path from MediaStore for Android 10+
     */
    private static String getPathFromMediaStore(Context context, Uri uri) {
        try {
            Cursor cursor = context.getContentResolver().query(uri,
                    new String[]{MediaStore.MediaColumns.DATA}, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
                        if (columnIndex >= 0) {
                            return cursor.getString(columnIndex);
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting path from MediaStore", e);
        }
        return null;
    }

    /**
     * Handle secondary storage (SD cards, USB drives)
     */
    private static String getPathFromSecondaryStorage(String storageId, String relativePath) {
        try {
            // Common paths for secondary storage
            String[] possiblePaths = {
                    "/storage/" + storageId + "/" + relativePath,
                    "/mnt/extSdCard/" + relativePath,
                    "/mnt/sdcard-ext/" + relativePath,
                    "/storage/extSdCard/" + relativePath
            };

            for (String path : possiblePaths) {
                if (new File(path).exists()) {
                    return path;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting path from secondary storage", e);
        }
        return null;
    }

    /**
     * Improved media provider handling
     */
    private static String getPathFromMediaProvider(Context context, Uri uri) {
        try {
            String docId = DocumentsContract.getDocumentId(uri);
            String[] split = docId.split(":");
            String type = split[0];

            Uri contentUri = null;
            if ("image".equals(type)) {
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            } else if ("video".equals(type)) {
                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            } else if ("audio".equals(type)) {
                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            }

            String selection = "_id=?";
            String[] selectionArgs = new String[]{split[1]};

            return getDataColumn(context, contentUri, selection, selectionArgs);
        } catch (Exception e) {
            Log.e(TAG, "Error getting path from media provider", e);
        }
        return null;
    }

    /**
     * Copy file from URI to app's cache directory (fallback for scoped storage)
     */
    private static String copyFileToCache(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            String fileName = getFileName(context, uri);
            if (TextUtils.isEmpty(fileName)) {
                fileName = "temp_file_" + System.currentTimeMillis();
            }

            File cacheFile = new File(context.getCacheDir(), fileName);
            FileOutputStream outputStream = new FileOutputStream(cacheFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();

            return cacheFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Error copying file to cache", e);
        }
        return null;
    }

    /**
     * Get file name from URI
     */
    public static String getFileName(Context context, Uri uri) {
        String fileName = null;

        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting file name", e);
            }
        }

        if (TextUtils.isEmpty(fileName)) {
            fileName = uri.getLastPathSegment();
        }

        return fileName;
    }

    /**
     * Get file size from URI
     */
    public static long getFileSize(Context context, Uri uri) {
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex >= 0) {
                    return cursor.getLong(sizeIndex);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting file size", e);
        }
        return -1;
    }

    /**
     * Get MIME type from URI
     */
    public static String getMimeType(Context context, Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        if (mimeType == null) {
            String fileName = getFileName(context, uri);
            if (fileName != null) {
                String extension = MimeTypeMap.getFileExtensionFromUrl(fileName);
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            }
        }
        return mimeType;
    }

    /**
     * Enhanced data column retrieval with better error handling
     */
    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        if (uri == null) return null;

        String column = "_data";
        String[] projection = {column};

        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndexOrThrow(column);
                String path = cursor.getString(index);
                if (path != null && new File(path).exists()) {
                    return path;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting data column", e);
        }
        return null;
    }

    // Provider identification methods
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private static boolean isGoogleDriveDocument(Uri uri) {
        return "com.google.android.apps.docs.storage".equals(uri.getAuthority());
    }

    private static boolean isCloudDocument(Uri uri) {
        String authority = uri.getAuthority();
        return authority != null && (
                authority.contains("cloud") ||
                        authority.contains("drive") ||
                        authority.contains("dropbox") ||
                        authority.contains("onedrive")
        );
    }

    /**
     * Utility method to check if we can get a direct file path
     */
    public static boolean canGetDirectPath(Context context, Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // On Android 10+, direct paths are limited due to scoped storage
            return false;
        }

        String path = getPath(context, uri);
        return path != null && new File(path).exists();
    }
}