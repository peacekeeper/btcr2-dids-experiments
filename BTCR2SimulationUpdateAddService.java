package test;

import com.danubetech.kms.clientkeyinterface.ClientKeyInterface;
import com.danubetech.kms.clientstateinterface.ClientStateInterface;
import com.danubetech.kms.clientstateinterface.impl.memory.InMemoryClientStateInterface;
import com.danubetech.uniregistrar.local.internalsecretmode.InternalSecretMode;
import uniregistrar.RegistrationException;
import uniregistrar.client.ClientUniRegistrar;
import uniregistrar.openapi.model.*;

import java.util.*;
import java.util.concurrent.Callable;

public class BTCR2SimulationUpdateAddService {

    public static Collection<Callable<UpdateState>> update(ClientUniRegistrar clientUniRegistrar, ClientKeyInterface<?> clientKeyInterface, List<String> dids, Map<String, Map<String, Object>> didResolutionOptions) throws Exception {

        Collection<Callable<UpdateState>> callables = new ArrayList<>();
        for (int i=0; i<dids.size(); i++) {
            String did = dids.get(i);
            Map<String, Object> resolutionOptions = didResolutionOptions.get(i);
            callables.add(() -> {
                try {

                    RequestOptions requestOptions = new RequestOptions();
                    requestOptions.setClientSecretMode(Boolean.TRUE);
                    requestOptions.putAdditionalProperty("resolutionOptions", resolutionOptions);

                    DidDocument didDocument = new DidDocument();
                    didDocument.addServiceItem(randomService());

                    UpdateRequest updateRequest = new UpdateRequest();
                    updateRequest.setDid(did);
                    updateRequest.setOptions(requestOptions);
                    updateRequest.addDidDocumentOperationItem("addToDidDocument");
                    updateRequest.addDidDocumentItem(didDocument);

                    ClientStateInterface clientStateInterface = new InMemoryClientStateInterface();

                    while(true) {
                        UpdateState updateState = clientUniRegistrar.update("btcr2", updateRequest);
                        Optional<RegistrarRequest> newRequest = InternalSecretMode.handleState(updateRequest, updateState, clientKeyInterface, clientStateInterface);

                        if ("finished".equals(updateState.getDidState().getState())) {
                            System.out.println(did + ": " + updateState.getDidState().getDid() + " -> " + updateState.getDidState().getState() + " (" + ((DidStateFinished) updateState.getDidState()).getDid() + ")");
                            return updateState;
                        }

                        if ("wait".equals(updateState.getDidState().getState())) {
                            System.out.println(did + ": " + updateState.getDidState().getDid() + " -> " + updateState.getDidState().getState() + " (" + ((DidStateWait) updateState.getDidState()).getWait() + ")");
                            Thread.sleep(1000);
                        }

                        if ("action".equals(updateState.getDidState().getState())) {
                            System.out.println(did + ": " + updateState.getDidState().getDid() + " -> " + updateState.getDidState().getState() + " (" + ((DidStateAction) updateState.getDidState()).getAction() + ")");
                            if (newRequest == null) throw new RegistrationException("'action' state not handled.");
                            if (newRequest.isEmpty()) throw new RegistrationException("No new request after handling 'action' state.");
                            updateRequest = (UpdateRequest) newRequest.get();
                        }
                    }
                } catch (RegistrationException ex) {
                    throw new RuntimeException(ex.getMessage(), ex);
                }
            });
        }
        return callables;
    }

    private static Map<String, Object> randomService() {
        return Map.of(
                "id", "#dwn",
                "type", "DecentralizedWebNode",
                "serviceEndpoint", "https://example.com/dwn/" + UUID.randomUUID().toString()
        );
    }
}
