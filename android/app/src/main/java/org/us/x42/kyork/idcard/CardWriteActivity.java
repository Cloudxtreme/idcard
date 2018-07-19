package org.us.x42.kyork.idcard;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcA;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.us.x42.kyork.idcard.desfire.DESFireCard;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CardWriteActivity extends AppCompatActivity {
    private static final String LOG_TAG = CardWriteActivity.class.getSimpleName();
    private NfcAdapter mAdapter;

    private PendingIntent scanIntent;
    private IntentFilter[] scanFilter;
    private String[][] scanTechs;

    private CardJob mJob;

    private TextView mStatusText;
    private Handler mHandler;
    private ProgressBar mLoading;

    private static final int MSG_ID_NFC_STATUS = 23;
    private static final int MSG_ID_DONE = 4;
    public static final String CARD_PAYLOAD = "CARD_PAYLOAD";
    public static final String CARD_RESULT = "CARD_RESULT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_communicate);

        Intent launchIntent = getIntent();
        mJob = launchIntent.getParcelableExtra(CARD_PAYLOAD);
        if (mJob == null) {
            Log.e("CardWriteActivity", "no CARD_DATA extra");
            setResult(Activity.RESULT_CANCELED);
            finish();
            return;
        }

        mAdapter = NfcAdapter.getDefaultAdapter(this);

        scanIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        scanFilter = new IntentFilter[]{new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)};
        scanTechs = new String[][] { new String[] { IsoDep.class.getName(), NfcA.class.getName() } };

        mStatusText = findViewById(R.id.communicate_text);
        mLoading = findViewById(R.id.card_communicate_progress);

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MSG_ID_NFC_STATUS) {
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

                    if (msgInt == R.string.nfc_done) {
                        mLoading.setProgress(100);
                    }
                } else if (msg.what == MSG_ID_DONE) {
                    Intent returnData = new Intent(Intent.ACTION_VIEW);
                    returnData.putExtra(CARD_RESULT, (Parcelable) msg.obj);
                    setResult(RESULT_OK, returnData);
                    finish();
                }
            }
        };
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

            Log.i(LOG_TAG, "got tag BANANAS");

            new NfcCommunicateTask(tagFromIntent, this.mHandler, mJob).execute(new byte[]{});
        }
    }

    private static class NfcCommunicateTask extends AsyncTask<byte[], String, byte[]> {
        Tag mTag;
        DESFireCard mCard;
        Handler destHandler;
        CardJob mJob;

        Cipher mSessionCipher;

        NfcCommunicateTask(Tag tag, Handler msgDest, CardJob job) {
            this.mTag = tag;
            this.destHandler = msgDest;
            this.mJob = job;
        }

        private static String stringifyByteArray(byte[] data) {
            if (data == null) {
                return "(null)";
            }

            StringBuilder sb = new StringBuilder();
            sb.append('[');
            sb.append(' ');
            for (byte d : data) {
                sb.append((int) d);
                sb.append(' ');
            }
            sb.append(']');
            return sb.toString();
        }

        byte[] sendAndLog(byte cmdId, byte[] data) throws Exception {
            Log.d(LOG_TAG, "Sending command " + cmdId + " - " + stringifyByteArray(data));

            try {
                byte[] response = mCard.sendRequest(cmdId, data);
                Log.d(LOG_TAG, "Command response: " + stringifyByteArray(response));

                return response;
            } catch (Exception e) {
                throw e;
            }
        }

        @Override
        protected byte[] doInBackground(byte[]... bytes) {
            publishProgress("found");
            Log.i(LOG_TAG, "started task, connecting to tag");
            try {
                byte[] tagId = mTag.getId();
                Log.i(LOG_TAG, "Tag ID: " + stringifyByteArray(tagId));

                mCard = new DESFireCard(mTag);
                mCard.connect();
                this.publishProgress("Connected...");

                // Select application
                byte[] response, args;

                mCard.selectApplication(mJob.appId);

                // Authenticated
                if (this.mJob.encKey != null) {
                    mCard.establishAuthentication(this.mJob.keyId, this.mJob.encKey);
                }

                // Perform each thing

                for (CardJob.CardOp op : mJob.commands) {
                    sendAndLog(op.getCommandId(), op.encode());
                }

                Log.i(LOG_TAG, "sleeping");
                Message.obtain(this.destHandler, MSG_ID_NFC_STATUS, R.string.nfc_done).sendToTarget();
                Thread.sleep(2 * 1000);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Exception while talking to tag", e);
                this.publishProgress(e.getMessage());
            }

            return null;
        }

        protected void onProgressUpdate(String statusMessageId) {
            switch (statusMessageId) {
                case "start":
                    Message.obtain(this.destHandler, MSG_ID_NFC_STATUS, R.string.nfc_open, 0).sendToTarget();
                case "found":
                    Message.obtain(this.destHandler, MSG_ID_NFC_STATUS, R.string.nfc_found, 0).sendToTarget();
                case "done":
                    Message.obtain(this.destHandler, MSG_ID_NFC_STATUS, R.string.nfc_done, 0).sendToTarget();
                default:
                    Message.obtain(this.destHandler, MSG_ID_NFC_STATUS, statusMessageId).sendToTarget();
            }
        }

        @Override
        protected void onPostExecute(byte[] bytes) {
            super.onPostExecute(bytes);
            // TODO(kyork): return a result?
            Message.obtain(this.destHandler, MSG_ID_DONE, (Parcelable) null).sendToTarget();
        }
    }


    static class CardCommunicationManager {
    }

    static class GetCardInfoRunnable implements Runnable {
        Tag mTag;

        GetCardInfoRunnable(Tag tag) {
            this.mTag = tag;
        }

        @Override
        public void run() {
        }
    }
}
