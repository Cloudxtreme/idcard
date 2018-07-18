package org.us.x42.kyork.idcard;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.common.io.BaseEncoding;

public class SetupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        final Spinner appid_spinner = findViewById(R.id.appid_spinner);
        final Spinner keyid_spinner = findViewById(R.id.keyid_spinner);
        final EditText cmd_id_edittext = findViewById(R.id.setup_cmdid_text);
        final EditText payload_edittext = findViewById(R.id.setup_payload_editText);
        final TextView errorText = findViewById(R.id.errorText);

        Button clickButton = (Button) findViewById(R.id.nfc_test_button);
        clickButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int appId;
                switch (appid_spinner.getSelectedItemPosition()) {
                    case 0:
                    default:
                        appId = CardJob.APP_ID_NULL;
                    case 1:
                        appId = CardJob.APP_ID_CARD42;
                }
                byte[] encKey;
                switch (keyid_spinner.getSelectedItemPosition()) {
                    default:
                    case 0: // No encryption
                        encKey = CardJob.ENC_KEY_NONE;
                    case 1: // test master
                        encKey = CardJob.ENC_KEY_MASTER_TEST;
                    case 2: // Null key
                        encKey = CardJob.ENC_KEY_NULL;
                    case 3: // public key
                        encKey = CardJob.ENC_KEY_ANDROID_PUBLIC;
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
                if (!BaseEncoding.base16().canDecode(inputText)) {
                    errorText.setText(R.string.error_not_valid_hex_bytes);
                    return;
                }
                byte[] data = BaseEncoding.base16().decode(inputText);

                Intent intent = new Intent(SetupActivity.this, CardWriteActivity.class);
                CardJob job = new CardJob(appId, encKey,
                        new CardJob.CardOpRaw(cmdId, data));
                intent.putExtra("CARD_PAYLOAD", job);
                startActivity(intent);
            }
        });
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
