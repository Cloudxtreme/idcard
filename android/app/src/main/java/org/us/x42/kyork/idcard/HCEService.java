package org.us.x42.kyork.idcard;

import android.content.Context;
import android.content.SharedPreferences;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

import org.us.x42.kyork.idcard.data.AbstractCardFile;
import org.us.x42.kyork.idcard.data.CardDataFormat;
import org.us.x42.kyork.idcard.data.FileDoorPermissions;
import org.us.x42.kyork.idcard.data.FileMetadata;
import org.us.x42.kyork.idcard.data.FileSignatures;
import org.us.x42.kyork.idcard.data.FileUserInfo;
import org.us.x42.kyork.idcard.data.IDCard;
import org.us.x42.kyork.idcard.desfire.DESFireCard;
import org.us.x42.kyork.idcard.desfire.DESFireProtocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

// interesting problem: need to pass data to this thing
// https://stackoverflow.com/questions/25018121/passing-information-to-a-hostapduservice-from-another-context

public class HCEService extends HostApduService {
    private static final String LOG_TAG = "HCEService";
    private static final byte[] SELECT_APDU = new byte[] {(byte)0x00, (byte)0xA4, (byte)0x04, (byte)0x00, (byte)0x07, (byte)0xFB, (byte)0x43, (byte)0x61, (byte)0x72, (byte)0x64, (byte)0x34, (byte)0x32, (byte)0x00};
    private static final byte[] FRAMING_ERROR = new byte[] {(byte)0x6A, (byte)0xA0};
    private static final byte[] APP_ID_CARD42 = new byte[] {(byte)0xFB, (byte)0x98, (byte)0x52};

    private static final byte[] TK_SERIAL_DEV = new byte[] {(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF};  //TEMPORARY, not sure about how serials work with tickets -apuel

    private IDCard ourCard;
    private byte[] authRndA;
    private byte[] authRndB;

    public HCEService() {
        ourCard = new IDCard();

        ourCard.fileMetadata = new FileMetadata(new byte[CardDataFormat.FORMAT_METADATA.expectedSize]);
        ourCard.fileMetadata.setProvisioningDate(new Date());
        ourCard.fileMetadata.setSchemaVersion((short)1); //(?)
        ourCard.fileMetadata.setDeviceType(FileMetadata.DEVICE_TYPE_TICKET); //BE 'TK'

        ourCard.fileUserInfo = new FileUserInfo(new byte[CardDataFormat.FORMAT_USERINFO.expectedSize]);

        ourCard.fileDoorPermissions = new FileDoorPermissions(new byte[CardDataFormat.FORMAT_DOORPERMS.expectedSize]);
        try {
            ourCard.fileDoorPermissions.signMAC(TK_SERIAL_DEV, CardJob.TK_MAC_KEY_DEV, ourCard.fileMetadata, ourCard.fileUserInfo);
        }
        catch (IOException e) { }

        ourCard.fileSignatures = new FileSignatures(new byte[CardDataFormat.FORMAT_SIGNATURES.expectedSize]);
    }

    public static final class HCEServiceUtils {
        public static final String SHAREDPREFS_HCE = "org.us.x42.kyork.idcard.ticket";
        public static final String READY_FOR_UPDATES = "update_mode";
        public static final String TICKET_STORAGE = "ticket";
        public static final String UPDATE_BY_DOOR_PREFIX = "update_d[";

        public static SharedPreferences getStorage(Context context) {
            return context.getSharedPreferences(SHAREDPREFS_HCE, 0);
        }
    }

    // if we cannot return immediately, return null for a time extension and use sendCommandApdu() later
    public byte[] processCommandApdu(byte[] apdu, Bundle extra) {
        Log.i(LOG_TAG, DESFireCard.stringifyByteArray(apdu));
        if ((apdu[0] == 0x00) && (apdu[1] == (byte)0xA4)) {
            // 7816 SELECT
            if (Arrays.equals(apdu, SELECT_APDU)) {
                return new byte[] {(byte)0x91, 0x00};
            }
            else {
                notifyUnhandled();
                return null;
            }
        }
        else if (apdu[0] == (byte)0x90) {
            if (apdu.length < 5) {
                return FRAMING_ERROR;
            }

            byte desfire_cmd = apdu[1];
            byte len = apdu[4];
            if (apdu.length != 6 + len) {
                return FRAMING_ERROR;
            }

            byte[] desfire_cmddata = Arrays.copyOfRange(apdu, 5, 5 + len);
            switch (desfire_cmd) {
                case DESFireProtocol.SELECT_APPLICATION:
                    if (!Arrays.equals(desfire_cmddata, APP_ID_CARD42)) {
                        notifyUnhandled();
                        return new byte[] {(byte)0x91, DESFireProtocol.StatusCode.APPLICATION_NOT_FOUND.getValue()};
                    }
                    return new byte[] {(byte)0x91, 0x00};

                case DESFireProtocol.AUTHENTICATE:


                case DESFireProtocol.READ_DATA:
                    byte fileno = desfire_cmddata[0];
                    int offset = PackUtil.readLE24(desfire_cmddata, 1);
                    byte length = desfire_cmddata[4];

                    AbstractCardFile file = ourCard.getFileByID(fileno);
                    if (file == null) {
                        return new byte[] {(byte)0x91, DESFireProtocol.StatusCode.FILE_NOT_FOUND.getValue()};
                    }

                    byte[] data = file.getRawContent();
                    if (offset >= data.length || (offset + length) > data.length) {
                        return new byte[] {(byte)0x91, DESFireProtocol.StatusCode.BOUNDARY_ERROR.getValue()};
                    }

                    byte[] response = new byte[length + 2];
                    System.arraycopy(data, offset, response, 0, length);
                    response[response.length - 2] = (byte)0x91;
                    response[response.length - 1] = (byte)0x00;
                    return response;
                // ...
            }
        }

        return new byte[] {(byte)0x91, DESFireProtocol.StatusCode.COMMAND_ABORTED.getValue()};
    }

    public void onDeactivated(int deactivationMode) {

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
}
