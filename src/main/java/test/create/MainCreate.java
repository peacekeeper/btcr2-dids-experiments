package test.create;

import com.danubetech.kms.clientkeyinterface.ClientKey;
import com.danubetech.kms.clientkeyinterface.ClientKeyInterface;
import com.danubetech.kms.clientkeyinterface.ClientKeyInterfaceFactory;
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
    static final int NUMBER = 100;
    static int COHORTS_AVERAGE_SIZE = 10;
    /*static final String[] COHORTS = new String[] {
            "cohort-" + NETWORK + "-smt-" + NUMBER + "*" + Math.abs(new Random().nextInt())
            //"cohort-" + NETWORK + "-cas-" + NUMBER + "*" + Math.abs(new Random().nextInt())
    };*/

    static final LinkedHashMap<String, Integer> COHORTS_CAS;
    static final LinkedHashMap<String, Integer> COHORTS_SMT;

    static final Map<Integer, List<String>> COHORTS = new HashMap<>();

    static {
        int COHORTS_NUMBER = NUMBER / COHORTS_AVERAGE_SIZE;
        Random RANDOM = new Random();
        COHORTS_CAS = new LinkedHashMap<>(COHORTS_NUMBER);
        COHORTS_SMT = new LinkedHashMap<>(COHORTS_NUMBER);
        for (int i=0; i<COHORTS_NUMBER; i++) {
            int COHORT_SIZE = COHORTS_AVERAGE_SIZE;
            COHORTS_CAS.put("cohort-" + NETWORK + "-cas-" + COHORT_SIZE + "*" + Math.abs(RANDOM.nextInt()), COHORT_SIZE);
            COHORTS_SMT.put("cohort-" + NETWORK + "-smt-" + COHORT_SIZE + "*" + Math.abs(RANDOM.nextInt()), COHORT_SIZE);
        }
        for (int i=0; i<NUMBER; i++) {
            List<String> COHORTS_CAS_LIST = new ArrayList<>(COHORTS_CAS.keySet());
            List<String> COHORTS_SMT_LIST = new ArrayList<>(COHORTS_SMT.keySet());
            String COHORT_CAS, COHORT_SMT;
            do {
                do { COHORT_CAS = COHORTS_CAS_LIST.get(RANDOM.nextInt(COHORTS_CAS_LIST.size())); } while (COHORTS_CAS.get(COHORT_CAS) == 0);
                do { COHORT_SMT = COHORTS_SMT_LIST.get(RANDOM.nextInt(COHORTS_SMT_LIST.size())); } while (COHORTS_SMT.get(COHORT_SMT) == 0);
            } while (COHORT_CAS.equals(COHORT_SMT));
            COHORTS_CAS.computeIfPresent(COHORT_CAS, (k, v) -> v - 1);
            COHORTS_SMT.computeIfPresent(COHORT_SMT, (k, v) -> v - 1);
            COHORTS.put(i, List.of(COHORT_CAS, COHORT_SMT));
        }
    }

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
        file = new File("./create-out.jsonl");
        if (file.exists()) file.delete();
        file = new File("./update-out.jsonl");
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
            resolutionOptions.put("versionId", "1");
            Map<String, Object> sidecar = new HashMap<>();
            sidecar.put("genesisDocument", didDocumentMetadataGenesisDocument);
            resolutionOptions.put("sidecar", sidecar);
            printWriter.println(did);
            printWriter.println(jsonMapper.writeValueAsString(resolutionOptions));
        }
        printWriter.close();
    }
}
