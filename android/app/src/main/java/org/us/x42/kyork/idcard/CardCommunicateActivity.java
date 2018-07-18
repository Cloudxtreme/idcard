package org.us.x42.kyork.idcard;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.nfc.tech.IsoDep;

import android.util.Log;
import android.widget.TextView;


import java.io.IOException;
import java.util.Objects;

public class CardCommunicateActivity extends AppCompatActivity {
    private static final String LOG_TAG = CardCommunicateActivity.getClass().getSimpleName();

    private NfcAdapter mAdapter;

    private PendingIntent scanIntent;
    private IntentFilter[] scanFilter;
    private String[][] scanTechs;

    private TextView mStatusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_communicate);

        mAdapter = NfcAdapter.getDefaultAdapter(this);

        scanIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        scanFilter = new IntentFilter[] { new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED) };
        scanTechs = new String[][] { new String[] { IsoDep.class.getName() } };

        mStatusText = findViewById(R.id.communicate_text);
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

            new Thread(new GetCardInfoRunnable(tagFromIntent)).start();;
        }
    }


    static class CardCommunicationManager {
    }

    static class GetCardInfoRunnable implements Runnable {
        IsoDep mTag;

        GetCardInfoRunnable(Tag tag) {
            this.mTag = IsoDep.get(tag);
        }

        @Override
        public void run() {
            Log.i(LOG_TAG, "started thread, connecting to tag");
            try {
                mTag.connect();
                mTag.transceive()

            } catch (IOException e) {
                Log.e(LOG_TAG, "Exception while talking to tag", e);
            }
        }
    }
}
