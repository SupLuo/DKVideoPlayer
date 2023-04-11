package xyz.doikki.dkplayer.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;

import java.lang.reflect.Field;

import droid.unicstar.videoplayer.UNSVideoView;
import xyz.doikki.videoplayer.controller.MediaController;

public final class Utils {

    private Utils() {
    }

    public static Object getCurrentPlayerFactoryInVideoView(MediaController controlWrapper) {
        Object playerFactory = null;
        try {
            Field mPlayerControlField = controlWrapper.getClass().getDeclaredField("mPlayer");
            mPlayerControlField.setAccessible(true);
            Object playerControl = mPlayerControlField.get(controlWrapper);
            if (playerControl instanceof UNSVideoView) {
                playerFactory = getCurrentPlayerFactoryInVideoView((UNSVideoView) playerControl);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return playerFactory;
    }

    public static Object getCurrentPlayerFactoryInVideoView(UNSVideoView videoView) {
        Object playerFactory = null;
        try {
            Field mPlayerFactoryField = videoView.getClass().getDeclaredField("mPlayerFactory");
            mPlayerFactoryField.setAccessible(true);
            playerFactory = mPlayerFactoryField.get(videoView);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return playerFactory;
    }

    /**
     * 将View从父控件中移除
     */
    public static void removeViewFormParent(View v) {
        if (v == null) return;
        ViewParent parent = v.getParent();
        if (parent instanceof FrameLayout) {
            ((FrameLayout) parent).removeView(v);
        }
    }


    /**
     * Gets the corresponding path to a file from the given content:// URI
     *
     * @param context    Context
     * @param contentUri The content:// URI to find the file path from
     * @return the file path as a string
     */
    public static String getFileFromContentUri(Context context, Uri contentUri) {
        if (contentUri == null) {
            return null;
        }
        if (ContentResolver.SCHEME_FILE.equals(contentUri.getScheme())) {
            return contentUri.toString();
        }
        String filePath = null;
        String[] filePathColumn = {MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME};
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(contentUri, filePathColumn, null,
                null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            filePath = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
            cursor.close();
        }
        return filePath;
    }

}
