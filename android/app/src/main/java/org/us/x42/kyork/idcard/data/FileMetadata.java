package org.us.x42.kyork.idcard.data;

import android.os.Parcel;

import com.google.common.collect.ImmutableList;

import org.us.x42.kyork.idcard.R;

import java.util.Date;
import java.util.List;

/**
 * File 0x1 on the card. Metadata, type of card (e.g. is this actually a card or is it something else?)
 */
public class FileMetadata extends AbstractCardFile {
    public static final byte FILE_ID = (byte) 0x01;
    public static final int SIZE = 16;

    public static final short DEVICE_TYPE_ID = 0x4944;
    public static final short DEVICE_TYPE_TICKET = 0x544b;
    public static final short DEVICE_TYPE_DOOR = 0x444f;
    public static final short DEVICE_TYPE_CANTINA = 0x4341;
    public static final short DEVICE_TYPE_UPDATE = 0x5550;

    public FileMetadata(byte[] content) {
        super(content);
    }

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
        return SIZE;
    }

    public static FileMetadata createUpdateMetadataFile() {
        FileMetadata ret = new FileMetadata(new byte[SIZE]);
        ret.setProvisioningDate(new Date());
        ret.setDeviceType(DEVICE_TYPE_UPDATE);
        ret.setSchemaVersion((short) 0x0001);
        return ret;
    }

    public static FileMetadata createTicketMetadataFile() {
        FileMetadata ret = new FileMetadata(new byte[SIZE]);
        ret.setProvisioningDate(new Date());
        ret.setDeviceType(DEVICE_TYPE_TICKET);
        ret.setSchemaVersion((short) 0x0001);
        return ret;
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
        writeBE16(0xa, type);
    }

    public void setUnused1(short val) {
        writeLE16(0xc, val);
    }

    public void setUnused2(short val) {
        writeLE16(0xe, val);
    }

    private static List<HexSpanInfo.Interface> SPAN_INFO;

    private static List<HexSpanInfo.Interface> getSpanInfo() {
        if (SPAN_INFO == null) {
            SPAN_INFO = ImmutableList.<HexSpanInfo.Interface>of(
                    HexSpanInfo.LittleEndian.builder().offsetAndLength(0x0, 8).fieldName(R.string.editor_meta_timestamp).build(),
                    HexSpanInfo.LittleEndian.builder().offsetAndLength(0x8, 2).fieldName(R.string.editor_meta_schema).build(),
                    HexSpanInfo.EnumeratedBytes.builder().offsetAndLength(0xa, 2).fieldName(R.string.editor_meta_type)
                            .addItem("4944", R.string.editor_meta_type_card)
                            .addItem("544B", R.string.editor_meta_type_ticket)
                            .addItem("5550", R.string.editor_meta_type_update)
                            .build(),
                    HexSpanInfo.Basic.builder().offsetAndLength(0xc, 2).fieldName(R.string.editor_reserved).build(),
                    HexSpanInfo.Basic.builder().offsetAndLength(0xe, 2).fieldName(R.string.editor_reserved).build()
                    );
        }
        return SPAN_INFO;
    }

    public void describeHexSpanContents(List<HexSpanInfo.Interface> destination) {
        destination.addAll(getSpanInfo());
    }
}
