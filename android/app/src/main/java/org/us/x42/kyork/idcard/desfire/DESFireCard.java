package org.us.x42.kyork.idcard.desfire;

import android.annotation.SuppressLint;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.support.annotation.NonNull;
import android.util.Log;

import org.us.x42.kyork.idcard.PackUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ProtocolException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

//import com.nxp.nfclib.desfire.DESFireEV1;

public class DESFireCard {
    private static final String LOG_TAG = DESFireCard.class.getSimpleName();

    private Tag mTag;
    private IsoDep mTagTech;
    private Cipher mSessionCipher;
    private byte[] sessionKey;

    public static class CardException extends IOException {
        private int errorCode;

        public CardException(int errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public CardException(DESFireProtocol.StatusCode code, String message) {
            super(code.toString() + ": " + message);
            this.errorCode = code.getValue();
        }

        public int getErrorCode() {
            return errorCode;
        }
        public DESFireProtocol.StatusCode getStatusCode() { return DESFireProtocol.StatusCode.byId((byte)errorCode); }
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
        PackUtil.writeBE24(args, 0, appID);
        sendRequest(DESFireProtocol.SELECT_APPLICATION, args);
    }

    public byte[] readFullFile(byte fileID, int expectedLength) throws IOException {
        byte[] settingsReply = this.sendRequest(DESFireProtocol.GET_FILE_SETTINGS, new byte[] { fileID });
        byte fileType = settingsReply[0];
        byte commSettings = settingsReply[1];
        short accessRights = (short)((short)settingsReply[2] | ((short)settingsReply[3] << 8));

        if (commSettings != DESFireProtocol.FileEncryptionMode.PLAIN.getValue()) {
            throw new IllegalArgumentException("NotImplemented");
        }
        if (fileType != DESFireProtocol.FILETYPE_STANDARD && fileType != DESFireProtocol.FILETYPE_BACKUP) {
            throw new IllegalArgumentException("Wrong file type");
        }

        byte[] fileRequest = new byte[7];
        fileRequest[0] = fileID;
        PackUtil.writeLE24(fileRequest, 1, 0); // TODO(kyork): check LE/BE
        PackUtil.writeLE24(fileRequest, 4, expectedLength);

        return this.sendRequest(DESFireProtocol.READ_DATA, fileRequest);
    }

    public void writeToFile(DESFireProtocol.FileEncryptionMode mode, byte fileID, byte[] content, int offset) throws IOException {
        if (mode != DESFireProtocol.FileEncryptionMode.PLAIN) {
            throw new IllegalArgumentException("NotImplemented");
        }

        // TODO encrypt content if necessary
        byte[] commandBytes = new byte[content.length + 7];
        commandBytes[0] = fileID;
        PackUtil.writeLE24(commandBytes, 1, offset);
        PackUtil.writeLE24(commandBytes, 4, content.length);
        System.arraycopy(content, 0, commandBytes, 7, content.length);
        int bytesWritten = 0;

        while (bytesWritten < commandBytes.length) {
            byte[] subCmdBytes;
            if (commandBytes.length - bytesWritten > 59) {
                subCmdBytes = new byte[59];
                System.arraycopy(commandBytes, bytesWritten, subCmdBytes, 0, 59);
            } else if (commandBytes.length < 59) {
                subCmdBytes = commandBytes;
            } else {
                subCmdBytes = new byte[commandBytes.length - bytesWritten];
                System.arraycopy(commandBytes, bytesWritten, subCmdBytes, 0, subCmdBytes.length);
            }

            if (bytesWritten == 0) {
                this.sendPartialRequest(DESFireProtocol.WRITE_DATA, subCmdBytes);
            } else {
                this.sendPartialRequest(DESFireProtocol.GET_ADDITIONAL_FRAME, subCmdBytes);
            }
            bytesWritten += subCmdBytes.length;
        }
    }

    /**
     * Reset the encryption/authentication with the card. Must be called after calling
     * any command that invalidates authentication.
     */
    public void resetEncryption() {
        mSessionCipher = null;
    }

    private byte[] perform3desSingleBlock(byte[] longKey, byte[] iv, byte[] data) throws GeneralSecurityException {
        @SuppressLint("GetInstance") final Cipher desCipher = Cipher.getInstance("DESede/ECB/NoPadding");

        if (iv != null) {
            desCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(longKey, "DESede"), new IvParameterSpec(iv));
        } else {
            desCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(longKey, "DESede"));
        }

        return desCipher.doFinal(data);
    }

    private byte[] doCipherDES(byte[] longKey, byte[] input, int xorMode) throws Exception {
        if (input.length % 8 != 0) throw new IllegalArgumentException("cipherDesFullBlocks: input not multiple of 8 bytes");

        byte[] result = new byte[input.length];
        byte[] cbcBuffer = new byte[8];
        byte[] xorBuffer = new byte[8];
        byte[] inpBuffer = new byte[8];

        if (xorMode == 1) {
            for (int offset = 0; offset < input.length; offset += 8) {
                System.arraycopy(input, offset, inpBuffer, 0, 8);
                for (int i = 0; i < 8; i++) {
                    xorBuffer[i] = (byte)(inpBuffer[i] ^ cbcBuffer[i]);
                }
                byte[] ciphered = perform3desSingleBlock(longKey, null, xorBuffer);
                System.arraycopy(ciphered, 0, result, offset, 8);
                System.arraycopy(ciphered, 0, cbcBuffer, 0, 8);
            }
            return result;
        } else {
            for (int offset = 0; offset < input.length; offset += 8) {
                System.arraycopy(input, offset, inpBuffer, 0, 8);
                byte[] ciphered = perform3desSingleBlock(longKey, null, inpBuffer);
                for (int i = 0; i < 8; i++) {
                    xorBuffer[i] = (byte)(ciphered[i] ^ cbcBuffer[i]);
                }
                System.arraycopy(xorBuffer, 0, result, offset, 8);
                System.arraycopy(input, offset, cbcBuffer, 0, 8);
            }
            return result;
        }
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
        byte[] longKey = new byte[24];
        System.arraycopy(key, 0, longKey, 0, 16);
        System.arraycopy(key, 0, longKey, 16, 8);

        byte[] rndA = new byte[8];
        SecureRandom rnd = new SecureRandom();
        rnd.nextBytes(rndA);

        byte[] rndBReply = sendPartialRequest((byte) 0x0A, new byte[]{keyId});
        Log.i(LOG_TAG, "Challenge B from card: " + stringifyByteArray(rndBReply));
        byte[] rndBActual = doCipherDES(longKey, rndBReply, 1);
        Log.i(LOG_TAG, "Decrypted RndB: " + stringifyByteArray(rndBActual));

        ByteArrayOutputStream midData = new ByteArrayOutputStream();
        midData.write(rndA, 0, 8);
        midData.write(rndBActual, 1, 7);
        midData.write(rndBActual, 0, 1);
        Log.i(LOG_TAG, "A+B' challenge to card: " + stringifyByteArray(midData.toByteArray()));
        byte[] midReply = doCipherDES(longKey, midData.toByteArray(), 1);
        Log.i(LOG_TAG, "A+B' encrypted: " + stringifyByteArray(midReply));

        byte[] finalReply = sendRequest(DESFireProtocol.ADDITIONAL_FRAME, midReply);
        Log.i(LOG_TAG, "Challenge A' from card: " + stringifyByteArray(finalReply));
        byte[] rndAPrime = doCipherDES(longKey, finalReply, 1);
        Log.i(LOG_TAG, "Decrypted A' from card: " + stringifyByteArray(rndAPrime));
        byte temp = rndA[0];
        System.arraycopy(rndA, 1, rndA, 0, 7);
        rndA[7] = temp;

        if (!MessageDigest.isEqual(rndA, rndAPrime)) {
            throw new CardException(DESFireProtocol.StatusCode.AUTHENTICATION_ERROR, "host: Card failed authentication");
        }

        byte[] sessionKey = new byte[16];
        System.arraycopy(rndA, 0, sessionKey, 0, 4);
        System.arraycopy(rndBActual, 0, sessionKey, 4, 4);
        System.arraycopy(rndA, 4, sessionKey, 8, 4);
        System.arraycopy(rndBActual, 4, sessionKey, 12, 4);
        this.sessionKey = sessionKey;
        Log.i(LOG_TAG, "established session key");
    }

    public byte[] sendRequest(byte command, byte[] parameters) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Log.i(LOG_TAG, "Sending command " + String.format("%02X", command) + ": " + stringifyByteArray(parameters));
        byte[] recvBuffer = mTagTech.transceive(wrapMessage(command, parameters));

        while (true) {
            Log.i(LOG_TAG, "response length " + recvBuffer.length);

            if (recvBuffer[recvBuffer.length - 2] != (byte) 0x91) {
                throw new CardException(-1, "Invalid framing response");
            }

            output.write(recvBuffer, 0, recvBuffer.length - 2);

            byte status = recvBuffer[recvBuffer.length - 1];
            if (status == 0) {
                break;
            } else if (status == DESFireProtocol.ADDITIONAL_FRAME) {
                recvBuffer = mTagTech.transceive(wrapMessage(DESFireProtocol.GET_ADDITIONAL_FRAME, null));
            } else {
                DESFireProtocol.StatusCode st = DESFireProtocol.StatusCode.byId(status);
                Log.i(LOG_TAG, "Card returned error " + String.format("%02X", command) + ": " + st.toString());
                throw new CardException(st, "command ID " + String.format("%02X", command));
            }
        }

        byte[] result = output.toByteArray();
        Log.i(LOG_TAG, "Response to command " + command + ": " + stringifyByteArray(result));
        return result;
    }

    private byte[] sendPartialRequest(byte command, byte[] parameters) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Log.i(LOG_TAG, "Sending command " + String.format("%02X", command) + ": " + stringifyByteArray(parameters));
        byte[] recvBuffer = mTagTech.transceive(wrapMessage(command, parameters));

        if (recvBuffer[recvBuffer.length - 2] != (byte) 0x91) {
            throw new CardException(-1, "Invalid framing response");
        }

        output.write(recvBuffer, 0, recvBuffer.length - 2);

        byte status = recvBuffer[recvBuffer.length - 1];
        if (status == 0) {
            return output.toByteArray();
        } else if (status == DESFireProtocol.ADDITIONAL_FRAME) {
            return output.toByteArray();
        } else {
            DESFireProtocol.StatusCode st = DESFireProtocol.StatusCode.byId(status);
            throw new CardException(st, "command ID " + String.format("%02X", command));
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
