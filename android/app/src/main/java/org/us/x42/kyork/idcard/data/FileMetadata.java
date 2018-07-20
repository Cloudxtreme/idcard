package org.us.x42.kyork.idcard.data;

import java.util.Date;

/**
 * File 0x1 on the card. Metadata, type of card (e.g. is this actually a card or is it something else?)
 */
public class FileMetadata extends AbstractCardFile {
    public static final byte FILE_ID = (byte)0x01;

    public static final short DEVICE_TYPE_ID = 0x4944;
    public static final short DEVICE_TYPE_TICKET = 0x544b;
    public static final short DEVICE_TYPE_DOOR = 0x444f;
    public static final short DEVICE_TYPE_CANTINA = 0x4341;

    public FileMetadata(byte[] content) {
        super(content);
    }

    @Override
    public int getFileID() {
        return FILE_ID;
    }

    @Override
    public int getExpectedFileSize() {
        return 16;
    }

    public Date getProvisioningDate() {
        long timestamp = readLE64(0x0);
        return new Date(timestamp);
    }

    public short getSchemaVersion() {
        return readLE16(0x8);
    }

    public short getDeviceType() {
        return readBE16(0xa);
    }

    public short getUnused1() {
        return readLE16(0xc);
    }

    public short getUnused2() {
        return readLE16(0xe);
    }
}
