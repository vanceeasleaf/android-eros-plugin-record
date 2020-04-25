package com.farwolf.recordvoice.module;

import android.Manifest;
import android.app.Activity;

import com.alibaba.weex.plugin.annotation.WeexModule;
import com.eros.framework.utils.PermissionUtils;
import com.farwolf.recordvoice.ModuleResultListener;
import com.farwolf.recordvoice.RecorderManager;
import com.taobao.weex.annotation.JSMethod;
import com.taobao.weex.bridge.JSCallback;
import com.taobao.weex.common.WXModule;

import java.util.HashMap;

//import com.farwolf.recordvoice.RecorderManager;
//import com.farwolf.weex.annotation.WeexModule;
//import com.farwolf.weex.base.WXModuleBase;

@WeexModule(name="record")
public class RecordModule extends WXModule {


    @JSMethod
    public void start(final HashMap param){
//        Manifest.permission.RECORD_AUDIO

//        audioService=new AudioService();
        if (!PermissionUtils.checkPermission((Activity) mWXSDKInstance.getContext(), Manifest.permission.RECORD_AUDIO)) {
            return;
        }
        if (!PermissionUtils.checkPermission((Activity) mWXSDKInstance.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            return;
        }


                        RecorderManager.getInstance().start(param, new ModuleResultListener() {
                            @Override
                            public void onResult(Object o) {

                            }
                        });







    }

    @JSMethod
    public void pause(){
        RecorderManager.getInstance().pause(new ModuleResultListener() {
            @Override
            public void onResult(Object o) {

            }
        });
    }



    @JSMethod
    public void stop(final JSCallback callback){
        RecorderManager.getInstance().stop(new ModuleResultListener() {
            @Override
            public void onResult(Object o) {

                callback.invoke(o);
            }
        });
    }


}
