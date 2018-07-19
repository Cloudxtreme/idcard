package org.us.x42.kyork.idcard.data;

import com.google.common.io.BaseEncoding;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by kyork on 7/19/18.
 */
public class FileSignaturesTest {
    @Test
    public void getPublicKeyBytes() throws Exception {
        System.out.println(
                BaseEncoding.base16().encode(FileSignatures.knownSigners.get(0x545354).getEncoded()));

    }


    @Test
    public void validateSignature() throws Exception {
    }

}