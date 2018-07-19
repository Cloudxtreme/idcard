package org.us.x42.kyork.idcard.desfire;

public class DESFirePlainFile {
    // class states:
    //  - read from card
    //  - created from application

    private byte[] contents;
    private boolean hasFileSettings;
    private boolean isBackupFile;
    private byte communicationSettings;
    private short accessRights;
    private int fileSize;

    public DESFirePlainFile(boolean isBackupFile, byte communicationSettings, short accessRights, int fileSize) {

    }

    public byte[] getContents() {
        return this.contents;
    }


}
