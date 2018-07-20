package org.us.x42.kyork.idcard.data;

import android.os.Parcel;
import android.os.Parcelable;

public class IDCard implements Parcelable {
    public FileMetadata fileMetadata;
    public FileUserInfo fileUserInfo;
    public FileDoorPermissions fileDoorPermissions;
    public FileSignatures fileSignatures;

    public IDCard() { }

    protected IDCard(Parcel parcel) {
        boolean[] values = new boolean[4];
        parcel.readBooleanArray(values);
        if (values[0])
            this.fileMetadata = FileMetadata.CREATOR.createFromParcel(parcel);
        if (values[1])
            this.fileUserInfo = FileUserInfo.CREATOR.createFromParcel(parcel);
        if (values[2])
            this.fileDoorPermissions = FileDoorPermissions.CREATOR.createFromParcel(parcel);
        if (values[3])
            this.fileSignatures = FileSignatures.CREATOR.createFromParcel(parcel);
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
        dest.writeBooleanArray(new boolean[] {
                this.fileMetadata != null,
                this.fileUserInfo != null,
                this.fileDoorPermissions != null,
                this.fileSignatures != null
        });
        if (this.fileMetadata != null)
            this.fileMetadata.writeToParcel(dest, flags);
        if (this.fileUserInfo != null)
            this.fileUserInfo.writeToParcel(dest, flags);
        if (this.fileDoorPermissions != null)
            this.fileDoorPermissions.writeToParcel(dest, flags);
        if (this.fileSignatures != null)
            this.fileSignatures.writeToParcel(dest, flags);
    }
}
