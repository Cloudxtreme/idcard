package org.us.x42.kyork.idcard.data;

import android.os.Parcel;

public class FileDoorPermissions extends AbstractCardFile {
    public static final byte FILE_ID = (byte)0x04;

    public FileDoorPermissions(byte[] content) {
        super(content);
    }

    protected FileDoorPermissions(Parcel parcel) { super(parcel); }

    public static final Creator<FileDoorPermissions> CREATOR = new Creator<FileDoorPermissions>() {
        @Override
        public FileDoorPermissions createFromParcel(Parcel in) {
            return new FileDoorPermissions(in);
        }

        @Override
        public FileDoorPermissions[] newArray(int size) {
            return new FileDoorPermissions[size];
        }
    };

    @Override
    public int getFileID() {
        return 0x04;
    }

    @Override
    public int getExpectedFileSize() {
        return 64;
    }

    public byte[] getMAC() { return getSlice(0x30, 0x40); }

    public void setMAC(byte[] newMAC) {
        setSlice(0x30, newMAC, 0, 0x10);
    }
}
