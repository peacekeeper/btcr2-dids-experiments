package test.create;

import com.danubetech.kms.clientkeyinterface.ClientKeyInterface;
import com.danubetech.kms.clientstateinterface.ClientStateInterface;
import com.danubetech.kms.clientstateinterface.impl.memory.InMemoryClientStateInterface;
import com.danubetech.uniregistrar.local.internalsecretmode.InternalSecretMode;
import test.util.StateUtil;
import uniregistrar.RegistrationException;
import uniregistrar.client.ClientUniRegistrar;
import uniregistrar.openapi.model.*;

import java.util.*;
import java.util.concurrent.Callable;

public class BTCR2SimulationCreateGenesisWithDIDCommService {

    public static Collection<Callable<CreateState>> create(ClientUniRegistrar clientUniRegistrar, ClientKeyInterface<?> clientKeyInterface, int number, String network, String[] cohorts) throws Exception {

        Collection<Callable<CreateState>> callables = new ArrayList<>();
        for (int i=0; i<number; i++) {
            int finalI = i;
            callables.add(() -> {
                try {

                    RequestOptions requestOptions = new RequestOptions();
                    requestOptions.setClientSecretMode(Boolean.TRUE);
                    requestOptions.putAdditionalProperty("network", network);
                    requestOptions.putAdditionalProperty("generateStandardBeacons", Boolean.TRUE);
                    requestOptions.putAdditionalProperty("generateAggregateBeacons", String.join(",", cohorts));
                    requestOptions.putAdditionalProperty("publishToIpfs", Boolean.FALSE);

                    DidDocument didDocument = new DidDocument();
                    didDocument.addServiceItem(service());

                    CreateRequest createRequest = new CreateRequest();
                    createRequest.setOptions(requestOptions);
                    createRequest.setDidDocument(didDocument);

                    ClientStateInterface clientStateInterface = new InMemoryClientStateInterface();

                    while (true) {

                        CreateState createState = clientUniRegistrar.create("btcr2", createRequest);
                        Optional<RegistrarRequest> newRequest = InternalSecretMode.handleState(createRequest, createState, clientKeyInterface, clientStateInterface);

                        System.out.println(finalI + ": " + StateUtil.stateToString(createState));
                        System.out.println(StateUtil.stateStats(finalI, createState));

                        if ("finished".equals(createState.getDidState().getState())) {
                            return createState;
                        }

                        if ("wait".equals(createState.getDidState().getState())) {
                            Thread.sleep(1000);
                            continue;
                        }

                        if ("action".equals(createState.getDidState().getState())) {
                            if (newRequest == null) {
                                Thread.sleep(1000);
                            } else {
                                createRequest = (CreateRequest) newRequest.get();
                            }
                            continue;
                        }
                    }
                } catch (Exception ex) {
                    throw new RuntimeException(finalI + ": " + ex.getMessage(), ex);
                }
            });
        }
        return callables;
    }

    private static Map<String, Object> service() {
        return Map.of(
                "id", "#didcomm",
                "type", "DIDCommMessaging",
                "serviceEndpoint", "https://example.com/didcomm/" + UUID.randomUUID().toString()
        );
    }
}
