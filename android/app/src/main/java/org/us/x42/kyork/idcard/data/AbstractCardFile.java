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

    protected void setSlice(int offset, byte[] data, int start, int end) {
        for (int i = 0; start + i < end; i++)
            rawContent[offset + i] = data[start + i];
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
     * Write a little-endian 16-bit value to the file.
     *
     * @param offset Byte offset.
     * @param value LE16 value.
     */
    protected void writeLE16(int offset, short value) {
        rawContent[offset] = (byte)(value & 0xFF);
        rawContent[offset + 1] = (byte)((value >> 8) & 0xFF);
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
     * Write a big-endian 16-bit value to the file.
     *
     * @param offset Byte offset.
     * @param value BE16 value.
     */
    protected void writeBE16(int offset, short value) {
        rawContent[offset + 1] = (byte)(value & 0xFF);
        rawContent[offset] = (byte)((value >> 8) & 0xFF);
    }

    /**
     * Read a little-endian 24-bit value from the file.
     *
     * @param offset Byte offset.
     * @return LE24 value.
     */
    protected int readLE24(int offset) {
        return ((rawContent[offset] & 0xFF) |
                ((rawContent[offset + 1] & 0xFF) << 8) |
                ((rawContent[offset + 2] & 0xFF) << 16));
    }

    /**
     * Write a little-endian 24-bit value to the file.
     *
     * @param offset Byte offset.
     * @param value LE24 value.
     */
    protected void writeLE24(int offset, int value) {
        rawContent[offset] = (byte)(value & 0xFF);
        rawContent[offset + 1] = (byte)((value >> 8) & 0xFF);
        rawContent[offset + 2] = (byte)((value >> 16) & 0xFF);
    }

    /**
     * Read a big-endian 24-bit value from the file.
     *
     * @param offset Byte offset.
     * @return BE24 value.
     */
    protected int readBE24(int offset) {
        return ((rawContent[offset + 2] & 0xFF) |
                ((rawContent[offset + 1] & 0xFF) << 8) |
                ((rawContent[offset] & 0xFF) << 16));
    }

    /**
     * Write a big-endian 24-bit value to the file.
     *
     * @param offset Byte offset.
     * @param value LE24 value.
     */
    protected void writeBE24(int offset, int value) {
        rawContent[offset + 2] = (byte)(value & 0xFF);
        rawContent[offset + 1] = (byte)((value >> 8) & 0xFF);
        rawContent[offset] = (byte)((value >> 16) & 0xFF);
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
     * Write a little-endian 32-bit value from the file.
     *
     * @param offset Byte offset.
     * @param value LE32 value.
     */
    protected void writeLE32(int offset, int value) {
        rawContent[offset] = (byte)(value & 0xFF);
        rawContent[offset + 1] = (byte)((value >> 8) & 0xFF);
        rawContent[offset + 2] = (byte)((value >> 16) & 0xFF);
        rawContent[offset + 3] = (byte)((value >> 24) & 0xFF);
    }

    /**
     * Write a big-endian 32-bit value from the file.
     *
     * @param offset Byte offset.
     * @return BE32 value.
     */
    protected int readBE32(int offset) {
        return ((rawContent[offset + 3] & 0xFF) |
                ((rawContent[offset + 2] & 0xFF) << 8) |
                ((rawContent[offset + 1] & 0xFF) << 16) |
                ((rawContent[offset] & 0xFF) << 24));
    }

    /**
     * Write a big-endian 32-bit value from the file.
     *
     * @param offset Byte offset.
     * @param value BE32 value.
     */
    protected void writeBE32(int offset, int value) {
        rawContent[offset + 3] = (byte)(value & 0xFF);
        rawContent[offset + 2] = (byte)((value >> 8) & 0xFF);
        rawContent[offset + 1] = (byte)((value >> 16) & 0xFF);
        rawContent[offset] = (byte)((value >> 24) & 0xFF);
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

    /**
     * Write a little-endian 64-bit value from the file.
     *
     * @param offset Byte offset.
     * @param value LE64 value.
     */
    protected void writeLE64(int offset, long value) {
       rawContent[offset] = (byte)(value & 0xFF);
       rawContent[offset + 1] = (byte)((value >> 8) & 0xFF);
       rawContent[offset + 2] = (byte)((value >> 16) & 0xFF);
       rawContent[offset + 3] = (byte)((value >> 24) & 0xFF);
       rawContent[offset + 4] = (byte)((value >> 32) & 0xFF);
       rawContent[offset + 5] = (byte)((value >> 40) & 0xFF);
       rawContent[offset + 6] = (byte)((value >> 48) & 0xFF);
       rawContent[offset + 7] = (byte)((value >> 56) & 0xFF);
    }

    /**
     * Read a big-endian 64-bit value from the file.
     *
     * @param offset Byte offset.
     * @return BE64 value.
     */
    protected long readBE64(int offset) {
        return (((long) ((rawContent[offset + 7] & 0xFF))) |
                ((long) ((rawContent[offset + 6] & 0xFF)) << 8) |
                ((long) ((rawContent[offset + 5] & 0xFF)) << 16) |
                ((long) ((rawContent[offset + 4] & 0xFF)) << 24) |
                ((long) ((rawContent[offset + 3] & 0xFF)) << 32) |
                ((long) ((rawContent[offset + 2] & 0xFF)) << 40) |
                ((long) ((rawContent[offset + 1] & 0xFF)) << 48) |
                ((long) ((rawContent[offset] & 0xFF)) << 56));
    }

    /**
     * Write a big-endian 64-bit value from the file.
     *
     * @param offset Byte offset.
     * @param value BE64 value.
     */
    protected void writeBE64(int offset, long value) {
        rawContent[offset + 7] = (byte)(value & 0xFF);
        rawContent[offset + 6] = (byte)((value >> 8) & 0xFF);
        rawContent[offset + 5] = (byte)((value >> 16) & 0xFF);
        rawContent[offset + 4] = (byte)((value >> 24) & 0xFF);
        rawContent[offset + 3] = (byte)((value >> 32) & 0xFF);
        rawContent[offset + 2] = (byte)((value >> 40) & 0xFF);
        rawContent[offset + 1] = (byte)((value >> 48) & 0xFF);
        rawContent[offset] = (byte)((value >> 56) & 0xFF);
    }
}
