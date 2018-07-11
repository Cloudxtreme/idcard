package org.us.x42.kyork.idcard.data;

/**
 * Created by kyork on 7/11/18.
 */

public interface CardFile {
    int getFileID();
    int getExpectedFileSize();

    byte[] getRawContent();
}
