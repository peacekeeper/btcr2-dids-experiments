package test.create;

import com.danubetech.kms.ClientKeyException;
import com.danubetech.kms.clientkeyinterface.ClientKey;
import com.danubetech.kms.clientkeyinterface.ClientKeyInterface;
import com.danubetech.kms.clientkeyinterface.ClientKeyInterfaceFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import uniregistrar.openapi.model.CreateState;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class MainCreate {

    static final String NETWORK = "mutinynet";
    static final int NUMBER = 5;
    static final String[] COHORTS = new String[] {
            "cohort-" + NETWORK + "-smt-" + NUMBER + "*" + Math.abs(new Random().nextInt())
    };

    static ClientKeyInterface<ClientKey> clientKeyInterface = (ClientKeyInterface<ClientKey>) ClientKeyInterfaceFactory.clientKeyInterface(ClientKeyInterfaceFactory.KMSProvider.LOCAL_FILE, null, null, null, null);

    static ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    static JsonMapper jsonMapper = JsonMapper.builder().build();

    public static void main(String[] args) throws Exception {

        // reset

        reset();

        // execute

        Collection<CompletableFuture<CreateState>> completableFutures = BTCR2SimulationCreateGenesisWithDIDCommService.create(executorService, clientKeyInterface, NUMBER, NETWORK, COHORTS);
        System.out.println("Waiting for " + completableFutures.size() + " results.");
        CompletableFuture.allOf(completableFutures.toArray(CompletableFuture[]::new)).join();
        Collection<CreateState> createStates = completableFutures.stream().map(CompletableFuture::join).toList();
        executorService.shutdown();

        // print result

        List<? extends ClientKey> resultKeys = clientKeyInterface.getKeys(null, null, null, null);
        List<String> resultDids = resultKeys.stream().map(ClientKey::getController).map(URI::toString).toList();
        System.out.println("DIDs: " + resultDids.size() + ", Keys: " + resultKeys.size());

        for (CreateState createState : createStates) {
            System.out.println("DONE: " + createState.getDidState().getDid() + " => " + createState.getDidState().getState());
        }

        // write result to file

        writeCreateOut(createStates);

    }

    private static void reset() {
        File file;
        file = new File("./create.jsonl");
        if (file.exists()) file.delete();
        file = new File("./keys.json");
        if (file.exists()) file.delete();
    }

    private static void writeCreateOut(Collection<CreateState> createStates) throws IOException {
        PrintWriter printWriter = new PrintWriter(new FileWriter("./create-out.jsonl"));
        for (CreateState createState : createStates) {
            String did = createState.getDidState().getDid();
            Map<String, Object> didDocumentMetadataGenesisDocument = (Map<String, Object>) createState.getDidDocumentMetadata().get("genesisDocument");
            Map<String, Object> resolutionOptions = new HashMap<>();
            Map<String, Object> sidecar = new HashMap<>();
            sidecar.put("genesisDocument", didDocumentMetadataGenesisDocument);
            resolutionOptions.put("sidecar", sidecar);
            printWriter.println(did);
            printWriter.println(jsonMapper.writeValueAsString(resolutionOptions));
        }
        printWriter.close();    }
}
