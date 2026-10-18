package test.analyze;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import foundation.identity.did.DIDDocument;
import foundation.identity.did.Service;
import foundation.identity.jsonld.JsonLDUtils;
import test.update.MainUpdate;
import uniresolver.result.ResolveResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AnalyzeAfterCreateAndUpdateAndResolve {

    private static final String PATH = "./data4/";

    private static final Pattern COHORT_ID_PATTERN_MAXSIZE = Pattern.compile("^cohort-([a-z0-9]+)-([a-z0-9]{3})-([0-9]+)(\\*([0-9]+))?$");

    private static final JsonMapper jsonMapper = JsonMapper.builder().build();

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

        List<List<String>> resolvedDidsRounds = new ArrayList<>();
        List<Map<String, ResolveResult>> resolvedResolveResults = new ArrayList<>();
        readResolveOuts(PATH, resolvedDidsRounds, resolvedResolveResults);
        System.out.println("readResolveOuts: " + resolvedDidsRounds);

        // analyze!

        // count DIDs

        final Set<String> didsSet = new HashSet<>();
        createdDids.forEach(didsSet::add);
        updatedDidsRounds.forEach(updatedDids -> updatedDids.forEach(didsSet::add));
        int didsCount = didsSet.size();

        // count update rounds

        int updateRoundsCount = updatedDidsRounds.size();

        // cohort types and sizes

        Set<String> cohorts = new HashSet<>();
        Map<String, String> cohortAddresses = new HashMap<>();
        resolvedResolveResults.getFirst().values().forEach(resolveResult -> {
            DIDDocument didDocument = resolveResult.getDidDocument();
            for (Service service : didDocument.getServices()) {
                if ("CASBeacon".equals(service.getType()) || "SMTBeacon".equals(service.getType())) {
                    cohorts.add(service.getId().toString().substring("#".length()));
                    cohortAddresses.put(JsonLDUtils.uriToString((URI) service.getServiceEndpoint()).substring("bitcoin:".length()), service.getId().toString().substring("#".length()));
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

        // count beacon services by type
        // count beacons by type

        final Map<String, Integer> beaconServicesByTypeCounts = new HashMap<>();
        beaconServicesByTypeCounts.putAll(Map.of("SingletonBeacon", 0, "CASBeacon", 0, "SMTBeacon", 0));

        Set<String> beaconsSingletonBeacons = new HashSet<>();
        Set<String> beaconsCASBeacons = new HashSet<>();
        Set<String> beaconsSMTBeacons = new HashSet<>();
        Map<String, Integer> beaconsByTypeCounts;

        resolvedResolveResults.getFirst().values().forEach(resolveResult -> {
            resolveResult.getDidDocument().getServices().forEach(service -> {
                if (service.getType().contains("Beacon")) {
                    beaconServicesByTypeCounts.computeIfPresent(service.getType(), (k, v) -> v+1);
                }
                switch (service.getType()) {
                    case "SingletonBeacon": beaconsSingletonBeacons.add(JsonLDUtils.uriToString((URI) service.getServiceEndpoint())); break;
                    case "CASBeacon": beaconsCASBeacons.add(JsonLDUtils.uriToString((URI) service.getServiceEndpoint())); break;
                    case "SMTBeacon": beaconsSMTBeacons.add(JsonLDUtils.uriToString((URI) service.getServiceEndpoint())); break;
                }
            });
        });

        beaconsByTypeCounts = Map.of("SingletonBeacon", beaconsSingletonBeacons.size(), "CASBeacon", beaconsCASBeacons.size(), "SMTBeacon", beaconsSMTBeacons.size());

        // count updates per round
        // count updates by DID
        // count updates by cohort
        // yes/no updates by round by DID
        // count updates by round by cohort
        // resolution time by round by DID
        // versionId by round by DID

        Map<Integer, Integer> updatesPerRound = new LinkedHashMap<>();
        for (int round=0; round<updatedDidsRounds.size(); round++) updatesPerRound.put(round, 0);

        Map<String, Integer> updatesByDid = new LinkedHashMap<>();
        for (String did : createdDids) updatesByDid.put(did, 0);

        Map<String, Integer> updatesByCohort = new LinkedHashMap<>();
        for (String cohort : cohorts) updatesByCohort.put(cohort, 0);

        Map<String, Map<Integer, Boolean>> updatesByRoundByDid = new LinkedHashMap<>();
        for (String did : createdDids) updatesByRoundByDid.put(did, new HashMap<>());
        for (String did : createdDids) for (int round=0; round<updatedDidsRounds.size(); round++) updatesByRoundByDid.get(did).put(round, Boolean.FALSE);

        Map<String, Map<Integer, Integer>> updatesByRoundByCohort = new LinkedHashMap<>();
        for (String cohort : cohorts) updatesByRoundByCohort.put(cohort, new HashMap<>());
        for (String cohort : cohorts) for (int round=0; round<updatedDidsRounds.size(); round++) updatesByRoundByCohort.get(cohort).put(round, 0);

        Map<String, Map<Integer, Integer>> versionIdsByRoundByDid = new LinkedHashMap<>();
        for (String did : createdDids) versionIdsByRoundByDid.put(did, new HashMap<>());

        Map<String, Map<Integer, Integer>> resolutionTimesByRoundByDid = new LinkedHashMap<>();
        for (String did : createdDids) resolutionTimesByRoundByDid.put(did, new HashMap<>());

        Map<String, Map<Integer, Integer>> casAnnouncementsByRoundByDid = new LinkedHashMap<>();
        for (String did : createdDids) casAnnouncementsByRoundByDid.put(did, new HashMap<>());

        Map<String, Map<Integer, Integer>> smtProofsByRoundByDid = new LinkedHashMap<>();
        for (String did : createdDids) smtProofsByRoundByDid.put(did, new HashMap<>());

        Map<String, Map<Integer, Integer>> genesisDocumentCidByRoundByDid = new LinkedHashMap<>();
        for (String did : createdDids) genesisDocumentCidByRoundByDid.put(did, new HashMap<>());

        Map<String, Map<Integer, Integer>> updateCidsByRoundByDid = new LinkedHashMap<>();
        for (String did : createdDids) updateCidsByRoundByDid.put(did, new HashMap<>());

        Map<String, Map<Integer, Integer>> casAnnouncementCidsByRoundByDid = new LinkedHashMap<>();
        for (String did : createdDids) casAnnouncementCidsByRoundByDid.put(did, new HashMap<>());

        Map<String, Integer> lastVersionId = new HashMap<>();

        for (int round=0; round<updatedDidsRounds.size(); round++) {

            Map<String, Map<String, Object>> updatedResolutionOptions = updatedResolutionOptionsRounds.get(round);

            for (Map.Entry<String, Map<String, Object>> updatedResolutionOptionsEntry : updatedResolutionOptions.entrySet()) {

                String did = updatedResolutionOptionsEntry.getKey();
                Map<String, Object> resolutionOptions = updatedResolutionOptionsEntry.getValue();

                int versionId = Integer.parseInt((String) resolutionOptions.get("versionId"));
                if (versionId <= lastVersionId.computeIfAbsent(did, (k) -> 1)) continue;

                ResolveResult resolveResult = resolvedResolveResults.get(round).get(did);
                String lastUpdateHash = (String) ((List<Map<String, Object>>) resolveResult.getDidDocumentMetadata().get("updates")).getLast().get("updateHash");
                Map<String, Object> beaconSignals = (Map<String, Object>) resolveResult.getDidDocumentMetadata().get("beaconSignals");
                String cohortAddress = beaconSignals.entrySet().stream().filter(
                        x -> ((Map<String, Object>) x.getValue()).containsValue(lastUpdateHash))
                        .map(Map.Entry::getKey)
                        .findAny()
                        .orElse(null);
                String cohort = cohortAddresses.get(cohortAddress);

                updatesPerRound.computeIfPresent(round, (k, v) -> v+1);
                updatesByDid.computeIfPresent(did, (k, v) -> v+1);
                updatesByCohort.computeIfPresent(cohort, (k, v) -> v+1);
                updatesByRoundByDid.get(did).put(round, Boolean.TRUE);
                if (cohort != null) updatesByRoundByCohort.get(cohort).computeIfPresent(round, (k, v) -> v+1);

                versionIdsByRoundByDid.get(did).put(round, Integer.parseInt((String) resolveResult.getDidDocumentMetadata().get("versionId")));
                resolutionTimesByRoundByDid.get(did).put(round, ((Number) resolveResult.getDidResolutionMetadata().get("duration")).intValue());
                casAnnouncementsByRoundByDid.get(did).put(round, resolveResult.getDidDocumentMetadata().containsKey("casAnnouncements") ? ((Map<String, Object>) resolveResult.getDidDocumentMetadata().get("casAnnouncements")).size() : 0);
                smtProofsByRoundByDid.get(did).put(round, resolveResult.getDidDocumentMetadata().containsKey("smtProofs") ? ((Map<String, Object>) resolveResult.getDidDocumentMetadata().get("smtProofs")).size() : 0);
                genesisDocumentCidByRoundByDid.get(did).put(round, resolveResult.getDidResolutionMetadata().containsKey("genesisDocumentCid") ? 1 : 0);
                updateCidsByRoundByDid.get(did).put(round, resolveResult.getDidResolutionMetadata().containsKey("updateCids") ? ((Map<String, Object>) resolveResult.getDidResolutionMetadata().get("updateCids")).size() : 0);
                casAnnouncementCidsByRoundByDid.get(did).put(round, resolveResult.getDidResolutionMetadata().containsKey("casAnnouncementCids") ? ((Map<String, Object>) resolveResult.getDidResolutionMetadata().get("casAnnouncementCids")).size() : 0);

                lastVersionId.put(did, versionId);
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
            for (String patchOp : patchOps) {
                updateOperations.putIfAbsent(patchOp, 0);
                updateOperations.computeIfPresent(patchOp, (k, v) -> v+1);
            }
        }

        int totalUpdates = lastUpdatedResolutionOptionsSidecarUpdatesCount;

        List<Integer> updateSizes = new ArrayList<>();
        lastUpdatedResolutionOptionsSidecarUpdates.stream().distinct().forEach(x -> updateSizes.add(toJson(x).length()));

        // output

        System.out.println("Number of DIDs: " + didsCount);
        System.out.println("Number of update rounds: " + updateRoundsCount);
        System.out.println("Total beacons: " + beaconsByTypeCounts);
        System.out.println("Total beacon services: " + beaconServicesByTypeCounts);
        System.out.println("Total aggregation cohort types and sizes: " + cohortTypesAndSizes);
        System.out.println("Update probabilities per round per DID: " + MainUpdate.DECIDE_UPDATE_STRATEGY);
        System.out.println("Actual update count: " + totalUpdates);
        System.out.println("Actual update counts per round: " + updatesPerRound.values().stream().map(x -> Integer.toString(x)).collect(Collectors.joining(",")));
        System.out.println("Beacon type probabilities per update: " + MainUpdate.SELECT_BEACON_STRATEGY);
        System.out.println("Actual updates by beacon types: " + updatesByBeaconTypes);
        System.out.println("Actual update operations: " + updateOperations);
        System.out.println("Actual update sizes: " + updateSizes.stream().map(i -> Integer.toString(i)).collect(Collectors.joining(",")));
        System.out.println("Actual updates per round per DID: ");
        for (String createdDid : createdDids) {
            System.out.println(createdDid + " -> " + updatesByRoundByDid.get(createdDid).values().stream().map(b -> b ? "O" : ".").collect(Collectors.joining(" ")) + " (" + updatesByDid.get(createdDid) + ")");
        }
        System.out.println("Actual updates per round per cohort: ");
        for (String cohort : cohorts) {
            System.out.println(String.format("%-35s", cohort) + " -> " + updatesByRoundByCohort.get(cohort).values().stream().map(i -> Integer.toString(i)).collect(Collectors.joining(" ")) + " (" + updatesByCohort.get(cohort) + ")");
        }
        System.out.println("Resolution times per round per DID: ");
        for (String createdDid : createdDids) {
            for (int round : new TreeSet<>(versionIdsByRoundByDid.get(createdDid).keySet())) {
                System.out.println(createdDid +
                        "," + versionIdsByRoundByDid.get(createdDid).get(round) +
                        "," + resolutionTimesByRoundByDid.get(createdDid).get(round) +
                        "," + casAnnouncementsByRoundByDid.get(createdDid).get(round) +
                        "," + smtProofsByRoundByDid.get(createdDid).get(round) +
                        "," + genesisDocumentCidByRoundByDid.get(createdDid).get(round) +
                        "," + updateCidsByRoundByDid.get(createdDid).get(round) +
                        "," + casAnnouncementCidsByRoundByDid.get(createdDid).get(round));
            }
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

    private static void readResolveOuts(String path, List<List<String>> resolvedDidsRounds, List<Map<String, ResolveResult>> resolvedResolveResults) throws IOException {
        int round = 0;
        while (true) {
            round++;
            File file = new File(path + "resolve-out-" + round + ".jsonl");
            if (! file.exists()) break;
            resolvedDidsRounds.add(new ArrayList<>());
            resolvedResolveResults.add(new HashMap<>());
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String line;
            while ((line=bufferedReader.readLine()) != null) {
                if (line.startsWith("did:"))
                    resolvedDidsRounds.get(round-1).add(line);
                else
                    resolvedResolveResults.get(round-1).put(resolvedDidsRounds.get(round-1).getLast(), ResolveResult.fromJson(line));
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
