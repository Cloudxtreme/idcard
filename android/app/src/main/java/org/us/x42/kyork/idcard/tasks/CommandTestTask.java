package org.us.x42.kyork.idcard.tasks;

import android.os.Parcel;

import org.us.x42.kyork.idcard.desfire.DESFireCard;

import java.util.List;

public class CommandTestTask extends CardNFCTask {
    private static final String LOG_TAG = CommandTestTask.class.getSimpleName();

    private int appId;
    private byte keyId;
    private byte[] encKey;
    private byte cmdId;
    private byte[] cmdData;

    private int errorCode = -2;
    private byte[] responseData;
    private String errorString;

    public CommandTestTask(int appId, byte keyId, byte[] encKey, byte cmdId, byte[] cmdData) {
        this.appId = appId;
        this.keyId = keyId;
        this.encKey = encKey;
        this.cmdId = cmdId;
        this.cmdData = cmdData;
    }

    @Override
    protected List<Object> doInBackground(Object... params) {
        try {
            super.setUpCard();

            mCard.selectApplication(appId);
            if (this.encKey != null) {
                mCard.establishAuthentication(this.keyId, this.encKey);
            }

            responseData = mCard.sendRequest(cmdId, cmdData);
            errorCode = 0;
        } catch (DESFireCard.CardException e) {
            errorCode = e.getErrorCode();
        } catch (Exception e) {
            errorString = e.getClass().getName() + " " + e.getMessage();
            errorCode = -1;
        }
        return null;
    }

    private CommandTestTask(Parcel in) {
        this.appId = in.readInt();
        this.keyId = in.readByte();
        this.cmdId = in.readByte();
        byte hasEncKey = in.readByte();
        if (hasEncKey == 1) {
            encKey = new byte[16];
            in.readByteArray(encKey);
        }
        int cmdDataLen = in.readInt();
        if (cmdDataLen > 0) {
            cmdData = new byte[cmdDataLen];
            in.readByteArray(cmdData);
        }
        byte hasResponse = in.readByte();
        if (hasResponse != 0) {
            if (hasResponse == 2) {
                int responseLength = in.readInt();
                responseData = new byte[responseLength];
                in.readByteArray(responseData);
            }
            errorCode = in.readInt();
            errorString = in.readString();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.appId);
        parcel.writeByte(this.keyId);
        parcel.writeByte(this.cmdId);
        if (encKey == null) {
            parcel.writeByte((byte)0);
        } else {
            parcel.writeByte((byte)1);
            parcel.writeByteArray(encKey);
        }
        if (cmdData == null) {
            parcel.writeInt(0);
        } else {
            parcel.writeInt(cmdData.length);
            parcel.writeByteArray(cmdData);
        }
        if (errorCode == -2) {
            // haven't run the task yet
            parcel.writeByte((byte)0);
        } else if (responseData == null) {
            parcel.writeByte((byte)1);
            parcel.writeInt(errorCode);
            parcel.writeString(errorString);
        } else {
            parcel.writeByte((byte)2);
            parcel.writeInt(responseData.length);
            parcel.writeByteArray(responseData);
            parcel.writeInt(errorCode);
            parcel.writeString(errorString);
        }
    }

    public static final Creator<CommandTestTask> CREATOR = new Creator<CommandTestTask>() {
        @Override
        public CommandTestTask createFromParcel(Parcel in) {
            return new CommandTestTask(in);
        }

        @Override
        public CommandTestTask[] newArray(int size) {
            return new CommandTestTask[size];
        }
    };

    public int getErrorCode() { return errorCode; }
    public String getErrorString() { return errorString; }
    public byte[] getResponseData() { return responseData; }
}
