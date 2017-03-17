package com.tzy.toast;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.ListIterator;

/**
 * 类描述：该类模仿了系统的NotificationManagerService 来维护 Toast 队列。
 * 该类有很多英文注释，这些注释是在搬运NotificationManagerService的方法时顺便搬过来的。
 * <p/>
 * Created by tzy on 2016/12/21.
 */
public class ToastManager {
    private static final String TAG = "ToastManager";
    private static final int MAX_PACKAGE_NOTIFICATIONS = 50;
    // message codes
    private static final int MESSAGE_TIMEOUT = 2;
    private static final int LONG_DELAY = 3500; // 3.5 seconds
    private static final int SHORT_DELAY = 2000; // 2 seconds

    private WorkerHandler mHandler;
    private ArrayList<ToastRecord> mToastQueue;

    private ToastManager() {
        mToastQueue = new ArrayList<>();
        mHandler = new WorkerHandler();
    }

    private static volatile ToastManager singleton;

    public static ToastManager getInstance() {
        if (singleton == null) {
            synchronized (ToastManager.class) {
                if (singleton == null) {
                    singleton = new ToastManager();
                }
            }
        }
        return singleton;
    }


    //---------------------维护Toast队列的方法------------------\\

    public void enqueueToast(String contextName, IToastShower callback, int duration) {
        Log.i(TAG, "enqueueToast contextName=" + contextName + " callback=" + callback + " duration=" + duration);

        if (contextName == null || callback == null) {
            Log.e(TAG, "Not doing toast. contextName=" + contextName + " callback=" + callback);
            return;
        }

        synchronized (mToastQueue) {

            ToastRecord record;
            int index = indexOfToastLocked(contextName, callback);
            // If it's already in the queue, we update it in place, we don't
            // move it to the end of the queue.
            if (index >= 0) {
                record = mToastQueue.get(index);
                record.update(duration);
            } else {
                // Limit the number of toasts that any given package except the android
                // package can enqueue.  Prevents DOS attacks and deals with leaks.
                int count = 0;
                final int N = mToastQueue.size();
                for (int i = 0; i < N; i++) {
                    final ToastRecord r = mToastQueue.get(i);
                    if (r.contextName.equals(contextName)) {
                        count++;
                        if (count >= MAX_PACKAGE_NOTIFICATIONS) {
                            Log.e(TAG, "Package has already posted " + count
                                    + " toasts. Not showing more. contextName=" + contextName);
                            return;
                        }
                    }
                }
                int callingPid = android.os.Process.myPid();
                record = new ToastRecord(callingPid, contextName, callback, duration);
                mToastQueue.add(record);
                index = mToastQueue.size() - 1;
                keepProcessAliveLocked(callingPid);
            }
            // If it's at index 0, it's the current toast.  It doesn't matter if it's
            // new or just been updated.  Call back and tell it to show itself.
            // If the callback fails, this will remove it from the list, so don't
            // assume that it's valid after this.
            if (index == 0) {
                showNextToastLocked();
            }

        }
    }

    private int indexOfToastLocked(String contextName, IToastShower callback) {
        ArrayList<ToastRecord> list = mToastQueue;
        int len = list.size();
        for (int i = 0; i < len; i++) {
            ToastRecord r = list.get(i);
            if (r.contextName.equals(contextName) && r.callback == callback) {
                return i;
            }
        }
        return -1;
    }

    private void handleTimeout(ToastRecord record) {
        Log.e(TAG, "Timeout contextName=" + record.contextName + " callback=" + record.callback);
        synchronized (mToastQueue) {
            int index = indexOfToastLocked(record.contextName, record.callback);
            if (index >= 0) {
                cancelToastLocked(index);
            }
        }
    }

    public void cancelToast(String contextName, IToastShower callback) {
        Log.i(TAG, "cancelToast contextName=" + contextName + " callback=" + callback);

        if (contextName == null || callback == null) {
            Log.e(TAG, "Not cancelling notification. contextName=" + contextName + " callback=" + callback);
            return;
        }

        synchronized (mToastQueue) {
            int index = indexOfToastLocked(contextName, callback);
            if (index >= 0) {
                cancelToastLocked(index);
            } else {
                Log.w(TAG, "MoaToast already cancelled. contextName=" + contextName + " callback=" + callback);
            }

        }
    }

    private void cancelToastLocked(int index) {
        ToastRecord record = mToastQueue.get(index);
        record.callback.hide();
        mToastQueue.remove(index);
        keepProcessAliveLocked(record.pid);
        if (mToastQueue.size() > 0) {
            // Show the next one. If the callback fails, this will remove
            // it from the list, so don't assume that the list hasn't changed
            // after this point.
            showNextToastLocked();
        }
    }


    private void showNextToastLocked() {
        ToastRecord record = mToastQueue.get(0);
        while (record != null) {
            Log.e(TAG, "Show contextName=" + record.contextName + " callback=" + record.callback);
            record.callback.show();
            scheduleTimeoutLocked(record, false);
            return;
        }
    }

    private void cancelAllToastsLocked() {
        ListIterator<ToastRecord> recordIterator = mToastQueue.listIterator();
        while(recordIterator.hasNext()){
            ToastRecord record = recordIterator.next();
            record.callback.hide();
            recordIterator.remove();
        }
    }

    private void scheduleTimeoutLocked(ToastRecord r, boolean immediate) {
        Message m = Message.obtain(mHandler, MESSAGE_TIMEOUT, r);
        long delay = immediate ? 0 : (r.duration == MoaToast.LENGTH_LONG ? LONG_DELAY : SHORT_DELAY);
        mHandler.removeCallbacksAndMessages(r);
        mHandler.sendMessageDelayed(m, delay);
    }

    private void keepProcessAliveLocked(int pid) {
        int toastCount = 0; // toasts from this pid
        ArrayList<ToastRecord> list = mToastQueue;
        int N = list.size();
        for (int i = 0; i < N; i++) {
            ToastRecord r = list.get(i);
            if (r.pid == pid) {
                toastCount++;
            }
        }
        // TODO: 2016/12/21 弹出toast时要保证进程在前台：
        // 类似于 mAm.setProcessForeground(mForegroundToken, pid, toastCount > 0);
    }


    //---------------------内部辅助类-----------------------------\\

    private final class WorkerHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_TIMEOUT:
                    handleTimeout((ToastRecord) msg.obj);
                    break;
            }
        }
    }

    private static final class ToastRecord {
        final int pid;
        final String contextName;
        final IToastShower callback;
        int duration;

        ToastRecord(int pid, String contextName, IToastShower callback, int duration) {
            this.pid = pid;
            this.contextName = contextName;
            this.callback = callback;
            this.duration = duration;
        }

        void update(int duration) {
            this.duration = duration;
        }

        void dump(PrintWriter pw, String prefix) {
            pw.println(prefix + this);
        }

        @Override
        public final String toString() {
            return "ToastRecord{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " contextName=" + contextName
                    + " callback=" + callback
                    + " duration=" + duration;
        }
    }
}
