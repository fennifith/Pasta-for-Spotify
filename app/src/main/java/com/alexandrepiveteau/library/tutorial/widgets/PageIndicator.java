/*
 * Copyright 2015 Alexandre Piveteau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alexandrepiveteau.library.tutorial.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Alexandre on 15.07.2015.
 */
public class PageIndicator extends View implements ViewPager.OnPageChangeListener{

    public static abstract class Engine {
        public abstract int getMeasuredHeight(int widthMeasuredSpec, int heightMeasuredSpec);
        public abstract int getMeasuredWidth(int widthMeasuredSpec, int heightMeasuredSpec);
        public abstract void onInitEngine(PageIndicator indicator, Context context);
        public abstract void onDrawIndicator(Canvas canvas);
    }

    private int mActualPosition;
    private float mPositionOffset;
    private int mTotalPages;
    private ViewPager mViewPager;

    private Engine mEngine;

    public PageIndicator(Context context) {
        this(context, null);
    }

    public PageIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PageIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mEngine = new DefaultPageIndicatorEngine();

        mEngine.onInitEngine(this, context);
        mTotalPages = 2;
    }

    public int getTotalPages() {
        return mTotalPages;
    }

    public int getActualPosition() {
        return mActualPosition;
    }

    public float getPositionOffset() {
        return mPositionOffset;
    }

    public void notifyNumberPagesChanged() {
        mTotalPages = mViewPager.getAdapter().getCount();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mEngine.onDrawIndicator(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(mEngine.getMeasuredWidth(widthMeasureSpec, heightMeasureSpec), mEngine.getMeasuredHeight(widthMeasureSpec, heightMeasureSpec));
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        mActualPosition = position;
        mPositionOffset = positionOffset;
        invalidate();
    }

    @Override
    public void onPageSelected(int position) {
        //Ignore
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        //Ignore
    }

    public void setEngine(Engine engine) {
        mEngine = engine;
        mEngine.onInitEngine(this, getContext());
        invalidate();
    }

    /**
     * You must call this AFTER setting the Adapter for the ViewPager, or it won't display the right amount of points.
     * @param viewPager
     */
    public void setViewPager(ViewPager viewPager) {
        mViewPager = viewPager;
        viewPager.addOnPageChangeListener(this);
        mTotalPages = viewPager.getAdapter().getCount();
        invalidate();
    }
}
