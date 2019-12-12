package cultoftheunicorn.marvel;

import android.graphics.Bitmap;
import android.util.Log;

import com.googlecode.javacv.cpp.opencv_contrib.FaceRecognizer;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_core.MatVector;
import com.googlecode.javacv.cpp.opencv_imgproc;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import static com.googlecode.javacv.cpp.opencv_highgui.cvLoadImage;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2GRAY;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;

public class PersonRecognizer {

    static final int WIDTH = 128;
    static final int HEIGHT = 128;
    FaceRecognizer faceRecognizer;
    String mPath;
    int count = 0;
    Labels labelsFile;
    private int mProb = 999;


    /***
     * createLBPHFaceRecognizer(int radius=1, int neighbors=8, int grid_x=8, int grid_y=8, double threshold=DBL_MAX
     *
     * radius – The radius used for building the Circular Local Binary Pattern. The greater the radius, the
     * neighbors – The number of sample points to build a Circular Local Binary Pattern from. An appropriate value is to use `` 8`` sample points. Keep in mind: the more sample points you include, the higher the computational cost.
     * grid_x – The number of cells in the horizontal direction, 8 is a common value used in publications. The more cells, the finer the grid, the higher the dimensionality of the resulting feature vector.
     * grid_y – The number of cells in the vertical direction, 8 is a common value used in publications. The more cells, the finer the grid, the higher the dimensionality of the resulting feature vector.
     * threshold – The threshold applied in the prediction. If the distance to the nearest neighbor is larger than the threshold, this method returns -1.
     *
     * details see: https://docs.opencv.org/2.4/modules/contrib/doc/facerec/facerec_api.html#Ptr%3CFaceRecognizer%3E%20createLBPHFaceRecognizer(int%20radius,%20int%20neighbors,%20int%20grid_x,%20int%20grid_y,%20double%20threshold)
     *
     * @param path
     */
    PersonRecognizer(String path) {
        faceRecognizer = com.googlecode.javacv.cpp.opencv_contrib.createLBPHFaceRecognizer(1, 8, 8, 8, 200);
        // path=Environment.getExternalStorageDirectory()+"/facerecog/faces/";
        mPath = path;
        labelsFile = new Labels(mPath);


    }

    void add(Mat m, String description) {
        Bitmap bmp = Bitmap.createBitmap(m.width(), m.height(), Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(m, bmp);
        bmp = Bitmap.createScaledBitmap(bmp, WIDTH, HEIGHT, false);

        FileOutputStream f;
        try {
            f = new FileOutputStream(mPath + description + "-" + count + ".jpg", true);
            count++;
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, f);
            f.close();

        } catch (Exception e) {
            Log.e("error", e.getCause() + " " + e.getMessage());
            e.printStackTrace();

        }
    }

    public boolean train() {

        File root = new File(mPath);

        FilenameFilter pngFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".jpg");

            }

        };

        File[] imageFiles = root.listFiles(pngFilter);

        MatVector images = new MatVector(imageFiles.length);

        int[] labels = new int[imageFiles.length];

        int counter = 0;
        int label;

        IplImage img;
        IplImage grayImg;

        int i1 = mPath.length();


        for (File image : imageFiles) {
            String p = image.getAbsolutePath();
            img = cvLoadImage(p);

            if (img == null)
                Log.e("Error", "Error cVLoadImage");
            Log.i("image", p);

            int i2 = p.lastIndexOf("-");
            int i3 = p.lastIndexOf(".");
            int icount = Integer.parseInt(p.substring(i2 + 1, i3));
            if (count < icount) count++;

            String description = p.substring(i1, i2);

            if (labelsFile.get(description) < 0)
                labelsFile.add(description, labelsFile.max() + 1);

            label = labelsFile.get(description);

            grayImg = IplImage.create(img.width(), img.height(), IPL_DEPTH_8U, 1);

            cvCvtColor(img, grayImg, CV_BGR2GRAY);

            images.put(counter, grayImg);

            labels[counter] = label;

            counter++;
        }
        if (counter > 0)
            if (labelsFile.max() > 0)
                faceRecognizer.train(images, labels);
        labelsFile.Save();
        return true;
    }

    public boolean canPredict() {
        return labelsFile.max() > 0;

    }

    public String predict(Mat m) {
        if (!canPredict())
            return "";
        int[] n = new int[1];
        double[] p = new double[1];
        IplImage ipl = MatToIplImage(m, WIDTH, HEIGHT);
//		IplImage ipl = MatToIplImage(m,-1, -1);

        faceRecognizer.predict(ipl, n, p);

        if (n[0] != -1)
            mProb = (int) p[0];
        else
            mProb = -1;
        //	if ((n[0] != -1)&&(p[0]<95))
        if (n[0] != -1)
            return labelsFile.get(n[0]);
        else
            return "Unknown";
    }


    IplImage MatToIplImage(Mat m, int width, int heigth) {
        Bitmap bmp = Bitmap.createBitmap(m.width(), m.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(m, bmp);
        return BitmapToIplImage(bmp, width, heigth);
    }

    IplImage BitmapToIplImage(Bitmap bmp, int width, int height) {

        if ((width != -1) || (height != -1)) {
            Bitmap bmp2 = Bitmap.createScaledBitmap(bmp, width, height, false);
            bmp = bmp2;
        }

        IplImage image = IplImage.create(bmp.getWidth(), bmp.getHeight(),
                IPL_DEPTH_8U, 4);

        bmp.copyPixelsToBuffer(image.getByteBuffer());

        IplImage grayImg = IplImage.create(image.width(), image.height(),
                IPL_DEPTH_8U, 1);

        cvCvtColor(image, grayImg, opencv_imgproc.CV_BGR2GRAY);

        return grayImg;
    }


    protected void SaveBmp(Bitmap bmp, String path) {
        FileOutputStream file;
        try {
            file = new FileOutputStream(path, true);

            bmp.compress(Bitmap.CompressFormat.JPEG, 100, file);
            file.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            Log.e("", e.getMessage() + e.getCause());
            e.printStackTrace();
        }

    }


    public void load() {
        train();

    }

    public int getProb() {
        // TODO Auto-generated method stub
        return mProb;
    }


}
