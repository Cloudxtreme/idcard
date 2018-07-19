package org.us.x42.kyork.idcard.data;

import android.util.Log;
import android.util.SparseArray;

import com.google.common.io.BaseEncoding;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.EdDSASecurityProvider;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.util.HashMap;

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

    public static final int MAX_SIGNATURE_COUNT = 5;
    public static final int SIGNATURE_LENGTH = 68;

//    public static SparseArray<EdDSAPublicKey> knownSigners = new SparseArray<>();
    public static HashMap<Integer, EdDSAPublicKey> knownSigners = new HashMap<>();
    private static EdDSAPrivateKey debugSigner;

    static {
        Security.addProvider(new EdDSASecurityProvider());

        debugSigner = new EdDSAPrivateKey(new EdDSAPrivateKeySpec(
                BaseEncoding.base16().decode("8AF5F616383DBA03A75BCA8054F3598EA4F752F90580D7F81B296ADD9F70D541"), EdDSANamedCurveTable.getByName("Ed25519")));
        EdDSAPublicKey debugPublic = new EdDSAPublicKey(new EdDSAPublicKeySpec(debugSigner.getA(), EdDSANamedCurveTable.getByName("Ed25519")));

        knownSigners.put(0x545354, new EdDSAPublicKey(new EdDSAPublicKeySpec(
                BaseEncoding.base16().decode("302A300506032B65700321002D34E2D9DE7E03A916CF04A5F4CB05355BB65C9C49A4C29D9239247193A8AC27"),
                EdDSANamedCurveTable.getByName("Ed25519"))));
        // TODO - replace with production signer
    }

    /**
     *
     * @param fileID ID of the file to validate
     * @param fullFileContent file to validate
     * @return key ID of the signer, or 0 on failure
     */
    public int validateSignature(byte fileID, byte[] fullFileContent) throws SecurityException {
        try {
            Signature sigEngine = Signature.getInstance("NONEwithEdDSA", "EdDSA");
            for (int i = 0; i < MAX_SIGNATURE_COUNT; i++) {
                int offset = i * SIGNATURE_LENGTH;
                byte sigFileID = getRawContent()[offset];
                if (sigFileID != fileID) continue;

                int signerID = readLE24(offset + 1);
                EdDSAPublicKey pubkey = knownSigners.get(signerID);
                if (pubkey == null) {
                    throw new SecurityException("Unknown signer ID " + signerID);
                }
                sigEngine.initVerify(pubkey);
                sigEngine.setParameter(EdDSAEngine.ONE_SHOT_MODE);
                try {
                    sigEngine.update(fullFileContent);
                    sigEngine.verify(getRawContent(), offset + 4, 64);
                    return signerID;
                } catch (SignatureException e) {
                    return 0;
                }
            }
            return 0;
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException("failed to initialize signature code", e);
        }
    }

    public void setSignature(byte fileID, int keyID, byte[] signature) {
        for (int i = 0; i < MAX_SIGNATURE_COUNT; i++) {
            int offset = i * SIGNATURE_LENGTH;
            byte sigFileID = getRawContent()[offset];
            if (sigFileID != fileID && sigFileID != 0) continue;

            // either a matching entry or empty entry
            byte[] newSignature = new byte[68];
            newSignature[0] = fileID;
            // writeLE24(newSignature, 1, keyID);
            // TODO(kyork)
        }
    }
}
