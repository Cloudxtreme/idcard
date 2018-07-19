package org.us.x42.kyork.idcard;

/**
 * Utilities for reading and writing packed data.
 */
public final class PackUtil {


    /**
     * Read a little-endian 16-bit value from the file.
     *
     * @param offset Byte offset.
     * @return LE16 value.
     */
    public static short readLE16(byte[] rawContent, int offset) {
        return (short) ((rawContent[offset] & 0xFF) | (rawContent[offset + 1] & 0xFF) << 8);
    }

    /**
     * Write a little-endian 16-bit value to the file.
     *
     * @param offset Byte offset.
     * @param value LE16 value.
     */
    public static void writeLE16(byte[] rawContent, int offset, short value) {
        rawContent[offset] = (byte)(value & 0xFF);
        rawContent[offset + 1] = (byte)((value >> 8) & 0xFF);
    }

    /**
     * Read a big-endian 16-bit value from the file.
     *
     * @param offset Byte offset.
     * @return BE16 value.
     */
    public static short readBE16(byte[] rawContent, int offset) {
        return (short) ((rawContent[offset] & 0xFF) << 8 | (rawContent[offset + 1] & 0xFF));
    }

    /**
     * Write a big-endian 16-bit value to the file.
     *
     * @param offset Byte offset.
     * @param value BE16 value.
     */
    public static void writeBE16(byte[] rawContent, int offset, short value) {
        rawContent[offset + 1] = (byte)(value & 0xFF);
        rawContent[offset] = (byte)((value >> 8) & 0xFF);
    }

    /**
     * Read a little-endian 24-bit value from the file.
     *
     * @param offset Byte offset.
     * @return LE24 value.
     */
    public static int readLE24(byte[] rawContent, int offset) {
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
    public static void writeLE24(byte[] rawContent, int offset, int value) {
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
    public static int readBE24(byte[] rawContent, int offset) {
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
    public static void writeBE24(byte[] rawContent, int offset, int value) {
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
    public static int readLE32(byte[] rawContent, int offset) {
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
    public static void writeLE32(byte[] rawContent, int offset, int value) {
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
    public static int readBE32(byte[] rawContent, int offset) {
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
    public static void writeBE32(byte[] rawContent, int offset, int value) {
        rawContent[offset + 3] = (byte)(value & 0xFF);
        rawContent[offset + 2] = (byte)((value >> 8) & 0xFF);
        rawContent[offset + 1] = (byte)((value >> 16) & 0xFF);
        rawContent[offset] = (byte)((value >> 24) & 0xFF);
    }

    /**
     * Read a little-endian 56-bit value from the file. (serial numbers)
     *
     * @param offset Byte offset.
     * @return LE56 value.
     */
    public static long readLE56(byte[] rawContent, int offset) {
        return (((long) ((rawContent[offset] & 0xFF))) |
                ((long) ((rawContent[offset + 1] & 0xFF)) << 8) |
                ((long) ((rawContent[offset + 2] & 0xFF)) << 16) |
                ((long) ((rawContent[offset + 3] & 0xFF)) << 24) |
                ((long) ((rawContent[offset + 4] & 0xFF)) << 32) |
                ((long) ((rawContent[offset + 5] & 0xFF)) << 40) |
                ((long) ((rawContent[offset + 6] & 0xFF)) << 48));
    }

    /**
     * Write a little-endian 56-bit value from the file. (serial numbers)
     *
     * @param offset Byte offset.
     * @param value LE56 value.
     */
    public static void writeLE56(byte[] rawContent, int offset, long value) {
        rawContent[offset] = (byte)(value & 0xFF);
        rawContent[offset + 1] = (byte)((value >> 8) & 0xFF);
        rawContent[offset + 2] = (byte)((value >> 16) & 0xFF);
        rawContent[offset + 3] = (byte)((value >> 24) & 0xFF);
        rawContent[offset + 4] = (byte)((value >> 32) & 0xFF);
        rawContent[offset + 5] = (byte)((value >> 40) & 0xFF);
        rawContent[offset + 6] = (byte)((value >> 48) & 0xFF);
    }

    /**
     * Read a little-endian 64-bit value from the file.
     *
     * @param offset Byte offset.
     * @return LE64 value.
     */
    public static long readLE64(byte[] rawContent, int offset) {
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
    public static void writeLE64(byte[] rawContent, int offset, long value) {
        rawContent[offset] = (byte)(value & 0xFF);
        rawContent[offset + 1] = (byte)((value >> 8) & 0xFF);
        rawContent[offset + 2] = (byte)((value >> 16) & 0xFF);
        rawContent[offset + 3] = (byte)((value >> 24) & 0xFF);
        rawContent[offset + 4] = (byte)((value >> 32) & 0xFF);
        rawContent[offset + 5] = (byte)((value >> 40) & 0xFF);
        rawContent[offset + 6] = (byte)((value >> 48) & 0xFF);
        rawContent[offset + 7] = (byte)((value >> 56) & 0xFF);
    }

}
