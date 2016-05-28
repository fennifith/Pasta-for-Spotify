package pasta.streamer.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.afollestad.async.Action;

import pasta.streamer.utils.StaticUtils;

public class CustomImageView extends ImageView {

    public CustomImageView(Context context) {
        super(context);
    }

    public CustomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void transition(final Drawable second) {
        if (second == null) return;
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
                Bitmap image2 = null;

                try {
                    image2 = StaticUtils.drawableToBitmap(second);
                    if (image2 != null) {
                        image2 = ThumbnailUtils.extractThumbnail(image2, size, size);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return image2;
            }

            @Override
            protected void done(@Nullable final Bitmap result) {
                if (result == null) {
                    setImageDrawable(second);
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
}
