package org.us.x42.kyork.idcard;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.util.Objects;

public class CardWriteActivity extends AppCompatActivity {
    private static final String LOG_TAG = CardWriteActivity.class.getSimpleName();
    private NfcAdapter mAdapter;

    private PendingIntent scanIntent;
    private IntentFilter[] scanFilter;
    private String[][] scanTechs;

    private CardJob mJob;

    private TextView mStatusText;
    private Handler mHandler;

    private static final int MSG_ID_NFC_STATUS = 23;
    public static final String CARD_PAYLOAD = "CARD_PAYLOAD";

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
        scanFilter = new IntentFilter[] { new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED) };
        scanTechs = new String[][] { new String[] { IsoDep.class.getName() } };

        mStatusText = findViewById(R.id.communicate_text);

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MSG_ID_NFC_STATUS) {
                    if (msg.arg1 != 0) {
                        mStatusText.setText(msg.arg1);
                    } else {
                        if (msg.obj instanceof String) {
                            mStatusText.setText((String) msg.obj);
                        } else if (msg.obj instanceof Integer) {
                            mStatusText.setText((Integer) msg.obj);
                        } else {
                            Log.i(LOG_TAG, "wtf did i just get in that Message: " + msg.obj.getClass().getName() + " " + msg.obj.toString());
                        }
                    }
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
        IsoDep mTagTech;
        Handler destHandler;
        CardJob mJob;

        NfcCommunicateTask(Tag tag, Handler msgDest, CardJob job) {
            this.mTag = tag;
            this.destHandler = msgDest;
            this.mJob = job;
        }

        byte[] sendAndLog(byte cmdId, byte[] data) throws Exception {
            StringBuilder sb = new StringBuilder();
            sb.append("Sending command [");
            sb.append((int)cmdId);
            if (data == null) {
                sb.append("] [no data");
            } else {
                sb.append("] data [ ");
                for (byte d : data) {
                    sb.append((int) d);
                    sb.append(' ');
                }
            }
            sb.append("]");

            Log.d(LOG_TAG, sb.toString());

            try {
                byte[] response = this.sendRequest(cmdId, data);
                sb = new StringBuilder();
                sb.append("Command response: [ ");;
                for (byte d : response) {
                    sb.append((int) d);
                    sb.append(' ');
                }
                sb.append("]");
                Log.d(LOG_TAG, sb.toString());

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

                StringBuilder sb = new StringBuilder();
                sb.append("Tag ID: [");
                for (byte aResponse : tagId) {
                    sb.append((int) aResponse);
                    sb.append(' ');
                }
                sb.append("]");
                Log.i(LOG_TAG, sb.toString());

                mTagTech = IsoDep.get(mTag);

                mTagTech.connect();
                this.publishProgress("Connected...");

                // Select application
                byte[] response, args;

                args = new byte[3];
                args[0] = (byte) ((mJob.appId & 0xFF0000) >> 16);
                args[1] = (byte) ((mJob.appId & 0xFF00) >> 8);
                args[2] = (byte) (mJob.appId & 0xFF);
                response = sendAndLog(CardJob.SELECT_APPLICATION, args);

                // Authenticated
                if (this.mJob.encKey != null) {
                    response = sendAndLog(CardJob.AUTHENTICATE, new byte[] { this.mJob.keyId });
                }
                // Perform each thing

                for (CardJob.CardOp op : mJob.commands) {
                    sendAndLog(op.getCommandId(), op.encode());
                }

                Log.i(LOG_TAG, "sleeping");
                Thread.sleep(2*1000);
                Log.i(LOG_TAG, "reached end");

            } catch (Exception e) {
                Log.e(LOG_TAG, "Exception while talking to tag", e);
                this.publishProgress(e.getMessage());
            }

            return null;
        }

        private byte[] sendRequest(byte command, byte[] parameters) throws Exception {
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            byte[] recvBuffer = mTagTech.transceive(wrapMessage(command, parameters));

            while (true) {
                Log.i(LOG_TAG, "response length " + recvBuffer.length);

                if (recvBuffer[recvBuffer.length - 2] != (byte) 0x91) {
                    throw new Exception("Invalid response");
                }

                output.write(recvBuffer, 0, recvBuffer.length - 2);

                byte status = recvBuffer[recvBuffer.length - 1];
                if (status == OPERATION_OK) {
                    break;
                } else if (status == ADDITIONAL_FRAME) {
                    recvBuffer = mTagTech.transceive(wrapMessage(CardJob.GET_ADDITIONAL_FRAME, null));
                } else if (status == PERMISSION_DENIED) {
                    throw new IllegalArgumentException("Permission denied");
                } else if (status == AUTHENTICATION_ERROR) {
                    throw new IllegalArgumentException("Authentication error");
                } else {
                    throw new Exception("Unknown status code: " + Integer.toHexString(status & 0xFF));
                }
            }

            return output.toByteArray();
        }

        private byte[] wrapMessage(byte command, byte[] parameters) throws Exception {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            stream.write((byte) 0x90);
            stream.write(command);
            stream.write((byte) 0x00);
            stream.write((byte) 0x00);
            if (parameters != null) {
                stream.write((byte) parameters.length);
                stream.write(parameters);
            }
            stream.write((byte) 0x00);

            return stream.toByteArray();
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
            Message.obtain(this.destHandler, MSG_ID_NFC_STATUS, R.string.nfc_done).sendToTarget();
        }
    }


    // Status codes (Section 3.4)
    private static final byte OPERATION_OK = (byte) 0x00;
    private static final byte PERMISSION_DENIED = (byte) 0x9D;
    private static final byte AUTHENTICATION_ERROR = (byte) 0xAE;
    private static final byte ADDITIONAL_FRAME = (byte) 0xAF;


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
