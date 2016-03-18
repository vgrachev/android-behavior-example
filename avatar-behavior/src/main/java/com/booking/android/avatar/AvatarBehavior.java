package com.booking.android.avatar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.os.Build;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

@SuppressWarnings("unused")
public class AvatarBehavior extends CoordinatorLayout.Behavior<View> {

    private Context mContext;

    private int mToolbarId;
    private int mStartViewId;
    private int mFinalViewId;

    private int mStartXPosition;
    private int mStartYPosition;
    private int mStartHeight;

    private int mFinalXPosition;
    private int mFinalYPosition;
    private int mFinalHeight;

    private int mToolbarTitleExpandedId;
    private int mToolbarSubtitleExpandedId;
    private int mToolbarTitleCollapsedId;
    private int mToolbarSubtitleCollapsedId;

    private TextView mToolbarTitleExpanded;
    private TextView mToolbarSubtitleExpanded;
    private TextView mToolbarTitleCollapsed;
    private TextView mToolbarSubtitleCollapsed;

    private int mStatusBarId;
    private int mStatusBarHeight;

    private float mStartX;
    private float mStartY;
    private float mFinalX;
    private float mFinalY;
    private float mLength;
    private float mAppBarHeight;

    public AvatarBehavior(Context context, AttributeSet attrs) {
        mContext = context;

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AvatarBehavior);
            mToolbarId = a.getResourceId(R.styleable.AvatarBehavior_pinnedToolbarId, 0);
            mStartViewId = a.getResourceId(R.styleable.AvatarBehavior_startViewId, 0);
            mFinalViewId = a.getResourceId(R.styleable.AvatarBehavior_finalViewId, 0);
            mToolbarTitleExpandedId = a.getResourceId(R.styleable.AvatarBehavior_toolbarTitleExpandedId, 0);
            mToolbarSubtitleExpandedId = a.getResourceId(R.styleable.AvatarBehavior_toolbarSubtitleExpandedId, 0);
            mToolbarTitleCollapsedId = a.getResourceId(R.styleable.AvatarBehavior_toolbarTitleCollapsedId, 0);
            mToolbarSubtitleCollapsedId = a.getResourceId(R.styleable.AvatarBehavior_toolbarSubtitleCollapsedId, 0);

            a.recycle();
        }

        mStatusBarId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        return dependency instanceof AppBarLayout;
    }

    @Override
    public boolean onDependentViewChanged(final CoordinatorLayout parent, final View child, final View dependency) {
        maybeInitProperties(parent, child, dependency);

        float factor = applyBezier(calculateFactor(dependency));

        PointF current = getCurrentPoint(factor);

        child.setX(current.x);
        child.setY(current.y);

        PointF size = getCurrentSize(current);

        CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) child.getLayoutParams();
        lp.width = (int) (size.x);
        lp.height = (int) (size.y);
        child.setLayoutParams(lp);

        adjustTitleVisibility(parent, child, factor);

        return true;
    }

    private void adjustTitleVisibility(CoordinatorLayout parent, View child, float factor) {
        if (factor < 0.25) {
            maybeSetAlpha(mToolbarTitleCollapsed, (int) ((1 - factor * 4) * 255));
            maybeSetAlpha(mToolbarSubtitleCollapsed, (int) ((1 - factor * 4) * 255));
        } else {
            maybeSetAlpha(mToolbarTitleCollapsed, 0);
            maybeSetAlpha(mToolbarSubtitleCollapsed, 0);
        }

        if (factor > 0.5) {
            maybeSetAlpha(mToolbarTitleExpanded, (int) ((factor * 2 - 1) * 255));
            maybeSetAlpha(mToolbarSubtitleExpanded, (int) ((factor * 2 - 1) * 255));
        } else {
            maybeSetAlpha(mToolbarTitleExpanded, 0);
            maybeSetAlpha(mToolbarSubtitleExpanded, 0);
        }

        if (factor == 1) {
            parent.findViewById(mStartViewId).setVisibility(View.VISIBLE);
        } else {
            parent.findViewById(mStartViewId).setVisibility(View.INVISIBLE);
        }

        if (factor == 0) {
            parent.findViewById(mFinalViewId).setVisibility(View.VISIBLE);
        } else {
            parent.findViewById(mFinalViewId).setVisibility(View.INVISIBLE);
        }

        if (factor == 0 || factor == 1) {
            child.setVisibility(View.GONE);
        } else {
            child.setVisibility(View.VISIBLE);
        }
    }

    private void maybeSetAlpha(TextView view, int alpha) {
        view.setTextColor(view.getTextColors().withAlpha(alpha));
    }

    private float calculateFactor(View dependency) {
        float y = dependency.getY();                    // top
        return (y + mAppBarHeight) / mAppBarHeight;     // factor
    }

    private PointF getCurrentPoint(float factor) {
        float x = mFinalX + mLength * factor;
        float y = mStartY - mLength * (1 - factor);

        if (y < mFinalY) {
            return new PointF(x, mFinalY);
        }

        return new PointF(mStartX, y);
    }

    private PointF getCurrentSize(PointF position) {
        float sizeFactor = 1f - (mStartX - position.x) / (mStartX - mFinalX);

        float size = mFinalHeight + (mStartHeight - mFinalHeight) * sizeFactor;

        return new PointF(size, size);
    }

    private float applyBezier(float t) {
        // Here I use bezier curve to adjust "time" factor to achieve easy-in-out behavior
        // point and move faster to the left and to the bottom.
        // The curve I use is http://cubic-bezier.com/#1,.05,0,.87 Y-coordinate ("Progression")
        //
        // Full computation looks like this:
        //
        // PointF bezier[] = {new PointF(0, 0), new PointF(1, 0.05), new PointF(0, 0.87), new PointF(1, 1)};
        // double factor = Math.pow(1-t, 3) * bezier[0].y
        //                + 3 * Math.pow(1-t, 2) * t * bezier[1].y
        //                + 3 * (1-t) * Math.pow(t, 2) * bezier[2].y
        //                + Math.pow(t, 3) * bezier[3].y;
        //
        // However it can be shortened:

         return (float) (3 * Math.pow(1-t, 2) * t * 0.05
                        + 3 * (1-t) * Math.pow(t, 2) * 0.87
                        + Math.pow(t, 3) * 1);
    }

    private void maybeInitProperties(CoordinatorLayout parent, View child, View dependency) {
        if (mStartHeight == 0)
            mStartHeight = parent.findViewById(mStartViewId).getHeight();

        if (mFinalHeight == 0)
            mFinalHeight = parent.findViewById(mFinalViewId).getHeight();


        if (mStartX == 0 || mStartY == 0) {
            int location[] = new int[2];
            parent.findViewById(mStartViewId).getLocationInWindow(location);

            if (mStartX == 0)
                mStartX = location[0];
            if (mStartY == 0)
                mStartY = location[1] - (Build.VERSION.SDK_INT > 19 /* KITKAT */ ? 0 : mStatusBarHeight);
        }

        if (mFinalX == 0 || mFinalY == 0) {
            int location[] = new int[2];
            parent.findViewById(mFinalViewId).getLocationInWindow(location);

            if (mFinalX == 0)
                mFinalX = location[0];
            if (mFinalY == 0)
                mFinalY = location[1] - (Build.VERSION.SDK_INT > 19 /* KITKAT */ ? 0 : mStatusBarHeight);
        }

        if (mLength < 0.01)
            mLength = Math.abs(mStartX - mFinalX) + Math.abs(mStartY - mFinalY);

        if (mToolbarTitleCollapsedId != 0 && mToolbarTitleCollapsed == null) {
            mToolbarTitleCollapsed = (TextView) parent.findViewById(mToolbarTitleCollapsedId);
        }

        if (mToolbarSubtitleCollapsedId != 0 && mToolbarSubtitleCollapsed == null) {
            mToolbarSubtitleCollapsed = (TextView) parent.findViewById(mToolbarSubtitleCollapsedId);
        }

        if (mToolbarTitleExpandedId != 0 && mToolbarTitleExpanded == null) {
            mToolbarTitleExpanded = (TextView) parent.findViewById(mToolbarTitleExpandedId);
        }

        if (mToolbarSubtitleExpandedId != 0 && mToolbarSubtitleExpanded == null) {
            mToolbarSubtitleExpanded = (TextView) parent.findViewById(mToolbarSubtitleExpandedId);
        }

        if (mStatusBarHeight == 0 && mStatusBarId != 0) {
            mStatusBarHeight = mContext.getResources().getDimensionPixelSize(mStatusBarId);
        }

        if (mAppBarHeight == 0) {
            float b = dependency.getHeight();                           // height with status bar
            float s = Build.VERSION.SDK_INT > 19 /* KITKAT */ ? mStatusBarHeight : 0; // status bar height
            float t = dependency.findViewById(mToolbarId).getHeight();  // toolbar height (is needed in "pin" collapseMode only)
            mAppBarHeight = b - s - t;                                  // actual height
        }
    }
}
