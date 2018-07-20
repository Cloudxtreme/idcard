package org.us.x42.kyork.idcard.data;

import org.us.x42.kyork.idcard.desfire.DESFireProtocol;

/**
 * Data format of the NFC cards.
 */
public class CardDataFormat {
    public static final int APPLICATION_ID = 0xFB9852;
    public static final byte[] APPLICATION_ID_BYTES = new byte[]{(byte) 0xFB, (byte) 0x98, (byte) 0x52};

    public static final short SCHEMA_ID = (short)0x0001;

    /**
     * Lookup table for the class representing each "file" format on the card.
     */
    public static final class FileFormatInfo {
        public final Class<? extends CardFile> dfnClass;
        public final byte fileID;
        public final int expectedSize;
        public final boolean isSigned;
        public final byte fileType;

        FileFormatInfo(Class<? extends CardFile> dfnClass, byte fileID, int expectedSize, boolean isSigned,
                       byte fileType) {
            this.dfnClass = dfnClass;
            this.fileID = fileID;
            this.expectedSize = expectedSize;
            this.isSigned = isSigned;
            this.fileType = fileType;
        }
    }

    public static final FileFormatInfo FORMAT_METADATA =
            new FileFormatInfo(FileMetadata.class, (byte)0x1, 16, false, DESFireProtocol.FILETYPE_STANDARD);
    public static final FileFormatInfo FORMAT_USERINFO =
            new FileFormatInfo(FileUserInfo.class, (byte)0x2, 32, true, DESFireProtocol.FILETYPE_BACKUP);
    // cantina file..?
    public static final FileFormatInfo FORMAT_DOORPERMS =
            new FileFormatInfo(FileDoorPermissions.class, (byte)0x4, 64, true, DESFireProtocol.FILETYPE_BACKUP);
    public static final FileFormatInfo FORMAT_SIGNATURES =
            new FileFormatInfo(FileSignatures.class, (byte)0x7, 68 * 5, false, DESFireProtocol.FILETYPE_STANDARD);

    public static final FileFormatInfo[] files = {
            FORMAT_METADATA, FORMAT_USERINFO, FORMAT_DOORPERMS, FORMAT_SIGNATURES,
    };
}
