package org.us.x42.kyork.idcard;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.nfc.tech.IsoDep;

import android.util.Log;
import android.widget.TextView;

import org.us.x42.kyork.idcard.data.CardDataFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

public class CardCommunicateActivity extends AppCompatActivity {
    private static final String LOG_TAG = CardCommunicateActivity.class.getSimpleName();
    private NfcAdapter mAdapter;

    private PendingIntent scanIntent;
    private IntentFilter[] scanFilter;
    private String[][] scanTechs;

    private TextView mStatusText;
    private Handler mHandler;

    private static final int MSG_ID_NFC_STATUS = 23;

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

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MSG_ID_NFC_STATUS) {
                    if (msg.arg1 != 0) {
                        mStatusText.setText(msg.arg1);
                    } else {
                        if (msg.obj instanceof String) {
                            mStatusText.setText((String) msg.obj);
                        } else {
                            Log.i(LOG_TAG, "wtf did i just get in that Message: " + msg.obj.toString());
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

            new NfcCommunicateTask(tagFromIntent, this.mHandler).execute(new byte[]{});
        }
    }

    private static class NfcCommunicateTask extends AsyncTask<byte[], String, byte[]> {
        Tag mTag;
        IsoDep mTagTech;
        Handler destHandler;

        NfcCommunicateTask(Tag tag, Handler msgDest) {
            this.mTag = tag;
            this.destHandler = msgDest;
        }

        byte[] sendAndLog(byte cmdId, byte[] data) throws Exception {
            StringBuilder sb = new StringBuilder();
            sb.append("Sending command [");
            sb.append((int)cmdId);
            sb.append("] data [ ");
            for (byte d : data) {
                sb.append((int) d);
                sb.append(' ');
            }
            sb.append("]");

            Log.d(LOG_TAG, sb.toString());

            try {
                byte[] response = this.sendRequest(cmdId, data);
                sb.append("Command response: [ ");;
                for (byte d : response) {
                    sb.append((int) d);
                    sb.append(' ');
                }
                sb.append("]");
                Log.d(LOG_TAG, sb.toString());

                return response;
            } catch (Exception e) {
                Log.d(LOG_TAG,"Got exception", e);
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

                // Authenticate
                byte[] response = sendAndLog((byte)0x0a, new byte[] { 0 });


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
                    recvBuffer = mTagTech.transceive(wrapMessage(GET_ADDITIONAL_FRAME, null));
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

    private static final byte GET_MANUFACTURING_DATA = (byte) 0x60;
    private static final byte GET_APPLICATION_DIRECTORY = (byte) 0x6A;
    private static final byte GET_ADDITIONAL_FRAME = (byte) 0xAF;
    private static final byte SELECT_APPLICATION = (byte) 0x5A;
    private static final byte READ_DATA = (byte) 0xBD;
    private static final byte READ_RECORD = (byte) 0xBB;
    private static final byte GET_VALUE = (byte) 0x6C;
    private static final byte GET_FILES = (byte) 0x6F;
    private static final byte GET_FILE_SETTINGS = (byte) 0xF5;

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
