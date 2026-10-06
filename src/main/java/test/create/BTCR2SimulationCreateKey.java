package test.create;

import com.danubetech.kms.clientkeyinterface.ClientKeyInterface;
import com.danubetech.kms.clientstateinterface.ClientStateInterface;
import com.danubetech.kms.clientstateinterface.impl.memory.InMemoryClientStateInterface;
import com.danubetech.uniregistrar.local.internalsecretmode.InternalSecretMode;
import test.util.ClientUtil;
import test.util.ExecutionUtil;
import test.util.StateUtil;
import uniregistrar.openapi.model.CreateRequest;
import uniregistrar.openapi.model.CreateState;
import uniregistrar.openapi.model.RegistrarRequest;
import uniregistrar.openapi.model.RequestOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BTCR2SimulationCreateKey {

    public static Collection<CompletableFuture<CreateState>> create(ScheduledExecutorService executorService, ClientKeyInterface<?> clientKeyInterface, int number, String network, String[] cohorts) throws Exception {

        Collection<CompletableFuture<CreateState>> futures = new ArrayList<>();

        for (int task=0; task<number; task++) {

            CompletableFuture<CreateState> future = new CompletableFuture<>();
            futures.add(future);

            RequestOptions requestOptions = new RequestOptions();
            requestOptions.setClientSecretMode(Boolean.TRUE);
            requestOptions.putAdditionalProperty("network", network);
            requestOptions.putAdditionalProperty("generateStandardBeacons", Boolean.TRUE);
            requestOptions.putAdditionalProperty("generateAggregateBeacons", String.join(",", cohorts));
            requestOptions.putAdditionalProperty("publishToIpfs", Boolean.FALSE);

            CreateRequest createRequest = new CreateRequest();
            createRequest.setOptions(requestOptions);

            ClientStateInterface clientStateInterface = new InMemoryClientStateInterface();

            ExecutionUtil.submitIteration(executorService, clientKeyInterface, clientStateInterface, task, 0, createRequest, future);
        }

        return futures;
    }
}
