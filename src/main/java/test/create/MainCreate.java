package test.create;

import com.danubetech.kms.ClientKeyException;
import com.danubetech.kms.clientkeyinterface.ClientKey;
import com.danubetech.kms.clientkeyinterface.ClientKeyInterface;
import com.danubetech.kms.clientkeyinterface.ClientKeyInterfaceFactory;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
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

public class MainCreate {

    static final URI BASE_URI = URI.create("https://api.dev.godiddy.com/1.0.0/universal-registrar");
    static final String API_KEY = "c4aa5dd6-4a08-4014-8510-17eb99eddb79";
    static final String NETWORK = "mutinynet";
    static final int NUMBER = 1000;

    static ClientUniRegistrar clientUniRegistrar = ClientUniRegistrar.create(BASE_URI);
    static ClientKeyInterface<ClientKey> clientKeyInterface = (ClientKeyInterface<ClientKey>) ClientKeyInterfaceFactory.clientKeyInterface(ClientKeyInterfaceFactory.KMSProvider.LOCAL_FILE, null, null, null, null);

    static ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
    static JsonMapper jsonMapper = JsonMapper.builder().build();

    static {
        clientUniRegistrar.setHttpClient(HttpClients.custom()
                .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create().setMaxConnTotal(1).build())
                .build());
        clientUniRegistrar.setHttpHeaders(Map.of("Authorization", "Bearer " + API_KEY));
    }

    static {
        List<ClientKey> clientKeys = null;
        try {
            clientKeys = clientKeyInterface.getKeys(null, null, null, null);
            for (ClientKey clientKey : clientKeys) clientKeyInterface.deleteKey(clientKey);
        } catch (ClientKeyException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static void main(String[] args) throws Exception {

        // execute

        String[] cohorts = new String[]{
                "cohort-" + NETWORK + "-smt-" + NUMBER + "*" + Math.abs(new Random().nextInt()),
                "cohort-" + NETWORK + "-cas-" + NUMBER + "*" + Math.abs(new Random().nextInt())
        };

        Collection<Callable<CreateState>> createCallables = BTCR2SimulationCreateGenesisWithDIDCommService.create(clientUniRegistrar, clientKeyInterface, NUMBER, NETWORK, cohorts);
        System.out.println("Invoking " + createCallables.size() + " virtual threads.");
        Collection<Future<CreateState>> createFutures = executorService.invokeAll(createCallables);
        executorService.shutdown();

        // log result

        List<? extends ClientKey> resultKeys = clientKeyInterface.getKeys(null, null, null, null);
        List<String> resultDids = resultKeys.stream().map(ClientKey::getController).map(URI::toString).toList();
        System.out.println("DIDs: " + resultDids.size() + ", Keys: " + resultKeys.size());

        for (Future<CreateState> createFuture : createFutures) {
            CreateState createState = createFuture.get();
            System.out.println("DONE: " + createState.getDidState().getDid() + " => " + createState.getDidState().getState());
        }

        // write result

        PrintWriter printWriter = new PrintWriter(new FileWriter("./create-out.jsonl"));
        for (Future<CreateState> createFuture : createFutures) {
            CreateState createState = createFuture.get();
            String did = createState.getDidState().getDid();
            Map<String, Object> didDocumentMetadataGenesisDocument = (Map<String, Object>) createState.getDidDocumentMetadata().get("genesisDocument");
            Map<String, Object> resolutionOptions = new HashMap<>();
            Map<String, Object> sidecar = new HashMap<>();
            sidecar.put("genesisDocument", didDocumentMetadataGenesisDocument);
            resolutionOptions.put("sidecar", sidecar);
            printWriter.println(did);
            printWriter.println(jsonMapper.writeValueAsString(resolutionOptions));
        }
        printWriter.close();
    }
}
