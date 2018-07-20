package org.us.x42.kyork.idcard.tasks;

import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;

import org.us.x42.kyork.idcard.R;
import org.us.x42.kyork.idcard.desfire.DESFireCard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Abstract base class for NFC Card AsyncTasks.
 */
public abstract class CardNFCTask extends AsyncTask<Object, String, List<Object>> implements Parcelable {
    protected Tag mTag;
    protected Handler mHandler;

    protected DESFireCard mCard;

    public static final int MSG_ID_NFC_STATUS = 23;
    public static final int MSG_ID_NFC_DONE = 24;

    protected CardNFCTask() {
    }

    /**
     * Must be called before executing the task.
     *
     * @param tag Android Tag value of a recently seen NFC card.
     */
    public void setTagAndHandler(Tag tag, Handler handler) {
        this.mTag = tag;
        this.mHandler = handler;
    }

    public static String stringifyByteArray(byte[] data) {
        if (data == null) {
            return "(null)";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        sb.append(' ');
        for (byte d : data) {
            sb.append((int) d);
            sb.append(' ');
        }
        sb.append(']');
        return sb.toString();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (this.mTag == null) {
            throw new IllegalStateException("CardNFCTask started without a tag");
        }
    }

    @Override
    protected void onProgressUpdate(String... statuses) {
        for (String statusMessageId : statuses) {
            switch (statusMessageId) {
                case "start":
                    Message.obtain(this.mHandler, MSG_ID_NFC_STATUS, R.string.nfc_open, 0).sendToTarget();
                case "found":
                    Message.obtain(this.mHandler, MSG_ID_NFC_STATUS, R.string.nfc_found, 0).sendToTarget();
                case "done":
                    Message.obtain(this.mHandler, MSG_ID_NFC_STATUS, R.string.nfc_done, 0).sendToTarget();
                default:
                    Message.obtain(this.mHandler, MSG_ID_NFC_STATUS, statusMessageId).sendToTarget();
            }
        }
    }

    @Override
    protected void onPostExecute(List<Object> result) {
        super.onPostExecute(result);
        Message.obtain(this.mHandler, MSG_ID_NFC_DONE).sendToTarget();
    }

    protected void setUpCard() throws IOException {
        mCard = new DESFireCard(mTag);
        mCard.connect();
    }

    abstract protected List<Object> doInBackground(Object... params);
}
