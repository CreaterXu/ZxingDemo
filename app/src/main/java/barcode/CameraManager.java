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

import java.io.IOException;
import java.util.List;


import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 * This object wraps the Camera service object and expects to be the only one
 * talking to it. The implementation encapsulates the steps needed to take
 * preview-sized images, which are used for both preview and decoding.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CameraManager {

	public static int getTime;
	public static int firstLeft = 120;
	public static int firstRight = 240;
	public static int firstTop = 200;
	public static int firstBottom = 600;
	public static int width = 120;
	public static int height = 400;

	public static int Left;
	public static int Right;
	public static int Top;
	public static int Bottom;

	private static CameraManager cameraManager;

	static final int SDK_INT; // Later we can use Build.VERSION.SDK_INT
	static {
		int sdkInt;
		try {
			sdkInt = Integer.parseInt(Build.VERSION.SDK);
		} catch (NumberFormatException nfe) {
			// Just to be safe
			sdkInt = 10000;
		}
		SDK_INT = sdkInt;
	}

	private final Context context;
	private final CameraConfigurationManager configManager;
	private Camera camera;
	private Rect framingRect;
	private Rect framingRectInPreview;
	private boolean initialized;
	private boolean previewing=false;
	private final boolean useOneShotPreviewCallback;
	/**
	 * Preview frames are delivered here, which we pass on to the registered
	 * handler. Make sure to clear the handler so it will only receive one
	 * message.
	 */
	private final PreviewCallback previewCallback;
	/**
	 * Autofocus callbacks arrive here, and are dispatched to the Handler which
	 * requested them.
	 */
	private final AutoFocusCallback autoFocusCallback;

	private Parameters parameter;

	/**
	 * Initializes this static object with the Context of the calling Activity.
	 *
	 * @param context
	 *            The Activity which wants to use the camera.
	 */
	public static void init(Context context) {
		if (cameraManager == null) {
			cameraManager = new CameraManager(context);
		}
	}

	/**
	 * Gets the CameraManager singleton instance.
	 *
	 * @return A reference to the CameraManager singleton.
	 */
	public static CameraManager get() {
		return cameraManager;
	}

	private CameraManager(Context context) {

		this.context = context;
		this.configManager = new CameraConfigurationManager(context);
		// Camera.setOneShotPreviewCallback() has a race condition in Cupcake,
		// so we use the older
		// Camera.setPreviewCallback() on 1.5 and earlier. For Donut and later,
		// we need to use
		// the more efficient one shot callback, as the older one can swamp the
		// system and cause it
		// to run out of memory. We can't use SDK_INT because it was introduced
		// in the Donut SDK.
		// useOneShotPreviewCallback = Integer.parseInt(Build.VERSION.SDK) >
		// Build.VERSION_CODES.CUPCAKE;
		useOneShotPreviewCallback = Integer.parseInt(Build.VERSION.SDK) > 3; // 3
																				// =
																				// Cupcake

		previewCallback = new PreviewCallback(configManager, useOneShotPreviewCallback);
		autoFocusCallback = new AutoFocusCallback();
	}

	/**
	 * Opens the camera driver and initializes the hardware parameters.
	 *
	 * @param holder
	 *            The surface object which the camera will draw preview frames
	 *            into.
	 * @throws IOException
	 *             Indicates the camera driver failed to open.
	 */
	@SuppressWarnings("deprecation")
	public void openDriver(SurfaceHolder holder) throws IOException {
		if (camera == null) {
			Thread.currentThread().interrupt();
			try {
				camera = Camera.open();
			} catch (Exception e) {
				// TODO: handle exception
			}
			if (camera == null) {
				throw new IOException();
			}
			Log.e("xv","in open");
			camera.setPreviewDisplay(holder);
			if (!initialized) {
				initialized = true;
				configManager.initFromCameraParameters(camera);
			}
			configManager.setDesiredCameraParameters(camera);
			FlashlightManager.enableFlashlight();
		}else{
			Log.e("CameraManager", "in openDriver camera is not null");	
		}
	}

	/**
	 * Closes the camera driver if still in use.
	 */
	@SuppressWarnings("deprecation")
	public void closeDriver() {
		if (camera != null) {
			Log.e("Cameramanager", "in closeDriver camera is not null");
			FlashlightManager.disableFlashlight();
			camera.stopPreview();
			camera.release();
			camera = null;
		}else{
			Log.e("CameraManager", "Camera null already");
			camera = null;
		}
		previewing=false;
	}

	/**
	 * Asks the camera hardware to begin drawing preview frames to the screen.
	 * @throws IOException 
	 */
	@SuppressWarnings("deprecation")
	public void startPreview()  {
		if(camera==null){
			try {
				camera=null;
				previewing=true;
				camera=Camera.open();
			} catch (RuntimeException e) {
				// TODO: handle exception
				Log.e("CameraManager", "exception:"+e.toString());
			}
		}
		if (camera != null && !previewing) {
			camera.startPreview();
			previewing = true;
		}
	}

	/**
	 * Tells the camera to stop drawing preview frames.
	 */
	public void stopPreview() {
		if (camera != null && previewing) {
			if (!useOneShotPreviewCallback) {
				camera.setPreviewCallback(null);
			}
			camera.stopPreview();
			previewCallback.setHandler(null, 0);
			autoFocusCallback.setHandler(null, 0);
			previewing = false;
		}
	}

	/**
	 * A single preview frame will be returned to the handler supplied. The data
	 * will arrive as byte[] in the message.obj field, with width and height
	 * encoded as message.arg1 and message.arg2, respectively.
	 *
	 * @param handler
	 *            The handler to send the message to.
	 * @param message
	 *            The what field of the message to be sent.
	 */
	public void requestPreviewFrame(Handler handler, int message) {
		if (camera == null) {
		}
		if (camera != null && previewing) {
			previewCallback.setHandler(handler, message);
			if (useOneShotPreviewCallback) {
				camera.setOneShotPreviewCallback(previewCallback);
			} else {
				camera.setPreviewCallback(previewCallback);
			}
		} else {
			Log.e("CameraManager", "not previewing");
		}

	}

	/**
	 * Asks the camera hardware to perform an autofocus.
	 *
	 * @param handler
	 *            The Handler to notify when the autofocus completes.
	 * @param message
	 *            The message to deliver.
	 */
	@SuppressWarnings("deprecation")
	public void requestAutoFocus(Handler handler, int message) {
		if (camera != null && previewing) {
			autoFocusCallback.setHandler(handler, message);
			camera.autoFocus(autoFocusCallback);
		}
	}

	/**
	 * Calculates the framing rect which the UI should draw to show the user
	 * where to place the barcode. This target helps with alignment as well as
	 * forces the user to hold the device far enough away to ensure the image
	 * will be in focus.
	 *
	 * @return The rectangle to draw on screen in window coordinates.
	 */
	public Rect getFramingRect() {
		if (camera == null) {
			return null;
		}
		firstRight = firstLeft + width;
		firstBottom = firstTop + height;

		// framingRect = new Rect(leftOffset, topOffset, leftOffset + width,
		// topOffset + height);
		framingRect = new Rect(firstLeft, firstTop, firstRight, firstBottom);
		return framingRect;
	}

	/**
	 * Like {@link #getFramingRect} but coordinates are in terms of the preview
	 * frame, not UI / screen.
	 */
	public Rect getFramingRectInPreview() {
		Rect rect = new Rect(getFramingRect());
		Point cameraResolution = configManager.getCameraResolution();// ��ȡprivizew�ֱ���
		Point screenResolution = configManager.getScreenResolution();// ��ȡ��Ļ�ֱ���

		int toLeft = rect.left;
		rect.left = ((screenResolution.x / 2) - rect.right) * 2 + rect.right;
		rect.right = ((screenResolution.x / 2) - toLeft) * 2 + toLeft;
		rect.top = (int) (rect.top * ((double) cameraResolution.x / (double) screenResolution.y));
		rect.bottom = (int) (rect.bottom * ((double) cameraResolution.x / (double) screenResolution.y));
		framingRectInPreview = rect;

		Left = rect.left;
		Right = rect.right;
		Top = rect.top;
		Bottom = rect.bottom;
		return framingRectInPreview;
	}

	/**
	 * Converts the result points from still resolution coordinates to screen
	 * coordinates.
	 *
	 * @param points
	 *            The points returned by the Reader subclass through
	 *            Result.getResultPoints().
	 * @return An array of Points scaled to the size of the framing rect and
	 *         offset appropriately so they can be drawn in screen coordinates.
	 */
	/*
	 * public Point[] convertResultPoints(ResultPoint[] points) { Rect frame =
	 * getFramingRectInPreview(); int count = points.length; Point[] output =
	 * new Point[count]; for (int x = 0; x < count; x++) { output[x] = new
	 * Point(); output[x].x = frame.left + (int) (points[x].getX() + 0.5f);
	 * output[x].y = frame.top + (int) (points[x].getY() + 0.5f); } return
	 * output; }
	 */

	/**
	 * A factory method to build the appropriate LuminanceSource object based on
	 * the format of the preview buffers, as described by Camera.Parameters.
	 *
	 * @param data
	 *            A preview frame.
	 * @param width
	 *            The width of the image.
	 * @param height
	 *            The height of the image.
	 * @return A PlanarYUVLuminanceSource instance.
	 */
	public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
		Rect rect = getFramingRectInPreview();
		int previewFormat = configManager.getPreviewFormat();
		String previewFormatString = configManager.getPreviewFormatString();
		switch (previewFormat) {
		// This is the standard Android format which all devices are REQUIRED to
		// support.
		// In theory, it's the only one we should ever care about.
		case PixelFormat.YCbCr_420_SP:
			// This format has never been seen in the wild, but is compatible as
			// we only care
			// about the Y channel, so allow it.
		case PixelFormat.YCbCr_422_SP:
			return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top, rect.width(), rect.height());
		default:
			// The Samsung Moment incorrectly uses this variant instead of the
			// 'sp' version.
			// Fortunately, it too has all the Y data up front, so we can read
			// it.
			if ("yuv420p".equals(previewFormatString)) {
				return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top, rect.width(),
						rect.height());
			}
		}
		throw new IllegalArgumentException("Unsupported picture format: " + previewFormat + '/' + previewFormatString);
	}

	public void openLight() {
		if (camera != null) {
			parameter = camera.getParameters();
			parameter.setFlashMode(Parameters.FLASH_MODE_TORCH);
			camera.setParameters(parameter);
		}
	}

	public void offLight() {
		if (camera != null) {
			parameter = camera.getParameters();
			parameter.setFlashMode(Parameters.FLASH_MODE_OFF);
			camera.setParameters(parameter);
		}
	}
	
	/**
	 * ���෽��
	 * �ṩ����ص�����
	 * */
	public void takePhoto(ShutterCallback shutter,PictureCallback raw,PictureCallback picture){
		camera.takePicture(shutter, raw, picture);
	}
	/**
	 * �������Ĳ���
	 * 
	 * */
	public void setCameraParmeters(){
		if (camera != null) {
			Parameters mParams = camera.getParameters();
			mParams.setPictureFormat(PixelFormat.JPEG);// �������պ�洢��ͼƬ��ʽ
			List<Size> pictureSizes = mParams.getSupportedPictureSizes();
			List<Size> previewSizes = mParams.getSupportedPreviewSizes();
			for (int i = 0; i < pictureSizes.size(); i++) {
				Size size = pictureSizes.get(i);
				Log.i("TakePicture",
						"initCamera:����ͷ֧�ֵ�pictureSizes: width = " + size.width + "height = " + size.height);
			}
			for (int i = 0; i < previewSizes.size(); i++) {
				Size size = previewSizes.get(i);
				Log.i("TakePicture",
						"initCamera:����ͷ֧�ֵ�previewSizes: width = " + size.width + "height = " + size.height);

			}
			// ����PreviewSize��PictureSize
//			Size pictureSize = camParaUtils.getInstance().getPropPictureSize(mParams.getSupportedPictureSizes(),
//					previewRate, 1600);
			mParams.setPictureSize(2592, 1944);
//			Size previewSize = camParaUtils.getInstance().getPropPreviewSize(mParams.getSupportedPreviewSizes(),
//					previewRate, 1280);
			// mParams.setPreviewSize(previewSize.width, previewSize.height);
			mParams.setPreviewSize(720, 480);
			camera.setDisplayOrientation(90);
			List<String> focusModes = mParams.getSupportedFocusModes();
			if (focusModes.contains(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
				mParams.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
			}
			mParams.setFocusMode(Parameters.FOCUS_MODE_AUTO);
			mParams.setSceneMode(Parameters.SCENE_MODE_ACTION);
			mParams.setExposureCompensation(0);
			camera.setParameters(mParams);
			startPreview();
			mParams = camera.getParameters(); // ����getһ��
			Log.e("TakePicture", "��������:PreviewSize--With = " + mParams.getPreviewSize().width + "Height = "
					+ mParams.getPreviewSize().height);
			Log.e("TakePicture", "��������:PictureSize--With = " + mParams.getPictureSize().width + "Height = "
					+ mParams.getPictureSize().height);
		}
	}

	/**
	 * �Զ��Խ�
	 * */
	public void autoFocus(Camera.AutoFocusCallback autoFocusCallback){
		camera.autoFocus(autoFocusCallback);
	}
}
