package org.us.x42.kyork.idcard.data;

import android.os.Parcel;
import android.os.Parcelable;

public class IDCard implements Parcelable {
    public FileMetadata fileMetadata;
    public FileUserInfo fileUserInfo;
    public FileDoorPermissions fileDoorPermissions;
    public FileSignatures fileSignatures;

    public IDCard() {
    }

    private IDCard(Parcel parcel) {
        boolean[] values = new boolean[4];
        parcel.readBooleanArray(values);
        if (values[0])
            this.fileMetadata = parcel.readParcelable(IDCard.class.getClassLoader());
        if (values[1])
            this.fileUserInfo = parcel.readParcelable(IDCard.class.getClassLoader());
        if (values[2])
            this.fileDoorPermissions = parcel.readParcelable(IDCard.class.getClassLoader());
        if (values[3])
            this.fileSignatures = parcel.readParcelable(IDCard.class.getClassLoader());
    }

    public AbstractCardFile getFileByID(byte id) {
        switch (id) {
            case (byte)0x1:
                return (this.fileMetadata);

            case (byte)0x2:
                return (this.fileUserInfo);

            case (byte)0x4:
                return (this.fileDoorPermissions);

            case (byte)0x7:
                return (this.fileSignatures);
        }
        return (null);
    }

    public static final Creator<IDCard> CREATOR = new Creator<IDCard>() {
        @Override
        public IDCard createFromParcel(Parcel in) {
            return new IDCard(in);
        }

        @Override
        public IDCard[] newArray(int size) {
            return new IDCard[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBooleanArray(new boolean[]{
                this.fileMetadata != null,
                this.fileUserInfo != null,
                this.fileDoorPermissions != null,
                this.fileSignatures != null
        });
        if (this.fileMetadata != null) {
            dest.writeParcelable(this.fileMetadata, 0);
        }
        if (this.fileUserInfo != null) {
            dest.writeParcelable(this.fileUserInfo, 0);
        }
        if (this.fileDoorPermissions != null) {
            dest.writeParcelable(this.fileDoorPermissions, 0);
        }
        if (this.fileSignatures != null) {
            dest.writeParcelable(this.fileSignatures, 0);
        }
    }
}
