package org.us.x42.kyork.idcard;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.common.io.BaseEncoding;

public class CardJob implements Parcelable {
    // DESFire Application ID values.
    public static final int APP_ID_NULL = 0;
    public static final int APP_ID_CARD42 = 0xFB9852;

    // Encryption key values.
    public static final byte[] ENC_KEY_NONE = null;
    public static final byte[] ENC_KEY_MASTER_TEST = decodeHex("EE7D54123E4349058AC318B18197405728CC82D5C4A3C9D8");
    public static final byte[] ENC_KEY_NULL = decodeHex("000000000000000000000000000000000000000000000000");
    public static final byte[] ENC_KEY_ANDROID_PUBLIC = decodeHex("BEEC8A3E031C969BC7ADC092F5F4DA02D1BF6427E9B6B54C");

    public static final byte AUTHENTICATE = (byte) 0x0A;
    public static final byte GET_MANUFACTURING_DATA = (byte) 0x60;
    public static final byte GET_APPLICATION_DIRECTORY = (byte) 0x6A;
    public static final byte GET_ADDITIONAL_FRAME = (byte) 0xAF;
    public static final byte SELECT_APPLICATION = (byte) 0x5A;
    public static final byte READ_DATA = (byte) 0xBD;
    public static final byte READ_RECORD = (byte) 0xBB;
    public static final byte GET_VALUE = (byte) 0x6C;
    public static final byte GET_FILES = (byte) 0x6F;
    public static final byte GET_FILE_SETTINGS = (byte) 0xF5;
    public static final byte CHANGE_KEY_SETTINGS = (byte) 0x54;
    public static final byte GET_KEY_SETTINGS = (byte) 0x45;

    public int appId;
    public byte[] encKey;
    public CardOp[] commands;

    public CardJob(int appId, byte[] encKey, CardOp... commands) {
        this.appId = appId;
        this.encKey = encKey;
        this.commands = commands;
    }

    /// ===============
    // Parcel Implementation

    private CardJob(Parcel in) {
        this.appId = in.readInt();
        boolean hasEncKey = in.readByte() == 1;
        if (hasEncKey) {
            this.encKey = new byte[24];
            in.readByteArray(encKey);
        }
        int numCommands = in.readInt();
        this.commands = new CardOp[numCommands];
        for (int i = 0; i < this.commands.length; i++) {
            byte cmdId;
            int dataLen;
            byte[] data;

            cmdId = in.readByte();
            dataLen = in.readInt();
            data = new byte[dataLen];
            in.readByteArray(data);
            this.commands[i] = new CardOpRaw(cmdId, data);
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.appId);
        if (this.encKey != null) {
            dest.writeByte((byte)1);
            dest.writeByteArray(this.encKey);
        } else {
            dest.writeByte((byte)0);
        }
        dest.writeInt(this.commands.length);
        for (CardOp command : this.commands) {
            byte cmdId = command.getCommandId();
            byte[] data = command.encode();
            dest.writeByte(cmdId);
            dest.writeInt(data.length);
            dest.writeByteArray(data);
        }
    }

    public static final Creator<CardJob> CREATOR = new Creator<CardJob>() {
        @Override
        public CardJob createFromParcel(Parcel in) {
            return new CardJob(in);
        }

        @Override
        public CardJob[] newArray(int size) {
            return new CardJob[size];
        }
    };

    @Override
    public int describeContents() { return 0; }

    // End Parcel Implementation
    /// ===============

    public interface CardOp {
        byte getCommandId();
        byte[] encode();
    }

    public static class CardOpRaw implements CardOp {
        private byte cmdId;
        private byte[] data;

        public CardOpRaw(byte cmdId, byte[] data) {
            this.cmdId = cmdId;
            this.data = data;
        }

        @Override
        public byte getCommandId() {
            return cmdId;
        }

        @Override
        public byte[] encode() {
            return data;
        }
    }

    private static byte[] decodeHex(String hex) {
        return BaseEncoding.base16().decode(hex);
    }
}
