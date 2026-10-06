package test.create;

import com.danubetech.kms.clientkeyinterface.ClientKeyInterface;
import com.danubetech.kms.clientstateinterface.ClientStateInterface;
import com.danubetech.kms.clientstateinterface.impl.memory.InMemoryClientStateInterface;
import com.danubetech.uniregistrar.local.internalsecretmode.InternalSecretMode;
import test.util.ClientUtil;
import test.util.ExecutionUtil;
import test.util.StateUtil;
import uniregistrar.openapi.model.*;

import java.util.*;
import java.util.concurrent.*;

public class BTCR2SimulationCreateGenesisWithDIDCommService {

    public static Collection<CompletableFuture<CreateState>> create(ScheduledExecutorService executorService, ClientKeyInterface<?> clientKeyInterface, int number, String network, Map<Integer, List<String>> cohorts) throws Exception {

        Collection<CompletableFuture<CreateState>> futures = new ArrayList<>();

        for (int task=0; task<number; task++) {

            CompletableFuture<CreateState> future = new CompletableFuture<>();
            futures.add(future);

            RequestOptions requestOptions = new RequestOptions();
            requestOptions.setClientSecretMode(Boolean.TRUE);
            requestOptions.putAdditionalProperty("network", network);
            requestOptions.putAdditionalProperty("generateStandardBeacons", Boolean.TRUE);
            requestOptions.putAdditionalProperty("generateAggregateBeacons", String.join(",", cohorts.get(task)));
            requestOptions.putAdditionalProperty("publishToIpfs", Boolean.FALSE);

            DidDocument didDocument = new DidDocument();
            didDocument.addServiceItem(service());

            final CreateRequest createRequest = new CreateRequest();
            createRequest.setOptions(requestOptions);
            createRequest.setDidDocument(didDocument);

            ClientStateInterface clientStateInterface = new InMemoryClientStateInterface();

            ExecutionUtil.submitIteration(executorService, clientKeyInterface, clientStateInterface, task, 0, createRequest, future);
        }

        return futures;
    }

    private static Map<String, Object> service() {
        return Map.of(
                "id", "#didcomm",
                "type", "DIDCommMessaging",
                "serviceEndpoint", "https://example.com/didcomm/" + UUID.randomUUID().toString()
        );
    }
}
