package org.us.x42.kyork.idcard.desfire;

import android.util.Log;

import org.us.x42.kyork.idcard.PackUtil;

/**
 * Contains protocol constants.
 */
public final class DESFireProtocol {
    // Command IDs.
    public static final byte AUTHENTICATE = (byte) 0x0A;
    public static final byte GET_MANUFACTURING_DATA = (byte) 0x60;
    public static final byte GET_APPLICATION_DIRECTORY = (byte) 0x6A;
    public static final byte GET_FREE_SPACE = (byte) 0x6E;
    public static final byte GET_ADDITIONAL_FRAME = (byte) 0xAF;
    public static final byte SELECT_APPLICATION = (byte) 0x5A;
    public static final byte WRITE_DATA = (byte) 0x3D;
    public static final byte READ_DATA = (byte) 0xBD;
    public static final byte READ_RECORD = (byte) 0xBB;
    public static final byte GET_VALUE = (byte) 0x6C;
    public static final byte GET_FILES = (byte) 0x6F;
    public static final byte GET_FILE_SETTINGS = (byte) 0xF5;
    public static final byte COMMIT_TRANSACTION = (byte) 0xC7;
    public static final byte ABORT_TRANSACTION = (byte) 0xA7;
    public static final byte CREATE_APPLICATION = (byte) 0xCA;
    public static final byte CHANGE_FILE_SETTINGS = (byte) 0x5F;
    public static final byte CREATE_BACKUP_FILE = (byte) 0xCB;
    public static final byte CREATE_VALUE_FILE = (byte) 0xCC;
    public static final byte CREATE_STDDATA_FILE = (byte) 0xCD;
    public static final byte CREATE_LINEARRECORD_FILE = (byte) 0xC1;
    public static final byte CREATE_CYCLICRECORD_FILE = (byte) 0xC0;
    public static final byte CHANGE_KEY_SETTINGS = (byte) 0x54;
    public static final byte GET_KEY_SETTINGS = (byte) 0x45;
    public static final byte ADDITIONAL_FRAME = (byte) 0xAF;
    public static final byte FORMAT_PICC = (byte) 0xFC;
    public static final byte CUSTOM_IS_READY = (byte) 0x2D; // Card42 Exclusive: is the card ready to present its files?


    public static final byte FILETYPE_STANDARD = 0;
    public static final byte FILETYPE_BACKUP = 1;
    public static final byte FILETYPE_VALUE = 2;
    public static final byte FILETYPE_LINEARRECORD = 3;
    public static final byte FILETYPE_CYCLICRECORD = 4;

    // Status codes (Section 3.4)
    public enum StatusCode {
        OPERATION_OK(0),
        NO_CHANGES(0xC),
        OUT_OF_MEMORY(0xE),
        ILLEGAL_COMMAND(0x1C),
        INTEGRITY_ERROR(0x1E),
        NO_SUCH_KEY(0x40),
        LENGTH_ERROR(0x7E),
        PERMISSION_DENIED(0x9D),
        PARAMETER_ERROR(0x9E),
        APPLICATION_NOT_FOUND(0xA0),
        APPL_INTEGRITY_ERROR(0xA1),
        AUTHENTICATION_ERROR(0xAE),
        ADDITIONAL_FRAME(0xAF),
        BOUNDARY_ERROR(0xBE),
        PICC_INTEGRITY_ERROR(0xC1),
        COMMAND_ABORTED(0xCA),
        PICC_DISABLED_ERROR(0xCD),
        COUNT_ERROR(0xCE),
        DUPLICATE_ERROR(0xDE),
        EEPROM_ERROR(0xEE),
        FILE_NOT_FOUND(0xF0),
        FILE_INTEGRITY_ERROR(0xF1),

        UNKNOWN_ERROR_CODE(0xFF);

        private byte value;

        StatusCode(int value) {
            this.value = (byte) value;
        }

        public byte getValue() {
            return value;
        }

        public static StatusCode byId(byte code) {
            switch (code) {
                case (byte) 0:
                    return OPERATION_OK;
                case (byte) 0xC:
                    return NO_CHANGES;
                case (byte) 0xE:
                    return OUT_OF_MEMORY;
                case (byte) 0x1C:
                    return ILLEGAL_COMMAND;
                case (byte) 0x1E:
                    return INTEGRITY_ERROR;
                case (byte) 0x7E:
                    return LENGTH_ERROR;
                case (byte) 0x9D:
                    return PERMISSION_DENIED;
                case (byte) 0x9E:
                    return PARAMETER_ERROR;
                case (byte) 0xA0:
                    return APPLICATION_NOT_FOUND;
                case (byte) 0xA1:
                    return APPL_INTEGRITY_ERROR;
                case (byte) 0xAE:
                    return AUTHENTICATION_ERROR;
                case (byte) 0xAF:
                    return ADDITIONAL_FRAME;
                case (byte) 0xBE:
                    return BOUNDARY_ERROR;
                case (byte) 0xC1:
                    return PICC_INTEGRITY_ERROR;
                case (byte) 0xCA:
                    return COMMAND_ABORTED;
                case (byte) 0xCD:
                    return PICC_DISABLED_ERROR;
                case (byte) 0xCE:
                    return COUNT_ERROR;
                case (byte) 0xDE:
                    return DUPLICATE_ERROR;
                case (byte) 0xEE:
                    return EEPROM_ERROR;
                case (byte) 0xF0:
                    return FILE_NOT_FOUND;
                case (byte) 0xF1:
                    return FILE_INTEGRITY_ERROR;
            }
            Log.e("DESFireProtocol", "unknown error code " + code);
            return UNKNOWN_ERROR_CODE;
        }
    }

    public enum FileEncryptionMode {
        PLAIN(0),
        MACED(1),
        INVALID(2),
        ENCRYPTED(3);

        private byte value;
        FileEncryptionMode(int value) { this.value = (byte)value; }
        public byte getValue() { return value; }
    }

    public static long getSerial(byte[] manufacturingData) {
        return PackUtil.readLE56(manufacturingData, 14);
    }
}
