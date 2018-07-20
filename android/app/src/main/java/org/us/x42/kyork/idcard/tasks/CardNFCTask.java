package org.us.x42.kyork.idcard.tasks;

import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;

import org.us.x42.kyork.idcard.R;
import org.us.x42.kyork.idcard.desfire.DESFireCard;

import java.io.IOException;
import java.util.List;

/**
 * Abstract base class for NFC Card AsyncTasks.
 */
public abstract class CardNFCTask extends AsyncTask<Object, String, List<Object>> implements Parcelable {
    /**
     * The Android tag. Must be set before calling execute() (see {@link #setTagAndHandler(Tag, Handler)}).
     */
    protected Tag mTag;
    /**
     * The message Handler. Must be set before calling execute() (see {@link #setTagAndHandler(Tag, Handler)}).
     */
    protected Handler mHandler;

    /**
     * The DESFireCard. Initialize by calling {@link #setUpCard()}.
     */
    protected DESFireCard mCard;

    public static final int MSG_ID_NFC_STATUS = 23;
    public static final int MSG_ID_NFC_DONE = 24;

    CardNFCTask() {
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

    public void setUpCard() throws IOException {
        mCard = new DESFireCard(mTag);
        mCard.connect();
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

    abstract protected List<Object> doInBackground(Object... params);
}
