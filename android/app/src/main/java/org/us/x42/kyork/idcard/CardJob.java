package org.us.x42.kyork.idcard;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.common.io.BaseEncoding;

import org.us.x42.kyork.idcard.data.AbstractCardFile;
import org.us.x42.kyork.idcard.desfire.DESFireCard;
import org.us.x42.kyork.idcard.desfire.DESFireProtocol;

import java.io.IOException;

public class CardJob {
    // DESFire Application ID values.
    public static final int APP_ID_NULL = 0;
    public static final int APP_ID_CARD42 = 0xFB9852;
    public static final String APP_IDSTR_CARD42 = "FB9852";

    // Encryption key values.
    public static final byte[] ENC_KEY_NONE = null;
    public static final byte[] ENC_KEY_MASTER_TEST = decodeHex("FBCE1357BAC06934167B1987DF09CFAF");
    public static final byte[] ENC_KEY_NULL = decodeHex("00000000000000000000000000000000");
    public static final byte[] ENC_KEY_ANDROID_PUBLIC = decodeHex("5BF8127E692E3F65CF8B78C79762E27A");

    public static final byte[] MAC_KEY_DEV = decodeHex("2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A2A");

    public interface CardOp {
        void execute(DESFireCard card) throws IOException;
    }

    // Gutted, replaced by tasks.CommandTestTask

    private static byte[] decodeHex(String hex) {
        return BaseEncoding.base16().decode(hex);
    }
}
