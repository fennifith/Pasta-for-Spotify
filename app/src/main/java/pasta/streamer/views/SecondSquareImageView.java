package pasta.streamer.views;

import android.content.Context;
import android.util.AttributeSet;

public class SecondSquareImageView extends CustomImageView {

    public SecondSquareImageView(Context context) {
        super(context);
    }

    public SecondSquareImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SecondSquareImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int size = getMeasuredHeight();
        setMeasuredDimension(size, size);
    }
}