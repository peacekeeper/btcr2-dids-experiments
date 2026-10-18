package test.resolve;

import com.fasterxml.jackson.databind.json.JsonMapper;
import foundation.identity.did.representations.Representations;
import uniresolver.client.ClientUniResolver;
import uniresolver.result.ResolveResult;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MainResolve {

    private static final ClientUniResolver clientUniResolver;

    private static final JsonMapper jsonMapper = JsonMapper.builder().build();

    static {
        clientUniResolver = ClientUniResolver.create(URI.create("http://localhost:8080/1.0/"));
        clientUniResolver.setSupportsOptions(true);
        clientUniResolver.setSupportsPost(true);
    }

    public static void main(String[] args) throws Exception {

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

        List<ResolveResult> createdResolveResults = new ArrayList<>();

        for (int i=0; i<createdDids.size(); i++) {

            String did = createdDids.get(i);
            Map<String, Object> resolutionOptions = createdResolutionOptions.get(did);
            resolutionOptions.put("accept", Representations.DEFAULT_MEDIA_TYPE);

            // resolve

            ResolveResult resolveResult = clientUniResolver.resolve(did, resolutionOptions);
            createdResolveResults.add(resolveResult);
        }

        // print result

        System.out.println("Resolved created DIDs: " + createdDids.size());

        // write result to file

        writeResolveOut(-1, createdDids, createdResolveResults);

        // rounds

        for (int round=0; round<updatedDidsRounds.size(); round++) {

            List<String> updateDids = updatedDidsRounds.get(round);
            Map<String, Map<String, Object>> updatedResolutionOptions = updatedResolutionOptionsRounds.get(round);

            List<ResolveResult> updatedResolveResults = new ArrayList<>();

            for (int i=0; i<createdDids.size(); i++) {

                String did = updateDids.get(i);
                Map<String, Object> resolutionOptions = updatedResolutionOptions.get(did);

                // resolve

                resolutionOptions.put("accept", Representations.DEFAULT_MEDIA_TYPE);
                ResolveResult resolveResult = clientUniResolver.resolve(did, resolutionOptions);
                updatedResolveResults.add(resolveResult);
            }

            // print result

            System.out.println("Resolved updated DIDs (round " + (round+1) + "): " + updateDids.size());

            // write result to file

            writeResolveOut(round, updateDids, updatedResolveResults);
        }
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

    private static void writeResolveOut(int round, List<String> dids, List<ResolveResult> resolveResults) throws IOException, ExecutionException, InterruptedException {
        PrintWriter printWriter = new PrintWriter(new FileWriter("./resolve-out-" + (round+1) + ".jsonl"));
        for (int i=0; i<dids.size(); i++) {
            String did = dids.get(i);
            ResolveResult resolveResult = resolveResults.get(i);
            printWriter.println(did);
            printWriter.println(jsonMapper.writeValueAsString(resolveResult.toMap()));
        }
        printWriter.flush();
        printWriter.close();
    }
}
