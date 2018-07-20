package org.us.x42.kyork.idcard;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.common.io.BaseEncoding;

import org.us.x42.kyork.idcard.tasks.CommandTestTask;

import java.util.ArrayList;
import java.util.List;

public class SetupActivity extends AppCompatActivity {

    private static final int NFC_REQUEST_CODE = 3;

    private TextView resultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final Spinner appid_spinner = findViewById(R.id.appid_spinner);
        final Spinner keyid_spinner = findViewById(R.id.keyid_spinner);
        final EditText cmd_id_edittext = findViewById(R.id.setup_cmdid_text);
        final EditText payload_edittext = findViewById(R.id.setup_payload_editText);
        final TextView errorText = findViewById(R.id.errorText);
        resultText = findViewById(R.id.nfc_test_results_box);

        Button clickButton = findViewById(R.id.nfc_test_button);
        clickButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int appId;
                Log.i(this.getClass().getSimpleName(), "appid spinner position " + appid_spinner.getSelectedItemPosition());
                switch (appid_spinner.getSelectedItemPosition()) {
                    case 0:
                    default:
                        appId = CardJob.APP_ID_NULL;
                        break;
                    case 1:
                        appId = CardJob.APP_ID_CARD42;
                        break;
                }
                byte[] encKey = null;
                byte keyId = 0xE;
                switch (keyid_spinner.getSelectedItemPosition()) {
                    default:
                    case 0: // No encryption
                        encKey = CardJob.ENC_KEY_NONE;
                        break;
                    case 1: // public key
                        encKey = CardJob.ENC_KEY_ANDROID_PUBLIC;
                        keyId = 2;
                        break;
                    case 2: // test master
                        encKey = CardJob.ENC_KEY_MASTER_TEST;
                        keyId = 0;
                        break;
                    case 3: // Null Master key
                        encKey = CardJob.ENC_KEY_NULL;
                        keyId = 0;
                        break;
                }
                String cmdIdStr = cmd_id_edittext.getText().toString();
                int cmdIdInt;
                try {
                    cmdIdInt = Integer.parseInt(cmdIdStr.trim(), 16);
                } catch (NumberFormatException e) {
                    errorText.setText(R.string.error_cmdid_out_of_range);
                    return;
                }
                if (cmdIdInt < 0 || cmdIdInt > 255) {
                    errorText.setText(R.string.error_cmdid_out_of_range);
                    return;
                }
                byte cmdId = (byte)cmdIdInt;

                String inputText = payload_edittext.getText().toString();
                inputText = inputText.replaceAll(" ", "");
                if (!BaseEncoding.base16().canDecode(inputText)) {
                    errorText.setText(R.string.error_not_valid_hex_bytes);
                    return;
                }
                byte[] data;
                if (inputText.isEmpty()) {
                    data = null;
                } else {
                    data = BaseEncoding.base16().decode(inputText);
                }
                errorText.setText("");

                Intent intent = new Intent(SetupActivity.this, CardWriteActivity.class);
                intent.putExtra(CardWriteActivity.CARD_JOB_TYPE, "CommandTestTask");
                CommandTestTask task = new CommandTestTask(appId, keyId, encKey, cmdId, data);
                intent.putExtra(CardWriteActivity.CARD_JOB_PARAMS, task);
                startActivityForResult(intent, NFC_REQUEST_CODE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == NFC_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                CommandTestTask task = data.getParcelableExtra(CardWriteActivity.CARD_JOB_PARAMS);
                StringBuilder sb = new StringBuilder();

                if (task.getErrorCode() != 0) {
                    sb.append("Card returned error: ");
                    sb.append(Integer.toHexString(task.getErrorCode()));
                } else {
                    byte[] responseData = task.getResponseData();
                    sb.append("Result: ").append(responseData.length).append("\n");
                    for (int i = 0; i < responseData.length; i++) {
                        sb.append(String.format("%02X ", responseData[i]));
                        if (i % 8 == 7) {
                            sb.append('\n');
                        } else if (i % 2 == 1) {
                            sb.append(' ');
                        }
                    }
                }

                resultText.setText(sb);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_setup, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onClick(View v) {
        if (v == findViewById(R.id.nfc_test_button)) {
            throw new RuntimeException("reached");
        }
        throw new RuntimeException("if returned false");
    }
}
