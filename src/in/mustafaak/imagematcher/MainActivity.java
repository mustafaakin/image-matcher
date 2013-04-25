package in.mustafaak.imagematcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements CvCameraViewListener2 {
	private static final String TAG = "OCVSample::Activity";
	private CameraBridgeViewBase mOpenCvCameraView;

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");
				mOpenCvCameraView.enableView();
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	public MainActivity() {
		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	ImageView matchDrawArea;
	Button addBtn;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.main_layout);
		matchDrawArea = (ImageView) findViewById(R.id.refImageView);
		addBtn = (Button) findViewById(R.id.button1);

		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_native_surface_view);
		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

		mOpenCvCameraView.setCvCameraViewListener(this);
		mOpenCvCameraView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return false;
			}
		});

		ToggleButton ransacSwitch = (ToggleButton) findViewById(R.id.toggleButton1);
		ransacSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				ransacEnabled = arg1;
			}
		});

		((ToggleButton) findViewById(R.id.toggleButton2))
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton arg0,
							boolean arg1) {
						imageOnly = !arg1;
					}
				});

	}

	boolean resize = false;
	boolean imageOnly = true;

	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this,
				mLoaderCallback);
	}

	public void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	public void onCameraViewStarted(int width, int height) {

	}

	public void onCameraViewStopped() {
	}

	boolean showOriginal = true;

	public void cameraclick(View w) {
		showOriginal = !showOriginal;
	}

	public Mat shapeDetect(Mat original, Mat gray) {
		Mat bw = new Mat();
		Imgproc.Canny(gray, bw, 0, 30);

		Mat draw = new Mat(bw.size(), bw.type());

		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(bw, contours, new Mat(), Imgproc.RETR_LIST,
				Imgproc.CHAIN_APPROX_SIMPLE);
		Log.d("CONTOURS", "" + contours.size());
		for (int i = 0; i < contours.size(); i++) {
			MatOfPoint kontor = contours.get(i);
			MatOfPoint2f kontor2f = new MatOfPoint2f(kontor.toArray());
			MatOfPoint2f approx2f = new MatOfPoint2f();
			Imgproc.approxPolyDP(kontor2f, approx2f,
					Imgproc.arcLength(kontor2f, true) * 0.02, true);

			double area = Math.abs(Imgproc.contourArea(approx2f));
			double points = approx2f.size().height;
			if (points >= 3 && points <= 6 && area > 500) {
				Imgproc.drawContours(draw, contours, i, new Scalar(255));
				String text = "A: " + area + ", " + points;
				ArrayList<Point> corner = new ArrayList<Point>();
				double[] pixels = new double[2];
				for (int i1 = 0; i1 < points; i1++) {
					double[] pix = approx2f.get(i1, 0);
					pixels[0] += pix[0];
					pixels[1] += pix[1];
					Point p = new Point(pix);
					corner.add(p);
				}

				Collections.sort(corner, new Comparator<Point>() {
					@Override
					public int compare(Point arg0, Point arg1) {
						return (int) (arg1.y - arg0.y);
					}
				});
				Point smallestY = corner.get(0);
				Point secSmallestY = corner.get(1);
				boolean left = smallestY.x < secSmallestY.x;

				Core.putText(draw, "X", smallestY, Core.FONT_HERSHEY_PLAIN, 3,
						new Scalar(255));
				Core.putText(draw, left ? "LEFT" : "RIGHT",
						new Point(550, 400), Core.FONT_HERSHEY_PLAIN, 2,
						new Scalar(255));

				pixels[0] = pixels[0] / points;
				pixels[1] = pixels[1] / points;

				Point p = new Point(pixels);
				// Core.putText(draw,text, p, Core.FONT_HERSHEY_PLAIN, 2, new
				// Scalar(255));
				Core.putText(draw, "*", p, Core.FONT_HERSHEY_PLAIN, 2,
						new Scalar(255));
				Core.line(draw, new Point(500, 0), new Point(500, 480),
						new Scalar(255));

				String t = (int) p.x + "-" + (int) p.y;
				boolean decision;
				if (left) {
					if (p.x < 500) {
						decision = true;
					} else {
						decision = false;
					}
				} else {
					if (p.x > 500) {
						decision = true;
					} else {
						decision = false;
					}
				}
				t = t + (decision ? "LEFT" : "RIGHT");

				Core.putText(draw, t, new Point(20, 400),
						Core.FONT_HERSHEY_PLAIN, 2, new Scalar(255));

			}
		}
		return draw;
	}

	Mat last;
	ArrayList<Scene> scenes = new ArrayList<Scene>();
	Scene refScene;
	ProgressDialog progress;

	public void takePic1(View w) {
		Scene scene = new Scene(last);
		scenes.add(scene);
		addBtn.setText("Add (" + scenes.size() + ")");
	}

	public void takePic2(View w) {
		Mat im = last.clone();
		// Imgproc.cvtColor(im, im, Imgproc.COLOR_BGR2RGB);
		Bitmap bmp = Bitmap.createBitmap(im.cols(), im.rows(),
				Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(im, bmp);
		matchDrawArea.setImageBitmap(bmp);
		refScene = new Scene(last);
	}

	public void compareClick(View w) {
		if (scenes.size() == 0) {
			AlertDialog alertDialog = new AlertDialog.Builder(this).create();
			alertDialog.setTitle("No scenes.");
			alertDialog
					.setMessage("You should add scenes to compare the reference image.");
			alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub

						}
					});
			alertDialog.show();
		} else if (refScene == null) {
			AlertDialog alertDialog = new AlertDialog.Builder(this).create();
			alertDialog.setTitle("No reference image.");
			alertDialog.setMessage("You should take a reference image to compare with the scenes you have taken before.");
			alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub

						}
					});

			alertDialog.show();

		} else {
			new BetterComparePics(this).execute();
		}
	}

	public void removeAll(View w) {
		scenes.clear();
		addBtn.setText("Add (" + scenes.size() + ")");
	}

	class BetterComparePics extends AsyncTask<Void, Integer, SceneDetectData> {
		Context context;

		public BetterComparePics(Context context) {
			this.context = context;
		}

		@Override
		protected void onPreExecute() {
			progress = new ProgressDialog(context);
			progress.setCancelable(false);
			progress.setMessage("Starting to Compare Images");
			progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progress.setProgress(1);
			progress.setMax(scenes.size());
			progress.show();
			super.onPreExecute();
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			progress.setProgress(values[0]);
			super.onProgressUpdate(values);
		}

		@Override
		protected SceneDetectData doInBackground(Void... params) {
			long s = System.currentTimeMillis();
			Scene max = null;
			SceneDetectData maxData = null;
			int maxDist = -1;
			int idx = -1;
			for (int i = 0; i < scenes.size(); i++) {
				Scene scn = scenes.get(i);
				SceneDetectData data = refScene.compare(scn, ransacEnabled,
						imageOnly);
				int currDist;
				if (ransacEnabled) {
					currDist = data.homo_matches;
				} else {
					currDist = data.dist_matches;
				}

				if (currDist > maxDist) {
					max = scn;
					maxData = data;
					maxDist = currDist;
					idx = i;
				}
				this.publishProgress(i + 1);
			}

			bmp = maxData.bmp;
			long e = System.currentTimeMillis();
			maxData.elapsed = e - s;
			maxData.idx = idx;

			return maxData;
		}

		@Override
		protected void onPostExecute(SceneDetectData maxData) {
			// info.setText(result);
			progress.dismiss();

			final Dialog settingsDialog = new Dialog(context);
			settingsDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
			settingsDialog.setContentView(getLayoutInflater().inflate(
					R.layout.image_layout, null));
			ImageView im = (ImageView) settingsDialog
					.findViewById(R.id.imagePopup);
			Button dismiss = (Button) settingsDialog
					.findViewById(R.id.dismissBtn);
			TextView info = (TextView) settingsDialog
					.findViewById(R.id.infoText);

			im.setImageBitmap(bmp);
			dismiss.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					settingsDialog.dismiss();
				}
			});

			info.setText(maxData.toString());

			settingsDialog.show();

			super.onPostExecute(maxData);
		}
	}

	boolean ransacEnabled = false;
	Bitmap bmp;

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		last = inputFrame.rgba();
		return last;
	}

}
