/*
 * Copyright (C) 2008 ZXing authors
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

package barcode;

import java.util.Vector;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import de.greenrobot.event.EventBus;


import com.creater.zxingdemo.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

/**
 * This class handles all the messaging which comprises the state machine for
 * capture.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CaptureActivityHandler extends Handler {
	private static final int MATRIX_DRAW_VIEWFINDER = 3003;

	private final Activity activity;
	private DecodeThread decodeThread;
	private State state;

	private enum State {
		PREVIEW, SUCCESS, DONE
	}

	public CaptureActivityHandler(Activity activity, Vector<BarcodeFormat> decodeFormats,
			String characterSet) {
		System.loadLibrary("opencv_java");
		this.activity = activity;
		this.decodeThread = new DecodeThread(activity, this, decodeFormats, characterSet,
				new ViewfinderResultPointCallback(null));
		this.decodeThread.start();
		state = State.SUCCESS;
		CameraManager.get().startPreview();
		requestPreviewAndDecode();
	}

	@Override
	public void handleMessage(Message message) {

		switch (message.what) {
		case R.id.auto_focus:
			if (state == State.PREVIEW) {
				CameraManager.get().requestAutoFocus(this, R.id.auto_focus);
			}
			break;
		case R.id.restart_preview:
			requestPreviewAndDecode();
			break;
		case R.id.decode_succeeded:
			state = State.SUCCESS;
			/*SubEvent event=new SubEvent(Contants.MATRIX_CHECK_SUCCESS);
			Result r=(Result)message.obj;
			event.setmObject((Result)message.obj);
			EventBus.getDefault().post(event);*/
			break;
		case R.id.decode_failed:
			state = State.PREVIEW;
			/*SubEvent event2=new SubEvent(Contants.MATRIX_CHECK_RESTART);
			EventBus.getDefault().post(event2);*/
			// ����Ԥ��
			CameraManager.get().requestPreviewFrame(decodeThread.getHandler(), R.id.decode);
			break;
		case R.id.return_scan_result:
			activity.setResult(Activity.RESULT_OK, (Intent) message.obj);
			activity.finish();
			break;
		case R.id.launch_product_query:
			String url = (String) message.obj;
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			activity.startActivity(intent);
			break;
		case R.id.light_distory:
			/*SubEvent event1=new SubEvent(Contants.MATRIX_CHECK_FAILED_LIGHT_DESTORY);
			EventBus.getDefault().post(event1);*/
			break;
		}

	}

	public void quitSynchronously() {
		state = State.DONE;
		CameraManager.get().stopPreview();
		Message quit = Message.obtain(decodeThread.getHandler(), R.id.quit);
		quit.sendToTarget();
		try {
			decodeThread.join();
		} catch (InterruptedException e) {
			// continue
		}

		// Be absolutely sure we don't send any queued up messages
		removeMessages(R.id.decode_succeeded);
		removeMessages(R.id.decode_failed);
	}

	private void requestPreviewAndDecode() {
		if (state == State.SUCCESS) {
			state = State.PREVIEW;
			CameraManager.get().requestPreviewFrame(decodeThread.getHandler(), R.id.decode);
			CameraManager.get().requestAutoFocus(this, R.id.auto_focus);
		}
	}
	
}
