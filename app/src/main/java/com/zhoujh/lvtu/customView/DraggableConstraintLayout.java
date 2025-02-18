package com.zhoujh.lvtu.customView;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.zhoujh.lvtu.R;

/**
 * 自定义View实现拖动并自动吸边效果
 * <p>
 * 处理滑动和贴边 {@link #onTouchEvent(MotionEvent)}
 * 处理事件分发 {@link #dispatchTouchEvent(MotionEvent)}
 * </p>
 *
 * @attr customIsAttach  //是否需要自动吸边
 * @attr customIsDrag    //是否可拖曳
 */
public class DraggableConstraintLayout extends ConstraintLayout {
    // 记录手指上一次触摸的原始X坐标
    private float mLastRawX;
    // 记录手指上一次触摸的原始Y坐标
    private float mLastRawY;
    // 日志标签，用于调试输出
    private final String TAG = "DraggableConstraintLayout";
    // 标记当前操作是否为拖动操作
    private boolean isDrug = false;
    // 父布局的测量宽度
    private int mRootMeasuredWidth = 0;
    // 父布局的测量高度
    private int mRootMeasuredHeight = 0;
    // 父布局在窗口中的顶部Y坐标
    private int mRootTopY = 0;
    // 是否需要自动吸边的标志
    private boolean customIsAttach;
    // 是否可拖动的标志
    private boolean customIsDrag;

    /**
     * 构造函数，用于在代码中直接创建DragConstraintLayout实例
     * @param context 上下文对象
     */
    public DraggableConstraintLayout(Context context) {
        // 调用另一个构造函数，传入null的AttributeSet
        this(context, null);
    }

    /**
     * 构造函数，用于在XML布局文件中使用DragConstraintLayout时调用
     * @param context 上下文对象
     * @param attrs 属性集合
     */
    public DraggableConstraintLayout(Context context, @Nullable AttributeSet attrs) {
        // 调用另一个构造函数，传入默认的样式属性值0
        this(context, attrs, 0);
    }

    /**
     * 构造函数，用于在XML布局文件中使用DragConstraintLayout并指定样式属性时调用
     * @param context 上下文对象
     * @param attrs 属性集合
     * @param defStyleAttr 默认的样式属性
     */
    public DraggableConstraintLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        // 调用父类的构造函数
        super(context, attrs, defStyleAttr);
        // 设置该View可点击，以便处理点击事件
        setClickable(true);
        // 初始化自定义属性
        initAttrs(context, attrs);
    }

    /**
     * 初始化自定义属性
     * @param context 上下文对象
     * @param attrs 属性集合
     */
    private void initAttrs(Context context, AttributeSet attrs) {
        // 获取自定义属性的TypedArray
        TypedArray mTypedAttay = context.obtainStyledAttributes(attrs, R.styleable.DraggableConstraintLayout);
        // 获取是否需要自动吸边的属性值，默认为true
        customIsAttach = mTypedAttay.getBoolean(R.styleable.DraggableConstraintLayout_customIsAttach, true);
        // 获取是否可拖动的属性值，默认为true
        customIsDrag = mTypedAttay.getBoolean(R.styleable.DraggableConstraintLayout_customIsDrag, true);
        // 回收TypedArray，避免内存泄漏
        mTypedAttay.recycle();
    }

    /**
     * 事件分发方法，用于控制事件是否继续向下分发
     * @param event 触摸事件
     * @return 返回true表示该View会处理该事件，不再继续向下分发
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // 调用父类的事件分发方法
        super.dispatchTouchEvent(event);
        // 返回true，表示该View会处理该事件
        return true;
    }

    /**
     * 处理触摸事件，实现拖动和自动吸边效果
     * @param ev 触摸事件
     * @return 返回true表示该View会处理该事件，不再继续向上传递
     */
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // 判断是否允许拖动
        if (customIsDrag) {
            // 获取当前手指触摸的原始X坐标
            float mRawX = ev.getRawX();
            // 获取当前手指触摸的原始Y坐标
            float mRawY = ev.getRawY();
            // 根据触摸事件的类型进行不同的处理
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN: // 手指按下事件
                    // 重置拖动标志
                    isDrug = false;
                    // 记录手指按下时的原始X坐标
                    mLastRawX = mRawX;
                    // 记录手指按下时的原始Y坐标
                    mLastRawY = mRawY;
                    // 获取该View的父布局
                    ViewGroup mViewGroup = (ViewGroup) getParent();
                    if (mViewGroup != null) {
                        // 用于存储父布局在窗口中的坐标
                        int[] location = new int[2];
                        // 获取父布局在窗口中的坐标
                        mViewGroup.getLocationInWindow(location);
                        // 获取父布局的测量高度
                        mRootMeasuredHeight = mViewGroup.getMeasuredHeight();
                        // 获取父布局的测量宽度
                        mRootMeasuredWidth = mViewGroup.getMeasuredWidth();
                        // 获取父布局在窗口中的顶部Y坐标
                        mRootTopY = location[1];
                    }
                    break;
                case MotionEvent.ACTION_MOVE: // 手指移动事件
                    // 判断手指是否在父布局范围内移动
                    if (mRawX >= 0 && mRawX <= mRootMeasuredWidth && mRawY >= mRootTopY && mRawY <= (mRootMeasuredHeight + mRootTopY)) {
                        // 计算手指在X轴上的滑动距离
                        float differenceValueX = mRawX - mLastRawX;
                        // 计算手指在Y轴上的滑动距离
                        float differenceValueY = mRawY - mLastRawY;
                        // 判断是否为拖动操作
                        if (!isDrug) {
                            // 根据滑动距离判断是否为拖动操作
                            if (Math.sqrt(differenceValueX * differenceValueX + differenceValueY * differenceValueY) < 2) {
                                isDrug = false;
                            } else {
                                isDrug = true;
                            }
                        }
                        // 获取该View当前的X坐标
                        float ownX = getX();
                        // 获取该View当前的Y坐标
                        float ownY = getY();
                        // 计算理论上该View在X轴上拖动后的位置
                        float endX = ownX + differenceValueX;
                        // 计算理论上该View在Y轴上拖动后的位置
                        float endY = ownY + differenceValueY;
                        // 计算该View在X轴上可以拖动的最大距离
                        float maxX = mRootMeasuredWidth - getWidth();
                        // 计算该View在Y轴上可以拖动的最大距离
                        float maxY = mRootMeasuredHeight - getHeight();
                        // 对X轴拖动位置进行边界限制
                        endX = endX < 0 ? 0 : endX > maxX ? maxX : endX;
                        // 对Y轴拖动位置进行边界限制
                        endY = endY < 0 ? 0 : endY > maxY ? maxY : endY;
                        // 设置该View的新X坐标
                        setX(endX);
                        // 设置该View的新Y坐标
                        setY(endY);
                        // 记录当前手指的原始X坐标
                        mLastRawX = mRawX;
                        // 记录当前手指的原始Y坐标
                        mLastRawY = mRawY;
                    }
                    break;
                case MotionEvent.ACTION_UP: // 手指离开事件
                    // 判断是否需要自动吸边
                    if (customIsAttach) {
                        // 判断是否为拖动操作
                        if (isDrug) {
                            // 计算父布局的中间位置
                            float center = mRootMeasuredWidth / 2;
                            // 根据手指离开时的位置判断向左还是向右吸边
                            if (mLastRawX <= center) {
                                // 向左吸边，使用弹性插值器BounceInterpolator，动画时长500毫秒
                                // 除了弹性插值器，还有线性插值器（LinearInterpolator），加速插值器（AccelerateInterpolator），
                                // 减速插值器（DecelerateInterpolator），加速减速插值器（AccelerateDecelerateInterpolator），
                                // 反冲插值器（AnticipateInterpolator），超冲插值器（OvershootInterpolator），反冲超冲插值器（AnticipateOvershootInterpolator）
                                DraggableConstraintLayout.this.animate()
                                        .setInterpolator(new AccelerateDecelerateInterpolator())
                                        .setDuration(500)
                                        .x(0)
                                        .start();
                            } else {
                                // 向右吸边，使用弹性插值器，动画时长500毫秒
                                DraggableConstraintLayout.this.animate()
                                        .setInterpolator(new AccelerateDecelerateInterpolator())
                                        .setDuration(500)
                                        .x(mRootMeasuredWidth - getWidth())
                                        .start();
                            }
                        }
                    }
                    break;
            }
        }
        // 根据是否为拖动操作决定是否拦截事件
        return isDrug ? isDrug : super.onTouchEvent(ev);
    }
}