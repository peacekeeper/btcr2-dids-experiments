package test.update;

import com.danubetech.kms.clientkeyinterface.ClientKeyInterface;
import com.danubetech.kms.clientstateinterface.ClientStateInterface;
import com.danubetech.kms.clientstateinterface.impl.memory.InMemoryClientStateInterface;
import com.danubetech.uniregistrar.local.internalsecretmode.InternalSecretMode;
import test.util.StateUtil;
import uniregistrar.client.ClientUniRegistrar;
import uniregistrar.openapi.model.*;

import java.util.*;
import java.util.concurrent.Callable;

public class BTCR2SimulationUpdateAddDWNService {

    public static Collection<Callable<UpdateState>> update(ClientUniRegistrar clientUniRegistrar, ClientKeyInterface<?> clientKeyInterface, List<String> dids, Map<String, Map<String, Object>> didResolutionOptions, String beaconServiceType) throws Exception {

        Collection<Callable<UpdateState>> callables = new ArrayList<>();
        for (int i=0; i<dids.size(); i++) {
            final String did = dids.get(i);
            final Map<String, Object> resolutionOptions = didResolutionOptions.get(did);
            callables.add(() -> {
                try {

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

                    while (true) {

                        UpdateState updateState = clientUniRegistrar.update("btcr2", updateRequest);
                        Optional<RegistrarRequest> newRequest = InternalSecretMode.handleState(updateRequest, updateState, clientKeyInterface, clientStateInterface);

                        System.out.println(did + ": " + StateUtil.stateToString(updateState));

                        if ("finished".equals(updateState.getDidState().getState())) {
                            return updateState;
                        }

                        if ("wait".equals(updateState.getDidState().getState())) {
                            Thread.sleep(1000);
                            continue;
                        }

                        if ("action".equals(updateState.getDidState().getState())) {
                            if (newRequest == null) {
                                Thread.sleep(1000);
                            } else {
                                updateRequest = (UpdateRequest) newRequest.get();
                            }
                            continue;
                        }
                    }
                } catch (Exception ex) {
                    throw new RuntimeException(did + ": " + ex.getMessage(), ex);
                }
            });
        }
        return callables;
    }

    private static Map<String, Object> service() {
        return Map.of(
                "id", "#dwn",
                "type", "DecentralizedWebNode",
                "serviceEndpoint", "https://example.com/dwn/" + UUID.randomUUID().toString()
        );
    }
}
