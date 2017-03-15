package com.weigan.loopview.bean;

import android.graphics.Bitmap;

/**
 * Created by wuhang on 17/3/15.
 */

public class Bean {
    private String mFirtLine;
    private String mSecondLine;
    private Bitmap mBitmap;

    private int mBitmapWidth;
    private int mBitmapHeight;

    public Bean() {
    }

    public Bean(String firtLine, String secondLine, Bitmap bitmap) {
        mFirtLine = firtLine;
        mSecondLine = secondLine;
        mBitmap = bitmap;
        if (mBitmap!=null) {
            mBitmapWidth = mBitmap.getWidth();
            mBitmapHeight = mBitmap.getHeight();
        }
    }

    public String getFirtLine() {
        return mFirtLine;
    }

    public void setFirtLine(String firtLine) {
        mFirtLine = firtLine;
    }

    public String getSecondLine() {
        return mSecondLine;
    }

    public void setSecondLine(String secondLine) {
        mSecondLine = secondLine;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
        if (mBitmap!=null) {
            mBitmapWidth = mBitmap.getWidth();
            mBitmapHeight = mBitmap.getHeight();
        }
    }


    public int getBitmapWidth() {
        return mBitmapWidth;
    }

    public int getBitmapHeight() {
        return mBitmapHeight;
    }

    @Override
    public String toString() {
        return "Bean{" +
                "mFirtLine='" + mFirtLine + '\'' +
                ", mSecondLine='" + mSecondLine + '\'' +
                '}';
    }
}
