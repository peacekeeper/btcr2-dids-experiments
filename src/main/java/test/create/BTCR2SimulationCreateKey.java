package test.create;

import com.danubetech.kms.clientkeyinterface.ClientKeyInterface;
import com.danubetech.kms.clientstateinterface.ClientStateInterface;
import com.danubetech.kms.clientstateinterface.impl.memory.InMemoryClientStateInterface;
import test.util.ExecutionUtil;
import uniregistrar.openapi.model.CreateRequest;
import uniregistrar.openapi.model.CreateState;
import uniregistrar.openapi.model.RequestOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

public class BTCR2SimulationCreateKey {

    public static synchronized Collection<CompletableFuture<CreateState>> create(
            ScheduledExecutorService executorService,
            ClientKeyInterface<?> clientKeyInterface,
            int number,
            String network,
            String[] generateAggregateBeacons) throws Exception {

        Collection<CompletableFuture<CreateState>> futures = new ArrayList<>();

        for (int task=0; task<number; task++) {

            CompletableFuture<CreateState> future = new CompletableFuture<>();
            futures.add(future);

            RequestOptions requestOptions = new RequestOptions();
            requestOptions.setClientSecretMode(Boolean.TRUE);
            requestOptions.putAdditionalProperty("network", network);
            requestOptions.putAdditionalProperty("generateStandardBeacons", Boolean.TRUE);
            requestOptions.putAdditionalProperty("generateAggregateBeacons", String.join(",", generateAggregateBeacons));
            requestOptions.putAdditionalProperty("publishToIpfs", Boolean.FALSE);

            CreateRequest createRequest = new CreateRequest();
            createRequest.setOptions(requestOptions);

            ClientStateInterface clientStateInterface = new InMemoryClientStateInterface();

            ExecutionUtil.submitIteration(executorService, clientKeyInterface, clientStateInterface, 0, task, 0, createRequest, future);
        }

        return futures;
    }
}
