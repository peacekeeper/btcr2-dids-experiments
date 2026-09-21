package test;

import com.danubetech.kms.clientkeyinterface.ClientKey;
import com.danubetech.kms.clientkeyinterface.ClientKeyInterface;
import com.danubetech.kms.clientkeyinterface.ClientKeyInterfaceFactory;
import com.fasterxml.jackson.databind.json.JsonMapper;
import uniregistrar.client.ClientUniRegistrar;
import uniregistrar.openapi.model.CreateState;
import uniregistrar.openapi.model.UpdateState;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class BTCR2SimulationUpdate {

    static final URI BASE_URI = URI.create("https://api.godiddy.com/1.0.0/universal-registrar");
    static final String API_KEY = "06c9d23a-589d-46f4-90e4-bc707b0252a1";
    static final String NETWORK = "mutinynet";
    static final int THREADS = 10;

    static ClientUniRegistrar clientUniRegistrar = ClientUniRegistrar.create(BASE_URI);
    static ClientKeyInterface<?> clientKeyInterface = ClientKeyInterfaceFactory.clientKeyInterface(ClientKeyInterfaceFactory.KMSProvider.LOCAL_INMEMORY, null, null, null, null);

    static ExecutorService executorService = Executors.newFixedThreadPool(THREADS);
    static JsonMapper jsonMapper = JsonMapper.builder().build();

    static {
        clientUniRegistrar.setHttpHeaders(Map.of("Authorization", "Bearer " + API_KEY));
    }

    public static void main(String[] args) throws Exception {

        // prepare input

        BufferedReader bufferedReader = new BufferedReader(new FileReader("./create-out.txt"));
        String line;
        List<String> inputDids = new ArrayList<>();
        Map<String, Map<String, Object>> inputResolutionOptions = new HashMap<>();
        while ((line=bufferedReader.readLine()) != null) {
            if (line.startsWith("did:"))
                inputDids.add(line);
            else
                inputResolutionOptions.put(inputDids.get(inputDids.size()-1), jsonMapper.readValue(line, Map.class));
        }
        bufferedReader.close();

        // execute

        Collection<Callable<UpdateState>> updateCallables = BTCR2SimulationUpdateAddService.update(clientUniRegistrar, clientKeyInterface, inputDids, inputResolutionOptions);
        Collection<Future<UpdateState>> updateFutures = executorService.invokeAll(updateCallables);
        executorService.shutdown();

        // log result

        List<? extends ClientKey> resultKeys = clientKeyInterface.getKeys(null, null, null, null);
        List<String> resultDids = resultKeys.stream().map(ClientKey::getController).map(URI::toString).toList();
        System.out.println("DIDs: " + resultDids.size() + ", Keys: " + resultKeys.size());

        for (Future<UpdateState> updateFuture : updateFutures) {
            UpdateState updateState = updateFuture.get();
            System.out.println(updateState.getDidState().getDid() + " => " + updateState.getDidState().getState());
        }

        // write result

        PrintWriter printWriter = new PrintWriter(new FileWriter("./update-out.txt"));
        for (Future<UpdateState> updateFuture : updateFutures) {
            UpdateState updateState = updateFuture.get();
            List<Map<String, Object>> didDocumentMetadataUpdates = (List<Map<String, Object>>) updateState.getDidDocumentMetadata().get("updates");
            String did = updateState.getDidState().getDid();
            Map<String, Object> resolutionOptions = new HashMap<>();
            resolutionOptions.putAll(inputResolutionOptions.get(did));
            resolutionOptions.put("updates", didDocumentMetadataUpdates);
            printWriter.println(did);
            printWriter.println(jsonMapper.writeValueAsString(resolutionOptions));
        }
        printWriter.close();
    }
}
