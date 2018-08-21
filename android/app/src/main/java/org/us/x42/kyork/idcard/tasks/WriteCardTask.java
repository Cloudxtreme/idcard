package org.us.x42.kyork.idcard.tasks;


import android.content.Context;
import android.os.Parcel;
import android.util.Log;

import com.google.common.collect.ImmutableList;

import org.us.x42.kyork.idcard.CardJob;
import org.us.x42.kyork.idcard.ProgressStep;
import org.us.x42.kyork.idcard.R;
import org.us.x42.kyork.idcard.ServerAPI;
import org.us.x42.kyork.idcard.ServerAPIDebug;
import org.us.x42.kyork.idcard.ServerAPIFactory;
import org.us.x42.kyork.idcard.data.AbstractCardFile;
import org.us.x42.kyork.idcard.data.CardDataFormat;
import org.us.x42.kyork.idcard.data.FileDoorPermissions;
import org.us.x42.kyork.idcard.data.FileMetadata;
import org.us.x42.kyork.idcard.data.FileSignatures;
import org.us.x42.kyork.idcard.data.FileUserInfo;
import org.us.x42.kyork.idcard.data.IDCard;
import org.us.x42.kyork.idcard.desfire.DESFireCard;
import org.us.x42.kyork.idcard.desfire.DESFireProtocol;

import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * Update the contents of an ID Card.
 */
public class WriteCardTask extends CardNFCTask {
    private static final String LOG_TAG = WriteCardTask.class.getSimpleName();

    private IDCard cardToWrite;
    private String errorString;
    private int errorStringResource;

    public WriteCardTask() {
        cardToWrite = null;
    }
    public WriteCardTask(IDCard updateContent) {
        cardToWrite = updateContent;
    }

    private void setError(String error) {
        errorString = error;
        errorStringResource = 0;
    }

    private void setError(int rsc) {
        errorStringResource = rsc;
        errorString = null;
    }

    @Override
    public List<ProgressStep> getListOfSteps() {
        return ImmutableList.of(
                new ProgressStep.WithDoneText(R.string.nfc_generic_findcard, R.string.nfc_generic_findcard_done, R.string.nfc_generic_findcard_fail),
                new ProgressStep(R.string.nfc_update_step1_server),
                new ProgressStep(R.string.nfc_update_step2),
                new ProgressStep(R.string.nfc_update_step3_server)
        );
    }

    @Override
    protected List<Object> doInBackground(Object... params) {
        try {
            // TODO maybe prefetch card contents?

            this.setUpCard();

            try {
                mCard.selectApplication(CardJob.APP_ID_CARD42);
            } catch (DESFireCard.CardException e) {
                if (e.getErrorCode() == DESFireProtocol.StatusCode.APPLICATION_NOT_FOUND.getValue()) {
                    setError(R.string.incomingscan_err_not_card42);
                    return null;
                }
                throw e;
            }

            stepProgress(1, ProgressStep.STATE_WORKING);
            FileUserInfo userFile = new FileUserInfo(mCard.readFullFile(FileUserInfo.FILE_ID, FileUserInfo.SIZE));
            if (cardToWrite == null || cardToWrite.fileUserInfo.getCardSerialRepeat() != userFile.getCardSerialRepeat()) {
                try {
                    cardToWrite = ServerAPIFactory.getAPI().getCardUpdates(mTag.getId(), userFile.getLastUpdated());
                } catch (Exception e) {
                    e.printStackTrace();
                    // TODO report error
                    return null;
                }
            }
            stepProgress(1, ProgressStep.STATE_DONE);

            if (cardToWrite == null) {
                Log.i(LOG_TAG, "Card is up to date");
                getListOfSteps().get(2).text = R.string.nfc_update_step2b;
                stepProgress(2, ProgressStep.STATE_DONE);
                return null;
            }

            // TODO switch to Android public key
            mCard.establishAuthentication((byte)0, CardJob.ENC_KEY_NULL);

            stepProgress(2, ProgressStep.STATE_WORKING);
            // Write files
            for (AbstractCardFile f : cardToWrite.files()) {
                if (f == null) continue;

                mCard.writeToFile(
                        DESFireProtocol.FileEncryptionMode.PLAIN,
                        (byte)f.getFileID(),
                        f.getRawContent(), 0);
            }

            // Commit
            mCard.sendRequest(DESFireProtocol.COMMIT_TRANSACTION, null);
            stepProgress(2, ProgressStep.STATE_DONE);


            stepProgress(3, ProgressStep.STATE_WORKING);
            // TODO contact server
            try {
                Thread.sleep(350);
            } catch (InterruptedException ignored) {}
            stepProgress(3, ProgressStep.STATE_DONE);

            return null;
        } catch (DESFireCard.CardException e) {
            setError("Card communication error: " + e.getStatusCode().toString());
        } catch (Exception e) {
            setError(e.getClass().getName() + ": " + e.getLocalizedMessage());
        }
        stepProgress(curStep, ProgressStep.STATE_FAIL);
        return null;
    }

    private WriteCardTask(Parcel in) {
        cardToWrite = in.readParcelable(WriteCardTask.class.getClassLoader());
        errorString = in.readString();
        errorStringResource = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(cardToWrite, 0);
        parcel.writeString(this.errorString);
        parcel.writeInt(this.errorStringResource);
    }

    public static final Creator<WriteCardTask> CREATOR = new Creator<WriteCardTask>() {
        @Override
        public WriteCardTask createFromParcel(Parcel in) {
            return new WriteCardTask(in);
        }

        @Override
        public WriteCardTask[] newArray(int size) {
            return new WriteCardTask[size];
        }
    };

    // Getters

    public IDCard getCard() {
        return cardToWrite;
    }

    public String getErrorString(Context context) {
        if (errorStringResource != 0) {
            return context.getResources().getString(errorStringResource);
        }
        if (errorString != null) {
            return errorString;
        }
        return "";
    }

}
