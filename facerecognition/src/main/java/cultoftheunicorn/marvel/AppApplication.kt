package cultoftheunicorn.marvel

import android.app.Application
import android.os.Environment

class AppApplication : Application() {

    lateinit var personRecognizer: PersonRecognizer
        private set

    override fun onCreate() {
        super.onCreate()
        personRecognizer = PersonRecognizer(Environment.getExternalStorageDirectory().toString() + "/facerecogOCV/")
    }
}