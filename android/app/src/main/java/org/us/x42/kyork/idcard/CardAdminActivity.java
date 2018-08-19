package org.us.x42.kyork.idcard;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

import org.us.x42.kyork.idcard.desfire.DESFireProtocol;
import org.us.x42.kyork.idcard.tasks.CommandTestTask;
import org.us.x42.kyork.idcard.tasks.ProvisionBlankCardTask;

public class CardAdminActivity extends AppCompatActivity {
    private static final int REQUEST_ID_PROVISION = 7;
    private static final int REQUEST_ID_RUNNABLE = 2;
    private static final int REQUEST_ID_FORMAT = 3;
    private EditText loginTextbox;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_admin);

        loginTextbox = (EditText) findViewById(R.id.login_textbox);

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == REQUEST_ID_RUNNABLE) {
                    ((Runnable)msg.obj).run();
                }
            }
        };

        findViewById(R.id.cadmin_launch_manual).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CardAdminActivity.this, SetupActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.cadmin_launch_provisioning).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String login = loginTextbox.getText().toString().toLowerCase();
                if (login.isEmpty()) {
                    Snackbar s = Snackbar.make(loginTextbox, R.string.err_need_login, Snackbar.LENGTH_SHORT);
                    s.show();
                    return;
                }
                Intent intent = new Intent(CardAdminActivity.this, CardWriteActivity.class);
                ProvisionBlankCardTask task = new ProvisionBlankCardTask(login, false);
                intent.putExtra(CardWriteActivity.CARD_JOB_PARAMS, task);
                startActivityForResult(intent, REQUEST_ID_PROVISION);
            }
        });

        findViewById(R.id.cadmin_wipe_card).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CardAdminActivity.this, CardWriteActivity.class);
                CommandTestTask task = new CommandTestTask(0, (byte)0, CardJob.ENC_KEY_NULL, DESFireProtocol.FORMAT_PICC, new byte[0]);
                intent.putExtra(CardWriteActivity.CARD_JOB_PARAMS, task);
                startActivityForResult(intent, REQUEST_ID_FORMAT);
            }
        });

        findViewById(R.id.cadmin_launch_markdirty).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String login = loginTextbox.getText().toString().toLowerCase();
                if (login.isEmpty()) {
                    Snackbar s = Snackbar.make(loginTextbox, R.string.err_need_login, Snackbar.LENGTH_SHORT);
                    s.show();
                    return;
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {

                    }
                }).start();
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ID_PROVISION:
                if (resultCode == RESULT_OK) {
                    ProvisionBlankCardTask result = data.getParcelableExtra(CardWriteActivity.CARD_JOB_PARAMS);

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.provision_result_title);
                    if (result.getErrorString() == null || result.getErrorString().isEmpty()) {
                        builder.setMessage(R.string.provision_result_success);
                    } else {
                        builder.setMessage("Error: " + result.getErrorString());
                    }
                    builder.create().show();
                }
                break;
            case REQUEST_ID_FORMAT:
                if (resultCode == RESULT_OK) {
                    CommandTestTask result = data.getParcelableExtra(CardWriteActivity.CARD_JOB_PARAMS);

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.provision_result_title);
                    if (result.getErrorString() == null || result.getErrorString().isEmpty()) {
                        builder.setMessage("Card formatted successfully");
                    } else {
                        builder.setMessage("Error: " + result.getErrorString());
                    }
                    builder.create().show();
                }
                break;
        }
    }
}
