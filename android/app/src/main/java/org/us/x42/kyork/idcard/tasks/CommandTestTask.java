package org.us.x42.kyork.idcard.tasks;

import android.os.Parcel;

import org.us.x42.kyork.idcard.desfire.DESFireCard;

import java.io.IOException;
import java.util.List;

public class CommandTestTask extends CardNFCTask {
    private int appId;
    private byte keyId;
    private byte[] encKey;
    private byte cmdId;
    private byte[] cmdData;

    private int errorCode;
    private byte[] responseData;

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

            try {
                responseData = mCard.sendRequest(cmdId, cmdData);
            } catch (DESFireCard.CardException e) {
                responseData = new byte[0];
                errorCode = e.getErrorCode();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
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
            this.errorCode = in.readInt();
            int responseLength = in.readInt();
            responseData = new byte[responseLength];
            in.readByteArray(responseData);
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
        if (responseData == null) {
            parcel.writeByte((byte)0);
        } else {
            parcel.writeByte((byte)1);
            parcel.writeInt(errorCode);
            parcel.writeInt(responseData.length);
            parcel.writeByteArray(responseData);
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
    public byte[] getResponseData() { return responseData; }
}
