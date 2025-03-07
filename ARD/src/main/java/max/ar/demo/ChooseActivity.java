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

package max.ar.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.huawei.arengine.demos.R;

import max.ar.demo.common.PermissionManager;
import max.ar.demo.java.instant.InstantActivity;
import max.ar.demo.java.camera.CameraActivity;

public class ChooseActivity extends Activity {
    private static final String TAG = ChooseActivity.class.getSimpleName();

    private boolean isFirstClick = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_choose);
        // AR Engine requires the camera permission.
        PermissionManager.checkPermission(this);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        isFirstClick = true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!PermissionManager.isPermissionEnable(this)) {
            Toast.makeText(this, "This application needs camera permission.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy start.");
        super.onDestroy();
        Log.i(TAG, "onDestroy end.");
    }

    /**
     * Choose activity.
     *
     * @param view View
     */
    public void onClick(View view) {
        if (!isFirstClick) {
            return;
        } else {
            isFirstClick = false;
        }
        switch (view.getId()) {
            case R.id.btn_WorldAR_Java:
                startActivity(new Intent(this,
                    CameraActivity.class));
                break;
            case R.id.btn_InstantAR:
                startActivity(new Intent(this,
                    InstantActivity.class));
                break;
            default:
                //do something;
                break;
        }
    }

}