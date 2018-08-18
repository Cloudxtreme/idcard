package org.us.x42.kyork.idcard;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import org.us.x42.kyork.idcard.data.FileMetadata;
import org.us.x42.kyork.idcard.data.IDCard;
import org.us.x42.kyork.idcard.desfire.DESFireCard;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

// interesting problem: need to pass data to this thing
// https://stackoverflow.com/questions/25018121/passing-information-to-a-hostapduservice-from-another-context

public class HCEService extends HostApduService {
    private static final String LOG_TAG = "HCEService";
    private IDCard ourCard;

    public HCEService() {
        ourCard = new IDCard();
        SharedPreferences prefs = HCEServiceUtils.getStorage(this);
        if (prefs.getBoolean(HCEServiceUtils.READY_FOR_UPDATES, false)) {
            // TODO
        } else {
            String maybeTicket = prefs.getString(HCEServiceUtils.TICKET_STORAGE, null);
            if (maybeTicket != null) {
                // TODO
            }
        }
    }

    public static final class HCEServiceUtils {
        public static final String SHAREDPREFS_HCE = "org.us.x42.kyork.idcard.ticket";
        public static final String READY_FOR_UPDATES = "update_mode";
        public static final String TICKET_STORAGE = "ticket";
        public static final String UPDATE_BY_DOOR_PREFIX = "update_d[";

        public static SharedPreferences getStorage(Context context) {
            return context.getSharedPreferences(SHAREDPREFS_HCE, 0);
        }
    }

    // if we cannot return immediately, return null for a time extension and use sendCommandApdu() later
    public byte[] processCommandApdu(byte[] var1, Bundle var2) {
        Log.i(LOG_TAG, DESFireCard.stringifyByteArray(var1));

        return new byte[] {(byte)0x91, 0x00};
    }

    public void onDeactivated(int var1) {
        throw new RuntimeException("Stub!");
    }

    private byte[] wrapMessage(byte command, byte[] parameters) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        stream.write((byte) 0x90);
        stream.write(command);
        stream.write((byte) 0x00);
        stream.write((byte) 0x00);
        if (parameters != null) {
            stream.write((byte) parameters.length);
            stream.write(parameters);
        }
        stream.write((byte) 0x00);

        return stream.toByteArray();
    }
}
