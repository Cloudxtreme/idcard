package org.us.x42.kyork.idcard;

import android.app.PendingIntent;
import android.content.DialogInterface;
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
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import org.us.x42.kyork.idcard.tasks.CardNFCTask;
import org.us.x42.kyork.idcard.tasks.ReadCardTask;

import java.util.Objects;

public class IncomingScanActivity extends AppCompatActivity {
    private static final String LOG_TAG = IncomingScanActivity.class.getSimpleName();

    private boolean needForegroundScan;
    @Nullable
    private NfcAdapter mAdapter;

    private ReadCardTask mTask;
    private TextView mStatusText;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_communicate);

        Log.i(LOG_TAG, "IncomingScanActivity");

        mStatusText = findViewById(R.id.communicate_text);

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == CardNFCTask.MSG_ID_NFC_STATUS) {
                    int msgInt = 0;
                    if (msg.arg1 != 0) {
                        mStatusText.setText(msg.arg1);
                        msgInt = msg.arg1;
                    } else if (msg.obj instanceof Integer) {
                        mStatusText.setText((Integer) msg.obj);
                        msgInt = (Integer) msg.obj;
                    } else if (msg.obj instanceof String) {
                        mStatusText.setText((String) msg.obj);
                    } else {
                        Log.i(LOG_TAG, "wtf did i just get in that Message: " + msg.obj.getClass().getName() + " " + msg.obj.toString());
                    }
                } else if (msg.what == CardNFCTask.MSG_ID_NFC_DONE) {
                    // Operation complete
                    Intent viewProfileIntent = new Intent(IncomingScanActivity.this, IntraProfileActivity.class);
                    String errorString = mTask.getErrorString(IncomingScanActivity.this);
                    if (errorString == null || errorString.isEmpty()) {
                        viewProfileIntent.putExtra("idcard", mTask.getCard());
                        viewProfileIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
                        IncomingScanActivity.this.startActivity(viewProfileIntent);
                        finish();
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(IncomingScanActivity.this);
                        builder.setTitle(R.string.incomingscan_err_title);
                        builder.setMessage(errorString);
                        DialogCloseListener l = new DialogCloseListener();
                        builder.setOnDismissListener(l);
                        builder.setOnCancelListener(l);
                        builder.create().show();
                    }
                }
            }
        };
    }

    private class DialogCloseListener implements DialogInterface.OnDismissListener, DialogInterface.OnCancelListener {
        @Override
        public void onDismiss(DialogInterface dialogInterface) {
            IncomingScanActivity.this.finish();
        }

        @Override
        public void onCancel(DialogInterface dialogInterface) {
            IncomingScanActivity.this.finish();
        }
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
            mTask = new ReadCardTask();
            mTask.setTagAndHandler(tag, mHandler);
            mTask.execute();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAdapter != null && needForegroundScan) {
            mAdapter.disableForegroundDispatch(this);
        }
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
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            needForegroundScan = false;
            if (mAdapter != null) {
                mAdapter.disableForegroundDispatch(this);
            }
            mTask = new ReadCardTask();
            mTask.setTagAndHandler(tag, mHandler);
            mTask.execute();
        }
    }
}
