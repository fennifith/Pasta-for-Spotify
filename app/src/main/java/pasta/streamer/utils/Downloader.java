package pasta.streamer.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import com.bumptech.glide.Glide;

import java.util.concurrent.ExecutionException;

import pasta.streamer.R;

public class Downloader {

    public static Bitmap downloadImage(Context context, String src) {
        try {
            return Glide.with(context).load(src).asBitmap().error(R.drawable.preload).into(-1, -1).get();
        } catch (InterruptedException | ExecutionException i) {
            i.printStackTrace();
        }
        Bitmap bmp = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.TRANSPARENT);
        return bmp;
    }

}
