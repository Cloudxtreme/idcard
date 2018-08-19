package org.us.x42.kyork.idcard.data;

import android.os.Parcel;

import com.google.common.io.BaseEncoding;
import com.google.errorprone.annotations.CheckReturnValue;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.EdDSASecurityProvider;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

import org.us.x42.kyork.idcard.desfire.DESFireCard;
import org.us.x42.kyork.idcard.desfire.DESFireProtocol;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.util.HashMap;

/**
 * Interface to the Signatures file on the card.
 *
 * TODO(kyork): Switch to production server keys once it exists
 */
public class FileSignatures extends AbstractCardFile {
    public static final byte FILE_ID = (byte)0x07;

    public FileSignatures(byte[] content) {
        super(content);
    }

    protected FileSignatures(Parcel parcel) {
        super(parcel);
    }

    public static final Creator<FileSignatures> CREATOR = new Creator<FileSignatures>() {
        @Override
        public FileSignatures createFromParcel(Parcel in) {
            return new FileSignatures(in);
        }

        @Override
        public FileSignatures[] newArray(int size) {
            return new FileSignatures[size];
        }
    };

    public static FileSignatures newBlank() {
        return new FileSignatures(new byte[SIGNATURE_LENGTH * MAX_SIGNATURE_COUNT]);
    }

    @Override
    public int getFileID() {
        return 0x7;
    }

    @Override
    public int getExpectedFileSize() {
        return SIGNATURE_LENGTH * MAX_SIGNATURE_COUNT;
    }

    public static final int MAX_SIGNATURE_COUNT = 5;
    public static final int SIGNATURE_LENGTH = 68;

    public static final int KEYID_DEBUG = 0x545354;

//    public static SparseArray<EdDSAPublicKey> knownSigners = new SparseArray<>();
    public static HashMap<Integer, EdDSAPublicKey> knownSigners = new HashMap<>();
    private static EdDSAPrivateKey debugSigner;

    static {
        Security.addProvider(new EdDSASecurityProvider());

        debugSigner = new EdDSAPrivateKey(new EdDSAPrivateKeySpec(
                BaseEncoding.base16().decode("8AF5F616383DBA03A75BCA8054F3598EA4F752F90580D7F81B296ADD9F70D541"), EdDSANamedCurveTable.getByName("Ed25519")));
        EdDSAPublicKey debugPublic = new EdDSAPublicKey(new EdDSAPublicKeySpec(debugSigner.getA(), EdDSANamedCurveTable.getByName("Ed25519")));

        knownSigners.put(KEYID_DEBUG, new EdDSAPublicKey(new EdDSAPublicKeySpec(
                debugPublic.getA(), //BaseEncoding.base16().decode("302A300506032B65700321002D34E2D9DE7E03A916CF04A5F4CB05355BB65C9C49A4C29D9239247193A8AC27"),
                EdDSANamedCurveTable.getByName("Ed25519"))));
        // TODO - replace with production signer
    }

    public void validateSignature(byte fileID, byte[] fullFileContent) throws DESFireCard.CardException {
        int result = getSignature(fileID, fullFileContent);

        // TODO switch to production signatures
        switch (result) {
            case KEYID_DEBUG:
                return;
            case 0:
            default:
                throw new DESFireCard.CardException(DESFireProtocol.StatusCode.AUTHENTICATION_ERROR, "Signature failed validation");
        }
    }

    /**
     * Checks the signature on file contents.
     *
     * @param fileID ID of the file to validate
     * @param fullFileContent file to validate
     * @return key ID of the signer, or 0 on failure
     */
    @CheckReturnValue
    public int getSignature(byte fileID, byte[] fullFileContent) throws DESFireCard.CardException {
        try {
            Signature sigEngine = Signature.getInstance("NONEwithEdDSA", "EdDSA");
            for (int i = 0; i < MAX_SIGNATURE_COUNT; i++) {
                int offset = i * SIGNATURE_LENGTH;
                byte sigFileID = getRawContent()[offset];
                if (sigFileID != fileID) continue;

                int signerID = readLE24(offset + 1);
                EdDSAPublicKey pubkey = knownSigners.get(signerID);
                if (pubkey == null) {
                    throw new DESFireCard.CardException(DESFireProtocol.StatusCode.NO_SUCH_KEY, "Unknown signer ID " + signerID + " -- need to update app?");
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

    /**
     * Sign the given file content with the debug key.
     *
     * @param fullFileContent file content to sign
     * @return signature bytes with debug key
     */
    public static byte[] signForDebug(byte[] fullFileContent) {
        try {
            Signature sigEngine = Signature.getInstance("NONEwithEdDSA", "EdDSA");
            EdDSAPrivateKey privKey = debugSigner;

            sigEngine.initSign(privKey);
            sigEngine.setParameter(EdDSAEngine.ONE_SHOT_MODE);
            try {
                sigEngine.update(fullFileContent);
                return sigEngine.sign();
            } catch (SignatureException e) {
                throw new RuntimeException("failed to sign data", e);
            }
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException("failed to initialize signature code", e);
        }
    }

    /**
     * Saves a new signature to the file.
     * @param fileID file ID this signature is for
     * @param keyID Ed25519 key ID that signed it
     * @param signature the signature bytes
     */
    public void setSignature(byte fileID, int keyID, byte[] signature) {
        for (int i = 0; i < MAX_SIGNATURE_COUNT; i++) {
            int offset = i * SIGNATURE_LENGTH;
            byte sigFileID = getRawContent()[offset];
            if (sigFileID != fileID && sigFileID != 0) continue;

            // either a matching entry or empty entry
            getRawContent()[offset] = fileID;
            writeLE24(offset + 1, keyID);
            setSlice(offset + 4, signature, 0, 64);

            setDirty(offset, 68);
            return;
        }
    }
}
