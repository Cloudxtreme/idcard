package org.us.x42.kyork.idcard;

import android.content.Context;
import android.content.SharedPreferences;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.util.Base64;
import android.util.Log;

import org.us.x42.kyork.idcard.data.AbstractCardFile;
import org.us.x42.kyork.idcard.data.FileMetadata;
import org.us.x42.kyork.idcard.data.IDCard;
import org.us.x42.kyork.idcard.desfire.DESFireCard;
import org.us.x42.kyork.idcard.desfire.DESFireProtocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

// interesting problem: need to pass data to this thing
// https://stackoverflow.com/questions/25018121/passing-information-to-a-hostapduservice-from-another-context

public class HCEService extends HostApduService {
    private static final String LOG_TAG = "HCEService";
    private static final byte[] SELECT_APDU = new byte[]{(byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00, (byte) 0x07, (byte) 0xFB, (byte) 0x43, (byte) 0x61, (byte) 0x72, (byte) 0x64, (byte) 0x34, (byte) 0x32, (byte) 0x00};
    private static final byte[] FRAMING_ERROR = new byte[]{(byte) 0x6A, (byte) 0xA0};
    private static final byte[] ERROR_GENERIC = new byte[]{(byte) 0x91, (byte) 0xA1};
    private static final byte[] APP_ID_CARD42 = new byte[]{(byte) 0xFB, (byte) 0x98, (byte) 0x52};

    private static final int BASE64_FLAGS = Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP;

    /**
     * TODO - get the authenticated user out of SharedPreferences
     */
    private static final String TEST_ACCT_LOGIN = "apuel";

    private IDCard ourCard;
    private boolean ticketReady;
    private IOException fetchException;
    private byte[] authRndA;
    private byte[] authRndB;
    private Handler handler;

    private void resetVariables() {
        ourCard = null;
        ticketReady = false;
        fetchException = null;
        authRndA = null;
        authRndB = null;
        handler = new ServerResultHandler(getMainLooper());
    }

    public HCEService() {
    }

    public static final class HCEServiceUtils {
        public static final String SHAREDPREFS_HCE = "org.us.x42.kyork.idcard.ticket";
        public static final String READY_FOR_UPDATES = "update_mode";
        public static final String TICKET_STORAGE = "ticket";
        public static final String TICKET_DATE = "ticket_date";
        public static final long TICKET_VALIDITY = 5 * 60;
        public static final String UPDATE_BY_DOOR_PREFIX = "update_d[";

        public static SharedPreferences getStorage(Context context) {
            return context.getSharedPreferences(SHAREDPREFS_HCE, 0);
        }

        public static IDCard tryGetStoredTicket(SharedPreferences prefs) {
            long ticketDate = prefs.getLong(TICKET_DATE, 0);
            if (ticketDate == 0) {
                return null;
            }
            if ((new Date().getTime() - TICKET_VALIDITY) > ticketDate) {
                prefs.edit().remove(TICKET_STORAGE).remove(TICKET_DATE).apply();
                return null;
            }
            String encodedTicket = prefs.getString(TICKET_STORAGE, null);
            if (encodedTicket == null) {
                return null;
            }

            byte[] parceled = Base64.decode(encodedTicket, BASE64_FLAGS);
            Parcel p = Parcel.obtain();
            try {
                p.unmarshall(parceled, 0, parceled.length);
                return IDCard.CREATOR.createFromParcel(p);
            } finally {
                p.recycle();
            }
        }

        public static void storeTicket(SharedPreferences prefs, IDCard card) {
            Parcel p = Parcel.obtain();
            String value;
            try {
                card.writeToParcel(p, 0);
                byte[] parceled = p.marshall();
                value = Base64.encodeToString(parceled, BASE64_FLAGS);
            } finally {
                p.recycle();
            }
            prefs.edit().putString(TICKET_STORAGE, value).putLong(TICKET_DATE, card.fileMetadata.getProvisioningDate().getTime()).apply();
        }
    }

    private void replyApdu(byte[] reply) {
        Log.i(LOG_TAG, "RPL " + DESFireCard.stringifyByteArray(reply));
        sendResponseApdu(reply);
    }

    // if we cannot return immediately, return null and use sendCommandApdu() later
    public byte[] processCommandApdu(byte[] apdu, Bundle extra) {
        Log.i(LOG_TAG, "REQ " + DESFireCard.stringifyByteArray(apdu));
        if ((apdu[0] == 0x00) && (apdu[1] == (byte) 0xA4)) {
            // 7816 SELECT
            if (Arrays.equals(apdu, SELECT_APDU)) {
                Log.i(LOG_TAG, "Matching select, starting prep");
                this.prepare();

                return new byte[]{(byte) 0x91, 0x00};
            } else {
                Log.i(LOG_TAG, "Non-matching select, punting to other apps");
                notifyUnhandled();
                return null;
            }
        } else if (apdu[0] == (byte) 0x90) {
            byte[] reply = processDESFireCommand(apdu);
            Log.i(LOG_TAG, "RPL " + DESFireCard.stringifyByteArray(reply));
            return reply;
        } else {
            return new byte[]{(byte) 0x91, DESFireProtocol.StatusCode.COMMAND_ABORTED.getValue()};
        }
    }

    private byte[] processDESFireCommand(byte[] apdu) {
        if (apdu.length < 5) {
            return FRAMING_ERROR;
        }

        byte desfire_cmd = apdu[1];
        byte len = apdu[4];
        if (apdu.length != 6 + len) {
            return FRAMING_ERROR;
        }

        byte[] desfire_cmddata = Arrays.copyOfRange(apdu, 5, 5 + len);
        byte[] response;
        switch (desfire_cmd) {
            case DESFireProtocol.SELECT_APPLICATION:
                if (!Arrays.equals(desfire_cmddata, APP_ID_CARD42)) {
                    notifyUnhandled();
                    return new byte[]{(byte) 0x91, DESFireProtocol.StatusCode.APPLICATION_NOT_FOUND.getValue()};
                }
                return new byte[]{(byte) 0x91, 0x00};

            case DESFireProtocol.AUTHENTICATE:

                break;

            case DESFireProtocol.CUSTOM_IS_READY:
                response = new byte[3];
                response[1] = (byte)0x91;
                response[2] = 0x00;
                if (ticketReady) {
                    response[0] = 1;
                } else if (fetchException != null) {
                    return ERROR_GENERIC;
                } else {
                    response[0] = 2; // pls wait
                }
                return response;

            case DESFireProtocol.READ_DATA:
                byte fileno = desfire_cmddata[0];
                int offset = PackUtil.readLE24(desfire_cmddata, 1);
                byte length = desfire_cmddata[4];

                AbstractCardFile file = ourCard.getFileByID(fileno);
                if (file == null) {
                    return new byte[]{(byte) 0x91, DESFireProtocol.StatusCode.FILE_NOT_FOUND.getValue()};
                }

                byte[] data = file.getRawContent();
                if (offset >= data.length || (offset + length) > data.length) {
                    return new byte[]{(byte) 0x91, DESFireProtocol.StatusCode.BOUNDARY_ERROR.getValue()};
                }

                response = new byte[length + 2];
                System.arraycopy(data, offset, response, 0, length);
                response[response.length - 2] = (byte) 0x91;
                response[response.length - 1] = (byte) 0x00;
                return response;
            // ...
        }
        return new byte[]{(byte) 0x91, DESFireProtocol.StatusCode.COMMAND_ABORTED.getValue()};
    }

    public void onDeactivated(int deactivationMode) {
        // TODO abort jobs?
    }

    /**
     * A ticket has been fetched from the server and stored in SharedPreferences.
     * @param IDCard the card object
     */
    private static final int MSG_TICKET_OBTAINED = 1;

    /**
     * An error was encountered fetching a ticket from the server.
     * @param Throwable an IOException
     */
    private static final int MSG_TICKET_OBTAIN_FAIL = 2;

    /**
     * We are contacting the server to get the ticket.
     */
    private static final int MSG_PROGRESS_GETTING_TICKET = 3;

    class ServerResultHandler extends Handler {
        ServerResultHandler(Looper looper) { super(looper); }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_TICKET_OBTAINED) {
                Log.i(LOG_TAG, "Got ticket from server");
                ourCard = (IDCard) msg.obj;
                ticketReady = true;
            } else if (msg.what == MSG_TICKET_OBTAIN_FAIL) {
                fetchException = (IOException) msg.obj;
            } else if (msg.what == MSG_PROGRESS_GETTING_TICKET) {
                Log.i(LOG_TAG, "Contacting server to get a ticket");
                // TODO blocked on showing a service Activity
            }
        }
    }

    private void prepare() {
        resetVariables();

        SharedPreferences prefs = HCEServiceUtils.getStorage(this);
        IDCard card = HCEServiceUtils.tryGetStoredTicket(prefs);
        if (card != null) {
            ticketReady = true;
            this.ourCard = card;
            Log.i(LOG_TAG, "Got ticket from cache");
        } else {
            ourCard = new IDCard();
            ourCard.fileMetadata = FileMetadata.createTicketMetadataFile();
            ticketReady = false;

            // TODO executor service
            new Thread(new Runnable() {
                @Override
                public void run() {
                    handler.sendMessage(Message.obtain(handler, MSG_PROGRESS_GETTING_TICKET));
                    try {
                        IDCard ticket = ServerAPIFactory.getAPI().getTicketForLogin(TEST_ACCT_LOGIN, ourCard.fileMetadata);
                        HCEServiceUtils.storeTicket(HCEServiceUtils.getStorage(HCEService.this), ticket);
                        handler.sendMessage(Message.obtain(handler, MSG_TICKET_OBTAINED, ticket));
                    } catch (IOException e) {
                        handler.sendMessage(Message.obtain(handler, MSG_TICKET_OBTAIN_FAIL, e));
                    }
                }
            }).start();
        }
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
