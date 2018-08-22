package org.us.x42.kyork.idcard.data;

import android.os.Parcel;

/**
 * FileUpdateData is how we deliver updates from the phone to the card reader.
 */
public class FileUpdateData extends AbstractCardFile {
    public static final byte FILE_ID = (byte)0x06;

    public FileUpdateData(byte[] content) {
        super(content);
    }

    protected FileUpdateData(Parcel parcel) {
        super(parcel);
    }

    public static final Creator<FileSignatures> CREATOR = new Creator<FileSignatures>() {
        @Override
        public FileSignatures createFromParcel(Parcel in) {
            return new FileSignatures(in);
        }

        @Override
        public FileSignatures[] newArray(int size) {
            return new FileSignatures[size];
        }
    };

    @Override
    public int getFileID() {
        return FILE_ID;
    }

    @Override
    public int getExpectedFileSize() {
        // As large as necessary.
        return 256;
    }
}
