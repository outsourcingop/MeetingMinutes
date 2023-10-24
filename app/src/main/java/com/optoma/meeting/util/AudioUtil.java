package com.optoma.meeting.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;

public class AudioUtil {
   public static final String TAG = AudioUtil.class.getSimpleName();

   public static ArrayList<Uri> getAllAudioUri(Context context) {
      ContentResolver contentResolver = context.getContentResolver();
      Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
      String[] projection = new String[]{
          MediaStore.Audio.Media._ID,
          MediaStore.Audio.Media.DISPLAY_NAME
      };

      String selection = MediaStore.Audio.Media.DISPLAY_NAME + " LIKE ?";
      String[] selectionArgs = new String[]{"%1693489709148%"};

      String sortOrder = MediaStore.Audio.Media.DATE_MODIFIED + " DESC";

      Cursor cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder);
      Log.d(TAG, "count: " + cursor.getCount());
      ArrayList<Uri> audioList = new ArrayList<>();


      if (cursor.moveToFirst()) {
         do {
            int idColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
            String name = cursor.getString(nameIndex);
            Log.d(TAG, "name: " + name);
            Uri audioUri = Uri.withAppendedPath(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    cursor.getString(idColumnIndex)
            );
            audioList.add(audioUri);
         } while (cursor.moveToNext());
      }
      cursor.close();

      return audioList;
   }

   // duration example 00:50:47.57
   public static int calculateSegments(String duration) {
      String[] parts = duration.split(":");
      int hours = Integer.parseInt(parts[0]);
      int minutes = Integer.parseInt(parts[1]);
      double seconds = Double.parseDouble(parts[2]);

      double totalSeconds = (hours * 3600) + (minutes * 60) + seconds;

      // how many 8 min pieces
      int segments = (int) Math.ceil(totalSeconds / 480.0);

      return segments;
   }
}
