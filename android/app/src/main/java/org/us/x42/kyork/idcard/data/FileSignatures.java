package org.us.x42.kyork.idcard.data;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.EdDSASecurityProvider;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveSpec;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.Signature;

public class FileSignatures extends AbstractCardFile {
    public FileSignatures(byte[] content) {
        super(content);
    }

    @Override
    public int getFileID() {
        return 0x7;
    }

    @Override
    public int getExpectedFileSize() {
        return 68 * 5;
    }

    static {
        Security.addProvider(new EdDSASecurityProvider());

    }

    public boolean validateSignature(byte fileID, byte[] fullFileContent) {
        try {
            Signature sigEngine = Signature.getInstance("NONEwithEdDSA", "EdDSA");
            new EdDSAPublicKey(new EdDSAPublicKeySpec(new byte[]{}, EdDSANamedCurveTable.getByName("Ed25519")));
//            sigEngine.initVerify();
        } catch (NoSuchAlgorithmException|NoSuchProviderException e) {
            throw new RuntimeException("failed to initialize signature code", e);
        }

        return false;
    }
}
