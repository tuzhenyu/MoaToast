/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tzy.toast;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.IntDef;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.TextView;

import com.pdf.sf.toast.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/**
 * 类描述：该类是为了解决MIUI8等系统上，Activity无法弹出Toast的问题。
 * 由于该类是是基于Dialog实现的，所以只能用于Activity。
 * 该类有很多英文注释，这些注释是在搬运Toast的方法时顺便搬过来的。
 */
public class MoaToast {
    static final String TAG = "MoaToast";
    static final boolean localLOGV = true;

    @IntDef({LENGTH_SHORT, LENGTH_LONG})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Duration {}

    /**
     * Show the view or text notification for a short period of time.  This time
     * could be user-definable.  This is the default.
     * @see #setDuration
     */
    public static final int LENGTH_SHORT = 0;

    /**
     * Show the view or text notification for a long period of time.  This time
     * could be user-definable.
     * @see #setDuration
     */
    public static final int LENGTH_LONG = 1;

    private final static String RESOURCE_DIMEN_TYPE = "dimen";
    private final static String RESOURCE_LAYOUT_TYPE = "layout";
    //private final static String RESOURCE_ID_TYPE = "id";

    private final static String RESOURCE_DEF_PACKAGE = "android";
    final Activity mContext;
    final TN mTN;
    int mDuration;
    View mNextView;

    /**
     * Construct an empty MoaToast object.  You must call {@link #setView} before you
     * can call {@link #show}.
     *
     * @param context  The context to use.  Usually your {@link android.app.Application}
     *                 or {@link android.app.Activity} object.
     */
    public MoaToast(Activity context) {
        mContext = context;
        mTN = new TN();
        Resources resources = context.getResources();
        int toastYOffsetRId = resources.getIdentifier("toast_y_offset", RESOURCE_DIMEN_TYPE, RESOURCE_DEF_PACKAGE);
        if(toastYOffsetRId <= 0){//系统厂商如果把名字改了的话，有可能找不到系统资源，则用自带的资源
            toastYOffsetRId = R.dimen.toast_y_offset;
        }
        mTN.mY = context.getResources().getDimensionPixelSize(
                toastYOffsetRId);
        //int toastGravity = resources.getIdentifier("config_toastDefaultGravity", RESOURCE_INTEGER_TYPE,RESOURCE_DEF_PACKAGE);
        //mTN.mGravity = context.getResources().getInteger(
              //  com.android.internal.R.integer.config_toastDefaultGravity);
        //居中显示
        mTN.mGravity = Gravity.CENTER;
    }

    /**
     * Show the view for the specified duration.
     */
    public void show() {
        if (mNextView == null) {
            throw new RuntimeException("setView must have been called");
        }

        ToastManager service = getService();
        String contextName = mContext.getClass().getSimpleName();
        TN tn = mTN;
        tn.mNextView = mNextView;
        service.enqueueToast(contextName, tn, mDuration);
    }

    /**
     * Close the view if it's showing, or don't show it if it isn't showing yet.
     * You do not normally have to call this.  Normally view will disappear on its own
     * after the appropriate duration.
     */
    public void cancel() {
        mTN.hide();
        getService().cancelToast(mContext.getPackageName(), mTN);
    }

    /**
     * Set the view to show.
     * @see #getView
     */
    public void setView(View view) {
        mNextView = view;
    }

    /**
     * Return the view.
     * @see #setView
     */
    public View getView() {
        return mNextView;
    }

    /**
     * Set how long to show the view for.
     * @see #LENGTH_SHORT
     * @see #LENGTH_LONG
     */
    public void setDuration(@Duration int duration) {
        mDuration = duration;
        mTN.mDuration = duration;
    }

    /**
     * Return the duration.
     * @see #setDuration
     */
    @Duration
    public int getDuration() {
        return mDuration;
    }

    /**
     * Set the margins of the view.
     *
     * @param horizontalMargin The horizontal margin, in percentage of the
     *        container width, between the container's edges and the
     *        notification
     * @param verticalMargin The vertical margin, in percentage of the
     *        container height, between the container's edges and the
     *        notification
     */
    public void setMargin(float horizontalMargin, float verticalMargin) {
        mTN.mHorizontalMargin = horizontalMargin;
        mTN.mVerticalMargin = verticalMargin;
    }

    /**
     * Return the horizontal margin.
     */
    public float getHorizontalMargin() {
        return mTN.mHorizontalMargin;
    }

    /**
     * Return the vertical margin.
     */
    public float getVerticalMargin() {
        return mTN.mVerticalMargin;
    }

    /**
     * Set the location at which the notification should appear on the screen.
     * @see android.view.Gravity
     * @see #getGravity
     */
    public void setGravity(int gravity, int xOffset, int yOffset) {
        mTN.mGravity = gravity;
        mTN.mX = xOffset;
        mTN.mY = yOffset;
    }

    /**
     * Get the location at which the notification should appear on the screen.
     * @see android.view.Gravity
     * @see #getGravity
     */
    public int getGravity() {
        return mTN.mGravity;
    }

    /**
     * Return the X offset in pixels to apply to the gravity's location.
     */
    public int getXOffset() {
        return mTN.mX;
    }

    /**
     * Return the Y offset in pixels to apply to the gravity's location.
     */
    public int getYOffset() {
        return mTN.mY;
    }

    /**
     * Gets the LayoutParams for the MoaToast window.
     * @hide
     */
    public WindowManager.LayoutParams getWindowParams() {
        return mTN.mParams;
    }

    /**
     * Make a standard toast that just contains a text view.
     *
     * @param context  The context to use.  Usually your {@link android.app.Application}
     *                 or {@link android.app.Activity} object.
     * @param text     The text to show.  Can be formatted text.
     * @param duration How long to display the message.  Either {@link #LENGTH_SHORT} or
     *                 {@link #LENGTH_LONG}
     *
     */
    public static MoaToast makeText(Activity context, CharSequence text, @Duration int duration) {
        MoaToast result = new MoaToast(context);

        LayoutInflater inflate = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        int layoutId = context.getResources().getIdentifier("transient_notification",RESOURCE_LAYOUT_TYPE,RESOURCE_DEF_PACKAGE);
        if(layoutId <= 0){//系统厂商如果把名字改了的话，有可能找不到系统资源，则用自带的资源
            layoutId = R.layout.transient_notification;
        }
        View v = inflate.inflate(layoutId, null);
        TextView tv = (TextView)v.findViewById(android.R.id.message);
        tv.setText(text);

        result.mNextView = v;
        result.mDuration = duration;

        return result;
    }

    /**
     * Make a standard toast that just contains a text view with the text from a resource.
     *
     * @param context  The context to use.  Usually your {@link android.app.Application}
     *                 or {@link android.app.Activity} object.
     * @param resId    The resource id of the string resource to use.  Can be formatted text.
     * @param duration How long to display the message.  Either {@link #LENGTH_SHORT} or
     *                 {@link #LENGTH_LONG}
     *
     * @throws Resources.NotFoundException if the resource can't be found.
     */
    public static MoaToast makeText(Activity context, @StringRes int resId, @Duration int duration)
            throws Resources.NotFoundException {
        return makeText(context, context.getResources().getText(resId), duration);
    }

    /**
     * Update the text in a MoaToast that was previously created using one of the makeText() methods.
     * @param resId The new text for the MoaToast.
     */
    public void setText(@StringRes int resId) {
        setText(mContext.getText(resId));
    }

    /**
     * Update the text in a MoaToast that was previously created using one of the makeText() methods.
     * @param s The new text for the MoaToast.
     */
    public void setText(CharSequence s) {
        if (mNextView == null) {
            throw new RuntimeException("This MoaToast was not created with MoaToast.makeText()");
        }
        TextView tv = (TextView) mNextView.findViewById(android.R.id.message);
        if (tv == null) {
            throw new RuntimeException("This MoaToast was not created with MoaToast.makeText()");
        }
        tv.setText(s);
    }

    // =======================================================================================
    // 以下方法维护toast队列
    // =======================================================================================

    private ToastManager getService() {
        return ToastManager.getInstance();
    }

    private static class TN implements IToastShower {
        final Runnable mShow = new Runnable() {
            @Override
            public void run() {
                handleShow();
            }
        };

        final Runnable mHide = new Runnable() {
            @Override
            public void run() {
                handleHide();
                // Don't do this in handleHide() because it is also invoked by handleShow()
                mNextView = null;
            }
        };

        private final WindowManager.LayoutParams mParams = new WindowManager.LayoutParams();
        final Handler mHandler = new Handler();

        int mGravity;
        int mX, mY;
        float mHorizontalMargin;
        float mVerticalMargin;


        View mView;
        View mNextView;
        int mDuration;

        WindowManager mWM;
        Dialog dialog ;
        TN() {
            // XXX This should be changed to use a Dialog, with a Theme.MoaToast
            // defined that sets up the layout params appropriately.
            final WindowManager.LayoutParams params = mParams;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.width = WindowManager.LayoutParams.WRAP_CONTENT;
            params.format = PixelFormat.TRANSLUCENT;
            //先不要带动画，动画在Activity关闭时，有点问题
            //params.windowAnimations = android.R.style.Animation_Toast;
            //params.type = WindowManager.LayoutParams.TYPE_TOAST;
            params.setTitle("MoaToast");
            params.flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        }

        /**
         * schedule handleShow into the right thread
         */
        @Override
        public void show() {
            if (localLOGV) Log.v(TAG, "SHOW: " + this);
            mHandler.post(mShow);
        }

        /**
         * schedule handleHide into the right thread
         */
        @Override
        public void hide() {
            if (localLOGV) Log.v(TAG, "HIDE: " + this);
            mHandler.post(mHide);
        }

        /**
         *
         * 这里是弹出一个Toast的具体实现。
         * 最开始，是采用与原生Toast一样的实现方式，但是经过测试，发现原生的实现方式
         * 在MIUI8上无效，即无法弹出Toast；在CM系统中，如果自定义的Toast和原生Toast同时弹出，会出现奔溃。
         * 所以无法采用原生的方案来实现Toast。现在改为用Dialog来实现，但是Dialog的生命周期是绑定于Activity的，
         * 也就是说该Toast的上下文对象只能是Activity,Service或者 Application等其他上下文对象不能使用该Toast.
         *
         * */
        public void handleShow() {
            if (localLOGV) Log.v(TAG, "HANDLE SHOW: " + this + " mView=" + mView
                    + " mNextView=" + mNextView);
            if (mView != mNextView) {
                // remove the old view if necessary
                handleHide();
                mView = mNextView;
                Activity context = (Activity) mView.getContext();
                if(context.isFinishing() ) {
                    return;
                }

                mWM = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
                // We can resolve the Gravity here by using the Locale for getting
                // the layout direction
                final Configuration config = mView.getContext().getResources().getConfiguration();
                final int gravity;
                if(Build.VERSION.SDK_INT >= 17){
                    gravity = Gravity.getAbsoluteGravity(mGravity, config.getLayoutDirection());
                }else{
                    gravity = mGravity;
                }
                mParams.gravity = gravity;
                if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.FILL_HORIZONTAL) {
                    mParams.horizontalWeight = 1.0f;
                }
                if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.FILL_VERTICAL) {
                    mParams.verticalWeight = 1.0f;
                }
                mParams.x = mX;
                mParams.y = mY;
                mParams.verticalMargin = mVerticalMargin;
                mParams.horizontalMargin = mHorizontalMargin;
                if (mView.getParent() != null && dialog != null && dialog.isShowing()) {
                    if (localLOGV) Log.v(TAG, "dialog= " + dialog + " dismiss in" + this);
                    dialog.dismiss();
                }
                if (localLOGV) Log.v(TAG, "dialog= " + dialog + " show in " + this);

                dialog = new Dialog(context,  R.style.moaToastDialog);
                dialog.setContentView(mView);
                dialog.getWindow().setAttributes(mParams);
                dialog.show();

                trySendAccessibilityEvent();
            }
        }

        private void trySendAccessibilityEvent() {
            AccessibilityManager accessibilityManager =
                    (AccessibilityManager) mView.getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (!accessibilityManager.isEnabled()) {
                return;
            }
            // treat toasts as notifications since they are used to
            // announce a transient piece of information to the user
            AccessibilityEvent event = AccessibilityEvent.obtain(
                    AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED);
            event.setClassName(getClass().getName());
            event.setPackageName(mView.getContext().getPackageName());
            mView.dispatchPopulateAccessibilityEvent(event);
            accessibilityManager.sendAccessibilityEvent(event);
        }

        public void handleHide() {
            if (localLOGV) Log.v(TAG, "HANDLE HIDE: " + this + " mView=" + mView);
            if (mView != null && mView.getWindowToken() != null) {

                Activity activity = (Activity) mView.getContext();
                mView = null;
                if(activity.isFinishing()){
                    return;
                }
                if(dialog != null && dialog.isShowing() ) {
                    dialog.dismiss();
                }
            }


        }
    }
}
