package org.us.x42.kyork.idcard.tasks;

import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;

import com.google.common.collect.ImmutableList;

import org.us.x42.kyork.idcard.ProgressStep;
import org.us.x42.kyork.idcard.R;
import org.us.x42.kyork.idcard.desfire.DESFireCard;

import java.io.IOException;
import java.util.List;

/**
 * Abstract base class for NFC Card AsyncTasks.
 */
public abstract class CardNFCTask extends AsyncTask<Object, Message, List<Object>> implements Parcelable {
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

    /**
     * Progress update message.
     * @param num1 Index into the getListOfSteps()
     * @param num2 A ProgressStep STATE constant
     */
    public static final int MSG_ID_NFC_STATUS = 0x50;
    public static final int MSG_ID_NFC_DONE = 0x51;
    protected int curStep;

    CardNFCTask() {
    }

    public final void writeListOfSteps(List<ProgressStep> dest) {
        dest.clear();
        dest.addAll(getListOfSteps());
    }

    public List<ProgressStep> getListOfSteps() {
        return ImmutableList.<ProgressStep>of(
                new ProgressStep.WithDoneText(R.string.nfc_generic_findcard, R.string.nfc_generic_findcard_done, R.string.nfc_generic_findcard_fail),
                new ProgressStep.WithDoneText(R.string.nfc_generic_modify, R.string.nfc_generic_modify, R.string.nfc_generic_modify_fail)
        );
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
        this.stepProgress(0, ProgressStep.STATE_WORKING);
        mCard = new DESFireCard(mTag);
        mCard.connect();
        this.stepProgress(0, ProgressStep.STATE_DONE);
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
    protected void onProgressUpdate(Message... statuses) {
        for (Message statusMessage : statuses) {
            statusMessage.setTarget(mHandler);
            statusMessage.sendToTarget();
        }
    }

    /**
     * Utility wrapper around {@link #publishProgress(Object[])}
     *
     * @param stepID index into {@link #getListOfSteps()}
     * @param newState A constant from {@link ProgressStep}
     */
    protected final void stepProgress(int stepID, int newState) {
        publishProgress(Message.obtain(null, MSG_ID_NFC_STATUS, stepID, newState));
        if (newState == ProgressStep.STATE_WORKING) {
            curStep = stepID;
        }
    }

    @Override
    protected void onPostExecute(List<Object> result) {
        super.onPostExecute(result);
        Message.obtain(this.mHandler, MSG_ID_NFC_DONE).sendToTarget();
    }

    abstract protected List<Object> doInBackground(Object... params);
}
