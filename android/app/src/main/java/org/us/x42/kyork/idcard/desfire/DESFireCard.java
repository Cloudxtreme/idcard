package org.us.x42.kyork.idcard.desfire;

import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

//import com.nxp.nfclib.desfire.DESFireEV1;

public class DESFireCard {
    private static final String LOG_TAG = DESFireCard.class.getSimpleName();

    private Tag mTag;
    private IsoDep mTagTech;
    private Cipher mSessionCipher;

    // Status codes (Section 3.4)
    public static final byte OPERATION_OK = (byte) 0x00;
    public static final byte PERMISSION_DENIED = (byte) 0x9D;
    public static final byte AUTHENTICATION_ERROR = (byte) 0xAE;
    public static final byte ADDITIONAL_FRAME = (byte) 0xAF;

    public static class CardException extends IOException {
        private int errorCode;

        public CardException(int errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public int getErrorCode() {
            return errorCode;
        }
    }

    public DESFireCard(Tag tag) {
        mTag = tag;
    }

    /**
     * Establish the NFC Technology instance and call connect().
     * @throws IOException if an I/O error occurs
     */
    public void connect() throws IOException {
        mTagTech = IsoDep.get(mTag);
        mTagTech.connect();
    }

    public void selectApplication(int appID) throws IOException {
        byte[] args = new byte[3];
        args[0] = (byte) ((appID & 0xFF0000) >> 16);
        args[1] = (byte) ((appID & 0xFF00) >> 8);
        args[2] = (byte) (appID & 0xFF);
        sendRequest(DESFireProtocol.SELECT_APPLICATION, args);
    }

    public DESFirePlainFile readPlainFile(byte fileID) throws Exception {
        byte[] settingsReply = this.sendRequest(DESFireProtocol.GET_FILE_SETTINGS, new byte[] { fileID });
        byte fileType = settingsReply[0];
        byte commSettings = settingsReply[1];
        short accessRights = (short)((short)settingsReply[2] | ((short)settingsReply[3] << 8));
        if (settingsReply[0] == DESFireProtocol.FILETYPE_STANDARD || settingsReply[0] == DESFireProtocol.FILETYPE_BACKUP) {

        }
        return null;
    }

    /**
     * Reset the encryption/authentication with the card. Must be called after calling
     * any command that invalidates authentication.
     */
    public void resetEncryption() {
        mSessionCipher = null;
    }

    private byte[] cipherDesFullBlocks(Cipher cipher, byte[] input) throws Exception {
        if (input.length % 8 != 0) throw new IllegalArgumentException("cipherDesFullBlocks: input not multiple of 8 bytes");

            return cipher.doFinal(input);
            /*
            byte[] output = new byte[input.length];
            int outputUsedBytes = cipher.update(input, 0, input.length, output, 0);
            outputUsedBytes += cipher.doFinal(input, input.length, 0, output, outputUsedBytes);
            if (outputUsedBytes != output.length) {
                throw new ShortBufferException("doFinal() failed to fill output buffer");
            }
            return output;
        } catch (ShortBufferException e) {
            throw new RuntimeException("Unexpected ShortBufferException (this is a code bug)", e);
        }
        */
    }

    /**
     * Establish DESFire authentication with the card. Should call selectApplication() first.
     *
     * @param keyId key ID in the selected application
     * @param key 16-byte 3DES key
     * @throws IOException for communication errors
     * @throws CardException if authentication fails
     * @throws Exception in other error cases
     */
    public void establishAuthentication(byte keyId, byte key[]) throws Exception {
        final SecretKey initialKey = new SecretKeySpec(key, "DESede");
        final IvParameterSpec blankIV = new IvParameterSpec(new byte[8]);
        final Cipher setupCipher = Cipher.getInstance("DESede/CBC/NoPadding");
        setupCipher.init(Cipher.DECRYPT_MODE, initialKey, blankIV);

        byte[] rndBReply = sendPartialRequest((byte) 0x0A, new byte[]{keyId});
        Log.i(LOG_TAG, "Challenge B from card: " + stringifyByteArray(rndBReply));
        byte[] rndBActual = cipherDesFullBlocks(setupCipher, rndBReply);
        Log.i(LOG_TAG, "Decrypted RndB: " + stringifyByteArray(rndBActual));

//        Class.forName("com.nxp.nfclib.desfire.DESFireEV1");
//        new DESFireEV1(null);

        byte[] rndA = new byte[8];
        SecureRandom rnd = new SecureRandom();
        rnd.nextBytes(rndA);

        ByteArrayOutputStream midData = new ByteArrayOutputStream();
        midData.write(rndA, 0, 8);
        midData.write(rndBActual, 1, 7);
        midData.write(rndBActual, 0, 1);
        Log.i(LOG_TAG, "A+B' challenge to card: " + stringifyByteArray(midData.toByteArray()));

        IvParameterSpec midIV = new IvParameterSpec(rndBReply);
        setupCipher.init(Cipher.DECRYPT_MODE, initialKey, midIV);
        byte[] midReply = cipherDesFullBlocks(setupCipher, midData.toByteArray());
        Log.i(LOG_TAG, "A+B' encrypted: " + stringifyByteArray(midReply));

        byte[] finalReply = sendRequest(ADDITIONAL_FRAME, midReply);
        // ----- this is where it's crashing
        Log.i(LOG_TAG, "Challenge A' from card: " + stringifyByteArray(finalReply));
        setupCipher.init(Cipher.DECRYPT_MODE, initialKey, blankIV);
        byte[] rotatedA = cipherDesFullBlocks(setupCipher, finalReply);
        Log.i(LOG_TAG, "Decrypted A' from card: " + stringifyByteArray(rotatedA));
        byte temp = rotatedA[0];
        System.arraycopy(rotatedA, 1, rotatedA, 0, 7);
        rotatedA[7] = temp;

        if (!MessageDigest.isEqual(rndA, rotatedA)) {
            throw new CardException(AUTHENTICATION_ERROR, "host: Card failed authentication");
        }

        byte[] sessionKey = new byte[16];
        System.arraycopy(rndA, 0, sessionKey, 0, 4);
        System.arraycopy(rndBActual, 0, sessionKey, 4, 4);
        System.arraycopy(rndA, 4, sessionKey, 8, 4);
        System.arraycopy(rndBActual, 4, sessionKey, 12, 4);
        this.mSessionCipher = Cipher.getInstance("DESede/CBC/ZeroBytePadding");
        final SecretKey sessionKeySpec = new SecretKeySpec(sessionKey, "DESede");
        mSessionCipher.init(Cipher.DECRYPT_MODE, sessionKeySpec, blankIV);
        Log.i(LOG_TAG, "established session key");
    }

    public byte[] sendRequest(byte command, byte[] parameters) throws IOException, CardException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        byte[] recvBuffer = mTagTech.transceive(wrapMessage(command, parameters));

        while (true) {
            Log.i(LOG_TAG, "response length " + recvBuffer.length);

            if (recvBuffer[recvBuffer.length - 2] != (byte) 0x91) {
                throw new CardException(-1, "Invalid framing response");
            }

            output.write(recvBuffer, 0, recvBuffer.length - 2);

            byte status = recvBuffer[recvBuffer.length - 1];
            if (status == OPERATION_OK) {
                break;
            } else if (status == ADDITIONAL_FRAME) {
                recvBuffer = mTagTech.transceive(wrapMessage(DESFireProtocol.GET_ADDITIONAL_FRAME, null));
            } else if (status == PERMISSION_DENIED) {
                throw new CardException(status, "Permission denied");
            } else if (status == AUTHENTICATION_ERROR) {
                throw new CardException(status, "Authentication error");
            } else {
                throw new CardException(status, "Unknown status code: " + Integer.toHexString(status & 0xFF));
            }
        }

        return output.toByteArray();
    }

    private byte[] sendPartialRequest(byte command, byte[] parameters) throws Exception {
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
            return output.toByteArray();
        } else if (status == PERMISSION_DENIED) {
            throw new CardException(status, "Permission denied");
        } else if (status == AUTHENTICATION_ERROR) {
            throw new CardException(status, "Authentication error");
        } else {
            throw new CardException(status, "Unknown status code: " + Integer.toHexString(status & 0xFF));
        }
    }

    private byte[] wrapMessage(byte command, byte[] parameters) throws IOException {
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

    @NonNull
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
}
