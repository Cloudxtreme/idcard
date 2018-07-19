package org.us.x42.kyork.idcard.data;

public class FileDoorPermissions extends AbstractCardFile {
    public FileDoorPermissions(byte[] content) {
        super(content);
    }

    @Override
    public int getFileID() {
        return 0x04;
    }

    @Override
    public int getExpectedFileSize() {
        return -1;
    }
}
