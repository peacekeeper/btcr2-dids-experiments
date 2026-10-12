package test.update.generateoperation;

import foundation.identity.did.Service;
import uniregistrar.openapi.model.DidDocument;
import uniregistrar.openapi.model.UpdateRequest;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

public class AddDWNServiceGenerateOperationStrategy implements GenerateOperationStrategy {

    @Override
    public void generateOperation(UpdateRequest updateRequest) {
        DidDocument didDocument = new DidDocument();
        didDocument.addServiceItem(service());
        updateRequest.addDidDocumentOperationItem("addToDidDocument");
        updateRequest.addDidDocumentItem(didDocument);
    }

    private static Map<String, Object> service() {
        return Service.builder()
                .id(URI.create("#dwn"))
                .type("DecentralizedWebNode")
                .serviceEndpoint(URI.create("https://example.com/dwn/" + UUID.randomUUID().toString()))
                .build()
                .toMap();
    }
}
