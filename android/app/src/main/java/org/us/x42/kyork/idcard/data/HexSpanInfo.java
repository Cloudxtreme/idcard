package org.us.x42.kyork.idcard;

import android.support.annotation.StringRes;

import java.util.Comparator;
import java.util.Arrays;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Shorts;
import com.google.common.primitives.UnsignedBytes;

public class HexSpanInfo {
	private @StringRes int nameRsc;
	private @StringRes int helpRsc;
	private int offset;
	private int size;

	protected static final int STRING_UNKNOWN = -1;
	protected static final int STRING_VARYING = -2;

	/**
	 * Construct a new HexSpanInfo over the specified bytes.
	 */
	public HexSpanInfo(int offset, int size) {
		this.offset = offset;
		this.size = size;
	}

	/**
	 * Get the file offset this span is describing.
	 */
	public int getOffset() { return offset; }

	/**
	 * Get the number of bytes this span is describing.
	 */
	public int getSize() { return size; }

	/**
	 * Get the name of this field.
	 */
	public @StringRes int getNameResource() { return nameRsc; }

	/**
	 * Get the help string for this field.
	 */
	public @StringRes int getHelpResource() { return helpRsc; }

	/**
	 * Set the field name and field help resource values.
	 */
	public T setNameAndHelp<? extends HexSpanResource>(@StringRes int nameResource, @StringRes int helpResource) {
		this.nameRsc = nameResource;
		this.helpRsc = helpResource;
		return this;
	}

	/**
	 * Subclasses should override this if they can produce a string resource
	 * representing a value.
	 *
	 * @param file the file
	 * @return Android string resource ID.
	 */
	protected @StringRes int getStringResourceByValue(byte[] file) {
		return STRING_VARYING;
	}

	/**
	 * Subclasses must override this if getStringResourceByValue() returns
	 * STRING_VARYING (the default) to describe the value in the byte array.
	 *
	 * @param file the file
	 * @return String describing the value.
	 */
	public String getStringVaryingByValue(byte[] file) {
		return null;
	}

	/**
	 * Get the string describing this byte range.
	 *
	 * @param Context a Context to convert resources into Strings
	 * @param value The byte range to describe
	 */
	public String getStringByValue(Context context, byte[] file) {
		int rsc = getStringResourceByValue(file);
		if (rsc == STRING_UNKNOWN) {
			return context.getResources().getString(R.strings.hexedit_unknown);
		} else if (rsc == STRING_VARYING) {
			String result = getStringVaryingByValue(value);
			if (result == null) {
				return context.getResources().getString(R.strings.hexedit_unknown);
			}
			return result;
		} else {
			return context.getResources().getString(rsc);
		}
	}

	/**
	 * Represents fields that should give a dropdown for the user to select from.
	 */
	public interface DropdownSpan {
		/**
		 * Get the options in the dropdown, other than 'Custom'.
		 */
		List<Integer> getOptions();

		/**
		 * Apply a dropdown entry to the target file.
		 *
		 * @return false if an error occurs
		 * @throws ArrayIndexOutOfBoundsException if the file is the wrong size
		 */
		public boolean applySelection(@StringRes int selectionResource, byte[] file);
	}
	
	public interface Numeric {
		/**
		 * Get the value of the span.
		 */
		public long getNumericValue(byte[] file);

		/**
		 * Apply a new value to the span.
		 *
		 * @return false if an error occurs
		 * @throws ArrayIndexOutOfBoundsException if the file is the wrong size
		 */
		public boolean applyNumericValue(long value, byte[] file);
	}

	/**
	 * A little-endian number up to 8 bytes long.
	 */
	public static class LittleEndian extends HexSpanInfo {
		public LittleEndian(int offset, int size) {
			super(offset, size);
		}

		@Override
		public String getStringVaryingByValue(byte[] file) {
			return Longs.toString(getNumericValue(file));
		}

		@Override
		public long getNumericValue(byte[] file) {
			int off = getOffset();
			switch (getSize()) {
				case 1:
					return (long)file[off];
				case 2:
					return (long)Shorts.fromBytes(file[off + 1], file[off]);
				case 3:
					return (long)Ints.fromBytes(0, file[off + 2], file[off + 1], file[off]);
				case 4:
					return (long)Ints.fromBytes(file[off + 3], file[off + 2], file[off + 1], file[off]);
				case 5:
					return Longs.fromBytes(0, 0, 0, file[off + 4], file[off + 3], file[off + 2], file[off + 1], file[off]);
				case 6:
					return Longs.fromBytes(0, 0, file[off + 5], file[off + 4], file[off + 3], file[off + 2], file[off + 1], file[off]);
				case 7:
					return Longs.fromBytes(0, file[off + 6], file[off + 5], file[off + 4], file[off + 3], file[off + 2], file[off + 1], file[off]);
				case 8:
					return Longs.fromBytes(file[off + 7], file[off + 6], file[off + 5], file[off + 4], file[off + 3], file[off + 2], file[off + 1], file[off]);
			}
		}

		@Override
		public boolean applyNumericValue(long value, byte[] file) {
			byte[] bytes = Longs.toByteArray(Long.reverseBytes(value));
			System.arraycopy(bytes, 8 - getSize(), file, getOffset(), getSize());
			return true;
		}
	}

	public static class EnumeratedSingleByte extends HexSpanInfo implements DropdownSpan {
		private @StringRes int[] strings;
		private byte[] values; // sorted

		public EnumeratedSingleByte(int offset) {
			super(offset, 1);
			strings = new int[0];
			values = new byte[0];
		}

		/**
		 * Construct an EnumeratedSingleByte HexSpanInfo using a pair of
		 * arrays: string resources, and byte values. The arrays must be sorted
		 * by the byte values.
		 *
		 * @param offset Offset into the file.
		 * @param strings Resource IDs for strings of the name of each value.
		 * @param values Values corresponding to each string.
		 */
		public EnumeratedSingleByte(int offset, int[] strings, byte[] values) {
			super(offset, 1);
			this.strings = strings;
			this.values = values;
		}

		/**
		 * Construct an EnumeratedSingleByte HexSpanInfo using alternating
		 * string resource Integers and Bytes. The Byte values must appear in
		 * sorted order.
		 *
		 * @param offset Offset into the file.
		 * @param strings Resource IDs for strings of the name of each value.
		 * @param values Values corresponding to each string.
		 */
		public EnumeratedSingleByte(int offset, Object... rest) {
			super(offset, 1);
			if (rest.length % 2 != 0) throw new IllegalArgumentException("Odd number of parameters to new HexSpanInfo.EnumeratedSingleByte()");

			this.strings = new int[rest.length / 2];
			this.values = new byte[rest.length / 2];

			for (int i = 0; i < rest.length; i += 2) {
				strings[i / 2] = ((Integer)rest[i]).intValue();
				values[i / 2] = ((Byte)rest[i]).byteValue();
			}
		}

		public EnumeratedSingleByte(int offset, int size, int string1, byte value1) {
			strings = new int[] {string1};
			values = new byte[] {value1};
		}

		public EnumeratedSingleByte(int offset, int size, int string1, byte value1, int string2, byte value2) {
			strings = new int[] {string1, string2};
			values = new byte[] {value1, value2};
		}

		@Override
		public int getStringByValue(byte[] file) {
			int idx = Arrays.binarySearch(values, file[getOffset()]);
			if (idx < 0) return STRING_UNKNOWN;
			return strings[idx];
		}

		@Override
		public List<Integer> getOptions() {
			return Ints.asList(strings);
		}

		@Override
		public boolean applySelection(int selectionResource, byte[] targetFile) {
			int idx = Ints.indexOf(strings, selectionResource);
			if (idx < 0) return false;

			targetFile[getOffset()] = values[idx];
			return true;
		}
	}

	public static class EnumeratedBytes extends HexSpanInfo implements DropdownSpan {
		private @StringRes int[] strings;
		private byte[][] values; // sorted

		public EnumeratedBytes(int offset, int size) {
			super(offset, size);
			strings = new int[0];
			values = new byte[0];
		}

		/**
		 * Construct an EnumeratedSingleByte HexSpanInfo using a pair of
		 * arrays: string resources, and bytearray values. The arrays must be
		 * sorted by the lexicographical order of the byte values.
		 *
		 * @param offset Offset into the file.
		 * @param strings Resource IDs for strings of the name of each value.
		 * @param values Values corresponding to each string.
		 */
		public EnumeratedBytes(int offset, int size, @StringRes int[] strings, byte[][] values) {
			super(offset, size);
			this.strings = strings;
			this.values = values;
			verify(size, strings, values);
		}

		/**
		 * Construct an EnumeratedSingleByte HexSpanInfo using alternating
		 * string resource Integers and Bytes. The Byte values must appear in
		 * sorted order.
		 *
		 * @param offset Offset into the file.
		 * @param strings Resource IDs for strings of the name of each value.
		 * @param values Values corresponding to each string.
		 */
		public EnumeratedBytes(int offset, int size, Object... rest) {
			super(offset, size);
			if (rest.length % 2 != 0) throw new IllegalArgumentException("Odd number of parameters to new HexSpanInfo.EnumeratedBytes()");

			this.strings = new int[rest.length / 2];
			this.values = new byte[][rest.length / 2];

			for (int i = 0; i < rest.length; i += 2) {
				strings[i / 2] = ((Integer)rest[i]).intValue();
				values[i / 2] = ((byte[])rest[i]);
			}
			verify(size, strings, values);
		}

		/**
		 * Check that the values are sorted properly.
		 *
		 * @throws IllegalArgumentException
		 */
		private static void verify(int size, int[] strings, byte[][] values) {
			Comparator<byte[]> c = UnsignedBytes.lexicographicalComparator();
			for (int i = 0; i < values.length - 1; i++) {
				if (values[i].length != size) {
					throw new IllegalArgumentException("bad size in HexSpanInfo.EnumeratedBytes constructor at index "
							+ i + ": wanted " + size + " got " + v.length);
				}
				if c.compare(values[i], values[i + 1]) > 0 {
					throw new IllegalArgumentException("bad order in HexSpanInfo.EnumeratedBytes constructor at index " + i);
				}
			}
			if (values.length > 0) {
				if (values[values.length - 1].length != size) {
					throw new IllegalArgumentException("bad size in HexSpanInfo.EnumeratedBytes constructor at index "
							+ values.length - 1 + ": wanted " + size + " got " + v.length);
				}
			}
		}

		@Override
		public int getStringByValue(byte[] file) {
			byte[] value = Arrays.copyOfRange(file, getOffset(), getOffset() + getSize());
			int idx = Arrays.binarySearch(values, value, UnsignedBytes.lexicographicalComparator());
			if (idx < 0) return STRING_UNKNOWN;
			return strings[idx];
		}

		@Override
		public List<Integer> getOptions() {
			return Ints.asList(strings);
		}

		@Override
		public boolean applySelection(int selectionResource, byte[] targetFile) {
			int idx = Ints.indexOf(strings, selectionResource);
			if (idx < 0) return false;

			System.arraycopy(values[idx], 0, targetFile, getOffset(), getSize());
			return true;
		}
	}
}
