package test.update.generateoperation;

import uniregistrar.openapi.model.DidDocument;
import uniregistrar.openapi.model.UpdateRequest;

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
        return Map.of(
                "id", "#dwn",
                "type", "DecentralizedWebNode",
                "serviceEndpoint", "https://example.com/dwn/" + UUID.randomUUID()
        );
    }
}
