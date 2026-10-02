package test.create;

import com.danubetech.kms.clientkeyinterface.ClientKeyInterface;
import com.danubetech.kms.clientstateinterface.ClientStateInterface;
import com.danubetech.kms.clientstateinterface.impl.memory.InMemoryClientStateInterface;
import com.danubetech.uniregistrar.local.internalsecretmode.InternalSecretMode;
import uniregistrar.RegistrationException;
import uniregistrar.client.ClientUniRegistrar;
import uniregistrar.openapi.model.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Callable;

public class BTCR2SimulationCreateKey {

    public static Collection<Callable<CreateState>> create(ClientUniRegistrar clientUniRegistrar, ClientKeyInterface<?> clientKeyInterface, int number, String network, String cohort) throws Exception {

        Collection<Callable<CreateState>> callables = new ArrayList<>();
        for (int i=0; i<number; i++) {
            int finalI = i;
            callables.add(() -> {
                try {

                    RequestOptions requestOptions = new RequestOptions();
                    requestOptions.setClientSecretMode(Boolean.TRUE);
                    requestOptions.putAdditionalProperty("network", network);
                    requestOptions.putAdditionalProperty("generateStandardBeacons", Boolean.TRUE);
                    requestOptions.putAdditionalProperty("generateAggregateBeacon", cohort);
                    requestOptions.putAdditionalProperty("publishToIpfs", Boolean.FALSE);

                    CreateRequest createRequest = new CreateRequest();
                    createRequest.setOptions(requestOptions);

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
                } catch (Exception ex) {
                    throw new RuntimeException(finalI + ": " + ex.getMessage(), ex);
                }
            });
        }
        return callables;
    }
}
