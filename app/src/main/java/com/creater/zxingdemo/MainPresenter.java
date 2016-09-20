package com.creater.zxingdemo;

import java.io.IOException;


import android.app.Activity;
import android.content.Context;
import android.view.SurfaceHolder;

import barcode.CameraManager;
import barcode.CaptureActivityHandler;
import barcode.InactivityTimer;
import de.greenrobot.event.EventBus;

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

	public void startTimeing() {
		mView.checking();
		EventBus.getDefault().register(this);
		/*
		 * mCountThread=TimeCountThread.getSubThreadInstance(Contants.
		 * TIMES_TEST_TIME); mCountThread.start();
		 */
		confirmMatrix();
	}

	/**
	 * ȷ�϶�ά����Ϣ
	 */
	public void confirmMatrix() {
		inactivityTimer = new InactivityTimer(mActivity);
		captureActivityHandler = new CaptureActivityHandler(mActivity, null, null);
	}



	/**
	 * �����
	 */
	public void openCam(SurfaceHolder holder) {
		try {

			CameraManager.get().init(mActivity);
			CameraManager.get().openDriver(holder);
			CameraManager.get().startPreview();
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
