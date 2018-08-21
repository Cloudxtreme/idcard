package org.us.x42.kyork.idcard.tasks;

import android.os.Parcel;
import android.util.Log;

import com.google.common.collect.ImmutableList;

import org.us.x42.kyork.idcard.CardJob;
import org.us.x42.kyork.idcard.IntraAPI;
import org.us.x42.kyork.idcard.PackUtil;
import org.us.x42.kyork.idcard.ProgressStep;
import org.us.x42.kyork.idcard.R;
import org.us.x42.kyork.idcard.ServerAPI;
import org.us.x42.kyork.idcard.ServerAPIFactory;
import org.us.x42.kyork.idcard.data.AbstractCardFile;
import org.us.x42.kyork.idcard.data.CardDataFormat;
import org.us.x42.kyork.idcard.data.FileMetadata;
import org.us.x42.kyork.idcard.data.IDCard;
import org.us.x42.kyork.idcard.desfire.DESFireCard;
import org.us.x42.kyork.idcard.desfire.DESFireProtocol;

import java.io.IOException;
import java.util.Date;
import java.util.List;


public class ProvisionBlankCardTask extends CardNFCTask {
    private static final String LOG_TAG = ProvisionBlankCardTask.class.getSimpleName();
    private boolean piccFormat;
    private String login;
    private long provisioningDate;
    private String errorString;

    public ProvisionBlankCardTask(String login, boolean piccFormat) {
        this.login = login;
        this.piccFormat = piccFormat;
    }

    private void setError(String prefix, Throwable t) {
        if (prefix.isEmpty()) {
            errorString = t.getMessage();
        } else if (t != null) {
            errorString = prefix + ": " + t.getMessage();
        } else {
            errorString = prefix;
        }
    }

    @Override
    public List<ProgressStep> getListOfSteps() {
        List<ProgressStep> steps = ImmutableList.of(
                new ProgressStep.WithDoneText(R.string.nfc_generic_findcard, R.string.nfc_generic_findcard_done, R.string.nfc_generic_findcard_fail),
                new ProgressStep(R.string.nfc_provision_step1a),
                new ProgressStep(R.string.nfc_provision_step2_server),
                new ProgressStep(R.string.nfc_provision_step3),
                new ProgressStep(R.string.nfc_provision_step4),
                new ProgressStep(R.string.nfc_provision_step5)
        );
        if (piccFormat) {
            steps.get(1).text = R.string.nfc_provision_step1b;
        }
        return steps;
    }

    @Override
    protected List<Object> doInBackground(Object... params) {
        this.stepProgress(0, ProgressStep.STATE_WORKING);
        try {
            super.setUpCard();

            stepProgress(1, ProgressStep.STATE_WORKING);
            mCard.selectApplication(0);
            byte[] infoResponse = mCard.sendRequest(DESFireProtocol.GET_MANUFACTURING_DATA, null);
            byte[] appListResponse;
            if (piccFormat) {
                mCard.sendRequest(DESFireProtocol.FORMAT_PICC, null);
            } else {
                appListResponse = mCard.sendRequest(DESFireProtocol.GET_APPLICATION_DIRECTORY, null);
                if (hasApp(appListResponse, CardJob.APP_ID_CARD42)) {
                    Log.i(LOG_TAG, "Card already has application");
                    setError("Card is already provisioned", null);
                    stepProgress(1, ProgressStep.STATE_FAIL);
                    return null;
                }
            }


            long serial = PackUtil.readLE56(infoResponse, 14);
            Date provisionDate = new Date();

            stepProgress(1, ProgressStep.STATE_DONE);
            stepProgress(2, ProgressStep.STATE_WORKING);
            Log.i(LOG_TAG, String.format("calling registerNewCard - %d %d %s", serial, provisionDate.getTime(), login));
            ServerAPIFactory.getAPI().registerNewCard(serial, provisionDate, login);

            IDCard cardContent = ServerAPIFactory.getAPI().getCardUpdates(serial, 0);
            if (cardContent == null) {
                Log.e(LOG_TAG, "ServerAPI returned null");
                return null;
            }

            stepProgress(2, ProgressStep.STATE_DONE);
            stepProgress(3, ProgressStep.STATE_WORKING);
            // CreateApplication
            try {
                Log.i(LOG_TAG, "Creating application");
                byte[] createApplicationData = new byte[5];
                PackUtil.writeBE24(createApplicationData, 0, CardJob.APP_ID_CARD42);
                // ChangeKey = E
                // Free access / not frozen = F
                createApplicationData[3] = (byte) 0xEF;
                createApplicationData[4] = 5; // Number of keys
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

            mCard.selectApplication(CardJob.APP_ID_CARD42);

            // CreateFile
            for (CardDataFormat.FileFormatInfo info : CardDataFormat.files) {
                byte[] createFileData = new byte[7];
                createFileData[0] = (byte) info.fileID;
                createFileData[1] = DESFireProtocol.FileEncryptionMode.PLAIN.getValue();
                PackUtil.writeLE16(createFileData, 2, (short) 0xEEEE);
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

            stepProgress(3, ProgressStep.STATE_DONE);
            stepProgress(4, ProgressStep.STATE_WORKING);

            try {
                for (AbstractCardFile f : cardContent.files()) {
                    // including FileMetadata
                    mCard.writeToFile(DESFireProtocol.FileEncryptionMode.PLAIN,
                            (byte) f.getFileID(),
                            f.getRawContent(), 0);
                }
            } catch (DESFireCard.CardException e) {
                Log.e(LOG_TAG, "Failed to write files", e);
                setError("Failed to write files", e);
                return null;
            }
            mCard.sendRequest(DESFireProtocol.COMMIT_TRANSACTION, null);

            mCard.changeFileAccess(FileMetadata.FILE_ID, DESFireProtocol.FileEncryptionMode.PLAIN, 0xE, 0xF, 0xF, 0xE, false);

            stepProgress(4, ProgressStep.STATE_DONE);
            stepProgress(5, ProgressStep.STATE_WORKING);
            ServerAPIFactory.getAPI().cardUpdatesApplied(serial, cardContent.fileUserInfo.getLastUpdated());
            stepProgress(5, ProgressStep.STATE_DONE);

            Log.i(LOG_TAG, "provision done");

        } catch (IOException e) {
            setError("Communication error", e);
            stepProgress(this.curStep, ProgressStep.STATE_FAIL);
            try {
                Thread.sleep(25);
            } catch (InterruptedException ignored) {
            }
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
        if (in.readByte() == 0x4C) {
            piccFormat = true;
        }
        login = in.readString();
        provisioningDate = in.readLong();
        errorString = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        if (piccFormat) {
            parcel.writeByte((byte) 0x4C);
        } else {
            parcel.writeByte((byte) 0);
        }
        parcel.writeString(login);
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

    public String getErrorString() {
        return errorString;
    }
}
