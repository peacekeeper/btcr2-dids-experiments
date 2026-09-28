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

public class BTCR2SimulationCreateGenesis {

    public static Collection<Callable<CreateState>> create(ClientUniRegistrar clientUniRegistrar, ClientKeyInterface<?> clientKeyInterface, int number, String network) throws Exception {

        Collection<Callable<CreateState>> callables = new ArrayList<>();
        for (int i=0; i<number; i++) {
            int finalI = i;
            callables.add(() -> {
                try {

                    RequestOptions requestOptions = new RequestOptions();
                    requestOptions.setClientSecretMode(Boolean.TRUE);
                    requestOptions.putAdditionalProperty("network", network);
                    requestOptions.putAdditionalProperty("generateStandardBeacons", Boolean.TRUE);
                    requestOptions.putAdditionalProperty("generateAggregateBeacon", "cohort-" + network + "-smt-2");

                    DidDocument didDocument = new DidDocument();
                    didDocument.addServiceItem(randomService());

                    CreateRequest createRequest = new CreateRequest();
                    createRequest.setOptions(requestOptions);
                    createRequest.setDidDocument(didDocument);

                    ClientStateInterface clientStateInterface = new InMemoryClientStateInterface();

                    while(true) {
                        CreateState createState = clientUniRegistrar.create("btcr2", createRequest);
                        Optional<RegistrarRequest> newRequest = InternalSecretMode.handleState(createRequest, createState, clientKeyInterface, clientStateInterface);

                        if ("finished".equals(createState.getDidState().getState())) {
                            System.out.println(finalI + ": " + createState.getDidState().getDid() + " -> " + createState.getDidState().getState() + " (" + ((DidStateFinished) createState.getDidState()).getDid() + ")");
                            return createState;
                        }

                        if ("wait".equals(createState.getDidState().getState())) {
                            System.out.println(finalI + ": " + createState.getDidState().getDid() + " -> " + createState.getDidState().getState() + " (" + ((DidStateWait) createState.getDidState()).getWait() + ")");
                            Thread.sleep(1000);
                        }

                        if ("action".equals(createState.getDidState().getState())) {
                            System.out.println(finalI + ": " + createState.getDidState().getDid() + " -> " + createState.getDidState().getState() + " (" + ((DidStateAction) createState.getDidState()).getAction() + ")");
                            if (newRequest == null) throw new RegistrationException("'action' state not handled.");
                            if (newRequest.isEmpty()) throw new RegistrationException("No new request after handling 'action' state.");
                            createRequest = (CreateRequest) newRequest.get();
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
                "id", "#didcomm",
                "type", "DIDCommMessaging",
                "serviceEndpoint", "https://example.com/didcomm/" + UUID.randomUUID().toString()
        );
    }
}
