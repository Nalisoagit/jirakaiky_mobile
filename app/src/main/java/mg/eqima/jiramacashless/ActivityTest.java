package mg.eqima.jiramacashless;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;

public class ActivityTest extends AppCompatActivity {

    Button buttonP;
    ProgressDialog pDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        buttonP = findViewById(R.id.button);
        pDialog = new ProgressDialog(ActivityTest.this);
        pDialog.setMax(100);
        pDialog.setMessage("Its loading....");
        pDialog.setTitle("ProgressDialog bar example");
        pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

        buttonP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pDialog.show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            while (pDialog.getProgress() <= pDialog
                                    .getMax()) {
                                Thread.sleep(200);
                                handle.sendMessage(handle.obtainMessage());
                                if (pDialog.getProgress() == pDialog
                                        .getMax()) {
                                    pDialog.dismiss();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }

            Handler handle = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    pDialog.incrementProgressBy(1);
                }
            };

        });

    }
}