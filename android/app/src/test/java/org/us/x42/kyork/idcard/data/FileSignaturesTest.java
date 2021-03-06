package org.us.x42.kyork.idcard.data;

import com.google.common.io.BaseEncoding;

import org.junit.Test;
import org.us.x42.kyork.idcard.HexUtil;
import org.us.x42.kyork.idcard.ServerAPIDebug;

import java.util.Arrays;

public class FileSignaturesTest {
    @Test
    public void getPublicKeyBytes() throws Exception {
        System.out.println(
                BaseEncoding.base16().encode(FileSignatures.knownSigners.get(0x545354).getEncoded()));

    }


    @Test
    public void testBlake2sVector() throws Exception {
        byte[] buf = new byte[64];

        Blake2sMessageDigest engine = new Blake2sMessageDigest(16, ServerAPIDebug.MAC_KEY_DEV);
        Arrays.fill(buf, (byte)1);
        engine.engineUpdate(buf, 0, 64);
        Arrays.fill(buf, (byte)2);
        engine.engineUpdate(buf, 0, 56);

        byte[] result = engine.engineDigest();
        System.out.println(HexUtil.stringifyByteArray(Arrays.copyOfRange(result, 0x00, 0x10)));

        engine.destroy();
    }
}