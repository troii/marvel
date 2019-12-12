package cultoftheunicorn.marvel;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class NameActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_name);

        final EditText name = (EditText) findViewById(R.id.name);
        Button nextButton = (Button) findViewById(R.id.nextButton);

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!name.getText().toString().equals("")) {
                    TrainingActivity.Companion.start(NameActivity.this, name.getText().toString().trim());
                } else {
                    Toast.makeText(NameActivity.this, "Please enter the name", Toast.LENGTH_LONG).show();
                }
            }
        });

    }
}
