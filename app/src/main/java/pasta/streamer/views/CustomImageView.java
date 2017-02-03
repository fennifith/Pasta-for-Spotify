package pasta.streamer.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.bumptech.glide.DrawableRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import pasta.streamer.utils.ImageUtils;

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

    public void transition(final Bitmap bitmap) {
        if (bitmap == null || bitmap.getWidth() < 1 || bitmap.getHeight() < 1) return;

        Animation exitAnim = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out);
        exitAnim.setDuration(150);
        exitAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                setImageBitmap(bitmap);
                Animation enterAnim = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in);
                enterAnim.setDuration(150);
                startAnimation(enterAnim);
            }
        });
        startAnimation(exitAnim);
    }

    public void transition(Drawable second) {
        transition(ImageUtils.drawableToBitmap(second));
    }

    public void loadFromUrl(String url) {
        load(Glide.with(getContext()).load(url));
    }

    public void load(final DrawableRequestBuilder request) {
        if (getWidth() > 0 && getHeight() > 0) {
            request.dontAnimate().into(new SimpleTarget<GlideDrawable>(getWidth(), getHeight()) {
                @Override
                public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                    transition(resource);
                }
            });
        } else {
            getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    load(request);
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });
        }
    }

}
