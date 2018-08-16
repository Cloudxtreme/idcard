package org.us.x42.kyork.idcard.data;

import android.os.Parcel;

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

    public FileMetadata(byte[] content) { super(content); }

    protected FileMetadata(Parcel parcel) {
        super(parcel);
    }

    public static final Creator<FileMetadata> CREATOR = new Creator<FileMetadata>() {
        @Override
        public FileMetadata createFromParcel(Parcel in) {
            return new FileMetadata(in);
        }

        @Override
        public FileMetadata[] newArray(int size) {
            return new FileMetadata[size];
        }
    };

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

    public void setProvisioningDate(Date date) {
        writeLE64(0x0, date.getTime());
    }

    public void setSchemaVersion(short ver) {
        writeLE16(0x8, ver);
    }

    public void setDeviceType(short type) {
        writeLE16(0xa, type);
    }

    public void setUnused1(short val) {
        writeLE16(0xc, val);
    }

    public void setUnused2(short val) {
        writeLE16(0xe, val);
    }
}
