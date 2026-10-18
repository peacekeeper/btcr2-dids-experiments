package test.publishipfs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.ipfs.api.AddArgs;
import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;
import io.ipfs.multiaddr.MultiAddress;
import org.erdtman.jcs.JsonCanonicalizer;
import test.update.publishtoipfs.FalsePublishIpfsStrategy;
import test.update.publishtoipfs.PublishToIpfsStrategy;
import test.update.publishtoipfs.RandomPublishToIpfsStrategy;
import test.update.publishtoipfs.TruePublishIpfsStrategy;
import uniresolver.client.ClientUniResolver;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class MainPublishIpfs {

    public static final PublishToIpfsStrategy PUBLISH_TO_IPFS_STRATEGY = new RandomPublishToIpfsStrategy(List.of(0.1f, 1.0f), List.of(
            new TruePublishIpfsStrategy(),
            new FalsePublishIpfsStrategy()
    ));

    private static final ClientUniResolver clientUniResolver;
    private static final IPFS ipfs;

    private static final JsonMapper jsonMapper = JsonMapper.builder().build();

    static {
        clientUniResolver = ClientUniResolver.create(URI.create("http://localhost:8080/1.0/"));
        clientUniResolver.setSupportsOptions(true);
        clientUniResolver.setSupportsPost(true);
    }

    static {
        ipfs = new IPFS(new MultiAddress("/ip4/172.17.0.1/tcp/5001"));
    }

    public static void main(String[] args) throws Exception {

        int totalGenesisDocumentCount = 0;
        int totalUpdateCount = 0;
        int totalCasUpdateCount = 0;
        int publishedGenesisDocumentCount = 0;
        int publishedUpdateCount = 0;
        int publishedCasUpdateCount = 0;

        // read input

        List<String> createdDids = new ArrayList<>();
        Map<String, Map<String, Object>> createdResolutionOptions = new HashMap<>();
        readCreateOut(createdDids, createdResolutionOptions);
        System.out.println("createdDids: " + createdDids);

        List<List<String>> updatedDidsRounds = new ArrayList<>();
        List<Map<String, Map<String, Object>>> updatedResolutionOptionsRounds = new ArrayList<>();
        readUpdateOuts(updatedDidsRounds, updatedResolutionOptionsRounds);
        System.out.println("updatedDidsRounds: " + updatedDidsRounds);

        // round 0

        for (int i=0; i<createdDids.size(); i++) {

            String did = createdDids.get(i);
            Map<String, Object> sidecar = (Map<String, Object>) createdResolutionOptions.get(did).get("sidecar");

            // publish ipfs

            Map<String, Object> genesisDocument = (Map<String, Object>) sidecar.get("genesisDocument");
            if (genesisDocument != null) {
                totalGenesisDocumentCount++;
                if (PUBLISH_TO_IPFS_STRATEGY.publishToIpfs()) {
                    publishToIpfs(genesisDocument);
                    sidecar.remove("genesisDocument");
                    publishedGenesisDocumentCount++;
                }
            }
        }

        // print result

        System.out.println("Published genesis documents: " + publishedGenesisDocumentCount + "/" + totalGenesisDocumentCount);

        // write result to file

        writeCreateOut(createdDids, createdResolutionOptions);

        // rounds

        for (int round=0; round<updatedDidsRounds.size(); round++) {

            List<String> updatedDids = updatedDidsRounds.get(round);
            Map<String, Map<String, Object>> updatedResolutionOptions = updatedResolutionOptionsRounds.get(round);

            for (int i=0; i<createdDids.size(); i++) {

                String did = updatedDids.get(i);
                Map<String, Object> sidecar = (Map<String, Object>) updatedResolutionOptions.get(did).get("sidecar");

                // publish ipfs

                List<Map<String, Object>> updates = (List<Map<String, Object>>) sidecar.get("updates");
                Iterator<Map<String, Object>> updatesIterator = updates == null ? null : updates.iterator();
                while (updatesIterator != null && updatesIterator.hasNext()) {
                    Map<String, Object> update = updatesIterator.next();
                    totalUpdateCount++;
                    if (PUBLISH_TO_IPFS_STRATEGY.publishToIpfs()) {
                        publishToIpfs(update);
                        updatesIterator.remove();
                        publishedUpdateCount++;
                    }
                }

                List<Map<String, Object>> casUpdates = (List<Map<String, Object>>) sidecar.get("casUpdates");
                Iterator<Map<String, Object>> casUpdatesIterator = casUpdates == null ? null : casUpdates.iterator();
                while (casUpdatesIterator != null && casUpdatesIterator.hasNext()) {
                    Map<String, Object> casUpdate = casUpdatesIterator.next();
                    totalCasUpdateCount++;
                    if (PUBLISH_TO_IPFS_STRATEGY.publishToIpfs()) {
                        publishToIpfs(casUpdate);
                        casUpdatesIterator.remove();
                        publishedCasUpdateCount++;
                    }
                }
            }

            // print result

            System.out.println("Published updates (round " + (round+1) + "): " + publishedUpdateCount + "/" + totalUpdateCount);
            System.out.println("Published CAS updates (round " + (round+1) + "): " + publishedCasUpdateCount + "/" + totalCasUpdateCount);

            // write result to file

            writeUpdateOut(round, updatedDids, updatedResolutionOptions);
        }
    }

    private static MerkleNode publishToIpfs(Map<String, Object> object) throws IOException {
        byte[] ipfsPayload = jsonDocumentCanonicalizing(object).getBytes(StandardCharsets.UTF_8);
        AddArgs addArgs = AddArgs.Builder.newInstance().setCidVersion(1).setRawLeaves().setHash("sha2-256").setPin().build();
        return ipfs.add(new NamedStreamable.ByteArrayWrapper(ipfsPayload), addArgs).getFirst();
    }

    public static String jsonDocumentCanonicalizing(String json) {
        String canonicalized;
        try {
            canonicalized = new JsonCanonicalizer(json).getEncodedString();
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
        return canonicalized;
    }

    private static String jsonDocumentCanonicalizing(Map<String, ?> map) {
        String json;
        try {
            json = jsonMapper.writeValueAsString(map);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
        return jsonDocumentCanonicalizing(json);
    }

    private static void readCreateOut(List<String> createdDids, Map<String, Map<String, Object>> createdResolutionOptions) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader("./create-out.jsonl"));
        String line;
        while ((line=bufferedReader.readLine()) != null) {
            if (line.startsWith("did:"))
                createdDids.add(line);
            else
                createdResolutionOptions.put(createdDids.getLast(), jsonMapper.readValue(line, Map.class));
        }
        bufferedReader.close();
    }

    private static void readUpdateOuts(List<List<String>> updatedDidsRounds, List<Map<String, Map<String, Object>>> updatedResolutionOptionsRounds) throws IOException {
        int round = 0;
        while (true) {
            round++;
            File file = new File("./update-out-" + round + ".jsonl");
            if (! file.exists()) break;
            updatedDidsRounds.add(new ArrayList<>());
            updatedResolutionOptionsRounds.add(new HashMap<>());
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String line;
            while ((line=bufferedReader.readLine()) != null) {
                if (line.startsWith("did:"))
                    updatedDidsRounds.get(round-1).add(line);
                else
                    updatedResolutionOptionsRounds.get(round-1).put(updatedDidsRounds.get(round-1).getLast(), jsonMapper.readValue(line, Map.class));
            }
            bufferedReader.close();
        }
    }

    private static void writeCreateOut(List<String> createdDids, Map<String, Map<String, Object>> createdResolutionOptions) throws IOException {
        PrintWriter printWriter = new PrintWriter(new FileWriter("./create-out.jsonl"));
        for (int i=0; i<createdDids.size(); i++) {
            String did = createdDids.get(i);
            Map<String, Object> resolutionOptions = createdResolutionOptions.get(did);
            printWriter.println(did);
            printWriter.println(jsonMapper.writeValueAsString(resolutionOptions));
        }
        printWriter.flush();
        printWriter.close();
    }

    private static void writeUpdateOut(int round, List<String> updatedDids, Map<String, Map<String, Object>> updatedResolutionOptions) throws IOException, ExecutionException, InterruptedException {
        PrintWriter printWriter = new PrintWriter(new FileWriter("./update-out-" + (round+1) + ".jsonl"));
        for (int i=0; i<updatedDids.size(); i++) {
            String did = updatedDids.get(i);
            Map<String, Object> resolutionOptions = updatedResolutionOptions.get(did);
            printWriter.println(did);
            printWriter.println(jsonMapper.writeValueAsString(resolutionOptions));
        }
        printWriter.flush();
        printWriter.close();
    }
}
