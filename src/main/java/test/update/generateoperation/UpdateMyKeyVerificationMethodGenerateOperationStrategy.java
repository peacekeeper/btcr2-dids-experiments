package test.update.generateoperation;

import com.danubetech.keyformats.PublicKey_to_JWK;
import com.danubetech.keyformats.crypto.provider.Ed25519Provider;
import com.danubetech.keyformats.jose.JWK;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import foundation.identity.did.VerificationMethod;
import org.bitcoinj.crypto.ECKey;
import uniregistrar.openapi.model.DidDocument;
import uniregistrar.openapi.model.UpdateRequest;

import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.interfaces.ECPublicKey;
import java.util.Map;
import java.util.Random;

public class UpdateMyKeyVerificationMethodGenerateOperationStrategy implements GenerateOperationStrategy {

    private static final Random RANDOM = new Random();

    private final int maxItems;

    public UpdateMyKeyVerificationMethodGenerateOperationStrategy(int maxItems) {
        this.maxItems = maxItems;
    }

    @Override
    public void generateOperation(UpdateRequest updateRequest) {
        int item = RANDOM.nextInt(this.getMaxItems());
        DidDocument didDocumentRemove = new DidDocument();
        didDocumentRemove.addVerificationMethodItem(verificationMethodRemove(item));
        updateRequest.addDidDocumentOperationItem("removeFromDidDocument");
        updateRequest.addDidDocumentItem(didDocumentRemove);
        DidDocument didDocumentAdd = new DidDocument();
        didDocumentAdd.addVerificationMethodItem(verificationMethodAdd(item, updateRequest.getDid()));
        updateRequest.addDidDocumentOperationItem("addToDidDocument");
        updateRequest.addDidDocumentItem(didDocumentAdd);
    }

    public int getMaxItems() {
        return maxItems;
    }

    private static Map<String, Object> verificationMethodRemove(int item) {
        return VerificationMethod.builder()
                .id(URI.create("#testkey" + item))
                .build()
                .toMap();
    }

    private static Map<String, Object> verificationMethodAdd(int item, String did) {
        return VerificationMethod.builder()
                .id(URI.create("#testkey" + item))
                .type("JsonWebKey")
                .controller(URI.create(did))
                .publicKeyJwk(publicKeyJwk().toMap())
                .build()
                .toMap();
    }

    private static JWK publicKeyJwk() {
        return switch (RANDOM.nextInt(3)) {
            case 0 -> {
                byte[] publicKeyBytesBuffer = new byte[32];
                try {
                    Ed25519Provider.get().generateEC25519KeyPair(publicKeyBytesBuffer, new byte[64]);
                } catch (GeneralSecurityException ex) {
                    throw new RuntimeException(ex.getMessage(), ex);
                }
                yield PublicKey_to_JWK.Ed25519PublicKey_to_JWK(publicKeyBytesBuffer, null, null);
            }
            case 1 -> {
                com.nimbusds.jose.jwk.ECKey ecJWK = null;
                try {
                    ecJWK = new ECKeyGenerator(Curve.P_256).generate();
                } catch (JOSEException e) {
                    throw new RuntimeException(e);
                }
                ECPublicKey ecPublicKey;
                try {
                    ecPublicKey = ecJWK.toECPublicKey();
                } catch (JOSEException e) {
                    throw new RuntimeException(e);
                }
                yield PublicKey_to_JWK.P_256PublicKey_to_JWK(ecPublicKey, null, null);
            }
            case 2 -> {
                ECKey ecKey = new ECKey();
                ECKey newEcKey = ecKey.decompress();
                yield PublicKey_to_JWK.secp256k1PublicKey_to_JWK(newEcKey, null, null);
            }
            default -> throw new IllegalStateException();
        };
    }
}
