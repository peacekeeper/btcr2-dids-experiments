package test.update.generateoperation;

import foundation.identity.did.Service;
import uniregistrar.openapi.model.DidDocument;
import uniregistrar.openapi.model.UpdateRequest;

import java.net.URI;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class UpdateMyServiceGenerateOperationStrategy implements GenerateOperationStrategy {

    private static final Random RANDOM = new Random();

    private final int maxItems;

    public UpdateMyServiceGenerateOperationStrategy(int maxItems) {
        this.maxItems = maxItems;
    }

    @Override
    public void generateOperation(UpdateRequest updateRequest) {
        int item = RANDOM.nextInt(this.getMaxItems());
        DidDocument didDocumentRemove = new DidDocument();
        didDocumentRemove.addServiceItem(serviceRemove(item));
        updateRequest.addDidDocumentOperationItem("removeFromDidDocument");
        updateRequest.addDidDocumentItem(didDocumentRemove);
        DidDocument didDocumentAdd = new DidDocument();
        didDocumentAdd.addServiceItem(serviceAdd(item));
        updateRequest.addDidDocumentOperationItem("addToDidDocument");
        updateRequest.addDidDocumentItem(didDocumentAdd);
    }

    public int getMaxItems() {
        return maxItems;
    }

    private static Map<String, Object> serviceRemove(int item) {
        return Service.builder()
                .id(URI.create("#myservice"+ item))
                .build()
                .toMap();
    }

    private static Map<String, Object> serviceAdd(int item) {
        return Service.builder()
                .id(URI.create("#myservice" + item))
                .type(serviceType())
                .serviceEndpoint(URI.create("https://example.com/myservice/" + UUID.randomUUID().toString()))
                .build()
                .toMap();
    }

    private static String serviceType() {
        return switch (RANDOM.nextInt(2)) {
            case 0 -> "DIDCommMessaging";
            case 1 -> "DecentralizedWebNode";
            default -> throw new IllegalStateException();
        };
    }
}
