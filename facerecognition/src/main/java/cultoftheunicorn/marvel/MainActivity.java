package cultoftheunicorn.marvel;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button recognizeButton = (Button) findViewById(R.id.recognizeButton);
        Button trainingButton = (Button) findViewById(R.id.trainingButton);
        Button clearData = (Button) findViewById(R.id.clearData);

        final String path = Environment.getExternalStorageDirectory().toString() + "/facerecog.yml";
        findViewById(R.id.buttonLoad).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((AppApplication) getApplication()).getPersonRecognizer().loadDataForFaceRecognizer(path);
                Log.d("MainActivity","stuff loaded");
            }
        });

        findViewById(R.id.buttonSave).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((AppApplication) getApplication()).getPersonRecognizer().saveDataForFaceRecognizer(path);
            }
        });

        recognizeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, Recognize.class));
            }
        });

        trainingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, NameActivity.class));
            }
        });

        clearData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mPath = Environment.getExternalStorageDirectory() + "/facerecogOCV/";
                deleteDirectory(new File(mPath));
                Toast.makeText(MainActivity.this, "Facedata cleared!", Toast.LENGTH_SHORT).show();
            }
        });

        OpenCVLoader.initDebug();
        System.loadLibrary("opencv_java");
    }

    public static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files == null) {
                return true;
            }
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }
}
