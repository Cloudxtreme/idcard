package org.us.x42.kyork.idcard.tasks;

import android.os.Parcel;
import android.util.Log;

import org.us.x42.kyork.idcard.CardJob;
import org.us.x42.kyork.idcard.PackUtil;
import org.us.x42.kyork.idcard.data.CardDataFormat;
import org.us.x42.kyork.idcard.data.FileMetadata;
import org.us.x42.kyork.idcard.desfire.DESFireCard;
import org.us.x42.kyork.idcard.desfire.DESFireProtocol;

import java.io.IOException;
import java.util.Date;
import java.util.List;


public class ProvisionBlankCardTask extends CardNFCTask {
    private static final String LOG_TAG = ProvisionBlankCardTask.class.getSimpleName();
    private int currentStep;
    private long provisioningDate;
    private String errorString;

    public ProvisionBlankCardTask(int resumeFromStep) {
        currentStep = resumeFromStep;
    }

    private void setError(String prefix, Throwable t) {
        if (prefix.isEmpty()) {
            errorString = t.getMessage();
        } else {
            errorString = prefix + ": " + t.getMessage();
        }
    }

    @Override
    protected List<Object> doInBackground(Object... params) {
        try {
            super.setUpCard();

            mCard.selectApplication(0);
            byte[] infoResponse = mCard.sendRequest(DESFireProtocol.GET_MANUFACTURING_DATA, null);
            byte[] appListResponse = mCard.sendRequest(DESFireProtocol.GET_APPLICATION_DIRECTORY, null);

            long serial = PackUtil.readLE56(infoResponse, 14);

            // TODO - if app list contains Card42, abort

            if (!hasApp(appListResponse, CardJob.APP_ID_CARD42)) {
                // CreateApplication
                try {
                    Log.i(LOG_TAG, "Creating application");
                    byte[] createApplicationData = new byte[5];
                    PackUtil.writeBE24(createApplicationData, 0, CardJob.APP_ID_CARD42);
                    // ChangeKey = E
                    // Free access / not frozen = F
                    createApplicationData[4] = (byte)0xEF;
                    createApplicationData[5] = 5; // Number of keys
                    mCard.sendRequest(DESFireProtocol.CREATE_APPLICATION, createApplicationData);
                } catch (DESFireCard.CardException e) {
                    if (e.getErrorCode() == DESFireProtocol.StatusCode.DUPLICATE_ERROR.getValue()) {
                        // ok
                    } else {
                        Log.e(LOG_TAG, "Failed to CreateApplication", e);
                        setError("Failed to CreateApplication", e);
                        return null;
                    }
                }
            } else {
                Log.i(LOG_TAG, "Card already has application");
            }

            mCard.selectApplication(CardJob.APP_ID_CARD42);

            // CreateFile
            for (CardDataFormat.FileFormatInfo info : CardDataFormat.files) {
                byte[] createFileData = new byte[7];
                createFileData[0] = (byte)info.fileID;
                createFileData[1] = DESFireProtocol.FileEncryptionMode.PLAIN.getValue();
                PackUtil.writeLE16(createFileData, 2, (short)0xEEEE);
                PackUtil.writeLE24(createFileData, 4, info.expectedSize);
                Log.i(LOG_TAG, "Creating file " + info.dfnClass.getSimpleName());
                try {
                    switch (info.fileType) {
                        case DESFireProtocol.FILETYPE_STANDARD:
                            mCard.sendRequest(DESFireProtocol.CREATE_STDDATA_FILE, createFileData);
                            break;
                        case DESFireProtocol.FILETYPE_BACKUP:
                            mCard.sendRequest(DESFireProtocol.CREATE_BACKUP_FILE, createFileData);
                            break;
                    }
                } catch (DESFireCard.CardException e) {
                    if (e.getErrorCode() == DESFireProtocol.StatusCode.DUPLICATE_ERROR.getValue()) {
                        Log.i(LOG_TAG, "(file already exists)");
                        // ok
                    } else {
                        Log.e(LOG_TAG, "Failed to CreateFile " + info.dfnClass.getSimpleName(), e);
                        setError("Failed to CreateFile " + info.dfnClass.getSimpleName(), e);
                        return null;
                    }
                }
            }

            provisioningDate = new Date().getTime();

            // Write provisioning file
            // TODO(apuel): convert to 'new FileMetadata(id_card)' ?
            byte[] provisioningFileContents = new byte[16];
            PackUtil.writeLE64(provisioningFileContents, 0, provisioningDate);
            PackUtil.writeLE16(provisioningFileContents, 8, CardDataFormat.SCHEMA_ID);
            PackUtil.writeBE16(provisioningFileContents, 0xa, FileMetadata.DEVICE_TYPE_ID);

            try {
                mCard.writeToFile(DESFireProtocol.FileEncryptionMode.PLAIN,
                        FileMetadata.FILE_ID,
                        provisioningFileContents, 0);
            } catch (DESFireCard.CardException e) {
                Log.e(LOG_TAG, "Failed to write metadata file", e);
                setError("Failed to write metadata file", e);
                return null;
            }

            // TODO - post to server that we did this

            Log.i(LOG_TAG, "provision done");

        } catch (IOException e) {
            setError("Communication error", e);
        }
        return null;
    }

    private boolean hasApp(byte[] appList, int appId) {
        for (int off = 0; off < appList.length; off += 3) {
            if (PackUtil.readBE24(appList, off) == appId) {
                return true;
            }
        }
        return false;
    }

    private ProvisionBlankCardTask(Parcel in) {
        currentStep = in.readInt();
        provisioningDate = in.readLong();
        errorString = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(currentStep);
        parcel.writeLong(provisioningDate);
        parcel.writeString(errorString);
    }

    public static final Creator<ProvisionBlankCardTask> CREATOR = new Creator<ProvisionBlankCardTask>() {
        @Override
        public ProvisionBlankCardTask createFromParcel(Parcel in) {
            return new ProvisionBlankCardTask(in);
        }

        @Override
        public ProvisionBlankCardTask[] newArray(int size) {
            return new ProvisionBlankCardTask[size];
        }
    };

    // Result Getters

    public String getErrorString() { return errorString; }
}
