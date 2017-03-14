package com.weigan.loopview;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

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

    private static final int DEFAULT_TEXT_SIZE = (int) (Resources.getSystem().getDisplayMetrics().density * 15);

    private static final float DEFAULT_LINE_SPACE = 2f;

    private static final int DEFAULT_VISIBIE_ITEMS = 9;

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

    private Paint mPaintOuterText;
    private Paint mPaintCenterText;
    private Paint mPaintIndicator;

    List<String> mItems;

    int mTextSize;
    int mMaxTextHeight;

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

    String[] mDrawingStrings;

    int mMeasuredHeight;
    int mMeasuredWidth;

    int mHalfCircumference; //圆的半周长
    int mRadius;

    private int mOffset = 0;
    private float mPreviousY;
    long mStartTime = 0;

    private Rect mTempRect = new Rect();

    private int mPaddingLeft, mPaddingRight;


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
        mPaintCenterText.setColor(centerTextColor);
    }

    /**
     * set center text color
     * @param outerTextColor
     */
    public void setOuterTextColor(int outerTextColor) {
        this.mOuterTextColor = outerTextColor;
        mPaintOuterText.setColor(outerTextColor);
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
        mTextSize = typedArray.getInteger(R.styleable.androidWheelView_awv_textsize, DEFAULT_TEXT_SIZE);
        mTextSize = (int) (Resources.getSystem().getDisplayMetrics().density * mTextSize);
        mLineSpacingMultiplier = typedArray.getFloat(R.styleable.androidWheelView_awv_lineSpace, DEFAULT_LINE_SPACE);
        mCenterTextColor = typedArray.getInteger(R.styleable.androidWheelView_awv_centerTextColor, 0xff313131);
        mOuterTextColor = typedArray.getInteger(R.styleable.androidWheelView_awv_outerTextColor, 0xffafafaf);
        mDividerColor = typedArray.getInteger(R.styleable.androidWheelView_awv_dividerTextColor, 0xffc5c5c5);
        mItemsVisibleCount = typedArray.getInteger(R.styleable.androidWheelView_awv_itemsVisibleCount, DEFAULT_VISIBIE_ITEMS);
        if (mItemsVisibleCount % 2 == 0) {
            mItemsVisibleCount = DEFAULT_VISIBIE_ITEMS;
        }
        isLoop = typedArray.getBoolean(R.styleable.androidWheelView_awv_isLoop, true);
        typedArray.recycle();

        mDrawingStrings = new String[mItemsVisibleCount];

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
            mDrawingStrings = new String[mItemsVisibleCount];
        }
    }


    private void initPaints() {
        mPaintOuterText = new Paint();
        mPaintOuterText.setColor(mOuterTextColor);
        mPaintOuterText.setAntiAlias(true);
        mPaintOuterText.setTypeface(Typeface.MONOSPACE);
        mPaintOuterText.setTextSize(mTextSize);

        mPaintCenterText = new Paint();
        mPaintCenterText.setColor(mCenterTextColor);
        mPaintCenterText.setAntiAlias(true);
        mPaintCenterText.setTextScaleX(scaleX);
        mPaintCenterText.setTypeface(Typeface.MONOSPACE);
        mPaintCenterText.setTextSize(mTextSize);

        mPaintIndicator = new Paint();
        mPaintIndicator.setColor(mDividerColor);
        mPaintIndicator.setAntiAlias(true);

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

        mPaintCenterText.getTextBounds("\u661F\u671F", 0, 2, mTempRect); // 星期
        mMaxTextHeight = mTempRect.height();
        mHalfCircumference = (int) (mMeasuredHeight * Math.PI / 2);

        //根据圆的半周长，计算最大的文本高度
        mMaxTextHeight = (int) (mHalfCircumference / (mLineSpacingMultiplier * (mItemsVisibleCount - 1)));

        //半径
        mRadius = mMeasuredHeight / 2;

        //第一条线的位置，控件的高度减去中间最大的条目的高度，再除以2
        mFirstLineY = (int) ((mMeasuredHeight - mLineSpacingMultiplier * mMaxTextHeight) / 2.0F);
        //第二条线的位置，直接用第一条线的位置加上中间最大的条目的高度不就好了
        mSecondLineY = (int) ((mMeasuredHeight + mLineSpacingMultiplier * mMaxTextHeight) / 2.0F);

        if (mInitPosition == -1) {
            if (isLoop) {
                mInitPosition = (mItems.size() + 1) / 2;
            } else {
                mInitPosition = 0;
            }
        }

        mPreCurrentIndex = mInitPosition;
    }

    void smoothScroll(ACTION action) {
        cancelFuture();
        if (action == ACTION.FLING || action == ACTION.DAGGLE) {
            float itemHeight = mLineSpacingMultiplier * mMaxTextHeight;
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
        mFuture = mExecutor.scheduleWithFixedDelay(new InertiaTimerTask(this, velocityY), 0, velocityFling, TimeUnit.MILLISECONDS);
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
    public final void setTextSize(float size) {
        if (size > 0.0F) {
            mTextSize = (int) (mContext.getResources().getDisplayMetrics().density * size);
            mPaintOuterText.setTextSize(mTextSize);
            mPaintCenterText.setTextSize(mTextSize);
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

    public final void setItems(List<String> items) {
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


    @Override
    protected void onDraw(Canvas canvas) {
        if (mItems == null) {
            return;
        }


        mChange = (int) (mTotalScrollY / (mLineSpacingMultiplier * mMaxTextHeight));
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

        int j2 = (int) (mTotalScrollY % (mLineSpacingMultiplier * mMaxTextHeight));
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
                mDrawingStrings[k1] = "";
            } else if (l1 > mItems.size() - 1) {
                mDrawingStrings[k1] = "";
            } else {
                mDrawingStrings[k1] = mItems.get(l1);
            }
            k1++;
        }
        log(String.format("drawFirstLine : mPaddingLeft : %s , mFirstLineY : %s , mMeasuredWidth : %s , ", mPaddingLeft, mFirstLineY, mMeasuredWidth));
        canvas.drawLine(mPaddingLeft, mFirstLineY, mMeasuredWidth, mFirstLineY, mPaintIndicator);
        log(String.format("drawSecondLine : mPaddingLeft : %s , mSecondLineY : %s , mMeasuredWidth : %s , ", mPaddingLeft, mSecondLineY, mMeasuredWidth));
        canvas.drawLine(mPaddingLeft, mSecondLineY, mMeasuredWidth, mSecondLineY, mPaintIndicator);


        int i = 0;
        while (i < mItemsVisibleCount) {
            canvas.save();
            float itemHeight = mMaxTextHeight * mLineSpacingMultiplier;
            double radian = ((itemHeight * i - j2) * Math.PI) / mHalfCircumference;
            log(String.format("i : %s , itemHeight : %s , radian : %s", i, itemHeight, radian));
            if (radian >= Math.PI || radian <= 0) {
                canvas.restore();
            } else {
                int translateY = (int) (mRadius - Math.cos(radian) * mRadius - (Math.sin(radian) * mMaxTextHeight) / 2D);
                canvas.drawLine(0,translateY,mMeasuredWidth,translateY,mPaintIndicator);
                log(String.format("i : %s , translateY : %s ", i, translateY));
                canvas.translate(0.0F, translateY);
                canvas.scale(1.0F, (float) Math.sin(radian));
                String drawingString = mDrawingStrings[i];
                log(String.format("i : %s , Math.sin(radian) : %s , drawingString : %s ", i, Math.sin(radian), drawingString));
                int textX = getTextX(drawingString, mPaintOuterText, mTempRect);
                int textX1 = getTextX(drawingString, mPaintCenterText, mTempRect);
                if (translateY <= mFirstLineY && mMaxTextHeight + translateY >= mFirstLineY) {
                    // first divider
                    canvas.save();
                    log(String.format("i : %s , first divider mFirstLineY - translateY : %s ", i, (mFirstLineY - translateY)));
                    canvas.clipRect(0, 0, mMeasuredWidth, mFirstLineY - translateY);
                    log(String.format("i : %s , first divider drawText textX : %s ,mMaxTextHeight : %s ", i, textX, mMaxTextHeight));
                    canvas.drawText(drawingString, textX, mMaxTextHeight, mPaintOuterText);
                    canvas.restore();
                    canvas.save();
                    log(String.format("i : %s , first divider mFirstLineY - translateY : %s , itemHeight : %s ", i, (mFirstLineY - translateY), itemHeight));
                    canvas.clipRect(0, mFirstLineY - translateY, mMeasuredWidth, (int) (itemHeight));
                    log(String.format("i : %s , first divider drawText textX1 : %s ,mMaxTextHeight : %s ", i, textX1, mMaxTextHeight));
                    canvas.drawText(drawingString, textX1, mMaxTextHeight, mPaintCenterText);
                    canvas.restore();
                } else if (translateY <= mSecondLineY && mMaxTextHeight + translateY >= mSecondLineY) {
                    // second divider
                    canvas.save();
                    log(String.format("i : %s , second divider mSecondLineY - translateY : %s ", i, (mSecondLineY - translateY)));
                    canvas.clipRect(0, 0, mMeasuredWidth, mSecondLineY - translateY);
                    log(String.format("i : %s , second divider drawText textX1 : %s ,mMaxTextHeight : %s ", i, textX1, mMaxTextHeight));
                    canvas.drawText(drawingString, textX1, mMaxTextHeight, mPaintCenterText);
                    canvas.restore();
                    canvas.save();
                    log(String.format("i : %s , second divider mSecondLineY - translateY : %s , itemHeight : %s ", i, (mSecondLineY - translateY), itemHeight));
                    canvas.clipRect(0, mSecondLineY - translateY, mMeasuredWidth, (int) (itemHeight));
                    log(String.format("i : %s , second divider drawText textX : %s ,mMaxTextHeight : %s ", i, textX, mMaxTextHeight));
                    canvas.drawText(drawingString, textX, mMaxTextHeight, mPaintOuterText);
                    canvas.restore();
                } else if (translateY >= mFirstLineY && mMaxTextHeight + translateY <= mSecondLineY) {
                    // center item
                    log(String.format("i : %s , center item mMeasuredWidth : %s , itemHeight : %s", i, mMeasuredWidth , itemHeight));
                    canvas.clipRect(0, 0, mMeasuredWidth, (int) (itemHeight));
                    log(String.format("i : %s , center item textX1 : %s , mMaxTextHeight : %s", i, textX1 , mMaxTextHeight));
                    canvas.drawText(drawingString, textX1, mMaxTextHeight, mPaintCenterText);
                    mSelectedItem = mItems.indexOf(drawingString);
                } else {
                    // other item
                    log(String.format("i : %s , other item mMeasuredWidth : %s , itemHeight : %s", i, mMeasuredWidth , itemHeight));
                    canvas.clipRect(0, 0, mMeasuredWidth, (int) (itemHeight));
                    log(String.format("i : %s , other item textX : %s , mMaxTextHeight : %s", i, textX , mMaxTextHeight));
                    canvas.drawText(drawingString, textX, mMaxTextHeight, mPaintOuterText);
                }
                canvas.restore();
            }
            i++;
        }
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
        float itemHeight = mLineSpacingMultiplier * mMaxTextHeight;

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
                        smoothScroll(ACTION.DAGGLE);
                    } else {
                        smoothScroll(ACTION.CLICK);
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
