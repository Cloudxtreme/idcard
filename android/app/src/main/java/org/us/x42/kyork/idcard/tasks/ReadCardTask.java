package org.us.x42.kyork.idcard.tasks;


import android.content.Context;
import android.os.Parcel;
import android.util.Log;

import org.us.x42.kyork.idcard.CardJob;
import org.us.x42.kyork.idcard.R;
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

public class ReadCardTask extends CardNFCTask {
    private static final String LOG_TAG = ReadCardTask.class.getSimpleName();

    private IDCard cardOut;
    private String errorString;
    private int errorStringResource;

    public ReadCardTask() {
        cardOut = new IDCard();
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

            byte[] mfrData = mCard.sendRequest(DESFireProtocol.GET_MANUFACTURING_DATA, null);
            long serial = DESFireProtocol.getSerial(mfrData);
            Log.i(LOG_TAG, "Incoming card serial number: " + serial);

            FileMetadata fileMetadata = new FileMetadata(mCard.readFullFile(CardDataFormat.FORMAT_METADATA.fileID, CardDataFormat.FORMAT_METADATA.expectedSize));
            switch (fileMetadata.getDeviceType()) {
                case FileMetadata.DEVICE_TYPE_ID:
                    // Good
                    break;
                case FileMetadata.DEVICE_TYPE_CANTINA:
                    setError(R.string.incomingscan_err_is_cantina);
                    return null;
                case FileMetadata.DEVICE_TYPE_DOOR:
                    setError(R.string.incomingscan_err_is_door);
                    return null;
                case FileMetadata.DEVICE_TYPE_TICKET:
                    setError(R.string.incomingscan_err_is_ticket);
                    return null;
                case 0:
                    setError(R.string.incomingscan_err_is_blank);
                    return null;
                default:
                    setError(R.string.incomingscan_err_not_card42);
                    return null;
            }

            FileUserInfo fileUserInfo = new FileUserInfo(mCard.readFullFile(CardDataFormat.FORMAT_USERINFO.fileID, CardDataFormat.FORMAT_USERINFO.expectedSize));
            FileDoorPermissions fileDoorPermissions = new FileDoorPermissions(mCard.readFullFile(CardDataFormat.FORMAT_DOORPERMS.fileID, CardDataFormat.FORMAT_DOORPERMS.expectedSize));
            FileSignatures fileSignatures = new FileSignatures(mCard.readFullFile(CardDataFormat.FORMAT_SIGNATURES.fileID, CardDataFormat.FORMAT_SIGNATURES.expectedSize));

            try {
                // Metadata - not signed
                fileSignatures.validateSignature((byte)fileUserInfo.getFileID(), fileUserInfo.getRawContent());
                fileSignatures.validateSignature((byte)fileDoorPermissions.getFileID(), fileDoorPermissions.getRawContent());
                if (fileUserInfo.getCardSerialRepeat() != serial) {
                    throw new DESFireCard.CardException(DESFireProtocol.StatusCode.INTEGRITY_ERROR, "Serial numbers do not match");
                }
            } catch (DESFireCard.CardException e) {
                if (e.getStatusCode() == DESFireProtocol.StatusCode.INTEGRITY_ERROR) {
                    setError(R.string.incomingscan_err_validate_serial);
                } else if (e.getStatusCode() == DESFireProtocol.StatusCode.AUTHENTICATION_ERROR) {
                    setError(R.string.incomingscan_err_validate_sig);
                }
                return null;
            }

            // Done
            cardOut = new IDCard();
            cardOut.fileMetadata = fileMetadata;
            cardOut.fileUserInfo = fileUserInfo;
            cardOut.fileDoorPermissions = fileDoorPermissions;
            cardOut.fileSignatures = fileSignatures;
            return null;
        } catch (DESFireCard.CardException e) {
            setError("Card communication error: " + e.getStatusCode().toString());
        } catch (IOException e) {
            setError(e.getClass().getName() + ": " + e.getLocalizedMessage());
        }
        return null;
    }

    private ReadCardTask(Parcel in) {
        cardOut = in.readParcelable(this.getClass().getClassLoader());
        errorString = in.readString();
        errorStringResource = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(cardOut, 0);
        parcel.writeString(this.errorString);
        parcel.writeInt(this.errorStringResource);
    }

    public static final Creator<ReadCardTask> CREATOR = new Creator<ReadCardTask>() {
        @Override
        public ReadCardTask createFromParcel(Parcel in) {
            return new ReadCardTask(in);
        }

        @Override
        public ReadCardTask[] newArray(int size) {
            return new ReadCardTask[size];
        }
    };

    // Getters

    public IDCard getCard() {
        return cardOut;
    }

    public String getErrorString(Context context) {
        if (errorStringResource != 0) {
            return context.getResources().getString(errorStringResource);
        }
        return errorString;
    }

}
