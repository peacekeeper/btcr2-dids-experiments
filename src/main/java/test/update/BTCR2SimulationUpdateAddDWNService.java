package test.update;

import com.danubetech.kms.clientkeyinterface.ClientKeyInterface;
import com.danubetech.kms.clientstateinterface.ClientStateInterface;
import com.danubetech.kms.clientstateinterface.impl.memory.InMemoryClientStateInterface;
import test.util.ExecutionUtil;
import uniregistrar.openapi.model.DidDocument;
import uniregistrar.openapi.model.RequestOptions;
import uniregistrar.openapi.model.UpdateRequest;
import uniregistrar.openapi.model.UpdateState;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

public class BTCR2SimulationUpdateAddDWNService {

    public static Collection<CompletableFuture<UpdateState>> update(ScheduledExecutorService executorService, ClientKeyInterface<?> clientKeyInterface, List<String> dids, Map<String, Map<String, Object>> didResolutionOptions, String beaconServiceType) throws Exception {

        Collection<CompletableFuture<UpdateState>> futures = new ArrayList<>();

        for (int i=0; i<dids.size(); i++) {

            final String did = dids.get(i);
            final Map<String, Object> resolutionOptions = didResolutionOptions.get(did);

            CompletableFuture<UpdateState> future = new CompletableFuture<>();
            futures.add(future);

            RequestOptions requestOptions = new RequestOptions();
            requestOptions.setClientSecretMode(Boolean.TRUE);
            requestOptions.putAdditionalProperty("resolutionOptions", resolutionOptions);
            requestOptions.putAdditionalProperty("beaconServiceType", beaconServiceType);
            requestOptions.putAdditionalProperty("publishToIpfs", Boolean.FALSE);

            DidDocument didDocument = new DidDocument();
            didDocument.addServiceItem(service());

            UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.setDid(did);
            updateRequest.setOptions(requestOptions);
            updateRequest.addDidDocumentOperationItem("addToDidDocument");
            updateRequest.addDidDocumentItem(didDocument);

            ClientStateInterface clientStateInterface = new InMemoryClientStateInterface();

            ExecutionUtil.submitIteration(executorService, clientKeyInterface, clientStateInterface, did, 0, updateRequest, future);
        }

        return futures;
    }

    private static Map<String, Object> service() {
        return Map.of(
                "id", "#dwn",
                "type", "DecentralizedWebNode",
                "serviceEndpoint", "https://example.com/dwn/" + UUID.randomUUID().toString()
        );
    }
}
