package org.us.x42.kyork.idcard;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import org.us.x42.kyork.idcard.tasks.CardNFCTask;

import java.util.Objects;

public class CardWriteActivity extends AppCompatActivity {
    private static final String LOG_TAG = CardWriteActivity.class.getSimpleName();
    private NfcAdapter mAdapter;

    private PendingIntent scanIntent;
    private IntentFilter[] scanFilter;
    private String[][] scanTechs;

    private CardNFCTask mTask;

    private TextView mStatusText;
    private Handler mHandler;

    public static final String CARD_JOB_PARAMS = "org.us.x42.kyork.idcard.CARD_JOB_PARAMS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_communicate);

        mAdapter = NfcAdapter.getDefaultAdapter(this);

        scanIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        scanFilter = new IntentFilter[]{new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)};
        scanTechs = new String[][] { new String[] { IsoDep.class.getName(), NfcA.class.getName() } };

        mStatusText = findViewById(R.id.communicate_text);

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == CardNFCTask.MSG_ID_NFC_STATUS) {
                    if (msg.arg1 != 0) {
                        mStatusText.setText(msg.arg1);
                    } else if (msg.obj instanceof Integer) {
                        mStatusText.setText((Integer) msg.obj);
                    } else if (msg.obj instanceof String) {
                        mStatusText.setText((String) msg.obj);
                    } else {
                        Log.i(LOG_TAG, "unrecognized Message payload: " + msg.obj.getClass().getName() + " " + msg.obj.toString());
                    }
                } else if (msg.what == CardNFCTask.MSG_ID_NFC_DONE) {
                    // Operation complete
                    Intent returnData = new Intent(Intent.ACTION_VIEW);
                    returnData.putExtra(CARD_JOB_PARAMS, mTask);
                    setResult(RESULT_OK, returnData);
                    finish();
                }
            }
        };

        Intent launchIntent = getIntent();
        mTask = launchIntent.getParcelableExtra(CARD_JOB_PARAMS);
        if (mTask == null) {
            Log.e("CardWriteActivity", "no CARD_DATA extra");
            setResult(Activity.RESULT_CANCELED);
            finish();
            return;
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        mAdapter.disableForegroundDispatch(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.enableForegroundDispatch(this, scanIntent, scanFilter, scanTechs);
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (Objects.equals(intent.getAction(), NfcAdapter.ACTION_TECH_DISCOVERED)) {
            // is an NDEF tag
            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            Log.i(LOG_TAG, "got tag");

            mTask.setTagAndHandler(tagFromIntent, mHandler);
            mTask.execute();
        }
    }
}
