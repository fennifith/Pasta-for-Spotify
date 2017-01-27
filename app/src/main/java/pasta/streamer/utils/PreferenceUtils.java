package pasta.streamer.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;

import com.spotify.sdk.android.player.PlaybackBitrate;

import pasta.streamer.R;

public class PreferenceUtils {

    final public static String PRELOAD = "preload", LIMIT = "limit", DEBUG = "debug", THUMBNAILS = "thumbnails", RETRY = "retry", QUALITY = "playback_quality", ORDER = "order";
    final public static int ORDER_ADDED = 0, ORDER_NAME = 1, ORDER_ARTIST = 2, ORDER_ALBUM = 3, ORDER_LENGTH = 4, ORDER_RANDOM = 5;

    public static boolean isPreload(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PRELOAD, true);
    }

    public static int getLimit(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(LIMIT, 0);
    }

    public static boolean isDebug(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(DEBUG, false);
    }

    public static boolean isThumbnails(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(THUMBNAILS, true);
    }

    public static int getRetryCount(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(RETRY, 3);
    }

    public static PlaybackBitrate getQuality(Context context) {
        return PlaybackBitrate.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString(QUALITY, PlaybackBitrate.BITRATE_NORMAL.toString()));
    }

    public static int getSelectedQuality(Context context) {
        String s = PreferenceManager.getDefaultSharedPreferences(context).getString(QUALITY, PlaybackBitrate.BITRATE_NORMAL.toString());
        if (s.matches(PlaybackBitrate.BITRATE_LOW.toString())) return 0;
        else if (s.matches(PlaybackBitrate.BITRATE_NORMAL.toString())) return 1;
        else if (s.matches(PlaybackBitrate.BITRATE_HIGH.toString())) return 2;
        else return -1;
    }

    public static AlertDialog getOrderingDialog(final Context context, DialogInterface.OnClickListener onItemListener, DialogInterface.OnClickListener onClickListener) {
        return new AlertDialog.Builder(context)
                .setTitle(R.string.organize)
                .setSingleChoiceItems(R.array.organization, getTrackOrder(context), onItemListener)
                .setPositiveButton(R.string.save, onClickListener)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        d.dismiss();
                    }
                }).create();
    }

    public static int getTrackOrder(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(ORDER, ORDER_ADDED);
    }

    public static int getColumnNumber(Context context, boolean landscape) {
        int columns = 2;
        if (landscape) columns++;
        return columns;
    }
}
