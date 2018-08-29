package org.us.x42.kyork.idcard.data;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import com.google.common.io.BaseEncoding;
import com.google.common.primitives.Ints;

import org.us.x42.kyork.idcard.HexUtil;
import org.us.x42.kyork.idcard.PackUtil;
import org.us.x42.kyork.idcard.R;

import java.io.UnsupportedEncodingException;
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

        void setRawContents(byte[] file, byte[] newContents);
    }

    public interface Numeric extends Interface {
        long getValue(byte[] file);

        /**
         * Check if the user-provided value fits in the field.
         *
         * @param newValue proposed value
         * @return null for OK, or an error message
         */
        @Nullable CharSequence checkValue(Context context, long newValue);

        void setValue(byte[] file, long newValue);
    }

    public interface Enumerated extends Interface {
        @StringRes
        List<Integer> getPossibleValues();

        @StringRes
        int getContentStringResource(byte[] file);

        void setContentByStringResource(byte[] file, @StringRes int stringID);
    }

    public interface Stringish extends Interface {
        @NonNull String getValue(byte[] file);
        @Nullable CharSequence checkValue(Context context, CharSequence input);
        void setValue(byte[] file, CharSequence input);
    }

    /**
     * A basic area with no editability other than the raw bytes.
     */
    public static class Basic implements Interface {
        @StringRes int fieldName;
        int offset;
        int length;

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
            return HexUtil.encodeHexLineWrapped(file, getOffset(), getOffset() + getLength());
        }

        public void setRawContents(byte[] file, byte[] newContents) {
            if (newContents.length != getLength()) throw new IndexOutOfBoundsException();

            System.arraycopy(newContents, 0, file, getOffset(), getLength());
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
        public @Nullable CharSequence checkValue(Context context, long proposedValue) {
            switch (getLength()) {
                case 1:
                    if ((proposedValue < -128) || (proposedValue >= 256)) {
                        return context.getString(R.string.editor_err_range_byte);
                    }
                    return null;
                case 2:
                    if ((proposedValue < -32768) || (proposedValue >= 65536)) {
                        return context.getString(R.string.editor_err_range_short);
                    }
                    return null;
                case 3:
                    if ((proposedValue < -8388608) || (proposedValue >= 16777216)) {
                        return context.getString(R.string.editor_err_range_three);
                    }
                    return null;
                case 4:
                    if ((proposedValue < -2147483648L) || (proposedValue >= 4294967296L)) {
                        return context.getString(R.string.editor_err_range_int);
                    }
                    return null;
                case 7:
                    if ((proposedValue < -36028797018963968L) || (proposedValue >= 72057594037927936L)) {
                        return context.getString(R.string.editor_err_range_seven);
                    }
                    return null;
                case 8:
                    if (false) {
                        return context.getString(R.string.editor_err_range_eight);
                    }
                    return null;
            }
            return null;
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
            public Builder offsetAndLength(int off, int len) {
                super.offsetAndLength(off, len);
                return this;
            }

            @Override
            public Builder fieldName(int name) {
                super.fieldName(name);
                return this;
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

    public static class StringF extends HexSpanInfo.Basic implements Stringish {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder extends HexSpanInfo.Basic.Builder {
            private Builder() {
                super(new HexSpanInfo.StringF());
            }
        }

        @Override
        public CharSequence getShortContents(Context context, byte[] file) {
            return getValue(file);
        }

        @Override
        public String getValue(byte[] file) {
            int i;
            int len;
            len = 0;
            i = getOffset();
            while (i < getLength() && file[i] != 0) {
                i++;
                len++;
            }
            try {
                return new java.lang.String(file, getOffset(), len, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Platform does not support UTF-8", e);
            }
        }

        @Override
        public CharSequence checkValue(Context context, CharSequence input) {
            try {
                byte[] inputBytes = input.toString().getBytes("UTF-8");
                if (inputBytes.length > getLength()) {
                    return context.getString(R.string.editor_err_string_too_long, getLength());
                }
                return null;

            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Platform does not support UTF-8", e);
            }
        }

        @Override
        public void setValue(byte[] file, CharSequence input) {
            byte[] inputBytes = null;
            try {
                inputBytes = input.toString().getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Platform does not support UTF-8", e);
            }
            if (inputBytes.length > getLength()) {
                throw new IllegalArgumentException("String too long");
            }

            System.arraycopy(inputBytes, 0, file, getOffset(), inputBytes.length);
            // fill in the null bytes
            for (int i = inputBytes.length; i < getLength(); i++) {
                file[i] = 0;
            }
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
