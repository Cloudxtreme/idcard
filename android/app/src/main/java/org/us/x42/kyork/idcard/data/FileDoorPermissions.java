package org.us.x42.kyork.idcard.data;

import android.nfc.Tag;
import android.os.Parcel;
import android.util.Log;

import org.us.x42.kyork.idcard.desfire.DESFireCard;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

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

    public void signMAC(byte[] uid, byte[] key, FileMetadata meta, FileUserInfo info) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        byte[] uidPadding = new byte[0x10 - uid.length];
        stream.write(uid);
        stream.write(uidPadding);
        stream.write(meta.rawContent, 0, 0x10);
        stream.write(info.rawContent, 0, 0x20);
        stream.write(rawContent, 0, 0x30);

        byte[] verifyData = stream.toByteArray();

        Blake2sMessageDigest engine = new Blake2sMessageDigest(16, key);
        engine.engineUpdate(verifyData, 0, verifyData.length);
        byte[] mac = engine.engineDigest();
        engine.destroy();

        Log.i("DATA", DESFireCard.stringifyByteArray(Arrays.copyOfRange(verifyData, 0x00, 0x10)));
        Log.i("DATA", DESFireCard.stringifyByteArray(Arrays.copyOfRange(verifyData, 0x10, 0x20)));
        Log.i("DATA", DESFireCard.stringifyByteArray(Arrays.copyOfRange(verifyData, 0x20, 0x30)));
        Log.i("DATA", DESFireCard.stringifyByteArray(Arrays.copyOfRange(verifyData, 0x30, 0x40)));
        Log.i("DATA", DESFireCard.stringifyByteArray(Arrays.copyOfRange(verifyData, 0x40, 0x50)));
        Log.i("DATA", DESFireCard.stringifyByteArray(Arrays.copyOfRange(verifyData, 0x50, 0x60)));
        Log.i("DATA", DESFireCard.stringifyByteArray(Arrays.copyOfRange(verifyData, 0x60, 0x70)));

        //Log.i("HASH", DESFireCard.stringifyByteArray(Arrays.copyOfRange(mac, 0x00, 0x10)));
        setSlice(0x30, mac, 0, 0x10);
        //Log.i("COPY", DESFireCard.stringifyByteArray(Arrays.copyOfRange(rawContent, 0x30, 0x40)));
    }
}
