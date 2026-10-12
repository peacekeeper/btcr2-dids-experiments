package test.update;

import com.danubetech.kms.clientkeyinterface.ClientKey;
import com.danubetech.kms.clientkeyinterface.ClientKeyInterface;
import com.danubetech.kms.clientkeyinterface.ClientKeyInterfaceFactory;
import com.fasterxml.jackson.databind.json.JsonMapper;
import test.update.decideupdate.DecideUpdateStrategy;
import test.update.decideupdate.FalseDecideUpdateStrategy;
import test.update.decideupdate.RandomDecideUpdateStrategy;
import test.update.decideupdate.TrueDecideUpdateStrategy;
import test.update.generateoperation.GenerateOperationStrategy;
import test.update.generateoperation.RandomGenerateOperationStrategy;
import test.update.generateoperation.UpdateMyKeyVerificationMethodGenerateOperationStrategy;
import test.update.generateoperation.UpdateMyServiceGenerateOperationStrategy;
import test.update.publishtoipfs.FalsePublishIpfsStrategy;
import test.update.publishtoipfs.PublishToIpfsStrategy;
import test.update.selectbeacon.CASSelectBeaconStrategy;
import test.update.selectbeacon.RandomSelectBeaconStrategy;
import test.update.selectbeacon.SMTSelectBeaconStrategy;
import test.update.selectbeacon.SelectBeaconStrategy;
import uniregistrar.openapi.model.UpdateState;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class MainUpdate {

    static final int ROUNDS = 20;
    static final SelectBeaconStrategy SELECT_BEACON_STRATEGY = new RandomSelectBeaconStrategy(List.of(0.25f, 1.0f), List.of(
            new SMTSelectBeaconStrategy(),
            new CASSelectBeaconStrategy()
    ));
    static final PublishToIpfsStrategy PUBLISH_TO_IPFS_STRATEGY = new FalsePublishIpfsStrategy();
    static final DecideUpdateStrategy DECIDE_UPDATE_STRATEGY = new RandomDecideUpdateStrategy(List.of(0.25f, 1.0f), List.of(
            new TrueDecideUpdateStrategy(),
            new FalseDecideUpdateStrategy()
    ));
    static final GenerateOperationStrategy GENERATE_OPERATION_STRATEGY = new RandomGenerateOperationStrategy(List.of(0.5f, 1.0f), List.of(
            new UpdateMyServiceGenerateOperationStrategy(2),
            new UpdateMyKeyVerificationMethodGenerateOperationStrategy(3)
    ));

    static ClientKeyInterface<?> clientKeyInterface = ClientKeyInterfaceFactory.clientKeyInterface(ClientKeyInterfaceFactory.KMSProvider.LOCAL_FILE, null, null, null, null);

    static JsonMapper jsonMapper = JsonMapper.builder().build();

    public static void main(String[] args) throws Exception {

        // rounds

        for (int round=0; round<ROUNDS; round++) {

            // read input

            List<String> inputDids = new ArrayList<>();
            Map<String, Map<String, Object>> inputResolutionOptions = new HashMap<>();
            readOut(round, inputDids, inputResolutionOptions);
            System.out.println("inputDids: " + inputDids);

            // execute

            ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

            String selectBeacon = SELECT_BEACON_STRATEGY.selectBeacon();

            Collection<CompletableFuture<UpdateState>> completableFutures = BTCR2SimulationUpdate.update(
                    executorService,
                    clientKeyInterface,
                    inputDids,
                    inputResolutionOptions,
                    round,
                    selectBeacon,
                    PUBLISH_TO_IPFS_STRATEGY,
                    DECIDE_UPDATE_STRATEGY,
                    GENERATE_OPERATION_STRATEGY);

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

            writeUpdateOut(round, updateStates, inputResolutionOptions);

            // pause before next round

            Thread.sleep(60000);
        }
    }

    private static void readOut(int round, List<String> inputDids, Map<String, Map<String, Object>> inputResolutionOptions) throws IOException {
        File outFile = (round == 0) ? new File("./create-out.jsonl") : new File("./update-out-" + round + ".jsonl");
        if (outFile.exists()) {
            readOut(outFile, inputDids, inputResolutionOptions);
        } else {
            throw new IOException("Create/Update Out file " + outFile + " not found");
        }
    }

    private static void readOut(File outFile, List<String> inputDids, Map<String, Map<String, Object>> inputResolutionOptions) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(outFile));
        String line;
        while ((line=bufferedReader.readLine()) != null) {
            if (line.startsWith("did:"))
                inputDids.add(line);
            else
                inputResolutionOptions.put(inputDids.get(inputDids.size()-1), jsonMapper.readValue(line, Map.class));
        }
        bufferedReader.close();
    }

    private static void writeUpdateOut(int round, Collection<UpdateState> updateStates, Map<String, Map<String, Object>> inputResolutionOptions) throws IOException, ExecutionException, InterruptedException {
        PrintWriter printWriter = new PrintWriter(new FileWriter("./update-out-" + (round+1) + ".jsonl"));
        for (UpdateState updateState : updateStates) {
            String did = updateState.getDidState().getDid();
            Map<String, Object> update = (Map<String, Object>) updateState.getDidDocumentMetadata().get("update");
            Map<String, Object> casAnnouncement = (Map<String, Object>) updateState.getDidDocumentMetadata().get("casAnnouncement");
            Map<String, Object> smtProof = (Map<String, Object>) updateState.getDidDocumentMetadata().get("smtProof");
            Map<String, Object> resolutionOptions = new HashMap<>(inputResolutionOptions.get(did));
            if (update != null) {
                resolutionOptions.computeIfPresent("versionId", (k, v) -> Integer.toString(Integer.parseInt((String) v) + 1));
            }
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
        printWriter.flush();
        printWriter.close();
    }
}
