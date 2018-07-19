package org.us.x42.kyork.idcard.desfire;

/**
 * Contains protocol constants.
 */
public final class DESFireProtocol {
    // Command IDs.
    public static final byte AUTHENTICATE = (byte) 0x0A;
    public static final byte GET_MANUFACTURING_DATA = (byte) 0x60;
    public static final byte GET_APPLICATION_DIRECTORY = (byte) 0x6A;
    public static final byte GET_ADDITIONAL_FRAME = (byte) 0xAF;
    public static final byte SELECT_APPLICATION = (byte) 0x5A;
    public static final byte READ_DATA = (byte) 0xBD;
    public static final byte READ_RECORD = (byte) 0xBB;
    public static final byte GET_VALUE = (byte) 0x6C;
    public static final byte GET_FILES = (byte) 0x6F;
    public static final byte GET_FILE_SETTINGS = (byte) 0xF5;
    public static final byte CHANGE_FILE_SETTINGS = (byte) 0x5F;
    public static final byte CREATE_BACKUP_FILE = (byte) 0xCB;
    public static final byte CREATE_VALUE_FILE = (byte) 0xCC;
    public static final byte CREATE_STDDATA_FILE = (byte) 0xCD;
    public static final byte CREATE_LINEARRECORD_FILE = (byte) 0xC1;
    public static final byte CREATE_CYCLICRECORD_FILE = (byte) 0xC0;
    public static final byte CHANGE_KEY_SETTINGS = (byte) 0x54;
    public static final byte GET_KEY_SETTINGS = (byte) 0x45;

    public static final byte FILETYPE_STANDARD = 0;
    public static final byte FILETYPE_BACKUP = 1;
    public static final byte FILETYPE_VALUE = 2;
    public static final byte FILETYPE_LINEARRECORD = 3;
    public static final byte FILETYPE_CYCLICRECORD = 4;
}
