package org.us.x42.kyork.idcard.data;

import android.content.Context;
import android.support.annotation.StringRes;

import com.google.common.io.BaseEncoding;
import com.google.common.primitives.Ints;

import org.us.x42.kyork.idcard.PackUtil;
import org.us.x42.kyork.idcard.R;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * This class is is basically a package by itself but I don't care.
 *
 * The objects in it represent ways to view and edit
 */
public final class HexSpanInfo {
    private HexSpanInfo() {
    }

    public interface Interface {
        int getOffset();

        int getLength();

        @StringRes
        int getFieldName();

        CharSequence getShortContents(Context context, byte[] file);
    }

    public interface Numeric extends Interface {
        long getValue(byte[] file);

        /**
         * Check if the user-provided value fits in the field.
         *
         * @param newValue proposed value
         * @return 0 for ok, or the ID of an error message
         */
        @StringRes
        int checkValue(long newValue);

        void setValue(byte[] file, long newValue);
    }

    public interface Enumerated extends Interface {
        @StringRes
        List<Integer> getPossibleValues();

        @StringRes
        int getContentStringResource(byte[] file);

        void setContentByStringResource(byte[] file, @StringRes int stringID);
    }

    public static class Basic implements Interface {
        private @StringRes
        int fieldName;
        private int offset;
        private int length;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            protected Basic target;

            protected Builder(Basic x) {
                target = x;
            }

            private Builder() {
                this(new HexSpanInfo.Basic());
            }

            public HexSpanInfo.Basic build() {
                return target;
            }

            public Builder offsetAndLength(int off, int len) {
                target.offset = off;
                target.length = len;
                return this;
            }

            public Builder fieldName(@StringRes int name) {
                target.fieldName = name;
                return this;
            }
        }

        public int getOffset() {
            return offset;
        }

        public int getLength() {
            return length;
        }

        public @StringRes
        int getFieldName() {
            return fieldName;
        }

        public CharSequence getShortContents(Context context, byte[] file) {
            // Encode to hexadecimal
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < length; i++) {
                if (i != 0) {
                    sb.append(' ');
                }
                sb.append(Integer.toHexString(file[offset + i]));
            }
            return sb;
        }
    }

    public static class LittleEndian extends Basic implements Numeric {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder extends HexSpanInfo.Basic.Builder {
            private Builder() {
                super(new LittleEndian());
            }
        }

        @Override
        public CharSequence getShortContents(Context context, byte[] file) {
            Locale fmtLocale = Locale.getDefault();
            NumberFormat nf = NumberFormat.getNumberInstance(fmtLocale);
            return nf.format(getValue(file));
        }

        @Override
        public long getValue(byte[] file) {
            int off = getOffset();
            switch (getLength()) {
                case 1:
                    return file[off];
                case 2:
                    return PackUtil.readLE16(file, off);
                case 3:
                    return PackUtil.readLE24(file, off);
                case 4:
                    return PackUtil.readLE32(file, off);
                case 7:
                    return PackUtil.readLE56(file, off);
                case 8:
                    return PackUtil.readLE64(file, off);
            }
            throw new IllegalArgumentException("HexSpanInfo.LittleEndian: unsupported length");
        }

        @Override
        public void setValue(byte[] file, long newValue) {
            int off = getOffset();
            switch (getLength()) {
                case 1:
                    file[off] = (byte) newValue;
                    return;
                case 2:
                    PackUtil.writeLE16(file, off, (short) newValue);
                    return;
                case 3:
                    PackUtil.writeLE24(file, off, (int) newValue);
                    return;
                case 4:
                    PackUtil.writeLE32(file, off, (int) newValue);
                    return;
                case 7:
                    PackUtil.writeLE56(file, off, newValue);
                    return;
                case 8:
                    PackUtil.writeLE64(file, off, newValue);
                    return;
                default:
                    throw new IllegalArgumentException("HexSpanInfo.LittleEndian: unsupported length");
            }
        }

        @Override
        public @StringRes int checkValue(long proposedValue) {
            switch (getLength()) {
                case 1:
                    if ((proposedValue < -128) || (proposedValue >= 256)) {
                        return R.string.editor_err_range_byte;
                    }
                    return 0;
                case 2:
                    if ((proposedValue < -32768) || (proposedValue >= 65536)) {
                        return R.string.editor_err_range_short;
                    }
                    return 0;
                case 3:
                    if ((proposedValue < -8388608) || (proposedValue >= 16777216)) {
                        return R.string.editor_err_range_three;
                    }
                    return 0;
                case 4:
                    if ((proposedValue < -2147483648L) || (proposedValue >= 4294967296L)) {
                        return R.string.editor_err_range_int;
                    }
                    return 0;
                case 7:
                    if ((proposedValue < -36028797018963968L) || (proposedValue >= 72057594037927936L)) {
                        return R.string.editor_err_range_seven;
                    }
                    return 0;
                case 8:
                    if (false) {
                        return R.string.editor_err_range_eight;
                    }
                    return 0;
            }
            return 0;
        }
    }

    public static class EnumeratedBytes extends Basic implements Enumerated {
        private byte[][] patterns;
        private @StringRes
        int[] names;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder extends HexSpanInfo.Basic.Builder {
            private List<byte[]> patterns = new ArrayList<>();
            private List<Integer> names = new ArrayList<>();

            private Builder() {
                super(new EnumeratedBytes());
            }

            public EnumeratedBytes build() {
                super.build();
                EnumeratedBytes casted = ((EnumeratedBytes) target);
                casted.patterns = patterns.toArray(new byte[patterns.size()][]);
                casted.names = Ints.toArray(names);
                return casted;
            }

            @Override
            public Basic.Builder offsetAndLength(int off, int len) {
                return super.offsetAndLength(off, len);
            }

            @Override
            public Basic.Builder fieldName(int name) {
                return super.fieldName(name);
            }

            public Builder addItem(byte[] pattern, @StringRes int name) {
                if (pattern.length != target.length) {
                    throw new IllegalArgumentException("EnumeratedBytes.Builder#addItem: wrong parameter length");
                }
                patterns.add(pattern);
                names.add(name);
                return this;
            }

            public Builder addItem(String hexPattern, @StringRes int name) {
                if (hexPattern.length() != (target.length * 2)) {
                    throw new IllegalArgumentException("EnumeratedBytes.Builder#addItem: wrong parameter length");
                }
                patterns.add(BaseEncoding.base16().decode(hexPattern));
                names.add(name);
                return this;
            }
        }

        @Override
        public CharSequence getShortContents(Context context, byte[] file) {
            int res = getContentStringResource(file);
            if (res != 0) {
                return context.getString(res);
            }
            return super.getShortContents(context, file);
        }

        @Override
        public @StringRes
        List<Integer> getPossibleValues() {
            return Ints.asList(names);
        }

        @Override
        public int getContentStringResource(byte[] file) {
            for (int i = 0; i < patterns.length; i++) {
                if (HexSpanInfo.offsetBytesEqual(file, getOffset(), patterns[i], 0, getLength())) {
                    return names[i];
                }
            }
            return 0;
        }

        @Override
        public void setContentByStringResource(byte[] file, int stringID) {
            int idx = -1;
            for (int i = 0; i < names.length; i++) {
                if (names[i] == stringID) {
                    idx = i;
                    break;
                }
            }
            if (idx == -1) throw new IllegalArgumentException("unknown string resource id");
            System.arraycopy(patterns[idx], 0, file, getOffset(), getLength());
        }
    }

    /**
     * Compare the bytes in a subsection of two byte arrays. Not timing-safe.
     *
     * @param pattern1 first byte array
     * @param offset1  offset to start comparing at in first byte array
     * @param pattern2 second byte array
     * @param offset2  offset to start comparing at in second byte array
     * @param length   number of bytes to compare
     * @return true if the two byte arrays have an equal subarray at the specified offset
     */
    private static boolean offsetBytesEqual(byte[] pattern1, int offset1, byte[] pattern2, int offset2, int length) {
        if ((offset1 + length > pattern1.length) || (offset2 + length > pattern2.length)) {
            throw new IllegalArgumentException("offsetBytesEqual: length too large");
        }

        for (int i = 0; i < length; i++) {
            if (pattern1[i + offset1] != pattern2[i + offset2]) {
                return false;
            }
        }
        return true;
    }
}
