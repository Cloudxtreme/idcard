package org.us.x42.kyork.idcard.tasks;


import android.content.Context;
import android.os.Parcel;
import android.util.Log;

import org.us.x42.kyork.idcard.CardJob;
import org.us.x42.kyork.idcard.R;
import org.us.x42.kyork.idcard.data.AbstractCardFile;
import org.us.x42.kyork.idcard.data.CardDataFormat;
import org.us.x42.kyork.idcard.data.FileDoorPermissions;
import org.us.x42.kyork.idcard.data.FileMetadata;
import org.us.x42.kyork.idcard.data.FileSignatures;
import org.us.x42.kyork.idcard.data.FileUserInfo;
import org.us.x42.kyork.idcard.data.IDCard;
import org.us.x42.kyork.idcard.desfire.DESFireCard;
import org.us.x42.kyork.idcard.desfire.DESFireProtocol;

import java.io.IOException;
import java.util.List;

public class WriteCardTask extends CardNFCTask {
    private static final String LOG_TAG = WriteCardTask.class.getSimpleName();

    private IDCard cardToWrite;
    private String errorString;
    private int errorStringResource;

    public WriteCardTask(IDCard modifiedCard) {
        cardToWrite = modifiedCard;
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
    protected List<Object> doInBackground(Object... params) {
        try {
            AbstractCardFile files[] = new AbstractCardFile[] {cardToWrite.fileUserInfo, cardToWrite.fileDoorPermissions};
            boolean anyDirty = false;

            // Sign files
            // TODO(kyork): this becomes an HTTP request to the signing server, or handled in the caller
            for (AbstractCardFile f : files) {
                if (!f.isDirty()) continue;
                cardToWrite.fileSignatures.setSignature(
                        (byte)f.getFileID(),
                        FileSignatures.KEYID_DEBUG,
                        FileSignatures.signForDebug(f.getRawContent()));
                anyDirty = true;
            }

            if (!anyDirty) {
                // Nothing to write
                return null;
            }

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

            mCard.establishAuthentication((byte)0, CardJob.ENC_KEY_NULL);

            // Write files
            for (AbstractCardFile f : files) {
                if (!f.isDirty()) continue;

                mCard.writeToFile(
                        DESFireProtocol.FileEncryptionMode.PLAIN,
                        (byte)f.getFileID(),
                        f.getRawContent(), 0);
            }


            // Write signatures
            for (int[] range : cardToWrite.fileSignatures.getDirtyRanges()) {
                mCard.writeToFile(
                        DESFireProtocol.FileEncryptionMode.PLAIN,
                        FileSignatures.FILE_ID,
                        cardToWrite.fileSignatures.getSlice(range[0], range[0] + range[1]),
                        range[0]);
            }

            // Commit
            mCard.sendRequest(DESFireProtocol.COMMIT_TRANSACTION, null);

            return null;
        } catch (DESFireCard.CardException e) {
            setError("Card communication error: " + e.getStatusCode().toString());
        } catch (Exception e) {
            setError(e.getClass().getName() + ": " + e.getLocalizedMessage());
        }
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
