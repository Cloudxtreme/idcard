package org.us.x42.kyork.idcard;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcA;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.us.x42.kyork.idcard.data.IDCard;

import java.util.Objects;

public class IncomingScanActivity extends AppCompatActivity {
    private static final String LOG_TAG = IncomingScanActivity.class.getSimpleName();
    private boolean needForegroundScan;
    private NfcAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_communicate);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent launchIntent = getIntent();
        Tag tag = launchIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null) {
            needForegroundScan = true;
        } else {
            needForegroundScan = false;

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
        if (needForegroundScan) {
            PendingIntent scanIntent = PendingIntent.getActivity(
                    this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
            IntentFilter[] scanFilter = new IntentFilter[] { new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED) };
            String[][] scanTechs = new String[][] { new String[] { IsoDep.class.getName(), NfcA.class.getName() } };
            mAdapter.enableForegroundDispatch(this, scanIntent, scanFilter, scanTechs);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (Objects.equals(intent.getAction(), NfcAdapter.ACTION_TECH_DISCOVERED)) {
            setIntent(intent);
        }
    }


    private static class NfcCommunicateTask extends AsyncTask<Tag, String, IDCard> {
        Tag mTag;
        IsoDep mTagTech;
        Handler destHandler;

        NfcCommunicateTask(Tag tag, Handler msgDest) {
            this.mTag = tag;
            this.destHandler = msgDest;
        }

        @Override
        protected IDCard doInBackground(Tag... tags) {
            return null;
        }
    }
}
