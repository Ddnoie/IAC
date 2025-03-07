/*
 * Copyright 2023. Huawei Technologies Co., Ltd. All rights reserved.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package max.ar.demo.java.instant.rendering;

import static max.ar.demo.java.instant.InstantActivity.instantActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.MotionEvent;
import android.widget.SeekBar;
import android.widget.Toast;

import com.huawei.arengine.demos.R;

import max.ar.demo.common.ArDemoRuntimeException;
import max.ar.demo.common.GestureEvent;
import max.ar.demo.common.LogUtil;
import max.ar.demo.common.BaseRendererManager;
import max.ar.demo.common.ObjectDisplay;
import max.ar.demo.common.VirtualObject;
import max.ar.demo.java.instant.InstantActivity;
import max.ar.demo.java.utils.CommonUtil;
import com.huawei.hiar.ARCamera;
import com.huawei.hiar.ARFrame;
import com.huawei.hiar.ARHitResult;
import com.huawei.hiar.ARLightEstimate;
import com.huawei.hiar.ARPlane;
import com.huawei.hiar.ARPoint;
import com.huawei.hiar.ARPointCloud;
import com.huawei.hiar.ARTrackable;
import com.huawei.hiar.exceptions.ARFatalException;
import com.huawei.hiar.exceptions.ARSessionPausedException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This class provides rendering management related to the instant scene, including
 * label rendering and virtual object rendering management.
 */
public class InstantRendererManager extends BaseRendererManager implements BaseRendererManager.BaseRenderer {
    private static final String TAG = "InstantRendererManager";

    private static final float[] GREEN_COLORS = new float[] {66.0f, 244.0f, 133.0f, 255.0f};
    private static final float[] BLUE_COLORS = new float[] {66.0f, 133.0f, 244.0f, 255.0f};
    private ObjectDisplay mObjectDisplay = new ObjectDisplay();

    private ArrayBlockingQueue<GestureEvent> mQueuedSingleTaps;

    private VirtualObject mSelectedObj = null;

    //private VirtualObject mVirtualObject = null;
    private ArrayList<VirtualObject> mVirtualObjects = new ArrayList<>();

    private SeekBar mScaleSeekBar = null;

    private SeekBar mRotationSeekBar = null;

    /**
     * The constructor passes activity.
     *
     * @param activity Activity.
     */
    public InstantRendererManager(Activity activity) {
        mActivity = activity;
        setRenderer(this);
        initSeekBar(activity);
    }

    /**
     * Set a gesture type queue.
     *
     * @param queuedSingleTaps Gesture type queue.
     */
    public void setQueuedSingleTaps(ArrayBlockingQueue<GestureEvent> queuedSingleTaps) {
        if (queuedSingleTaps == null) {
            LogUtil.error(TAG, "setQueuedSingleTaps error, queuedSingleTaps is null!");
            return;
        }
        mQueuedSingleTaps = queuedSingleTaps;
    }

    @Override
    public void surfaceCreated(GL10 gl, EGLConfig config) {
        mObjectDisplay.init(mActivity);
    }

    @Override
    public void surfaceChanged(GL10 unused, int width, int height) {
        mObjectDisplay.setSize(width, height);
    }

    @Override
    public void drawFrame(GL10 unused) {
        try {
            //updateInstantObjPose();
            StringBuilder sb = new StringBuilder();
            updateMessageData(sb);
            mTextDisplay.onDrawFrame(sb.toString());
            handleGestureEvent(mArFrame, mArCamera, mProjectionMatrix, mViewMatrix);
            ARLightEstimate lightEstimate = mArFrame.getLightEstimate();
            float lightPixelIntensity = 1.0f;
            if (lightEstimate.getState() != ARLightEstimate.State.NOT_VALID) {
                lightPixelIntensity = lightEstimate.getPixelIntensity();
            }
            drawAllObjects(mProjectionMatrix, mViewMatrix, lightPixelIntensity);
        } catch (ArDemoRuntimeException e) {
            LogUtil.error(TAG, "Exception on the ArDemoRuntimeException!");
        } catch (ARFatalException | ARSessionPausedException t) {
            LogUtil.error(TAG, "Exception on the OpenGL thread. Name:" + t.getClass());
        }
    }

    /*private void updateInstantObjPose() {
        if (mSession == null) {
            return;
        }
        Iterator<VirtualObject> ite = mVirtualObjects.iterator();
    }*/

    private void initSeekBar(Activity activity) {
        mScaleSeekBar = activity.findViewById(R.id.scaleSeekBar);
        mScaleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mSelectedObj != null) {
                    mSelectedObj.updateScaleFactor(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        mRotationSeekBar = activity.findViewById(R.id.rotationSeekBar);
        mRotationSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mSelectedObj != null) {
                    mSelectedObj.updateRotation(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void drawAllObjects(float[] projectionMatrix, float[] viewMatrix, float lightPixelIntensity) {
        Iterator<VirtualObject> ite = mVirtualObjects.iterator();
        while (ite.hasNext()) {
            VirtualObject obj = ite.next();
            if (obj.getAnchor().getTrackingState() == ARTrackable.TrackingState.STOPPED) {
                ite.remove();
            }
            if (obj.getAnchor().getTrackingState() == ARTrackable.TrackingState.TRACKING) {
                mObjectDisplay.onDrawFrame(viewMatrix, projectionMatrix, lightPixelIntensity, obj);
            }
        }
    }

    private void updateMessageData(StringBuilder sb) {
        float fpsResult = doFpsCalculate();
        sb.append("FPS = ").append(fpsResult).append(System.lineSeparator());
    }

    private void handleGestureEvent(ARFrame arFrame, ARCamera arCamera, float[] projectionMatrix, float[] viewMatrix) {
        GestureEvent event = mQueuedSingleTaps.poll();
        if (event == null) {
            return;
        }

        if (arCamera.getTrackingState() != ARTrackable.TrackingState.TRACKING) {
            LogUtil.warn(TAG, "Object is not tracking state.");
            return;
        }

        switch (event.getType()) {
            case GestureEvent.GESTURE_EVENT_TYPE_DOUBLETAP: {
                doWhenEventTypeDoubleTap(viewMatrix, projectionMatrix, event);
                break;
            }
            case GestureEvent.GESTURE_EVENT_TYPE_SCROLL: {
                if (mSelectedObj == null) {
                    LogUtil.info(TAG, "Selected object is null when instant scroll event.");
                    break;
                }
                CommonUtil.hitTest(arFrame, event.getEventSecond());
                break;
            }
            case GestureEvent.GESTURE_EVENT_TYPE_SINGLETAPCONFIRMED: {
                // Do not perform anything when an object is selected.
                if (mSelectedObj != null) {
                    mSelectedObj.setIsSelected(false);
                    mSelectedObj = null;
                }
                MotionEvent tap = event.getEventFirst();
                ARHitResult hitResult = hitTest4Result(arFrame,arCamera,tap);
                if (hitResult == null) {
                    break;
                }
                doWhenEventTypeSingleTap(hitResult);
            }
            default: {
                LogUtil.error(TAG, "Unknown motion event type, and do nothing.");
            }
        }
    }

    private void doWhenEventTypeDoubleTap(float[] viewMatrix, float[] projectionMatrix, GestureEvent event) {
        if (mSelectedObj != null) {
            mSelectedObj.setIsSelected(false);
            mSelectedObj = null;
        }
        for (VirtualObject obj : mVirtualObjects) {
            if (mObjectDisplay.hitTest(viewMatrix, projectionMatrix, obj, event.getEventFirst())) {
                obj.setIsSelected(true);
                mSelectedObj = obj;
                break;
            }
        }
    }

    private void doWhenEventTypeSingleTap(ARHitResult hitResult) {
        // The hit results are sorted by distance. Only the nearest hit point is valid.
        // Set the number of stored objects to 10 to avoid the overload of rendering and AR Engine.
        if (mVirtualObjects.size() >= 11) {
            mVirtualObjects.get(0).getAnchor().detach();
            mVirtualObjects.remove(0);
        }
        mVirtualObjects.add(new VirtualObject(hitResult.createAnchor(), GREEN_COLORS,"AR_logo"));
    }

    /**
     * Release the anchor when destroying Activity.
     */
    public void releaseARAnchor() {
        if (mVirtualObjects == null) {
            return;
        }
        for (VirtualObject object : mVirtualObjects) {
            object.detachAnchor();
        }
    }
}