package test.update.generateoperation;

import uniregistrar.openapi.model.UpdateRequest;

public interface GenerateOperationStrategy {

    void generateOperation(UpdateRequest updateRequest);
}
