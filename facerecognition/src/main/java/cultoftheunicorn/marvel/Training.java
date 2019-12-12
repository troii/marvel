package cultoftheunicorn.marvel;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.cultoftheunicorn.marvel.R;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Training extends AppCompatActivity {

    static {
        OpenCVLoader.initDebug();
        System.loadLibrary("opencv_java");
    }

    public static final int TRAINING = 0;
    public static final int IDLE = 2;
    private static final long MAXIMG = 10;
    private static final String TAG = "OCVSample::Activity";
    private static final Scalar FACE_RECT_COLOR = new Scalar(255, 0, 0, 255);

    String mPath = "";
    String userName;
    Bitmap mBitmap;
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage");
            if (msg.obj == "IMG") {
                Canvas canvas = new Canvas();
                canvas.setBitmap(mBitmap);
                imageView.setImageBitmap(mBitmap);
                if (countImages >= MAXIMG - 1) {
                    buttonCapture.setChecked(false);
                    captureOnClick();
                }
            }
        }
    };

    PersonRecognizer fr;

    int countImages = 0;
    private int faceState = IDLE;
    private Mat mRgba;
    private Mat mGray;
    private CascadeClassifier mJavaDetector;
    private int mAbsoluteFaceSize = 0;

    private ToggleButton buttonCapture;
    private Tutorial3View tutorial3View;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);

        userName = getIntent().getStringExtra("name");

        imageView = (ImageView) findViewById(R.id.imagePreview);

        buttonCapture = (ToggleButton) findViewById(R.id.capture);
        buttonCapture.setOnCheckedChangeListener(onCheckedChangedListener);

        tutorial3View = (Tutorial3View) findViewById(R.id.tutorial3_activity_java_surface_view);
        tutorial3View.setCvCameraViewListener(cvCameraViewListener2);

        mPath = Environment.getExternalStorageDirectory() + "/facerecogOCV/";

        if (!new File(mPath).mkdirs()) {
            Log.e("Error", "Error creating directory");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (tutorial3View != null)
            tutorial3View.disableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tutorial3View.disableView();
    }

    private void captureOnClick() {
        if (buttonCapture.isChecked())
            faceState = TRAINING;
        else {
            Toast.makeText(this, "Captured", Toast.LENGTH_SHORT).show();
            countImages = 0;
            faceState = IDLE;
            imageView.setImageResource(R.drawable.user_image);
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

                    fr = new PersonRecognizer(mPath);
                    fr.load();

                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        File mCascadeFile = new File(cascadeDir, "lbpcascade.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    tutorial3View.setCamFront();
                    tutorial3View.enableView();

                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;


            }
        }
    };

    CameraBridgeViewBase.CvCameraViewListener2 cvCameraViewListener2 = new CameraBridgeViewBase.CvCameraViewListener2() {
        @Override
        public void onCameraViewStarted(int width, int height) {
            Log.d(TAG, "onCameraViewStarted");
            mGray = new Mat();
            mRgba = new Mat();
        }

        @Override
        public void onCameraViewStopped() {
            Log.d(TAG, "onCameraViewStopped");
            mGray.release();
            mRgba.release();
        }

        @Override
        public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
            Log.d(TAG, "onCameraFrame");
            mRgba = inputFrame.rgba();
            mGray = inputFrame.gray();

            if (mAbsoluteFaceSize == 0) {
                int height = mGray.rows();
                float mRelativeFaceSize = 0.2f;
                if (Math.round(height * mRelativeFaceSize) > 0) {
                    mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
                }
            }

            MatOfRect faces = new MatOfRect();

            if (mJavaDetector != null)
                mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());

            Rect[] facesArray = faces.toArray();

            if ((facesArray.length == 1) && (faceState == TRAINING) && (countImages < MAXIMG) && (!userName.equals(""))) {
                Mat m;
                Rect r = facesArray[0];

                m = mRgba.submat(r);
                mBitmap = Bitmap.createBitmap(m.width(), m.height(), Bitmap.Config.ARGB_8888);

                Utils.matToBitmap(m, mBitmap);

                Message msg = new Message();
                msg.obj = "IMG";
                mHandler.sendMessage(msg);
                if (countImages < MAXIMG) {
                    fr.add(m, userName);
                    countImages++;
                }

            }
            for (Rect rect : facesArray) {
                Core.rectangle(mRgba, rect.tl(), rect.br(), FACE_RECT_COLOR, 3);
            }

            return mRgba;
        }
    };

    private CompoundButton.OnCheckedChangeListener onCheckedChangedListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            captureOnClick();
        }
    };
}
