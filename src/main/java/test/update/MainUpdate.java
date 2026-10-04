package test.update;

import com.danubetech.kms.clientkeyinterface.ClientKey;
import com.danubetech.kms.clientkeyinterface.ClientKeyInterface;
import com.danubetech.kms.clientkeyinterface.ClientKeyInterfaceFactory;
import com.fasterxml.jackson.databind.json.JsonMapper;
import uniregistrar.client.ClientUniRegistrar;
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

public class MainUpdate {

    static final URI BASE_URI = URI.create("https://api.dev.godiddy.com/1.0.0/universal-registrar");
    static final String API_KEY = "c4aa5dd6-4a08-4014-8510-17eb99eddb79";
    static final String NETWORK = "mutinynet";
    static final int THREADS = 10;

    static ClientUniRegistrar clientUniRegistrar = ClientUniRegistrar.create(BASE_URI);
    static ClientKeyInterface<?> clientKeyInterface = ClientKeyInterfaceFactory.clientKeyInterface(ClientKeyInterfaceFactory.KMSProvider.LOCAL_FILE, null, null, null, null);

    static ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
    static JsonMapper jsonMapper = JsonMapper.builder().build();

    static {
        clientUniRegistrar.setHttpHeaders(Map.of("Authorization", "Bearer " + API_KEY));
    }

    public static void main(String[] args) throws Exception {

        // prepare input

        BufferedReader bufferedReader = new BufferedReader(new FileReader("./create-out.jsonl"));
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

        System.out.println("inputDids: " + inputDids);

        // execute

        Collection<Callable<UpdateState>> updateCallables = BTCR2SimulationUpdateAddDWNService.update(clientUniRegistrar, clientKeyInterface, inputDids, inputResolutionOptions, "SMTBeacon");
        System.out.println("Invoking " + updateCallables.size() + " virtual threads.");
        Collection<Future<UpdateState>> updateFutures = executorService.invokeAll(updateCallables);
        executorService.shutdown();

        // log result

        List<? extends ClientKey> resultKeys = clientKeyInterface.getKeys(null, null, null, null);
        List<String> resultDids = resultKeys.stream().map(ClientKey::getController).map(URI::toString).toList();
        System.out.println("DIDs: " + resultDids.size() + ", Keys: " + resultKeys.size());

        for (Future<UpdateState> updateFuture : updateFutures) {
            UpdateState updateState = updateFuture.get();
            System.out.println("DONE: " + updateState.getDidState().getDid() + " => " + updateState.getDidState().getState());
        }

        // write result

        PrintWriter printWriter = new PrintWriter(new FileWriter("./update-out.jsonl"));
        for (Future<UpdateState> updateFuture : updateFutures) {
            UpdateState updateState = updateFuture.get();
            String did = updateState.getDidState().getDid();
            Map<String, Object> update = (Map<String, Object>) updateState.getDidDocumentMetadata().get("update");
            Map<String, Object> casAnnouncement = (Map<String, Object>) updateState.getDidDocumentMetadata().get("casAnnouncement");
            Map<String, Object> smtProof = (Map<String, Object>) updateState.getDidDocumentMetadata().get("smtProof");
            Map<String, Object> resolutionOptions = new HashMap<>(inputResolutionOptions.get(did));
            Map<String, Object> sidecar = (Map<String, Object>) resolutionOptions.computeIfAbsent("sidecar", (key) -> new HashMap<>());
            if (update != null) {
                List<Map<String, Object>> updates = (List<Map<String, Object>>) sidecar.computeIfAbsent("updates", (key) -> new LinkedList<>());
                updates.add(update);
            }
            if (casAnnouncement != null) {
                List<Map<String, Object>> casUpdates = (List<Map<String, Object>>) sidecar.computeIfAbsent("casUpdates", (key) -> new LinkedList<>());
                casUpdates.add(casAnnouncement);
            }
            if (smtProof != null) {
                List<Map<String, Object>> smtProofs = (List<Map<String, Object>>) sidecar.computeIfAbsent("smtProofs", (key) -> new LinkedList<>());
                smtProofs.add(smtProof);
            }
            printWriter.println(did);
            printWriter.println(jsonMapper.writeValueAsString(resolutionOptions));
        }
        printWriter.close();
    }
}
