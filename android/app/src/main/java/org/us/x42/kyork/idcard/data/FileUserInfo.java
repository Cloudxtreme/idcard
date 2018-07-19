package org.us.x42.kyork.idcard.data;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class FileUserInfo extends AbstractCardFile {
    public FileUserInfo(byte[] content) {
        super(content);
    }

    @Override
    public int getFileID() {
        return 0x02;
    }

    @Override
    public int getExpectedFileSize() {
        return 32;
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

    public int getIntraUserID() {
        return readLE32(0x8);
    }

    public byte getCampusID() {
        return getRawContent()[0xc];
    }

    public byte getAccountType() {
        return getRawContent()[0xd];
    }

    public int getPiscineDMYCompressed() {
        return readLE24(0xe);
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

    public byte[] getCardSerialVerify() {
        return getSlice(0x11, 0x18);
    }

    public long getLastUpdated() {
        return readLE64(0x18);
    }
}
