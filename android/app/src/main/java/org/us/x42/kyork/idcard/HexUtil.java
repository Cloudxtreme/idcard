package org.us.x42.kyork.idcard;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.io.BaseEncoding;

import java.util.Locale;

/**
 * Utilities for reading and writing hexadecimal data.
 */

public final class HexUtil {
    public static byte[] decodeHex(String hex) throws IllegalArgumentException {
        return BaseEncoding.base16().decode(hex);
    }

    @NonNull
    public static byte[] decodeUserInput(String userHex) throws DecodeException {
        return new UserHexDecoder(userHex).toByteArray();
    }

    @Nullable
    public static byte[] tryDecodeUserInput(String userHex) {
        try {
            return new UserHexDecoder(userHex).toByteArray();
        } catch (DecodeException ignored) {
            return null;
        }
    }

    public static String encodeHex(byte[] data) {
        return BaseEncoding.base16().encode(data);
    }

    public static CharSequence encodeHexLineWrapped(byte[] data) {
        StringBuilder sb = new StringBuilder();
        appendLineWrappedHex(sb, data);
        return sb;
    }

    public static CharSequence encodeHexLineWrapped(byte[] data, int start, int end) {
        StringBuilder sb = new StringBuilder();
        appendLineWrappedHex(sb, data, start, end);
        return sb;
    }

    public static void appendLineWrappedHex(StringBuilder sb, byte[] data) {
        appendLineWrappedHex(sb, data, 0, data.length);
    }

    public static void appendLineWrappedHex(StringBuilder sb, byte[] data, int start, int end) {
        for (int i = start; i < end; i++) {
            sb.append(Integer.toHexString(data[i]));
            sb.append(' ');
            if (i % 8 == 7) {
                sb.append('\n');
            } else if (i % 2 == 1) {
                sb.append(' ');
            }
        }
    }

    @NonNull
    public static String stringifyByteArray(byte[] data) {
        if (data == null) {
            return "(null)";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        sb.append(' ');
        for (byte d : data) {
            sb.append(String.format("%02X", d));
            sb.append(' ');
        }
        sb.append(']');
        return sb.toString();
    }

    private static class UserHexDecoder {
        String input;
        int inputPos;
        int outputPos;
        boolean prevHadWhitespace = true;

        UserHexDecoder(String input) { this.input = input; }

        int nextValue() throws DecodeException {
            // note: do not need over-BMP safety here

            char c1;
            boolean hadWhitespace = false;
            do {
                if (inputPos >= input.length()) {
                    return -1;
                }
                c1 = input.charAt(inputPos);
                inputPos++;
                if (Character.isWhitespace(c1)) { // skip whitespace to next hex char
                    hadWhitespace = true;
                    continue;
                } else if (-1 == Character.digit(c1, 16)) {
                    throw new DecodeException(c1, inputPos);
                }
                break;
            } while (true);

            char c2 = 0;
            if (inputPos < input.length()) { // handle EOF as whitespace
                c2 = input.charAt(inputPos);
                inputPos++;
            }
            if ((c2 == 0 || Character.isWhitespace(c2)) && prevHadWhitespace) { // 0x1, 0x2 -> 0x01, 0x02
                c2 = c1;
                c1 = '0';
            } else if (c2 == 0) {
                // bad EOF
                throw new DecodeException('␄', input.length());
            }
            prevHadWhitespace = hadWhitespace;
            int highNibble = Character.digit(c1, 16);
            int lowNibble = Character.digit(c2, 16);
            if (highNibble == -1) {
                throw new DecodeException(c1, inputPos);
            } else if (lowNibble == -1) {
                throw new DecodeException(c2, inputPos);
            }
            return ((highNibble << 4) + lowNibble);
        }

        int getLength() throws DecodeException {
            inputPos = 0;
            outputPos = 0;
            while (nextValue() != -1) {
                outputPos++;
            }
            return outputPos;
        }

        byte[] toByteArray() throws DecodeException {
            int len = getLength();
            byte[] result = new byte[len];
            inputPos = 0;
            outputPos = 0;
            while (true) {
                int tmp = nextValue();
                if (tmp == -1) {
                    return result;
                }
                result[outputPos] = (byte)tmp;
                outputPos++;
            }
        }
    }

    /**
     * Interface for a {@link Throwable} that can localize its message on Android.
     */
    public interface AndroidLocalizedException {
        String getLocalizedMessage(Context context);
    }

    static class DecodeException extends IllegalArgumentException implements AndroidLocalizedException {
        private static final String MSG_ENGLISH = "Invalid character %1$c in hex string at position %2$d";
        char badChar;
        int position;

        DecodeException(char badChar, int position) {
            this.badChar = badChar;
            this.position = position;
        }

        @Override
        public String getMessage() {
            return String.format(Locale.ENGLISH, MSG_ENGLISH, badChar, position);
        }

        public String getLocalizedMessage(Context context) {
            return context.getString(R.string.hex_decode_error, badChar, position);
        }
    }
}
