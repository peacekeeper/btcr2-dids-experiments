package test.update;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import foundation.identity.did.DIDDocument;
import foundation.identity.did.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AnalyzeUpdate {

    private static final String PATH = "./data1/";

    private static final Pattern COHORT_ID_PATTERN_MAXSIZE = Pattern.compile("^cohort-([a-z0-9]+)-([a-z0-9]{3})-([0-9]+)(\\*([0-9]+))?$");

    static JsonMapper jsonMapper = JsonMapper.builder().build();

    public static void main(String[] args) throws Exception {

        // read input

        List<String> createdDids = new ArrayList<>();
        Map<String, Map<String, Object>> createdResolutionOptions = new HashMap<>();
        readCreateOut(PATH, createdDids, createdResolutionOptions);
        System.out.println("createdDids: " + createdDids);

        List<List<String>> updatedDidsRounds = new ArrayList<>();
        List<Map<String, Map<String, Object>>> updatedResolutionOptionsRounds = new ArrayList<>();
        readUpdateOuts(PATH, updatedDidsRounds, updatedResolutionOptionsRounds);
        System.out.println("updatedDidsRounds: " + updatedDidsRounds);

        // analyze!

        // count DIDs

        final Set<String> didsSet = new HashSet<>();
        createdDids.forEach(didsSet::add);
        updatedDidsRounds.forEach(updatedDids -> updatedDids
                .forEach(didsSet::add));
        int didsCount = didsSet.size();

        // count update rounds

        int updateRoundsCount = updatedDidsRounds.size();

        // cohort types and sizes

        Set<String> cohorts = new HashSet<>();
        createdResolutionOptions.forEach((key, value) -> {
            Map<String, Object> sidecar = (Map<String, Object>) value.get("sidecar");
            DIDDocument genesisDocument = DIDDocument.fromMap((Map<String, Object>) sidecar.get("genesisDocument"));
            for (Service service : genesisDocument.getServices()) {
                if ("CASBeacon".equals(service.getType()) || "SMTBeacon".equals(service.getType())) {
                    cohorts.add(service.getId().toString().substring("#".length()));
                }
            }
        });

        Map<String, Map.Entry<String, Integer>> cohortTypesAndSizes = new HashMap<>();
        for (String cohort : cohorts) {
            Matcher matcher = COHORT_ID_PATTERN_MAXSIZE.matcher(cohort);
            matcher.matches();
            String cohortType = matcher.group(2);
            Integer cohortSize = Integer.parseInt(matcher.group(3));
            cohortTypesAndSizes.put(cohort, new AbstractMap.SimpleEntry<>(cohortType, cohortSize));
        }

        // count beacons by type

        final Map<String, Integer> beaconTypeCounts = new HashMap<>();
        beaconTypeCounts.putAll(Map.of("SingletonBeacon", 0, "CASBeacon", 0, "SMTBeacon", 0));

        createdResolutionOptions.values().stream()
                .map(x -> (Map<String, Object>) x.get("sidecar"))
                .map(x -> (Map<String, Object>) x.get("genesisDocument"))
                .map(DIDDocument::fromMap)
                .forEach(didDocument -> {
                    didDocument.getServices().forEach(service -> {
                        if (service.getType().contains("Beacon")) {
                            beaconTypeCounts.computeIfPresent(service.getType(), (k, v) -> v+1);
                        }
                    });
                });

        // count updates per round
        // count updates by DID
        // yes/no updates by round by DID

        List<Integer> updatesPerRound = new ArrayList<>();
        for (int round=0; round<updatedDidsRounds.size(); round++) updatesPerRound.add( 0);

        Map<String, Integer> updatesByDid = new HashMap<>();
        for (String did : createdDids) updatesByDid.put(did, 0);

        Map<String, Map<Integer, Boolean>> updatesByRoundByDid = new HashMap<>();
        for (String did : createdDids) updatesByRoundByDid.put(did, new HashMap<>());
        for (String did : createdDids) for (int round=0; round<updatedDidsRounds.size(); round++) updatesByRoundByDid.get(did).put(round, Boolean.FALSE);

        Map<String, Integer> lastVersionId = new HashMap<>();

        for (int round=0; round<updatedDidsRounds.size(); round++) {
            Map<String, Map<String, Object>> updatedResolutionOptions = updatedResolutionOptionsRounds.get(round);
            for (Map.Entry<String, Map<String, Object>> updatedResolutionOptionsEntry : updatedResolutionOptions.entrySet()) {
                String did = updatedResolutionOptionsEntry.getKey();
                Map<String, Object> resolutionOptions = updatedResolutionOptionsEntry.getValue();
                int versionId = Integer.parseInt((String) resolutionOptions.get("versionId"));
                if (versionId > lastVersionId.computeIfAbsent(did, (k) -> 1)) {
                    updatesPerRound.set(round, updatesPerRound.get(round) + 1);
                    updatesByDid.computeIfPresent(did, (k, v) -> v+1);
                    updatesByRoundByDid.get(did).put(round, Boolean.TRUE);
                    lastVersionId.put(did, versionId);
                }
            }
        }

        // count updates by beacon types
        // count updates by update operation
        // total updates
        // update sizes

        Map<String, Map<String, Object>> lastUpdatedResolutionOptions = updatedResolutionOptionsRounds.getLast();

        List<Map<String, Object>> lastUpdatedResolutionOptionsSidecars = lastUpdatedResolutionOptions.values().stream().map(x -> (Map<String, Object>) x.get("sidecar")).collect(Collectors.toList());
        List<Map<String, Object>> lastUpdatedResolutionOptionsSidecarUpdates = new ArrayList<>();
        List<Map<String, Object>> lastUpdatedResolutionOptionsSidecarCasUpdates = new ArrayList<>();
        List<Map<String, Object>> lastUpdatedResolutionOptionsSidecarSmtProofs = new ArrayList<>();

        for (Map<String, Object> lastUpdatedResolutionOptionsSidecar : lastUpdatedResolutionOptionsSidecars) {
            if (lastUpdatedResolutionOptionsSidecar.containsKey("updates")) lastUpdatedResolutionOptionsSidecarUpdates.addAll((List<Map<String, Object>>) lastUpdatedResolutionOptionsSidecar.get("updates"));
            if (lastUpdatedResolutionOptionsSidecar.containsKey("casUpdates")) lastUpdatedResolutionOptionsSidecarCasUpdates.addAll((List<Map<String, Object>>) lastUpdatedResolutionOptionsSidecar.get("casUpdates"));
            if (lastUpdatedResolutionOptionsSidecar.containsKey("smtProofs")) lastUpdatedResolutionOptionsSidecarSmtProofs.addAll((List<Map<String, Object>>) lastUpdatedResolutionOptionsSidecar.get("smtProofs"));
        }

        int lastUpdatedResolutionOptionsSidecarUpdatesCount = (int) lastUpdatedResolutionOptionsSidecarUpdates.stream().distinct().count();
        int lastUpdatedResolutionOptionsSidecarCasUpdatesCount = lastUpdatedResolutionOptionsSidecarCasUpdates.stream().distinct().map(Map::size).reduce(0, Integer::sum);
        int lastUpdatedResolutionOptionsSidecarSmtProofsCount = (int) lastUpdatedResolutionOptionsSidecarSmtProofs.stream().distinct().filter(x -> x.containsKey("updateId")).count();

        final Map<String, Integer> updatesByBeaconTypes = Map.of(
                "SingletonBeacon", lastUpdatedResolutionOptionsSidecarUpdatesCount - lastUpdatedResolutionOptionsSidecarCasUpdatesCount - lastUpdatedResolutionOptionsSidecarSmtProofsCount,
                "CASBeacon", lastUpdatedResolutionOptionsSidecarCasUpdatesCount,
                "SMTBeacon", lastUpdatedResolutionOptionsSidecarSmtProofsCount);

        Map<String, Integer> updateOperations = new HashMap<>();

        for (Map<String, Object> lastUpdatedResolutionOptionsSidecarUpdate : lastUpdatedResolutionOptionsSidecarUpdates) {
            List<Map<String, Object>> patches = (List<Map<String, Object>>) lastUpdatedResolutionOptionsSidecarUpdate.get("patch");
            Set<String> patchOps = new HashSet<>();
            for (Map<String, Object> patch : patches) patchOps.add((String) patch.get("op"));
            for  (String patchOp : patchOps) {
                updateOperations.computeIfAbsent(patchOp, (k) -> 0);
                updateOperations.computeIfPresent(patchOp, (k, v) -> v+1);
            }
        }

        int totalUpdates = lastUpdatedResolutionOptionsSidecarUpdatesCount;

        List<Integer> updateSizes = new ArrayList<>();
        lastUpdatedResolutionOptionsSidecarUpdates.stream().distinct().forEach(x -> updateSizes.add(toJson(x).length()));

        // output

        System.out.println("Number of DIDs: " + didsCount);
        System.out.println("Number of update rounds: " + updateRoundsCount);
        System.out.println("Total beacon types: " + beaconTypeCounts);
        System.out.println("Total aggregation cohort types and sizes: " + cohortTypesAndSizes);
        System.out.println("Update probabilities per round per DID: " + MainUpdate.DECIDE_UPDATE_STRATEGY);
        System.out.println("Actual update count: " + totalUpdates);
        System.out.println("Actual update counts per round: " + updatesPerRound.stream().map(i -> Integer.toString(i)).collect(Collectors.joining(",")));
        System.out.println("Beacon type probabilities per update: " + MainUpdate.SELECT_BEACON_STRATEGY);
        System.out.println("Actual updates by beacon types: " + updatesByBeaconTypes);
        System.out.println("Actual update operations: " + updateOperations);
        System.out.println("Actual update sizes: " + updateSizes.stream().map(i -> Integer.toString(i)).collect(Collectors.joining(",")));
        System.out.println("Actual updates per round per DID: ");
        for (String createdDid : createdDids) {
            System.out.println(createdDid + " -> " + updatesByRoundByDid.get(createdDid).values().stream().map(b -> b ? "O" : ".").collect(Collectors.joining(" ")) + " (" + updatesByDid.get(createdDid) + ")");
        }
    }

    private static void readCreateOut(String path, List<String> createdDids, Map<String, Map<String, Object>> createdResolutionOptions) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(path + "create-out.jsonl"));
        String line;
        while ((line=bufferedReader.readLine()) != null) {
            if (line.startsWith("did:"))
                createdDids.add(line);
            else
                createdResolutionOptions.put(createdDids.getLast(), jsonMapper.readValue(line, Map.class));
        }
        bufferedReader.close();
    }

    private static void readUpdateOuts(String path, List<List<String>> updatedDidsRounds, List<Map<String, Map<String, Object>>> updatedResolutionOptionsRounds) throws IOException {
        int round = 0;
        while (true) {
            round++;
            File file = new File(path + "update-out-" + round + ".jsonl");
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

    private static String toJson(Object o) {
        try {
            return jsonMapper.writeValueAsString(o);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }
}
