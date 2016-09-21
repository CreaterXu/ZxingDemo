package com.creater.zxingdemo;

import java.io.IOException;


import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.SurfaceHolder;

import com.creater.zxingdemo.utils.Contants;
import com.creater.zxingdemo.utils.SubEvent;

import barcode.CameraManager;
import barcode.CaptureActivityHandler;
import barcode.InactivityTimer;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;

/**
 * ����ʱviewҵ���� ��ӦTestingCountTimeActivity
 * 
 */
public class MainPresenter {
	private Activity mActivity;
	private MainViewInterface mView;
	private MainModelInterface mModel;

	InactivityTimer inactivityTimer;
	CaptureActivityHandler captureActivityHandler;

	public MainPresenter( Activity activity, MainViewInterface view) {
		this.mActivity = activity;
		this.mView = view;
		this.mModel=new MainModel();
	}

	public void startChecking() {
        Log.e("xv","in start checking");
		EventBus.getDefault().register(this);
        inactivityTimer = new InactivityTimer(mActivity);
        captureActivityHandler = new CaptureActivityHandler(mActivity, null, null);
    }


    public void stopCheck(){
        if (captureActivityHandler!=null){
            captureActivityHandler.quitSynchronously();
            captureActivityHandler=null;
        }
        if (inactivityTimer!=null){
            inactivityTimer.shutdown();
            inactivityTimer=null;
        }
    }
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void handlSubeventOnMainThread(SubEvent event) {
        switch (event.getMsg()) {
            case Contants.MATRIX_CHECK_SUCCESS:
                Log.e("xv","success");
                String decodeMsg = event.getmObject().toString();
                stopCheck();
                mView.checkSuccess(decodeMsg);
                EventBus.getDefault().unregister(this);
                break;
            case Contants.MATRIX_CHECK_FAILED_LIGHT_DESTORY:
                break;
            case Contants.MATRIX_PREVIEW_TIME_END:
                stopCheck();
                mView.timeOut();
                EventBus.getDefault().unregister(this);
                break;
            case Contants.MATRIX_CHECK_RESTART:// 重启预览即正在扫描中
                Log.e("xv","checking");
                mView.checking();
                break;
            default:
                break;
        }

    }
	/**
	 * �����
	 */
	public void openCam(SurfaceHolder holder) {
		try {
			CameraManager.get().init(mActivity);
			CameraManager.get().openDriver(holder);
            //CameraManager.get().startPreview();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * �ر����
	 */
	public void closeCam() {
		CameraManager.get().init(mActivity);
		CameraManager.get().closeDriver();
	}
}
