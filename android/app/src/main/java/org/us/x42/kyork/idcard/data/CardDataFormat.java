package org.us.x42.kyork.idcard.data;

/**
 * Data format of the NFC cards.
 */
public class CardDataFormat {
    public static final int APPLICATION_ID = 0xFB9852;
    public static final byte[] APPLICATION_ID_BYTES = new byte[]{(byte) 0xFB, (byte) 0x98, (byte) 0x52};

    /**
     * Lookup table for the class representing each "file" format on the card.
     */
    public static final class FileFormatInfo {
        public final Class<? extends CardFile> dfnClass;
        public final int fileID;
        public final int expectedSize;
        public final boolean isSigned;

        FileFormatInfo(Class<? extends CardFile> dfnClass, int fileID, int expectedSize, boolean isSigned) {
            this.dfnClass = dfnClass;
            this.fileID = fileID;
            this.expectedSize = expectedSize;
            this.isSigned = isSigned;
        }
    }

    public static final FileFormatInfo[] files = {
            new FileFormatInfo(FileMetadata.class, 0x1, 16, false),
            new FileFormatInfo(FileUserInfo.class, 0x2, 32, true),
            new FileFormatInfo(FileSignatures.class, 0x7, 68 * 5, false),
    };
}
