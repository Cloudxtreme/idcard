package org.us.x42.kyork.idcard.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class IDCard implements Parcelable {
    public FileMetadata fileMetadata;
    public FileUserInfo fileUserInfo;
    public FileDoorPermissions fileDoorPermissions;
    public FileSignatures fileSignatures;

    public IDCard() { }

    private IDCard(Parcel parcel) {
        byte[] rawContent = new byte[parcel.readInt()];
        parcel.readByteArray(rawContent);
        this.fileMetadata = new FileMetadata(rawContent);
        int length = parcel.readInt();
        for (int i = 0; i < length; i++)
            this.fileMetadata.setDirty(parcel.readInt(), parcel.readInt());

        rawContent = new byte[parcel.readInt()];
        parcel.readByteArray(rawContent);
        this.fileUserInfo = new FileUserInfo(rawContent);
        length = parcel.readInt();
        for (int i = 0; i < length; i++)
            this.fileUserInfo.setDirty(parcel.readInt(), parcel.readInt());

        rawContent = new byte[parcel.readInt()];
        parcel.readByteArray(rawContent);
        this.fileDoorPermissions = new FileDoorPermissions(rawContent);
        length = parcel.readInt();
        for (int i = 0; i < length; i++)
            this.fileDoorPermissions.setDirty(parcel.readInt(), parcel.readInt());

        rawContent = new byte[parcel.readInt()];
        parcel.readByteArray(rawContent);
        this.fileSignatures = new FileSignatures(rawContent);
        length = parcel.readInt();
        for (int i = 0; i < length; i++)
            this.fileSignatures.setDirty(parcel.readInt(), parcel.readInt());
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
        dest.writeInt(fileMetadata.getRawContent().length);
        dest.writeByteArray(fileMetadata.getRawContent());
        List<int[]> dirtyRanges = fileMetadata.getDirtyRanges();
        dest.writeInt(dirtyRanges.size());
        for (int[] dirtyRange : dirtyRanges) {
            dest.writeInt(dirtyRange[0]);
            dest.writeInt(dirtyRange[1]);
        }

        dest.writeInt(fileUserInfo.getRawContent().length);
        dest.writeByteArray(fileUserInfo.getRawContent());
        dirtyRanges = fileUserInfo.getDirtyRanges();
        dest.writeInt(dirtyRanges.size());
        for (int[] dirtyRange : dirtyRanges) {
            dest.writeInt(dirtyRange[0]);
            dest.writeInt(dirtyRange[1]);
        }

        dest.writeInt(fileDoorPermissions.getRawContent().length);
        dest.writeByteArray(fileDoorPermissions.getRawContent());
        dirtyRanges = fileDoorPermissions.getDirtyRanges();
        dest.writeInt(dirtyRanges.size());
        for (int[] dirtyRange : dirtyRanges) {
            dest.writeInt(dirtyRange[0]);
            dest.writeInt(dirtyRange[1]);
        }

        dest.writeInt(fileSignatures.getRawContent().length);
        dest.writeByteArray(fileSignatures.getRawContent());
        dirtyRanges = fileSignatures.getDirtyRanges();
        dest.writeInt(dirtyRanges.size());
        for (int[] dirtyRange : dirtyRanges) {
            dest.writeInt(dirtyRange[0]);
            dest.writeInt(dirtyRange[1]);
        }
    }
}
