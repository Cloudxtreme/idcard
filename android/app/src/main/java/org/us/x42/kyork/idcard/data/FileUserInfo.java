package org.us.x42.kyork.idcard.data;

import android.os.Parcel;

import com.google.common.collect.ImmutableList;

import org.us.x42.kyork.idcard.PackUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class FileUserInfo extends AbstractCardFile {
    public static final byte FILE_ID = (byte)0x02;

    public FileUserInfo(byte[] content) {
        super(content);
    }

    protected FileUserInfo(Parcel parcel) {
        super(parcel);
    }

    public static final Creator<FileUserInfo> CREATOR = new Creator<FileUserInfo>() {
        @Override
        public FileUserInfo createFromParcel(Parcel in) {
            return new FileUserInfo(in);
        }

        @Override
        public FileUserInfo[] newArray(int size) {
            return new FileUserInfo[size];
        }
    };

    @Override
    public int getFileID() {
        return 0x02;
    }

    @Override
    public int getExpectedFileSize() {
        return 32;
    }

	private static final List<HexSpanInfo> spanInfo = ImmutableList.of(
			(new HexSpanInfo(0x0, 8) {
				@Override
				public String getStringVaryingByValue(byte[] file) {
					return new FileUserInfo(file).getLogin();
				}
			}).setNameAndHelp(/* Intra login */ 0, 0),
			(new HexSpanInfo.LittleEndian(0x8, 4)).setNameAndHelp(/* Intra user ID */ 0, 0),
			(new HexSpanInfo.EnumeratedSingleByte(0xc,
				/* paris */ 0, Byte.valueOf(1),
				/* fremont */ 0, Byte.valueOf(7))
			 ).setNameAndHelp(/* Campus ID */ 0, 0),
			(new HexSpanInfo.EnumeratedSingleByte(0xd,
				/* Student */ 0, Byte.valueOf(1),
				/* Piscine */ 0, Byte.valueOf(2),
				/* Bocal */ 0, Byte.valueOf(3),
				/* Security */ 0, Byte.valueOf(4),
				/* Employee */ 0, Byte.valueOf(5))
			 ).setNameAndHelp(/* Account Type */ 0, 0),
			(new HexSpanInfo(0xe, 3)).setNameAndHelp(/* Piscine Day/Month/Year */ 0, 0),
			(new HexSpanInfo.LittleEndian(0x11, 7)).setNameAndHelp(/* Card serial number */ 0, 0),
			(new HexSpanInfo.LittleEndian(0x18, 8)).setNameAndHelp(/* Last updated timestamp */ 0, /* Set automatically by the system */ 0)
			);

	public List<HexSpanInfo> getSpanInfo() { return spanInfo; }

    public String getLogin() {
        byte[] slice = getSlice(0, 8);
        int i;
        i = 0;
        while (i < slice.length && slice[i] != 0) {
            i++;
        }
        try {
            return new String(slice, 0, i, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Platform does not support UTF-8", e);
        }
    }

    public void setLogin(String login) {
        byte[] string = login.getBytes(Charset.forName("UTF-8"));
        int i;
        i = 0;
        while (i < string.length && string[i] != 0) {
            i++;
        }
        setSlice(0, string, 0, i);
    }

    public int getIntraUserID() {
        return readLE32(0x8);
    }

    public void setIntraUserID(int id) { writeLE32(0x8, id); }

    public byte getCampusID() { return getRawContent()[0xc]; }

    public void setCampusID(byte id) { getRawContent()[0xc] = id; }

    public byte getAccountType() {
        return getRawContent()[0xd];
    }

    public void setAccountType(byte type) { getRawContent()[0xd] = type; }

    public int getPiscineDMYCompressed() {
        return readLE24(0xe);
    }

    public void setPiscineEndDate(Date date) {
        TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
        Calendar cal = Calendar.getInstance(tz);

        cal.clear();
        cal.setTime(date);
        int dmy = cal.get(Calendar.YEAR);
        dmy <<= 4;
        dmy |= cal.get(Calendar.MONTH);
        dmy <<= 5;
        dmy |= cal.get(Calendar.DAY_OF_MONTH);

        writeLE24(0xe, dmy);
    }

    /**
     * Get the end-date of the piscine card.
     *
     * @return Midnight UTC on the end date, or null if not a piscine card.
     * @throws IOException If there is non-zero malformed data
     */
    public Date getPiscineEndDate() throws IOException {
        int dmy = getPiscineDMYCompressed();
        if (dmy == 0) {
            return null;
        }
        int day = (dmy) & 0x1F;
        int month = (dmy >> 5) & 0xF;
        int year = (dmy >> 9);
        if (day < 1 || day > 31)
            throw new IOException("Card has malformed data (Piscine DMY Day)");
        if (month < 1 || month > 12)
            throw new IOException("Card has malformed data (Piscine DMY Month)");

        TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
        Calendar cal = Calendar.getInstance(tz);
        cal.clear();
        cal.set(year, month, day);
        return cal.getTime();
    }

    public long getCardSerialRepeat() {
        return PackUtil.readLE56(getRawContent(), 0x11);
    }

    public void setCardSerialRepeat(long serial) {
        PackUtil.writeLE56(getRawContent(), 0x11, serial);
    }

    public long getLastUpdated() {
        return readLE64(0x18);
    }
}
