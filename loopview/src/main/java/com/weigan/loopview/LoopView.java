package com.weigan.loopview;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.weigan.loopview.bean.Bean;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by Weidongjian on 2015/8/18.
 */
public class LoopView extends View {

    private float scaleX = 1.0F;

    private static final int DEFAULT_FIRST_LINE_TEXT_SIZE = (int) (Resources.getSystem().getDisplayMetrics().density * 12);
    private static final int DEFAULT_SECOND_LINE_TEXT_SIZE = (int) (Resources.getSystem().getDisplayMetrics().density * 15);

    private static final float DEFAULT_LINE_SPACE = 1f;

    private static final int DEFAULT_VISIBLE_ITEMS = 9;

    private static final int DEFAULT_SPACING_OF_FIRST_AND_SECOND = (int) (Resources.getSystem().getDisplayMetrics().density * 3);

    private static final int DEFAULT_SPACINF_OF_FIRST_AND_IMG = (int) (Resources.getSystem().getDisplayMetrics().density * 5);

    public enum ACTION {
        CLICK, FLING, DAGGLE
    }

    private Context mContext;

    Handler mHandler;
    private GestureDetector mFlingGestureDetector;
    OnItemSelectedListener mOnItemSelectedListener;

    // Timer mTimer;
    ScheduledExecutorService mExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> mFuture;

    private Paint mFirstLinePaintOuterText;
    private Paint mFirstLinePaintCenterText;

    private Paint mSecondLinePaintOuterText;
    private Paint mSecondLinePaintCenterText;

    private Paint mBitmapPaintOuter;
    private Paint mBitmapPaintCenter;

    private Paint mPaintIndicator;

//    private Paint mTestPaint;
//
//    private Paint mTestPaint2;

//    List<String> mItems;
    List<Bean> mItems;

    int mFirstLineTextSize;
    int mSecondLineTextSize;

    int mMaxItemHeight;

    int mOuterTextColor;

    int mCenterTextColor;
    int mDividerColor;

    //文本的行高相对于文字高度的倍数
    float mLineSpacingMultiplier;
    boolean isLoop;

    int mFirstLineY;
    int mSecondLineY;

    int mTotalScrollY;
    int mInitPosition;
    private int mSelectedItem;
    int mPreCurrentIndex;
    int mChange;

    int mItemsVisibleCount;

//    String[] mDrawingStrings;
    Bean[] mDrawingStrings;

    int mMeasuredHeight;
    int mMeasuredWidth;

    int mHalfCircumference; //圆的半周长
    int mRadius;

    private int mOffset = 0;
    private float mPreviousY;
    long mStartTime = 0;

    private Rect mTempRect = new Rect();

    private int mPaddingLeft, mPaddingRight;


    //第一行图片与文字之间的间距
    private int mSpacingOfFirstAndImg;

    //第一行文字和第二行文字之间的间距
    private int mSpacingOfFirstLineAndSecondLine;

    //第一行文字的高度
    private float mFirstLineStringHeight;

    //第二行文字的高度
    private float mSecondLineStingHeight;

    //绘制第一行文字的Y
    private float mFirstLineStringStartY;

    //绘制第二行文字的Y
    private float mSecondLineStringStartY;

    float[] src = new float[]{
            1, 0, 0, 0, 0,
            0, 1, 0, 0, 0,
            0, 0, 1, 0, 0,
            0, 0, 0, 1, 0};

    float[] white = new float[]{
            1, 0, 0, 0, 255,
            0, 1, 0, 0, 255,
            0, 0, 1, 0, 255,
            0, 0, 0, 1, 0};

    float[] black = new float[]{
            1, 0, 0, 0, -255,
            0, 1, 0, 0, -255,
            0, 0, 1, 0, -255,
            0, 0, 0, 1, 0};

    /**
     * set text line space, must more than 1
     * @param lineSpacingMultiplier
     */
    public void setLineSpacingMultiplier(float lineSpacingMultiplier) {
        if (lineSpacingMultiplier > 1.0f) {
            this.mLineSpacingMultiplier = lineSpacingMultiplier;
        }
    }


    /**
     * set outer text color
     * @param centerTextColor
     */
    public void setCenterTextColor(int centerTextColor) {
        this.mCenterTextColor = centerTextColor;
        mSecondLinePaintCenterText.setColor(centerTextColor);
    }

    /**
     * set center text color
     * @param outerTextColor
     */
    public void setOuterTextColor(int outerTextColor) {
        this.mOuterTextColor = outerTextColor;
        mSecondLinePaintOuterText.setColor(outerTextColor);
    }

    /**
     * set divider color
     * @param dividerColor
     */
    public void setDividerColor(int dividerColor) {
        this.mDividerColor = dividerColor;
        mPaintIndicator.setColor(dividerColor);
    }



    public LoopView(Context context) {
        super(context);
        initLoopView(context, null);
    }

    public LoopView(Context context, AttributeSet attributeset) {
        super(context, attributeset);
        initLoopView(context, attributeset);
    }

    public LoopView(Context context, AttributeSet attributeset, int defStyleAttr) {
        super(context, attributeset, defStyleAttr);
        initLoopView(context, attributeset);
    }

    private void initLoopView(Context context, AttributeSet attributeset) {
        this.mContext = context;
        mHandler = new MessageHandler(this);
        mFlingGestureDetector = new GestureDetector(context, new LoopViewGestureListener(this));
        mFlingGestureDetector.setIsLongpressEnabled(false);

        TypedArray typedArray = context.obtainStyledAttributes(attributeset, R.styleable.androidWheelView);
        mSecondLineTextSize = typedArray.getInteger(R.styleable.androidWheelView_awv_textsize, DEFAULT_SECOND_LINE_TEXT_SIZE);
//        mSecondLineTextSize = (int) (Resources.getSystem().getDisplayMetrics().density * mSecondLineTextSize);
        mLineSpacingMultiplier = typedArray.getFloat(R.styleable.androidWheelView_awv_lineSpace, DEFAULT_LINE_SPACE);
        mCenterTextColor = typedArray.getInteger(R.styleable.androidWheelView_awv_centerTextColor, 0xff313131);
        mOuterTextColor = typedArray.getInteger(R.styleable.androidWheelView_awv_outerTextColor, 0xffafafaf);
        mDividerColor = typedArray.getInteger(R.styleable.androidWheelView_awv_dividerTextColor, 0xffc5c5c5);
        mItemsVisibleCount = typedArray.getInteger(R.styleable.androidWheelView_awv_itemsVisibleCount, DEFAULT_VISIBLE_ITEMS);
        if (mItemsVisibleCount % 2 == 0) {
            mItemsVisibleCount = DEFAULT_VISIBLE_ITEMS;
        }
        isLoop = typedArray.getBoolean(R.styleable.androidWheelView_awv_isLoop, true);
        typedArray.recycle();

        mFirstLineTextSize = DEFAULT_FIRST_LINE_TEXT_SIZE;
//        mDrawingStrings = new String[mItemsVisibleCount];

        mSpacingOfFirstAndImg = DEFAULT_SPACINF_OF_FIRST_AND_IMG;

        mSpacingOfFirstLineAndSecondLine = DEFAULT_SPACING_OF_FIRST_AND_SECOND;

        mDrawingStrings = new Bean[mItemsVisibleCount];

        mTotalScrollY = 0;
        mInitPosition = -1;

        initPaints();
    }


    /**
     * visible item count, must be odd number
     *
     * @param visibleNumber
     */
    public void setItemsVisibleCount(int visibleNumber) {
        if (visibleNumber % 2 == 0) {
            return;
        }
        if (visibleNumber != mItemsVisibleCount) {
            mItemsVisibleCount = visibleNumber;
//            mDrawingStrings = new String[mItemsVisibleCount];
            mDrawingStrings = new Bean[mItemsVisibleCount];
        }
    }


    private void initPaints() {
        mSecondLinePaintOuterText = new Paint();
        mSecondLinePaintOuterText.setColor(mOuterTextColor);
        mSecondLinePaintOuterText.setAntiAlias(true);
//        mSecondLinePaintOuterText.setTypeface(Typeface.MONOSPACE);
        mSecondLinePaintOuterText.setTextSize(mSecondLineTextSize);

        mFirstLinePaintOuterText = new Paint(mSecondLinePaintOuterText);
        mFirstLinePaintOuterText.setTextSize(mFirstLineTextSize);

        mBitmapPaintOuter = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBitmapPaintOuter.setColorFilter(new ColorMatrixColorFilter(black));

        mSecondLinePaintCenterText = new Paint();
        mSecondLinePaintCenterText.setColor(mCenterTextColor);
        mSecondLinePaintCenterText.setAntiAlias(true);
//        mSecondLinePaintCenterText.setTextScaleX(scaleX);
//        mSecondLinePaintCenterText.setTypeface(Typeface.MONOSPACE);
        mSecondLinePaintCenterText.setTextSize(mSecondLineTextSize);

        mFirstLinePaintCenterText = new Paint(mSecondLinePaintCenterText);
        mFirstLinePaintCenterText.setTextSize(mFirstLineTextSize);

        mBitmapPaintCenter = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBitmapPaintCenter.setColorFilter(new ColorMatrixColorFilter(black));

        mPaintIndicator = new Paint();
        mPaintIndicator.setColor(mDividerColor);
        mPaintIndicator.setAntiAlias(true);


//        mTestPaint = new Paint();
//        mTestPaint.setColor(Color.RED);
//        mTestPaint.setAntiAlias(true);
//
//        mTestPaint2 = new Paint();
//        mTestPaint2.setColor(Color.BLUE);
//        mTestPaint2.setAntiAlias(true);

    }

    private void remeasure() {
        if (mItems == null) {
            return;
        }

        mMeasuredWidth = getMeasuredWidth();

        mMeasuredHeight = getMeasuredHeight();

        if (mMeasuredWidth == 0 || mMeasuredHeight == 0) {
            return;
        }

        mPaddingLeft = getPaddingLeft();
        mPaddingRight = getPaddingRight();

        mMeasuredWidth = mMeasuredWidth - mPaddingRight;

//        mSecondLinePaintCenterText.getTextBounds("\u661F\u671F", 0, 2, mTempRect); // 星期
//        mMaxItemHeight = mTempRect.height();
        mHalfCircumference = (int) (mMeasuredHeight * Math.PI / 2);

        //根据圆的半周长，计算最大的item高度
        mMaxItemHeight = (int) (mHalfCircumference / (mLineSpacingMultiplier * (mItemsVisibleCount - 1)));

        //半径
        mRadius = mMeasuredHeight / 2;

        //第一条线的位置，控件的高度减去中间最大的条目的高度，再除以2
        mFirstLineY = (int) ((mMeasuredHeight - mLineSpacingMultiplier * mMaxItemHeight) / 2.0F);
        log(String.format("mFirstLineY : %s , mMeasuredHeight : %s , mLineSpacingMultiplier : %s ,mMaxItemHeight : %s  ",
                mFirstLineY,mMeasuredHeight,mLineSpacingMultiplier, mMaxItemHeight));

        //第二条线的位置，直接用第一条线的位置加上中间最大的条目的高度不就好了
        mSecondLineY = (int) ((mMeasuredHeight + mLineSpacingMultiplier * mMaxItemHeight) / 2.0F);

        if (mInitPosition == -1) {
            if (isLoop) {
                mInitPosition = (mItems.size() + 1) / 2;
            } else {
                mInitPosition = 0;
            }
        }

        mPreCurrentIndex = mInitPosition;

        Paint.FontMetrics firstFontMetrics = mFirstLinePaintCenterText.getFontMetrics();

        mFirstLineStringHeight = firstFontMetrics.bottom - firstFontMetrics.top;

        Paint.FontMetrics secondFontMetrics = mSecondLinePaintCenterText.getFontMetrics();

        mSecondLineStingHeight = secondFontMetrics.bottom  - secondFontMetrics.top;

        float contentHeight = mFirstLineStringHeight + mSecondLineStingHeight + mSpacingOfFirstLineAndSecondLine;

        //计算第一行文字开始绘制的底部Y
        mFirstLineStringStartY = mMaxItemHeight / 2 - contentHeight / 2 + mFirstLineStringHeight - firstFontMetrics.bottom;

        //计算第二行文字开始绘制的底部Y
        mSecondLineStringStartY = mMaxItemHeight / 2 + contentHeight / 2 - secondFontMetrics.bottom;


    }

    void smoothScroll(ACTION action) {
        cancelFuture();
        if (action == ACTION.FLING || action == ACTION.DAGGLE) {
            float itemHeight = mLineSpacingMultiplier * mMaxItemHeight;
            mOffset = (int) ((mTotalScrollY % itemHeight + itemHeight) % itemHeight);
            if ((float) mOffset > itemHeight / 2.0F) {
                mOffset = (int) (itemHeight - (float) mOffset);
            } else {
                mOffset = -mOffset;
            }
        }
        mFuture = mExecutor.scheduleWithFixedDelay(new SmoothScrollTimerTask(this, mOffset), 0, 10, TimeUnit.MILLISECONDS);
    }

    protected final void scrollBy(float velocityY) {
        cancelFuture();
        // mChange this number, can mChange fling speed
        int velocityFling = 10;
//        mFuture = mExecutor.scheduleWithFixedDelay(new InertiaTimerTask(this, velocityY), 0, velocityFling, TimeUnit.MILLISECONDS);
    }

    public void cancelFuture() {
        if (mFuture != null && !mFuture.isCancelled()) {
            mFuture.cancel(true);
            mFuture = null;
        }
    }

    /**
     * set not loop
     */
    public void setNotLoop() {
        isLoop = false;
    }

    /**
     * set text size in dp
     * @param size
     */
    public final void setSecondLineTextSize(float size) {
        if (size > 0.0F) {
            mSecondLineTextSize = (int) (mContext.getResources().getDisplayMetrics().density * size);
            mSecondLinePaintOuterText.setTextSize(mSecondLineTextSize);
            mSecondLinePaintCenterText.setTextSize(mSecondLineTextSize);
        }
    }

    public final void setInitPosition(int initPosition) {
        if (initPosition < 0) {
            this.mInitPosition = 0;
        } else {
            if (mItems != null && mItems.size() > initPosition) {
                this.mInitPosition = initPosition;
            }
        }
    }

    public final void setListener(OnItemSelectedListener OnItemSelectedListener) {
        mOnItemSelectedListener = OnItemSelectedListener;
    }

//    public final void setItems(List<String> items) {
//        this.mItems = items;
//        remeasure();
//        invalidate();
//    }

    public final void setItems(List<Bean> items) {
        this.mItems = items;
        remeasure();
        invalidate();
    }

    public final int getSelectedItem() {
        return mSelectedItem;
    }
//
//    protected final void scrollBy(float velocityY) {
//        Timer timer = new Timer();
//        mTimer = timer;
//        timer.schedule(new InertiaTimerTask(this, velocityY, timer), 0L, 20L);
//    }

    protected final void onItemSelected() {
        if (mOnItemSelectedListener != null) {
            postDelayed(new OnItemSelectedRunnable(this), 200L);
        }
    }


    /**
     * link https://github.com/weidongjian/androidWheelView/issues/10
     *
     * @param scaleX
     */
    public void setScaleX(float scaleX) {
        this.scaleX = scaleX;
    }


    /**
     * set current item position
     * @param position
     */
    public void setCurrentPosition(int position) {
        if (position > 0 && position < mItems.size() && position != mSelectedItem) {
            mInitPosition = position;
            mTotalScrollY = 0;
            mOffset = 0;
            invalidate();
        }
    }

    private Bean mBean = new Bean("","",null);

    @Override
    protected void onDraw(Canvas canvas) {
        if (mItems == null) {
            return;
        }

        mChange = (int) (mTotalScrollY / (mLineSpacingMultiplier * mMaxItemHeight));
        mPreCurrentIndex = mInitPosition + mChange % mItems.size();

        if (!isLoop) {
            if (mPreCurrentIndex < 0) {
                mPreCurrentIndex = 0;
            }
            if (mPreCurrentIndex > mItems.size() - 1) {
                mPreCurrentIndex = mItems.size() - 1;
            }
        } else {
            if (mPreCurrentIndex < 0) {
                mPreCurrentIndex = mItems.size() + mPreCurrentIndex;
            }
            if (mPreCurrentIndex > mItems.size() - 1) {
                mPreCurrentIndex = mPreCurrentIndex - mItems.size();
            }
        }

        int j2 = (int) (mTotalScrollY % (mLineSpacingMultiplier * mMaxItemHeight));
        // put value to drawingString
        int k1 = 0;
        while (k1 < mItemsVisibleCount) {
            int l1 = mPreCurrentIndex - (mItemsVisibleCount / 2 - k1);
            if (isLoop) {
                while (l1 < 0) {
                    l1 = l1 + mItems.size();
                }
                while (l1 > mItems.size() - 1) {
                    l1 = l1 - mItems.size();
                }
                mDrawingStrings[k1] = mItems.get(l1);
            } else if (l1 < 0) {
                mDrawingStrings[k1] = mBean;
            } else if (l1 > mItems.size() - 1) {
                mDrawingStrings[k1] = mBean;
            } else {
                mDrawingStrings[k1] = mItems.get(l1);
            }
            k1++;
        }

//        canvas.drawRect(0,0,mMeasuredWidth,mMeasuredHeight,mTestPaint2);

//        canvas.drawLine(0,0,0,mMeasuredHeight,mTestPaint2);
//        canvas.drawLine(0,0,mMeasuredWidth,0,mTestPaint2);
//        canvas.drawLine(mMeasuredWidth,0,mMeasuredWidth,mMeasuredHeight,mTestPaint2);
//        canvas.drawLine(0,mMeasuredHeight,mMeasuredWidth,mMeasuredHeight,mTestPaint2);

        log(String.format("drawFirstLine : mPaddingLeft : %s , mFirstLineY : %s , mMeasuredWidth : %s , ", mPaddingLeft, mFirstLineY, mMeasuredWidth));
        canvas.drawLine(mPaddingLeft, mFirstLineY, mMeasuredWidth, mFirstLineY, mPaintIndicator);
        log(String.format("drawSecondLine : mPaddingLeft : %s , mSecondLineY : %s , mMeasuredWidth : %s , ", mPaddingLeft, mSecondLineY, mMeasuredWidth));
        canvas.drawLine(mPaddingLeft, mSecondLineY, mMeasuredWidth, mSecondLineY, mPaintIndicator);


        int i = 0;
        while (i < mItemsVisibleCount) {
            canvas.save();
            float itemHeight = mMaxItemHeight * mLineSpacingMultiplier;
            double radian = ((itemHeight * i - j2) * Math.PI) / mHalfCircumference;
            log(String.format("i : %s , itemHeight : %s , radian : %s , j2 : %s , mTotalScrollY : %s ",
                    i, itemHeight, radian,j2,mTotalScrollY));
            if (radian >= Math.PI || radian <= 0) {
                canvas.restore();
            } else {
                int translateY = (int) (mRadius - Math.cos(radian) * mRadius - (Math.sin(radian) * itemHeight) / 2D);
//                canvas.drawLine(0,translateY,mMeasuredWidth,translateY,mTestPaint);
                log(String.format("i : %s , translateY : %s ", i, translateY));
                canvas.translate(0.0F, translateY);
                canvas.scale(1.0F, (float) Math.sin(radian));
                Bean drawingString = mDrawingStrings[i];
                log(String.format("i : %s , Math.sin(radian) : %s , drawingString : %s ", i, Math.sin(radian), drawingString));
                int textX = getTextX(drawingString.getSecondLine(), mSecondLinePaintOuterText, mTempRect);
                int textX1 = getTextX(drawingString.getSecondLine(), mSecondLinePaintCenterText, mTempRect);
                if (translateY <= mFirstLineY && mMaxItemHeight + translateY >= mFirstLineY) {
                    // first divider
                    canvas.save();
                    log(String.format("i : %s , first divider mFirstLineY - translateY : %s ", i, (mFirstLineY - translateY)));
                    canvas.clipRect(0, 0, mMeasuredWidth, mFirstLineY - translateY);
                    log(String.format("i : %s , first divider drawText textX : %s ,mMaxItemHeight : %s ", i, textX, mMaxItemHeight));
//                    canvas.drawText(drawingString.getSecondLine(), textX, mMaxItemHeight, mSecondLinePaintOuterText);

                    drawFirstLineImg(canvas,
                            drawingString,
                            mFirstLinePaintOuterText,
                            mTempRect,
                            drawingString.getFirtLine(),
                            mBitmapPaintOuter);

                    canvas.drawText(drawingString.getFirtLine(),
                            getFirstLineTextX(drawingString.getFirtLine(), mFirstLinePaintOuterText, mTempRect,drawingString),
                            mFirstLineStringStartY, mFirstLinePaintOuterText);

                    canvas.drawText(drawingString.getSecondLine(),
                            getTextX(drawingString.getSecondLine(),mSecondLinePaintOuterText,mTempRect),
                            mSecondLineStringStartY,mSecondLinePaintOuterText);


                    canvas.restore();


                    canvas.save();
                    log(String.format("i : %s , first divider mFirstLineY - translateY : %s , itemHeight : %s ", i, (mFirstLineY - translateY), itemHeight));
                    canvas.clipRect(0, mFirstLineY - translateY, mMeasuredWidth, (int) (itemHeight));
                    log(String.format("i : %s , first divider drawText textX1 : %s ,mMaxItemHeight : %s ", i, textX1, mMaxItemHeight));
//                    canvas.drawText(drawingString.getSecondLine(), textX1, mMaxItemHeight, mSecondLinePaintCenterText);

                    drawFirstLineImg(canvas,
                            drawingString,
                            mFirstLinePaintCenterText,
                            mTempRect,
                            drawingString.getFirtLine(),
                            mBitmapPaintCenter);

                    canvas.drawText(drawingString.getFirtLine(),
                            getFirstLineTextX(drawingString.getFirtLine(), mFirstLinePaintCenterText, mTempRect,drawingString),
                            mFirstLineStringStartY, mFirstLinePaintCenterText);


                    canvas.drawText(drawingString.getSecondLine(),
                            getTextX(drawingString.getSecondLine(),mSecondLinePaintCenterText,mTempRect),
                            mSecondLineStringStartY,mSecondLinePaintCenterText);


                    canvas.restore();
                } else if (translateY <= mSecondLineY && mMaxItemHeight + translateY >= mSecondLineY) {
                    // second divider
                    canvas.save();
                    log(String.format("i : %s , second divider mSecondLineY - translateY : %s ", i, (mSecondLineY - translateY)));
                    canvas.clipRect(0, 0, mMeasuredWidth, mSecondLineY - translateY);
                    log(String.format("i : %s , second divider drawText textX1 : %s ,mMaxItemHeight : %s ", i, textX1, mMaxItemHeight));
//                    canvas.drawText(drawingString.getSecondLine(), textX1, mMaxItemHeight, mSecondLinePaintCenterText);

                    drawFirstLineImg(canvas,
                            drawingString,
                            mFirstLinePaintCenterText,
                            mTempRect,
                            drawingString.getFirtLine(),
                            mBitmapPaintCenter);

                    canvas.drawText(drawingString.getFirtLine(),
                            getFirstLineTextX(drawingString.getFirtLine(), mFirstLinePaintCenterText, mTempRect,drawingString),
                            mFirstLineStringStartY, mFirstLinePaintCenterText);


                    canvas.drawText(drawingString.getSecondLine(),
                            getTextX(drawingString.getSecondLine(),mSecondLinePaintCenterText,mTempRect),
                            mSecondLineStringStartY,mSecondLinePaintCenterText);

                    canvas.restore();


                    canvas.save();
                    log(String.format("i : %s , second divider mSecondLineY - translateY : %s , itemHeight : %s ", i, (mSecondLineY - translateY), itemHeight));
                    canvas.clipRect(0, mSecondLineY - translateY, mMeasuredWidth, (int) (itemHeight));

                    log(String.format("i : %s , second divider drawText textX : %s ,mMaxItemHeight : %s ", i, textX, mMaxItemHeight));
//                    canvas.drawText(drawingString.getSecondLine(), textX, mMaxItemHeight, mSecondLinePaintOuterText);

                    drawFirstLineImg(canvas,
                            drawingString,
                            mFirstLinePaintOuterText,
                            mTempRect,
                            drawingString.getFirtLine(),
                            mBitmapPaintCenter);

                    canvas.drawText(drawingString.getFirtLine(),
                            getFirstLineTextX(drawingString.getFirtLine(), mFirstLinePaintOuterText, mTempRect,drawingString),
                            mFirstLineStringStartY, mFirstLinePaintOuterText);

                    canvas.drawText(drawingString.getSecondLine(),
                            getTextX(drawingString.getSecondLine(),mSecondLinePaintOuterText,mTempRect),
                            mSecondLineStringStartY,mSecondLinePaintOuterText);

                    canvas.restore();


                } else if (translateY >= mFirstLineY && mMaxItemHeight + translateY <= mSecondLineY) {
                    // center item
                    log(String.format("i : %s , center item mMeasuredWidth : %s , itemHeight : %s", i, mMeasuredWidth , itemHeight));
                    canvas.clipRect(0, 0, mMeasuredWidth, (int) (itemHeight));
                    log(String.format("i : %s , center item textX1 : %s , mMaxItemHeight : %s", i, textX1 , mMaxItemHeight));
//                    canvas.drawText(drawingString.getSecondLine(), textX1, mMaxItemHeight, mSecondLinePaintCenterText);


                    drawFirstLineImg(canvas,
                            drawingString,
                            mFirstLinePaintCenterText,
                            mTempRect,
                            drawingString.getFirtLine(),
                            mBitmapPaintCenter);

                    canvas.drawText(drawingString.getFirtLine(),
                            getFirstLineTextX(drawingString.getFirtLine(), mFirstLinePaintCenterText, mTempRect,drawingString),
                            mFirstLineStringStartY, mFirstLinePaintCenterText);

                    canvas.drawText(drawingString.getSecondLine(),
                            getTextX(drawingString.getSecondLine(),mSecondLinePaintCenterText,mTempRect),
                            mSecondLineStringStartY,mSecondLinePaintCenterText);



                    mSelectedItem = mItems.indexOf(drawingString);
                } else {
                    // other item
                    log(String.format("i : %s , other item mMeasuredWidth : %s , itemHeight : %s", i, mMeasuredWidth , itemHeight));
                    canvas.clipRect(0, 0, mMeasuredWidth, (int) (itemHeight));
                    log(String.format("i : %s , other item textX : %s , mMaxItemHeight : %s", i, textX , mMaxItemHeight));
//                    canvas.drawText(drawingString.getSecondLine(), textX, mMaxItemHeight, mSecondLinePaintOuterText);


                    drawFirstLineImg(canvas,
                            drawingString,
                            mFirstLinePaintOuterText,
                            mTempRect,
                            drawingString.getFirtLine(),
                            mBitmapPaintOuter);


                    canvas.drawText(drawingString.getFirtLine(),
                            getFirstLineTextX(drawingString.getFirtLine(), mFirstLinePaintOuterText, mTempRect,drawingString),
                            mFirstLineStringStartY, mFirstLinePaintOuterText);

                    canvas.drawText(drawingString.getSecondLine(),
                            getTextX(drawingString.getSecondLine(),mSecondLinePaintOuterText,mTempRect),
                            mSecondLineStringStartY,mSecondLinePaintOuterText);

                }
                canvas.restore();
            }
            i++;
        }
    }

    private void drawFirstLineImg(Canvas canvas, Bean bean, Paint firstLineTextPaint, Rect rect, String content,Paint bitmapPaint) {
        firstLineTextPaint.getTextBounds(content, 0, content.length(), rect);
        int textWidth = rect.width();
        int firstLineWidth = bean.getBitmapWidth() + mSpacingOfFirstAndImg + textWidth;
        int imgStartX = mMeasuredWidth / 2 - firstLineWidth / 2;

        int textHeight = rect.height();

        float imgStartY = mFirstLineStringStartY + textHeight / 2 - bean.getBitmapHeight() / 2;

        if (bean.getBitmap() != null) {
            canvas.drawBitmap(bean.getBitmap(), imgStartX, imgStartY, bitmapPaint);
        }
    }

    private int getFirstLineTextX(String content, Paint paint, Rect rect, Bean bean) {
        paint.getTextBounds(content, 0, content.length(), rect);
        int textWidth = rect.width();
        int firstLineWidth = bean.getBitmapWidth() + mSpacingOfFirstAndImg + textWidth;
        int imgStartX = mMeasuredWidth / 2 - firstLineWidth / 2;
        return imgStartX + bean.getBitmapWidth() + mSpacingOfFirstAndImg;
    }

    // text start drawing position
    private int getTextX(String a, Paint paint, Rect rect) {
        paint.getTextBounds(a, 0, a.length(), rect);
        int textWidth = rect.width();
        textWidth *= scaleX;
        return (mMeasuredWidth - mPaddingLeft - textWidth) / 2 + mPaddingLeft;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        remeasure();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean eventConsumed = mFlingGestureDetector.onTouchEvent(event);
        float itemHeight = mLineSpacingMultiplier * mMaxItemHeight;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mStartTime = System.currentTimeMillis();
                cancelFuture();
                mPreviousY = event.getRawY();
                break;

            case MotionEvent.ACTION_MOVE:
                float dy = mPreviousY - event.getRawY();
                mPreviousY = event.getRawY();

                mTotalScrollY = (int) (mTotalScrollY + dy);

                if (!isLoop) {
                    float top = -mInitPosition * itemHeight;
                    float bottom = (mItems.size() - 1 - mInitPosition) * itemHeight;

                    if (mTotalScrollY < top) {
                        mTotalScrollY = (int) top;
                    } else if (mTotalScrollY > bottom) {
                        mTotalScrollY = (int) bottom;
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            default:
                if (!eventConsumed) {
                    float y = event.getY();
                    double l = Math.acos((mRadius - y) / mRadius) * mRadius;
                    int circlePosition = (int) ((l + itemHeight / 2) / itemHeight);

                    float extraOffset = (mTotalScrollY % itemHeight + itemHeight) % itemHeight;
                    mOffset = (int) ((circlePosition - mItemsVisibleCount / 2) * itemHeight - extraOffset);

                    if ((System.currentTimeMillis() - mStartTime) > 120) {
//                        smoothScroll(ACTION.DAGGLE);
                    } else {
//                        smoothScroll(ACTION.CLICK);
                    }
                }
                break;
        }

        invalidate();
        return true;
    }

    private static final String TAG = LoopView.class.getSimpleName();

    private static void log(String content){
        Log.d(TAG,content);
    }
}
