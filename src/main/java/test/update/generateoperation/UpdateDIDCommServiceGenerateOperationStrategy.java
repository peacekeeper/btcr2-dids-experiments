package test.update.generateoperation;

import uniregistrar.openapi.model.DidDocument;
import uniregistrar.openapi.model.UpdateRequest;

import java.util.Map;
import java.util.UUID;

public class UpdateDIDCommServiceGenerateOperationStrategy implements GenerateOperationStrategy {

    @Override
    public void generateOperation(UpdateRequest updateRequest) {
        DidDocument didDocumentRemove = new DidDocument();
        didDocumentRemove.addServiceItem(serviceRemove());
        updateRequest.addDidDocumentOperationItem("removeFromDidDocument");
        updateRequest.addDidDocumentItem(didDocumentRemove);
        DidDocument didDocumentAdd = new DidDocument();
        didDocumentAdd.addServiceItem(serviceAdd());
        updateRequest.addDidDocumentOperationItem("addToDidDocument");
        updateRequest.addDidDocumentItem(didDocumentAdd);
    }

    private static Map<String, Object> serviceRemove() {
        return Map.of(
                "id", "#didcomm"
        );
    }

    private static Map<String, Object> serviceAdd() {
        return Map.of(
                "id", "#didcomm",
                "type", "DIDCommMessaging",
                "serviceEndpoint", "https://example.com/didcomm/" + UUID.randomUUID().toString()
        );
    }
}
