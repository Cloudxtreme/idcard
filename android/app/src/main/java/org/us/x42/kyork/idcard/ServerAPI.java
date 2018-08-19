package org.us.x42.kyork.idcard;

import org.us.x42.kyork.idcard.data.FileMetadata;
import org.us.x42.kyork.idcard.data.IDCard;

import java.io.IOException;
import java.util.Date;

public interface ServerAPI {
    IDCard getCardUpdates(long serial, long last_updated) throws IOException;
    IDCard getCardUpdates(byte[] serial, long last_updated) throws IOException;

    void submitCardUpdates(long serial, IDCard newContent) throws IOException;
    void submitCardUpdates(byte[] serial, IDCard newContent) throws IOException;

    void registerNewCard(long serial, Date provisionDate, String login) throws IOException;
    void cardUpdatesApplied(long serial, long new_last_updated);
    IDCard getTicketForLogin(String login, FileMetadata metaFile) throws IOException;
}
