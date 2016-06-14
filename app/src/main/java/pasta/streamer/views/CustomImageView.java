package pasta.streamer.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.afollestad.async.Action;

import pasta.streamer.utils.StaticUtils;

public class CustomImageView extends AppCompatImageView {

    public CustomImageView(Context context) {
        super(context);
    }

    public CustomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void transition(final Bitmap second) {
        if (second == null || second.getWidth() < 1 || second.getHeight() < 1) return;
        final int size = Math.min(getMeasuredWidth(), getMeasuredHeight());
        new Action<Bitmap>() {
            @NonNull
            @Override
            public String id() {
                return "transitionDrawable";
            }

            @Nullable
            @Override
            protected Bitmap run() throws InterruptedException {
                try {
                    return ThumbnailUtils.extractThumbnail(second, size, size);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void done(@Nullable final Bitmap result) {
                if (result == null) {
                    setImageBitmap(second);
                    return;
                }

                Animation exitAnim = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out);
                exitAnim.setDuration(150);
                exitAnim.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation animation) {

                    }

                    @Override public void onAnimationRepeat(Animation animation) {

                    }

                    @Override public void onAnimationEnd(Animation animation) {
                        setImageBitmap(result);
                        Animation enterAnim = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in);
                        enterAnim.setDuration(150);
                        startAnimation(enterAnim);
                    }
                });
                startAnimation(exitAnim);
            }
        }.execute();
    }

    public void transition(Drawable second) {
        transition(StaticUtils.drawableToBitmap(second));
    }
}
