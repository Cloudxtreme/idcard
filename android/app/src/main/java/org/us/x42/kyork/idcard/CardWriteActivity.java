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
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

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
                } else if (msg.what == MSG_ID_DONE) {
                    Intent returnData = new Intent(Intent.ACTION_VIEW);
                    returnData.putExtra(CARD_RESULT, (Parcelable)msg.obj);
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
        IsoDep mTagTech;
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
                byte[] response = this.sendRequest(cmdId, data);
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
                    if (!startEncryption(this.mJob.keyId, this.mJob.encKey)) {
                        throw new SecurityException("NFC - Failed to authenticate");
                    }
                }
                // Perform each thing

                for (CardJob.CardOp op : mJob.commands) {
                    sendAndLog(op.getCommandId(), op.encode());
                }

                Log.i(LOG_TAG, "sleeping");
                Message.obtain(this.destHandler, MSG_ID_NFC_STATUS, R.string.nfc_done).sendToTarget();
                Thread.sleep(2*1000);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Exception while talking to tag", e);
                this.publishProgress(e.getMessage());
            }

            return null;
        }

        private boolean startEncryption(byte keyId, byte[] key) {
            final SecretKey initialKey = new SecretKeySpec(key, "DESede");
            final IvParameterSpec iv = new IvParameterSpec(new byte[8]);
            try {
                final Cipher setupCipherDecrypt = Cipher.getInstance("DESede/CBC/ZeroBytePadding");
                setupCipherDecrypt.init(Cipher.DECRYPT_MODE, initialKey, iv);

                byte[] rndBReply = sendPartialRequest((byte)0x0A, new byte[] { keyId });
                byte[] rndBActual = setupCipherDecrypt.doFinal(rndBReply);
                Log.i(LOG_TAG, "Challenge B from card: " + stringifyByteArray(rndBReply));
                Log.i(LOG_TAG, "Decrypted RndB: " + stringifyByteArray(rndBActual));

                byte[] rndA = new byte[8];
                SecureRandom rnd = new SecureRandom();
                rnd.nextBytes(rndA);

                ByteArrayOutputStream midData = new ByteArrayOutputStream();
                midData.write(rndA, 0, 8);
                midData.write(rndBActual, 1, 7);
                midData.write(rndBActual, 0, 1);
                Log.i(LOG_TAG, "A+B' challenge to card: " + stringifyByteArray(midData.toByteArray()));
                byte[] midReply = setupCipherDecrypt.doFinal(midData.toByteArray());
                Log.i(LOG_TAG, "A+B' encrypted: " + stringifyByteArray(midReply));

                byte[] finalReply = sendRequest(ADDITIONAL_FRAME, midReply);
                Log.i(LOG_TAG, "Challenge A' from card: " + stringifyByteArray(finalReply));
                byte[] rotatedA = setupCipherDecrypt.doFinal(finalReply);
                Log.i(LOG_TAG, "Decrypted A' from card: " + stringifyByteArray(rotatedA));
                byte temp = rotatedA[0];
                System.arraycopy(rotatedA, 1, rotatedA, 0, 7);
                rotatedA[7] = temp;

                if (!MessageDigest.isEqual(rndA, rotatedA)) {
                    return false;
                }

                byte[] sessionKey = new byte[16];
                System.arraycopy(rndA, 0, sessionKey, 0, 4);
                System.arraycopy(rndBActual, 0, sessionKey, 4, 4);
                System.arraycopy(rndA, 4, sessionKey, 8, 4);
                System.arraycopy(rndBActual, 4, sessionKey, 12, 4);
                this.mSessionCipher = Cipher.getInstance("DESede/CBC/ZeroBytePadding");
                final SecretKey sessionKeySpec = new SecretKeySpec(sessionKey, "DESede");
                mSessionCipher.init(Cipher.DECRYPT_MODE, sessionKeySpec, iv);
                Log.i(LOG_TAG, "established session key");
                return true;
            } catch (Exception e) {
                Log.e(LOG_TAG, "Exception performing crypto authentication", e);
                e.printStackTrace();
                return false;
            }
        }

        private byte[] sendPartialRequest(byte command, byte[] parameters) throws Exception {
            // like sendRequest but without the ADDITIONAL_FRAME loop
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            byte[] recvBuffer = mTagTech.transceive(wrapMessage(command, parameters));

            if (recvBuffer[recvBuffer.length - 2] != (byte) 0x91) {
                throw new Exception("Invalid response");
            }

            output.write(recvBuffer, 0, recvBuffer.length - 2);

            byte status = recvBuffer[recvBuffer.length - 1];
            if (status == ADDITIONAL_FRAME) {
                return output.toByteArray();
            } else if (status == OPERATION_OK) {
                throw new IllegalStateException("Got OK, expected ADDITIONAL_FRAME");
            } else if (status == PERMISSION_DENIED) {
                throw new IllegalArgumentException("Permission denied");
            } else if (status == AUTHENTICATION_ERROR) {
                throw new IllegalArgumentException("Authentication error");
            } else {
                throw new Exception("Unknown status code: " + Integer.toHexString(status & 0xFF));
            }
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
            // TODO(kyork): return a result?
            Message.obtain(this.destHandler, MSG_ID_DONE, (Parcelable)null).sendToTarget();
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
