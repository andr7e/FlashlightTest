package ru.andr7e.flashlighttest;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

/**
 * Created by andre on 15.10.16.
 */
public class FlashlightControl {

    private static final String TAG = "FlashlightController";

    private Context mContext;
    private final CameraManager mCameraManager;

    /** Call {@link #ensureHandler()} before using */
    private Handler mHandler;

    private final String mCameraId;
    private boolean mTorchAvailable;

    /**
     * Lock on {@code this} when accessing
     */
    private boolean mFlashlightEnabled;

    public void test() {
        System.out.println("FlashlightControl");

        try {
            getCameraId();
        } catch (Throwable e) {
            Log.e(TAG, "Error:", e);
            return;
        }

        setFlashlight(true);

    }

    private synchronized void ensureHandler() {
        if (mHandler == null) {
            HandlerThread thread = new HandlerThread(TAG);
            thread.start();
            mHandler = new Handler(thread.getLooper());
        }
    }

    private String getCameraId() throws CameraAccessException {
        String[] ids = mCameraManager.getCameraIdList();
        for (String id : ids) {
            CameraCharacteristics c = mCameraManager.getCameraCharacteristics(id);
            Boolean flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);

            System.out.println("flashAvailable" + flashAvailable);

            Integer lensFacing = c.get(CameraCharacteristics.LENS_FACING);
            if (flashAvailable != null && flashAvailable
                    && lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {

                System.out.println("id" + id);
                return id;
            }
        }
        return null;
    }

    public FlashlightControl(Context mContext) {
        this.mContext = mContext;
        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);

        String cameraId = null;
        try {
            cameraId = getCameraId();
        } catch (Throwable e) {
            Log.e(TAG, "Couldn't initialize.", e);
            return;
        } finally {
            mCameraId = cameraId;
        }

        //mUseWakeLock = mContext.getResources().getBoolean(R.bool.flashlight_use_wakelock);

        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        //mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        if (mCameraId != null) {
            ensureHandler();
            mCameraManager.registerTorchCallback(mTorchCallback, mHandler);
        }
    }

    public void setFlashlight(boolean enabled) {
        boolean pendingError = false;
        synchronized (this) {
            if (mFlashlightEnabled != enabled) {
                mFlashlightEnabled = enabled;
/*
                if (mUseWakeLock) {
                    if (enabled) {
                        if (!mWakeLock.isHeld()) mWakeLock.acquire();
                    } else {
                        if (mWakeLock.isHeld()) mWakeLock.release();
                    }
                }
*/
                try {
                    mCameraManager.setTorchMode(mCameraId, enabled);

                    //mCameraManager.openCamera(mCameraId);
                } catch (CameraAccessException e) {
                    Log.e(TAG, "Couldn't set torch mode", e);
                    mFlashlightEnabled = false;
                    pendingError = true;
/*
                    if (mUseWakeLock && mWakeLock.isHeld()) {
                        mWakeLock.release();
                    }
                    */
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Couldn't set torch mode", e);
                }
            }
        }

        /*
        dispatchModeChanged(mFlashlightEnabled);
        if (pendingError) {
            dispatchError();
        }*/
    }

    private final CameraManager.TorchCallback mTorchCallback =
            new CameraManager.TorchCallback() {

                @Override
                public void onTorchModeUnavailable(String cameraId) {
                    if (TextUtils.equals(cameraId, mCameraId)) {
                        setCameraAvailable(false);
                        //setListenForScreenOff(false);
                    }
                }

                @Override
                public void onTorchModeChanged(String cameraId, boolean enabled) {
                    if (TextUtils.equals(cameraId, mCameraId)) {
                        setCameraAvailable(true);
                        setTorchMode(enabled);
                        //setListenForScreenOff(enabled);
                    }
                }

                private void setCameraAvailable(boolean available) {
                    boolean changed;
                    synchronized (FlashlightControl.this) {
                        changed = mTorchAvailable != available;
                        mTorchAvailable = available;
/*
                        if (mUseWakeLock && !available) {
                            if (mWakeLock.isHeld())
                                mWakeLock.release();
                        }
                        */
                    }
                    /*
                    if (changed) {
                        if (DEBUG) Log.d(TAG, "dispatchAvailabilityChanged(" + available + ")");
                        dispatchAvailabilityChanged(available);
                    }
                    */
                }

                private void setTorchMode(boolean enabled) {
                    boolean changed;
                    synchronized (FlashlightControl.this) {
                        changed = mFlashlightEnabled != enabled;
                        mFlashlightEnabled = enabled;

                        /*
                        if (mUseWakeLock && !enabled) {
                            if (mWakeLock.isHeld())
                                mWakeLock.release();
                        }
                        */
                    }
                    if (changed) {
                        //if (DEBUG) Log.d(TAG, "dispatchModeChanged(" + enabled + ")");
                        //dispatchModeChanged(enabled);
                    }
                }
            };
}