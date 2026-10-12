package test.update;

import com.danubetech.kms.clientkeyinterface.ClientKeyInterface;
import com.danubetech.kms.clientstateinterface.ClientStateInterface;
import com.danubetech.kms.clientstateinterface.impl.memory.InMemoryClientStateInterface;
import test.update.decideupdate.DecideUpdateStrategy;
import test.update.generateoperation.GenerateOperationStrategy;
import test.update.publishtoipfs.PublishToIpfsStrategy;
import test.util.ExecutionUtil;
import uniregistrar.openapi.model.RequestOptions;
import uniregistrar.openapi.model.UpdateRequest;
import uniregistrar.openapi.model.UpdateState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

public class BTCR2SimulationUpdate {

    public static synchronized Collection<CompletableFuture<UpdateState>> update(
            ScheduledExecutorService executorService,
            ClientKeyInterface<?> clientKeyInterface,
            List<String> dids,
            Map<String, Map<String, Object>> didResolutionOptions,
            int round,
            String selectBeacon,
            PublishToIpfsStrategy publishToIpfsStrategy,
            DecideUpdateStrategy decideUpdateStrategy,
            GenerateOperationStrategy generateOperationStrategy) throws Exception {

        Collection<CompletableFuture<UpdateState>> futures = new ArrayList<>();

        for (int i=0; i<dids.size(); i++) {

            final String did = dids.get(i);
            final Map<String, Object> resolutionOptions = didResolutionOptions.get(did);

            CompletableFuture<UpdateState> future = new CompletableFuture<>();
            futures.add(future);

            RequestOptions requestOptions = new RequestOptions();
            requestOptions.setClientSecretMode(Boolean.TRUE);
            requestOptions.putAdditionalProperty("resolutionOptions", resolutionOptions);
            requestOptions.putAdditionalProperty("beaconServiceType", selectBeacon);
            requestOptions.putAdditionalProperty("publishToIpfs", publishToIpfsStrategy.publishToIpfs());

            UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.setDid(did);
            updateRequest.setOptions(requestOptions);

            if (decideUpdateStrategy.decideUpdate()) {
                generateOperationStrategy.generateOperation(updateRequest);
            }

            ClientStateInterface clientStateInterface = new InMemoryClientStateInterface();

            ExecutionUtil.submitIteration(executorService, clientKeyInterface, clientStateInterface, round, did, 0, updateRequest, future);
        }

        return futures;
    }
}
