package com.optoma.meeting.util;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.optoma.meeting.R;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileUtil {

    public static final String TAG = FileUtil.class.getSimpleName();

    public static final String DOCUMENTS_DIR = "documents";

    public static File getExternalStoragePublicDirectory() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    }

    public static File createMeetingMinutesFile(Context context, long timestamp) {
        String fileName = "Meeting_Minutes_" + convertTimestampToDateTime(context,
                timestamp) + ".txt";
        return new File(getExternalStoragePublicDirectory(), fileName);
    }

    public static File createMeetingActionsFile(Context context, long timestamp) {
        String fileName = "Meeting_Actions_" + convertTimestampToDateTime(context,
                timestamp) + ".txt";
        return new File(getExternalStoragePublicDirectory(), fileName);
    }

    private static String convertTimestampToDateTime(Context context, long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat(context.getString(R.string.date_format));
        return sdf.format(date);
    }

    public static String getAbsolutePath(Context context, Uri inputUri) {
        boolean needToCheckUri = Build.VERSION.SDK_INT >= 19;
        String selection = null;
        String[] selectionArgs = null;

        // Uri is different in versions after KITKAT (Android 4.4), we need to deal with different Uris.
        Uri uri = inputUri;
        if (needToCheckUri && DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                String docId = DocumentsContract.getDocumentId(uri);
                String[] split = docId.split(":");
                return Environment.getExternalStorageDirectory().toString() + "/" + split[1];
            } else if (isDownloadsDocument(uri)) {
                String id = DocumentsContract.getDocumentId(uri);
                if (id.startsWith("raw:")) {
                    return id.replaceFirst("raw:", "");
                }
                String[] contentUriPrefixesToTry = new String[]{
                        "content://downloads/public_downloads",
                        "content://downloads/my_downloads"
                };
                for (String contentUriPrefix : contentUriPrefixesToTry) {
                    try {
                        Uri contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix),
                                Long.parseLong(id));
                        String path = getDataColumn(context, contentUri, null, null);
                        if (path != null && !path.equals("")) {
                            return path;
                        }
                    } catch (Exception e) {
                        Log.w(TAG, e);
                    }
                }
                String fileName = getFileName(context, uri);
                File cacheDir = getDocumentCacheDir(context);
                File file = generateFileName(fileName, cacheDir);
                String destinationPath = null;
                if (file != null) {
                    destinationPath = file.getAbsolutePath();
                    saveFileFromUri(context, uri, destinationPath);
                }
                return destinationPath;
            } else if (isMediaDocument(uri)) {
                String docId = DocumentsContract.getDocumentId(uri);
                String[] split = docId.split(":");
                String type = split[0];
                switch (type) {
                    case "image":
                        uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                        break;
                    case "video":
                        uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                        break;
                    case "audio":
                        uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                        break;
                }
                selection = "_id=?";
                selectionArgs = new String[]{split[1]};
            }
        }
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = new String[]{
                    MediaStore.Audio.Media.DATA
            };
            try (Cursor cursor = context.getContentResolver().query(uri, projection, selection,
                    selectionArgs, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(
                            MediaStore.Images.Media.DATA);
                    return cursor.getString(columnIndex);
                }
            } catch (Exception e) {
                Log.e(TAG, "on getPath, Exception %s", e);
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    public static String createNewAudioFilePath(String path) {
        if (path == null) {
            Log.e(TAG, "transcodeAudioResource failed. The absolute path is null");
            return null;
        }
        Log.d(TAG, "File Path: " + path);

        /**
         this command takes an input audio file,
         resamples it to a lower sample rate of 8000 samples per second,
         converts it to mono audio, and encodes it with the pcm_s16le codec.
         The resulting output file should contain a lower quality version of the original audio file,
         suitable for some specific use cases such as voice recording or telephony.
         */
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault());
        String timestamp = format.format(new Date());
        String newFileName = path.substring(path.lastIndexOf("/") + 1,
                path.lastIndexOf(".")) + "_" + timestamp + ".wav";
        String newFileAbsolutePath = path.substring(0,
                path.lastIndexOf("/")) + "/" + newFileName;
        File newFile = new File(newFileAbsolutePath);

        try {
            newFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (newFile.exists()) {
            newFile.setWritable(true);
        } else {
            Log.e(TAG, "transAudioResource: Fail to setWritable to " + newFileAbsolutePath);
        }

        return newFileAbsolutePath;
    }

    public static String createNewAudioFilePath(String path, int partNumber) {
        if (path == null) {
            Log.e(TAG, "transcodeAudioResource failed. The absolute path is null");
            return null;
        }
        Log.d(TAG, "File Path: " + path);

        /**
         this command takes an input audio file,
         resamples it to a lower sample rate of 8000 samples per second,
         converts it to mono audio, and encodes it with the pcm_s16le codec.
         The resulting output file should contain a lower quality version of the original audio file,
         suitable for some specific use cases such as voice recording or telephony.
         */
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault());
        String timestamp = format.format(new Date());
        String newFileName = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf(".")) +
                "_" +
                timestamp +
                "_" +
                partNumber +
                ".wav";
        String newFileAbsolutePath = path.substring(0,
                path.lastIndexOf("/")) + "/" + newFileName;
        File newFile = new File(newFileAbsolutePath);

        try {
            newFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (newFile.exists()) {
            newFile.setWritable(true);
        } else {
            Log.e(TAG, "transAudioResource: Fail to setWritable to " + newFileAbsolutePath);
        }

        return newFileAbsolutePath;
    }

    public static int extractPartNumber(String absolutePath) {
        String fileName = absolutePath.substring(absolutePath.lastIndexOf("/") + 1);

        Log.d(TAG, "fileName: " + fileName);
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex >= 0) {
            String fileNameWithoutExtension = fileName.substring(0, lastDotIndex);
            String[] lastInt = fileNameWithoutExtension.split("_");
            Log.d(TAG, "lastInt[lastInt.length - 1]: " + lastInt[lastInt.length - 1]);
            return Integer.parseInt(lastInt[lastInt.length - 1]);
        }

        return -1;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private static String getDataColumn(Context context, Uri uri, String selection,
            String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        String path = "";
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs
                    , null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(column);
                path = cursor.getString(columnIndex);
                return path;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return path;
    }

    private static String getFileName(@NonNull Context context, Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        String filename = null;
        if (mimeType == null) {
            String path = getAbsolutePath(context, uri);
            if (path == null) {
                filename = getName(uri.toString());
            } else {
                File file = new File(path);
                filename = file.getName();
            }
        } else {
            Cursor returnCursor = context.getContentResolver().query(uri, null, null, null, null);
            if (returnCursor != null) {
                int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                returnCursor.moveToFirst();
                filename = returnCursor.getString(nameIndex);
                returnCursor.close();
            }
        }
        return filename;
    }

    private static String getName(String filename) {
        if (filename == null) {
            return null;
        }
        int index = filename.lastIndexOf('/');
        return filename.substring(index + 1);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static File getDocumentCacheDir(@NonNull Context context) {
        File dir = new File(context.getCacheDir(), DOCUMENTS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    @Nullable
    private static File generateFileName(@Nullable String name, File directory) {
        if (name == null) {
            return null;
        }
        File file = new File(directory, name);
        if (file.exists()) {
            String fileName = name;
            String extension = "";
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex > 0) {
                fileName = name.substring(0, dotIndex);
                extension = name.substring(dotIndex);
            }
            int index = 0;
            while (file.exists()) {
                index++;
                name = fileName + '(' + index + ')' + extension;
                file = new File(directory, name);
            }
        }
        try {
            if (!file.createNewFile()) {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
        return file;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void saveFileFromUri(Context context, Uri uri, String destinationPath) {
        InputStream is = null;
        BufferedOutputStream bos = null;
        try {
            is = context.getContentResolver().openInputStream(uri);
            bos = new BufferedOutputStream(new FileOutputStream(destinationPath, false));
            byte[] buf = new byte[1024];
            is.read(buf);
            do {
                bos.write(buf);
            } while (is.read(buf) != -1);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (bos != null) {
                    bos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void deleteCache(Context context) {
        try {
            File dir = context.getCacheDir();
            deleteDir(dir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }
}
