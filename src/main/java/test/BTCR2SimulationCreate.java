package test;

import com.danubetech.kms.clientkeyinterface.ClientKey;
import com.danubetech.kms.clientkeyinterface.ClientKeyInterface;
import com.danubetech.kms.clientkeyinterface.ClientKeyInterfaceFactory;
import com.fasterxml.jackson.databind.json.JsonMapper;
import uniregistrar.client.ClientUniRegistrar;
import uniregistrar.openapi.model.CreateState;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class BTCR2SimulationCreate {

    static final URI BASE_URI = URI.create("https://api.dev.godiddy.com/1.0.0/universal-registrar");
    static final String API_KEY = "c4aa5dd6-4a08-4014-8510-17eb99eddb79";
    static final String NETWORK = "mutinynet";
    static final int NUMBER = 100;
    static final int THREADS = 10;

    static ClientUniRegistrar clientUniRegistrar = ClientUniRegistrar.create(BASE_URI);
    static ClientKeyInterface<?> clientKeyInterface = ClientKeyInterfaceFactory.clientKeyInterface(ClientKeyInterfaceFactory.KMSProvider.LOCAL_INMEMORY, null, null, null, null);

    static ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
    static JsonMapper jsonMapper = JsonMapper.builder().build();

    static {
        clientUniRegistrar.setHttpHeaders(Map.of("Authorization", "Bearer " + API_KEY));
    }

    public static void main(String[] args) throws Exception {

        // execute

        String cohort = "cohort-" + NETWORK + "-smt-" + NUMBER + "-" + Math.abs(new Random().nextInt());

        Collection<Callable<CreateState>> createCallables = BTCR2SimulationCreateGenesis.create(clientUniRegistrar, clientKeyInterface, NUMBER, NETWORK, cohort);
        Collection<Future<CreateState>> createFutures = executorService.invokeAll(createCallables);
        executorService.shutdown();

        // log result

        List<? extends ClientKey> resultKeys = clientKeyInterface.getKeys(null, null, null, null);
        List<String> resultDids = resultKeys.stream().map(ClientKey::getController).map(URI::toString).toList();
        System.out.println("DIDs: " + resultDids.size() + ", Keys: " + resultKeys.size());

        for (Future<CreateState> createFuture : createFutures) {
            CreateState createState = createFuture.get();
            System.out.println(createState.getDidState().getDid() + " => " + createState.getDidState().getState());
        }

        // write result

        PrintWriter printWriter = new PrintWriter(new FileWriter("./create-out.jsonl"));
        for (Future<CreateState> createFuture : createFutures) {
            CreateState createState = createFuture.get();
            Map<String, Object> didDocumentMetadataGenesisDocument = (Map<String, Object>) createState.getDidDocumentMetadata().get("genesisDocument");
            String did = createState.getDidState().getDid();
            Map<String, Object> resolutionOptions = new HashMap<>();
            resolutionOptions.put("genesisDocument", didDocumentMetadataGenesisDocument);
            printWriter.println(did);
            printWriter.println(jsonMapper.writeValueAsString(resolutionOptions));
        }
        printWriter.close();
    }
}
