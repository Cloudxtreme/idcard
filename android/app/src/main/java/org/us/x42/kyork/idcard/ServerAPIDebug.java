package org.us.x42.kyork.idcard;

import android.util.Log;
import android.util.LongSparseArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.us.x42.kyork.idcard.data.AbstractCardFile;
import org.us.x42.kyork.idcard.data.Blake2sMessageDigest;
import org.us.x42.kyork.idcard.data.CardDataFormat;
import org.us.x42.kyork.idcard.data.FileDoorPermissions;
import org.us.x42.kyork.idcard.data.FileMetadata;
import org.us.x42.kyork.idcard.data.FileSignatures;
import org.us.x42.kyork.idcard.data.FileUserInfo;
import org.us.x42.kyork.idcard.data.IDCard;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class ServerAPIDebug implements ServerAPI {
    private static final byte[] ID_MAC_KEY_DEV = HexUtil.decodeHex("2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A");
    private static final byte[] TK_MAC_KEY_DEV = HexUtil.decodeHex("4242424242424242424242424242424242424242424242424242424242424242");
    private static final byte[] TK_FAKE_SERIAL = HexUtil.decodeHex("FFFFFFFFFFFFFFFFFFFFFF"); // longer than any possible NFC-A serial
    private final Object pendingUpdatesLock = new Object();
    private LongSparseArray<IDCard> pendingUpdates = new LongSparseArray<IDCard>();

    @Override
    public IDCard getCardUpdates(long serial, long last_updated) {
        simulatedDelay();
        IDCard card;
        synchronized (pendingUpdatesLock) {
            card = pendingUpdates.get(serial);
        }
        if (card == null) {
            return null;
        }
        if (card.fileUserInfo.getLastUpdated() > last_updated) {
            return card;
        }
        return null;
    }

    @Override
    public IDCard getCardUpdates(byte[] serial, long last_updated) {
        return getCardUpdates(PackUtil.readLE56(serial, 0), last_updated);
    }

    @Override
    public void submitCardUpdates(long serial, IDCard newCard) {
        byte[] serialBytes = new byte[7];
        PackUtil.writeLE56(serialBytes, 0, serial);
        submitCardUpdates(serialBytes, newCard);
    }

    @Override
    public void submitCardUpdates(byte[] serialBytes, IDCard newCard) {
        long serial = PackUtil.readLE56(serialBytes, 0);
        simulatedDelay();

        // Update timestamps and sign
        newCard.fileUserInfo.setLastUpdated(new Date());
        newCard.fileUserInfo.setCardSerialRepeat(serialBytes);
        this.signDoorMAC(serialBytes, ID_MAC_KEY_DEV, newCard);

        if (newCard.fileSignatures == null) {
            newCard.fileSignatures = FileSignatures.newBlank();
        }
        for (AbstractCardFile f : newCard.files()) {
            if (f instanceof FileSignatures)
                continue;

            newCard.fileSignatures.setSignature((byte)f.getFileID(), FileSignatures.KEYID_DEBUG, FileSignatures.signForDebug(f.getRawContent()));
        }

        synchronized (pendingUpdatesLock) {
            pendingUpdates.put(serial, newCard);
        }
        Log.i("ServerAPIDebug", "stored update for " + serial);
    }

    @Override
    public void registerNewCard(long serial, Date provisionDate, String login) throws IOException {
        byte[] serialBytes = new byte[7];
        PackUtil.writeLE56(serialBytes, 0, serial);
        simulatedDelay();

        IDCard card = new IDCard();

        card.fileMetadata = new FileMetadata(new byte[FileMetadata.SIZE]);
        card.fileMetadata.setProvisioningDate(provisionDate);
        card.fileMetadata.setDeviceType(FileMetadata.DEVICE_TYPE_ID);
        card.fileMetadata.setSchemaVersion((short)1);
        card.fileUserInfo = this.getUserInfoFileForLogin(login);
        card.fileDoorPermissions = this.getUnsignedDoorPermissionsForLogin(login);
        this.submitCardUpdates(serial, card);
    }

    @Override
    public void cardUpdatesApplied(long serial, long new_last_updated) {
        simulatedDelay();

        synchronized (pendingUpdatesLock) {
            IDCard card = pendingUpdates.get(serial);
            if (card != null) {
                if (card.fileUserInfo.getLastUpdated() == new_last_updated) {
                    pendingUpdates.remove(serial);
                }
            }
        }
    }

    @Override
    public IDCard getTicketForLogin(String login, FileMetadata metaFile) throws IOException {
        simulatedDelay();

        // TODO - check if they're logged in as that account

        IDCard card = new IDCard();
        card.fileMetadata = metaFile;
        card.fileUserInfo = getUserInfoFileForLogin(login);
        card.fileDoorPermissions = getUnsignedDoorPermissionsForLogin(login);
        this.signDoorMAC(TK_FAKE_SERIAL, TK_MAC_KEY_DEV, card);
        return card;
    }

    private FileUserInfo getUserInfoFileForLogin(String login) throws IOException {
        try {
            JSONObject user = IntraAPI.get().queryUser(login, /* allow cache */ true);
            FileUserInfo fileUserInfo = new FileUserInfo(new byte[CardDataFormat.FORMAT_USERINFO.expectedSize]);

            fileUserInfo.setLogin(login);
            fileUserInfo.setIntraUserID(user.getInt("id"));

            fileUserInfo.setCampusID((byte)0xFF);
            JSONArray campus_users = user.getJSONArray("campus_users");
            for (int i = 0; i < campus_users.length(); i++) {
                JSONObject campus_user = campus_users.getJSONObject(i);
                if (campus_user.getBoolean("is_primary")) {
                    fileUserInfo.setCampusID((byte)campus_user.getInt("campus_id"));
                    break;
                }
            }

            fileUserInfo.setAccountType(IntraAPI.getAccountType(user));
            if (fileUserInfo.getAccountType() == 2 /* piscine */) {
                Calendar c = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
                c.set(2018, 8, 18);
                fileUserInfo.setPiscineEndDate(c.getTime());
            }

            // fileUserInfo.setLastUpdated(new Date()); // TODO load from db
            // fileUserInfo.setCardSerialRepeat(null);
            return fileUserInfo;
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    private FileDoorPermissions getUnsignedDoorPermissionsForLogin(String login) throws IOException {
        FileDoorPermissions doorPermissions = new FileDoorPermissions(new byte[CardDataFormat.FORMAT_DOORPERMS.expectedSize]);

        byte[] raw = doorPermissions.getRawContent();
        PackUtil.writeLE24(raw, 0, 0xFEDCBA);
        raw[20] = 0x41;

        return doorPermissions;
    }

    private void signDoorMAC(byte[] serialBytes, byte[] key, IDCard card) {
        Blake2sMessageDigest engine = new Blake2sMessageDigest(16, key);
        engine.update(serialBytes);
        engine.update(new byte[0x10 - serialBytes.length]);
        engine.update(card.fileMetadata.getRawContent());
        engine.update(card.fileUserInfo.getRawContent());
        engine.update(card.fileDoorPermissions.getRawContent(), 0, 0x30);
        byte[] mac = engine.digest();
        card.fileDoorPermissions.setMAC(mac);
    }

    private void simulatedDelay() {
        try {
            Thread.sleep(350);
        } catch (InterruptedException ignored) {
        }
    }
}
