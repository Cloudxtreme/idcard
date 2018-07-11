package org.us.x42.kyork.idcard.data;

import java.util.Arrays;

/**
 * Base class for card file format classes. Provides integer decoding utility functions.
 */
public abstract class AbstractCardFile implements CardFile {
    private byte[] rawContent;

    public AbstractCardFile(byte[] content) {
        rawContent = content;
    }

    @Override
    public byte[] getRawContent() {
        return rawContent;
    }

    protected byte[] getSlice(int start, int end) {
        return Arrays.copyOfRange(rawContent, start, end);
    }

    /**
     * Read a little-endian 16-bit value from the file.
     *
     * @param offset Byte offset.
     * @return LE16 value.
     */
    protected short readLE16(int offset) {
        return (short) ((rawContent[offset] & 0xFF) | (rawContent[offset + 1] & 0xFF) << 8);
    }

    /**
     * Read a big-endian 16-bit value from the file.
     *
     * @param offset Byte offset.
     * @return BE16 value.
     */
    protected short readBE16(int offset) {
        return (short) ((rawContent[offset] & 0xFF) << 8 | (rawContent[offset + 1] & 0xFF));
    }

    /**
     * Read a little-endian 32-bit value from the file.
     *
     * @param offset Byte offset.
     * @return LE32 value.
     */
    protected int readLE32(int offset) {
        return ((rawContent[offset] & 0xFF) |
                ((rawContent[offset + 1] & 0xFF) << 8) |
                ((rawContent[offset + 2] & 0xFF) << 16) |
                ((rawContent[offset + 3] & 0xFF) << 24));
    }

    /**
     * Read a little-endian 64-bit value from the file.
     *
     * @param offset Byte offset.
     * @return LE64 value.
     */
    protected long readLE64(int offset) {
        return (((long) ((rawContent[offset] & 0xFF))) |
                ((long) ((rawContent[offset + 1] & 0xFF)) << 8) |
                ((long) ((rawContent[offset + 2] & 0xFF)) << 16) |
                ((long) ((rawContent[offset + 3] & 0xFF)) << 24) |
                ((long) ((rawContent[offset + 4] & 0xFF)) << 32) |
                ((long) ((rawContent[offset + 5] & 0xFF)) << 40) |
                ((long) ((rawContent[offset + 6] & 0xFF)) << 48) |
                ((long) ((rawContent[offset + 7] & 0xFF)) << 56));
    }
}
