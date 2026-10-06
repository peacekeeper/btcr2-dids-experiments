package test.update;

import com.danubetech.kms.clientkeyinterface.ClientKey;
import com.danubetech.kms.clientkeyinterface.ClientKeyInterface;
import com.danubetech.kms.clientkeyinterface.ClientKeyInterfaceFactory;
import com.fasterxml.jackson.databind.json.JsonMapper;
import uniregistrar.openapi.model.UpdateState;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class MainUpdate {

    static ClientKeyInterface<?> clientKeyInterface = ClientKeyInterfaceFactory.clientKeyInterface(ClientKeyInterfaceFactory.KMSProvider.LOCAL_FILE, null, null, null, null);

    static ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    static JsonMapper jsonMapper = JsonMapper.builder().build();

    public static void main(String[] args) throws Exception {

        // read input

        List<String> inputDids = new ArrayList<>();
        Map<String, Map<String, Object>> inputResolutionOptions = new HashMap<>();
        readCreateOut(inputDids, inputResolutionOptions);
        System.out.println("inputDids: " + inputDids);

        // execute

        Collection<CompletableFuture<UpdateState>> completableFutures = BTCR2SimulationUpdateAddDWNService.update(executorService, clientKeyInterface, inputDids, inputResolutionOptions, "SMTBeacon");
        System.out.println("Waiting for " + completableFutures.size() + " results.");
        CompletableFuture.allOf(completableFutures.toArray(CompletableFuture[]::new)).join();
        Collection<UpdateState> updateStates = completableFutures.stream().map(CompletableFuture::join).toList();
        executorService.shutdown();

        // print result

        List<? extends ClientKey> resultKeys = clientKeyInterface.getKeys(null, null, null, null);
        List<String> resultDids = resultKeys.stream().map(ClientKey::getController).map(URI::toString).toList();
        System.out.println("DIDs: " + resultDids.size() + ", Keys: " + resultKeys.size());

        for (UpdateState updateState : updateStates) {
            System.out.println("DONE: " + updateState.getDidState().getDid() + " => " + updateState.getDidState().getState());
        }

        // write result to file

        writeUpdateOut(updateStates, inputResolutionOptions);
    }

    private static void readCreateOut(List<String> inputDids, Map<String, Map<String, Object>> inputResolutionOptions) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader("./create-out.jsonl"));
        String line;
        while ((line=bufferedReader.readLine()) != null) {
            if (line.startsWith("did:"))
                inputDids.add(line);
            else
                inputResolutionOptions.put(inputDids.get(inputDids.size()-1), jsonMapper.readValue(line, Map.class));
        }
        bufferedReader.close();
    }

    private static void writeUpdateOut(Collection<UpdateState> updateStates, Map<String, Map<String, Object>> inputResolutionOptions) throws IOException, ExecutionException, InterruptedException {
        PrintWriter printWriter = new PrintWriter(new FileWriter("./update-out.jsonl"));
        for (UpdateState updateState : updateStates) {
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
