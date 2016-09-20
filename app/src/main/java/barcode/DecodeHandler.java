/*
 * Copyright (C) 2010 ZXing authors
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

import java.io.ByteArrayOutputStream;
import java.util.Hashtable;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;


import com.creater.zxingdemo.R;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;

import com.google.zxing.common.HybridBinarizer;

final class DecodeHandler extends Handler {

	private final Activity activity;
	private Handler handler;
	private final MultiFormatReader multiFormatReader;
	private int decodeTime = 0;

	DecodeHandler(Activity activity,Handler handler,
			Hashtable<DecodeHintType, Object> hints) {
		multiFormatReader = new MultiFormatReader();
		multiFormatReader.setHints(hints);
		this.activity = activity;
		this.handler=handler;
	}

	@Override
	public void handleMessage(Message message) {
		switch (message.what) {
		case R.id.decode:
			decode((byte[]) message.obj, message.arg1, message.arg2);
			break;
		case R.id.quit:
			Looper.myLooper().quit();
			break;
		}
	}

	/**
	 * Decode the data within the viewfinder rectangle, and time how long it
	 * took. For efficiency, reuse the same reader objects from one decode to
	 * the next.
	 * 
	 * @param data
	 *            The YUV preview frame.
	 * @param width
	 *            The width of the preview frame.
	 * @param height
	 *            The height of the preview frame.
	 */
	private void decode(byte[] data, int width, int height) {
		decodeTime++;
		long start = System.currentTimeMillis();
		YuvImage image1 = new YuvImage(data, ImageFormat.NV21, width, height,
				null);
		ByteArrayOutputStream stream1 = new ByteArrayOutputStream();
		image1.compressToJpeg(new Rect(0, 0, width, height), 100, stream1);
		byte[] myByte1 = stream1.toByteArray();
		Bitmap bmp1 = BitmapFactory.decodeByteArray(myByte1, 0, stream1.size());
		if (decodeTime > 1) {
			/*if (OpecvUtils.JudgeLightLed(bmp1)) {
				Message message = Message.obtain(handler, R.id.light_distory);
				message.sendToTarget();
				return;
			}*/
		}
		try {
			/*int[] testCardPoint = OpecvUtils.getTestCardPoint(bmp1);
			CameraManager.firstLeft = testCardPoint[0];
			CameraManager.width = testCardPoint[2];
			CameraManager.firstTop = (int) (testCardPoint[1] * ((double) 800 / 720));
			CameraManager.height = (int) (testCardPoint[3] * ((double) 800 / 720));*/
		} catch (Exception e) {
			// TODO: handle exception
		}

		Result rawResult = null;
		byte[] rotatedData = new byte[data.length];
		
		int k = 0;
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				rotatedData[k] = data[width * j + i];
				k++;
			}
		}

		int tmp = width; // Here we are swapping, that's the difference to
		width = height;
		height = tmp;
		data = rotatedData;

		PlanarYUVLuminanceSource source = CameraManager.get()
				.buildLuminanceSource(data, width, height);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
		try {
			rawResult = multiFormatReader.decodeWithState(bitmap);
		} catch (ReaderException re) {
			// continue
		} finally {
			multiFormatReader.reset();
		}

		if (rawResult != null) {
			long end = System.currentTimeMillis();

			Message message = Message.obtain(handler,
					R.id.decode_succeeded, rawResult);
			message.sendToTarget();
		} else {
			Message message = Message.obtain(handler,
					R.id.decode_failed);
			message.sendToTarget();
		}
	}
}
