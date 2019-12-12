package cultoftheunicorn.marvel

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import android.widget.ToggleButton
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.roundToInt

private const val TAG = "TrainingActivity"
private const val MAXIMG: Long = 10
private val FACE_RECT_COLOR = Scalar(255.0, 0.0, 0.0, 255.0)

class TrainingActivity : AppCompatActivity() {
    private lateinit var pathToImages: String
    private var mBitmap: Bitmap? = null

    private lateinit var username: String
    private lateinit var personRecognizer: PersonRecognizer
    private var countImages = 0

    private var faceState = FaceState.IDLE
    private var mRgba: Mat? = null
    private var mGray: Mat? = null
    private lateinit var mJavaDetector: CascadeClassifier

    private var mAbsoluteFaceSize = 0
    private lateinit var buttonCapture: ToggleButton
    private lateinit var tutorial3View: Tutorial3View
    private lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)

        username = intent.getStringExtra(EXTRA_USERNAME)

        imageView = findViewById(R.id.imagePreview) as ImageView
        buttonCapture = findViewById(R.id.capture) as ToggleButton
        tutorial3View = findViewById(R.id.tutorial3_activity_java_surface_view) as Tutorial3View

        buttonCapture.setOnCheckedChangeListener { _, _ -> captureOnClick() }

        tutorial3View.setCvCameraViewListener(cameraViewListener)
        pathToImages = Environment.getExternalStorageDirectory().toString() + "/facerecogOCV/"
        if (!File(pathToImages).mkdirs()) {
            Log.e("Error", "Error creating directory")
        }
    }

    override fun onResume() {
        super.onResume()
        loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
    }

    override fun onPause() {
        super.onPause()
        tutorial3View.disableView()
    }

    override fun onDestroy() {
        super.onDestroy()
        tutorial3View.disableView()
    }

    private fun captureOnClick() {
        if (buttonCapture.isChecked) {
            faceState = FaceState.TRAINING
        } else {
            Toast.makeText(this, "Captured", Toast.LENGTH_SHORT).show()
            countImages = 0
            faceState = FaceState.IDLE
            imageView.setImageResource(R.drawable.user_image)
        }
    }

    private val loaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.i(TAG, "OpenCV loaded successfully")
                    personRecognizer = PersonRecognizer(pathToImages)
                    try { // load cascade file from application resources
                        val inputStream = resources.openRawResource(R.raw.lbpcascade_frontalface)
                        val cascadeDir = getDir("cascade", Context.MODE_PRIVATE)
                        val mCascadeFile = File(cascadeDir, "lbpcascade.xml")
                        val os = FileOutputStream(mCascadeFile)
                        val buffer = ByteArray(4096)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            os.write(buffer, 0, bytesRead)
                        }
                        inputStream.close()
                        os.close()
                        mJavaDetector = CascadeClassifier(mCascadeFile.absolutePath)
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier")
                        } else Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.absolutePath)
                        cascadeDir.delete()
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Log.e(TAG, "Failed to load cascade. Exception thrown: $e")
                    }
                    tutorial3View.setCamFront()
                    tutorial3View.enableView()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }
    private var cameraViewListener = object : CvCameraViewListener2 {
        override fun onCameraViewStarted(width: Int, height: Int) {
            Log.d(TAG, "onCameraViewStarted")
            mGray = Mat()
            mRgba = Mat()
        }

        override fun onCameraViewStopped() {
            Log.d(TAG, "onCameraViewStopped")
            mGray?.release()
            mRgba?.release()
        }

        override fun onCameraFrame(inputFrame: CvCameraViewFrame): Mat? {
            Log.d(TAG, "onCameraFrame")
            mRgba = inputFrame.rgba()
            mGray = inputFrame.gray()
            if (mAbsoluteFaceSize == 0) {
                val height = mGray?.rows() ?: 0
                val mRelativeFaceSize = 0.2f
                if ((height * mRelativeFaceSize).roundToInt() > 0) {
                    mAbsoluteFaceSize = (height * mRelativeFaceSize).roundToInt()
                }
            }
            val faces = MatOfRect()

            mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, Size(mAbsoluteFaceSize.toDouble(), mAbsoluteFaceSize.toDouble()), Size())

            val facesArray = faces.toArray()
            if (facesArray.size == 1 && faceState == FaceState.TRAINING && countImages < MAXIMG && username != "") {
                val r = facesArray[0]
                val m: Mat = mRgba?.submat(r) ?: Mat()

                mBitmap = Bitmap.createBitmap(m.width(), m.height(), Bitmap.Config.ARGB_8888)
                Utils.matToBitmap(m, mBitmap)

                runOnUiThread {
                    val canvas = Canvas()
                    canvas.setBitmap(mBitmap)
                    imageView.setImageBitmap(mBitmap)
                    if (countImages >= MAXIMG - 1) {
                        buttonCapture.isChecked = false
                        captureOnClick()
                    }
                }

                if (countImages < MAXIMG) {
                    personRecognizer.add(m, username)
                    countImages++
                }
            }
            for (rect in facesArray) {
                Core.rectangle(mRgba, rect.tl(), rect.br(), FACE_RECT_COLOR, 3)
            }
            return mRgba
        }
    }

    companion object {
        private const val EXTRA_USERNAME = "username"
        fun start(context: Context, username: String) {
            context.startActivity(Intent(context, TrainingActivity::class.java).apply {
                putExtra(EXTRA_USERNAME, username)
            })
        }
    }
}

enum class FaceState {
    IDLE, TRAINING
}