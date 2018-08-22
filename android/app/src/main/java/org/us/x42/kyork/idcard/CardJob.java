package org.us.x42.kyork.idcard;

import org.us.x42.kyork.idcard.desfire.DESFireCard;

import java.io.IOException;

public class CardJob {
    // DESFire Application ID values.
    public static final int APP_ID_NULL = 0;
    public static final int APP_ID_CARD42 = 0xFB9852;
    public static final String ISO_APPID_CARD42 = "FB436172643432";

    // Encryption key values.
    public static final byte[] ENC_KEY_NONE = null;
    public static final byte[] ENC_KEY_MASTER_TEST = HexUtil.decodeHex("FBCE1357BAC06934167B1987DF09CFAF");
    public static final byte[] ENC_KEY_NULL = HexUtil.decodeHex("00000000000000000000000000000000");
    public static final byte[] ENC_KEY_ANDROID_PUBLIC = HexUtil.decodeHex("5BF8127E692E3F65CF8B78C79762E27A");

    public interface CardOp {
        void execute(DESFireCard card) throws IOException;
    }

    // Gutted, replaced by tasks.CommandTestTask

}
