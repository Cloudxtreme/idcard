package org.us.x42.kyork.idcard.data;

import android.os.Parcel;

public class FileDoorPermissions extends AbstractCardFile {
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
        return -1;
    }
}
