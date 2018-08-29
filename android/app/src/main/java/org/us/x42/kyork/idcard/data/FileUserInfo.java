package org.us.x42.kyork.idcard.data;

import android.content.Context;
import android.os.Parcel;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;

import org.us.x42.kyork.idcard.PackUtil;
import org.us.x42.kyork.idcard.R;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class FileUserInfo extends AbstractCardFile {
    public static final byte FILE_ID = (byte) 0x02;
    public static final int SIZE = 32;

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
        return FILE_ID;
    }

    @Override
    public int getExpectedFileSize() {
        return SIZE;
    }

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

    public void setIntraUserID(int id) {
        writeLE32(0x8, id);
    }

    public byte getCampusID() {
        return getRawContent()[0xc];
    }

    public void setCampusID(byte id) {
        getRawContent()[0xc] = id;
    }

    public byte getAccountType() {
        return getRawContent()[0xd];
    }

    public void setAccountType(byte type) {
        getRawContent()[0xd] = type;
    }

    public int getPiscineDMYCompressed() {
        return readLE24(0xe);
    }

    public static class PiscineDMYDescriptor extends HexSpanInfo.Basic {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder extends HexSpanInfo.Basic.Builder {
            private Builder() {
                super(new PiscineDMYDescriptor());
            }

            public PiscineDMYDescriptor build() {
                super.build();
                return ((PiscineDMYDescriptor) target);
            }

            @Override
            public Builder fieldName(int name) {
                super.fieldName(name);
                return this;
            }
        }

        @Override
        public int getOffset() {
            return 0xE;
        }

        @Override
        public int getLength() {
            return 3;
        }

        @Override
        public CharSequence getShortContents(Context context, byte[] file) {
            Date endDate = getPiscineEndDate(file);
            if (endDate == null) {
                int val = PackUtil.readLE24(file, getOffset());
                if (val == 0) {
                    return context.getString(R.string.editor_user_piscine_zero);
                }
                return context.getString(R.string.editor_err_date_invalid);
            }
            Locale fmtLocale = Locale.getDefault();
            DateFormat format = DateFormat.getDateInstance(DateFormat.LONG, fmtLocale);

            return format.format(getPiscineEndDate(file));
        }


        public Date getPiscineEndDate(byte[] file) {
            int dmy = PackUtil.readLE24(file, getOffset());
            if (dmy == 0) {
                return null;
            }
            int day = (dmy) & 0x1F;
            int month = (dmy >> 5) & 0xF;
            int year = (dmy >> 9);
            if (day < 1 || day > 31)
                return null;
            if (month < 1 || month > 12)
                return null;

            TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles"); // TODO getCampusTimeZone()
            Calendar cal = Calendar.getInstance(tz);
            cal.clear();
            cal.set(year, month, day);
            return cal.getTime();
        }

        public void setPiscineEndDate(byte[] file, Date date) {
            if (date == null) {
                PackUtil.writeLE24(file, getOffset(), 0);
                return;
            }

            TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles"); // TODO getCampusTimeZone()
            Calendar cal = Calendar.getInstance(tz);

            cal.clear();
            cal.setTime(date);
            int dmy = cal.get(Calendar.YEAR);
            dmy <<= 4;
            dmy |= cal.get(Calendar.MONTH);
            dmy <<= 5;
            dmy |= cal.get(Calendar.DAY_OF_MONTH);

            PackUtil.writeLE24(file, getOffset(), dmy);
        }
    }
    public void setPiscineEndDate(Date date) {
        TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles"); // TODO getCampusTimeZone()
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

    public void setCardSerialRepeat(byte[] tagId) {
        setSlice(0x11, tagId, 0, 7);
    }

    public long getLastUpdated() {
        return readLE64(0x18);
    }

    public void setLastUpdated(Date date) {
        PackUtil.writeLE64(getRawContent(), 0x18, date.getTime());
    }

    private static List<HexSpanInfo.Interface> SPAN_INFO;

    private static List<HexSpanInfo.Interface> getSpanInfo() {
        if (SPAN_INFO == null) {
            SPAN_INFO = ImmutableList.<HexSpanInfo.Interface>of(
                    HexSpanInfo.StringF.builder().offsetAndLength(0, 8).fieldName(R.string.editor_user_intra).build(),
                    HexSpanInfo.LittleEndian.builder().offsetAndLength(0x8, 4).fieldName(R.string.editor_user_id).build(),
                    HexSpanInfo.EnumeratedBytes.builder().offsetAndLength(0xc, 1).fieldName(R.string.editor_user_campus)
                            .addItem("01", R.string.editor_user_campus_paris)
                            .addItem("07", R.string.editor_user_campus_fremont)
                            .build(),
                    HexSpanInfo.EnumeratedBytes.builder().offsetAndLength(0xd, 1).fieldName(R.string.editor_user_act)
                            .addItem("01", R.string.editor_user_act_student)
                            .addItem("02", R.string.editor_user_act_piscine)
                            .addItem("03", R.string.editor_user_act_bocal)
                            .addItem("04", R.string.editor_user_act_employee)
                            .addItem("05", R.string.editor_user_act_security)
                            .build(),
                    PiscineDMYDescriptor.builder().offsetAndLength(0xe, 3).fieldName(R.string.editor_user_piscine).build(),
                    HexSpanInfo.LittleEndian.builder().offsetAndLength(0x11, 7).fieldName(R.string.editor_user_serial).build(),
                    HexSpanInfo.LittleEndian.builder().offsetAndLength(0x18, 8).fieldName(R.string.editor_user_timestamp).build()
            );
        }
        return SPAN_INFO;
    }

    public void describeHexSpanContents(List<HexSpanInfo.Interface> destination) {
        destination.addAll(getSpanInfo());
    }
}
