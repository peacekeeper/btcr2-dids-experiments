package test.analyze;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import foundation.identity.did.DIDDocument;
import foundation.identity.did.Service;
import foundation.identity.jsonld.JsonLDUtils;
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

    private static final String PATH = "./data3-3/";
    private static final int TOTAL_RESOLUTION_TIME_PER_VERSIONID = 5;
    private static final int TOTAL_RESOLUTION_TIME_PER_CID = 20;

    private static final Pattern COHORT_ID_PATTERN_MAXSIZE = Pattern.compile("^cohort-([a-z0-9]+)-([a-z0-9]{3})-([0-9]+)(\\*([0-9]+))?$");

    private static final JsonMapper jsonMapper = JsonMapper.builder().build();

    public static void main(String[] args) throws Exception {

        // read input

        List<String> createdDids = new ArrayList<>();
        Map<String, Map<String, Object>> createdResolutionOptions = new HashMap<>();
        readCreateOut(PATH, createdDids, createdResolutionOptions);
        System.out.println("createdDids: " + createdDids.size());

        List<List<String>> updatedDidsRounds = new ArrayList<>();
        List<Map<String, Map<String, Object>>> updatedResolutionOptionsRounds = new ArrayList<>();
        readUpdateOuts(PATH, updatedDidsRounds, updatedResolutionOptionsRounds);
        System.out.println("updatedDidsRounds: " + updatedDidsRounds.size());

        List<List<String>> resolvedDidsRounds = new ArrayList<>();
        List<Map<String, ResolveResult>> resolvedResolveResults = new ArrayList<>();
        readResolveOuts(PATH, resolvedDidsRounds, resolvedResolveResults);
        System.out.println("readResolveOuts: " + resolvedDidsRounds.size());

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
        // versionId,resolutionTime,casAnnouncementCount,smtProofCount,genesisDocumentCidCount,updateCidCount,casAnnouncementCid  by round by DID

        Map<Integer, Integer> updateCountPerRound = new LinkedHashMap<>();
        for (int updateRound=1; updateRound<updatedDidsRounds.size()+1; updateRound++) updateCountPerRound.put(updateRound, 0);

        Map<String, Integer> updateCountByDid = new LinkedHashMap<>();
        for (String did : createdDids) updateCountByDid.put(did, 0);

        Map<String, Integer> updateCountByCohort = new LinkedHashMap<>();
        for (String cohort : cohorts) updateCountByCohort.put(cohort, 0);

        Map<String, Map<Integer, Boolean>> isUpdateByRoundByDid = new LinkedHashMap<>();
        for (String did : createdDids) isUpdateByRoundByDid.put(did, new HashMap<>());
        for (String did : createdDids) for (int updateRound=1; updateRound<updatedDidsRounds.size()+1; updateRound++) isUpdateByRoundByDid.get(did).put(updateRound, Boolean.FALSE);

        Map<String, Map<Integer, Integer>> updateCountByRoundByCohort = new LinkedHashMap<>();
        for (String cohort : cohorts) updateCountByRoundByCohort.put(cohort, new HashMap<>());
        for (String cohort : cohorts) for (int round=1; round<updatedDidsRounds.size()+1; round++) updateCountByRoundByCohort.get(cohort).put(round, 0);

        Map<String, Map<Integer, Integer>> versionIdByRoundByDid = new LinkedHashMap<>();
        for (String did : createdDids) versionIdByRoundByDid.put(did, new HashMap<>());

        Map<String, Map<Integer, Integer>> resolutionTimeByRoundByDid = new LinkedHashMap<>();
        for (String did : createdDids) resolutionTimeByRoundByDid.put(did, new HashMap<>());

        Map<String, Map<Integer, Integer>> casAnnouncementCountByRoundByDid = new LinkedHashMap<>();
        for (String did : createdDids) casAnnouncementCountByRoundByDid.put(did, new HashMap<>());

        Map<String, Map<Integer, Integer>> smtProofCountByRoundByDid = new LinkedHashMap<>();
        for (String did : createdDids) smtProofCountByRoundByDid.put(did, new HashMap<>());

        Map<String, Map<Integer, Integer>> genesisDocumentCidCountByRoundByDid = new LinkedHashMap<>();
        for (String did : createdDids) genesisDocumentCidCountByRoundByDid.put(did, new HashMap<>());

        Map<String, Map<Integer, Integer>> updateCidCountByRoundByDid = new LinkedHashMap<>();
        for (String did : createdDids) updateCidCountByRoundByDid.put(did, new HashMap<>());

        Map<String, Map<Integer, Integer>> casAnnouncementCidCountByRoundByDid = new LinkedHashMap<>();
        for (String did : createdDids) casAnnouncementCidCountByRoundByDid.put(did, new HashMap<>());

        Map<String, Integer> lastVersionId = new HashMap<>();

        for (int resolveRound=0; resolveRound<resolvedResolveResults.size(); resolveRound++) {

            Map<String, Map<String, Object>> roundResolutionOptions = resolveRound == 0 ? createdResolutionOptions : updatedResolutionOptionsRounds.get(resolveRound-1);

            for (Map.Entry<String, Map<String, Object>> resolveResolutionOptionsEntry : roundResolutionOptions.entrySet()) {

                String did = resolveResolutionOptionsEntry.getKey();
                Map<String, Object> resolutionOptions = resolveResolutionOptionsEntry.getValue();
                ResolveResult resolveResult = resolvedResolveResults.get(resolveRound).get(did);

                int versionId = Integer.parseInt((String) resolutionOptions.get("versionId"));
                if (versionId <= lastVersionId.computeIfAbsent(did, (k) -> 0)) continue;

                String lastUpdateHash = ! ((List<Map<String, Object>>) resolveResult.getDidDocumentMetadata().get("updates")).isEmpty() ? (String) ((List<Map<String, Object>>) resolveResult.getDidDocumentMetadata().get("updates")).getLast().get("updateHash") : "";
                Map<String, Object> beaconSignals = (Map<String, Object>) resolveResult.getDidDocumentMetadata().get("beaconSignals");
                String cohortAddress = beaconSignals.entrySet().stream().filter(x -> ((Map<String, Object>) x.getValue()).containsValue(lastUpdateHash)).map(Map.Entry::getKey).findAny().orElse(null);
                String cohort = cohortAddresses.get(cohortAddress);

                if (resolveRound > 0) updateCountPerRound.computeIfPresent(resolveRound, (k, v) -> v+1);
                if (resolveRound > 0) updateCountByDid.computeIfPresent(did, (k, v) -> v+1);
                if (resolveRound > 0 && cohort != null) updateCountByCohort.computeIfPresent(cohort, (k, v) -> v+1);
                if (resolveRound > 0) isUpdateByRoundByDid.get(did).put(resolveRound, Boolean.TRUE);
                if (resolveRound > 0 && cohort != null) updateCountByRoundByCohort.get(cohort).computeIfPresent(resolveRound, (k, v) -> v+1);

                versionIdByRoundByDid.get(did).put(resolveRound, Integer.parseInt((String) resolveResult.getDidDocumentMetadata().get("versionId")));
                resolutionTimeByRoundByDid.get(did).put(resolveRound, ((Number) resolveResult.getDidResolutionMetadata().get("duration")).intValue());
                casAnnouncementCountByRoundByDid.get(did).put(resolveRound, resolveResult.getDidDocumentMetadata().containsKey("casAnnouncements") ? ((Map<String, Object>) resolveResult.getDidDocumentMetadata().get("casAnnouncements")).size() : 0);
                smtProofCountByRoundByDid.get(did).put(resolveRound, resolveResult.getDidDocumentMetadata().containsKey("smtProofs") ? ((Map<String, Object>) resolveResult.getDidDocumentMetadata().get("smtProofs")).size() : 0);
                genesisDocumentCidCountByRoundByDid.get(did).put(resolveRound, resolveResult.getDidResolutionMetadata().containsKey("genesisDocumentCid") ? 1 : 0);
                updateCidCountByRoundByDid.get(did).put(resolveRound, resolveResult.getDidResolutionMetadata().containsKey("updateCids") ? ((Map<String, Object>) resolveResult.getDidResolutionMetadata().get("updateCids")).size() : 0);
                casAnnouncementCidCountByRoundByDid.get(did).put(resolveRound, resolveResult.getDidResolutionMetadata().containsKey("casAnnouncementCids") ? ((Map<String, Object>) resolveResult.getDidResolutionMetadata().get("casAnnouncementCids")).size() : 0);

                lastVersionId.put(did, versionId);
            }
        }

        // count sidecar and result objects

        Map<String, Map<String, Object>> lastResolutionOptions = updatedResolutionOptionsRounds.getLast();

        List<Map<String, Object>> lastSidecars = lastResolutionOptions.values().stream().map(x -> (Map<String, Object>) x.get("sidecar")).toList();
        List<Map<String, Object>> lastSidecarGenesisDocuments = new ArrayList<>();
        List<Map<String, Object>> lastSidecarUpdates = new ArrayList<>();
        List<Map<String, Object>> lastSidecarCasUpdates = new ArrayList<>();
        List<Map<String, Object>> lastSidecarSmtProofs = new ArrayList<>();

        for (Map<String, Object> lastSidecar : lastSidecars) {
            if (lastSidecar.containsKey("genesisDocument")) lastSidecarGenesisDocuments.add((Map<String, Object>) lastSidecar.get("genesisDocument"));
            if (lastSidecar.containsKey("updates")) lastSidecarUpdates.addAll((List<Map<String, Object>>) lastSidecar.get("updates"));
            if (lastSidecar.containsKey("casUpdates")) lastSidecarCasUpdates.addAll((List<Map<String, Object>>) lastSidecar.get("casUpdates"));
            if (lastSidecar.containsKey("smtProofs")) lastSidecarSmtProofs.addAll((List<Map<String, Object>>) lastSidecar.get("smtProofs"));
        }

        int totalLastSidecarGenesisDocumentCount = lastSidecarGenesisDocuments.size();
        int totalLastSidecarUpdatesCount = lastSidecarUpdates.size();
        int totalLastSidecarCasUpdatesCount = lastSidecarCasUpdates.size();
        int totalLastSidecarSmtProofsCount = lastSidecarSmtProofs.size();

        List<Integer> lastGenesisDocumentCidCounts = genesisDocumentCidCountByRoundByDid.values().stream().map(m -> m.values().stream().mapToInt(i -> i).max().orElse(0)).toList();
        List<Integer> lastUpdateCidCounts = updateCidCountByRoundByDid.values().stream().map(m -> m.values().stream().mapToInt(i -> i).max().orElse(0)).toList();
        List<Integer> lastCasAnnouncementCidCounts = casAnnouncementCidCountByRoundByDid.values().stream().map(m -> m.values().stream().mapToInt(i -> i).max().orElse(0)).toList();

        int totalLastGenesisDocumentCidCount = lastGenesisDocumentCidCounts.stream().mapToInt(Integer::intValue).sum();
        int totalLastUpdateCidCount = lastUpdateCidCounts.stream().mapToInt(Integer::intValue).sum();
        int totalLastCasAnnouncementCidCount = lastCasAnnouncementCidCounts.stream().mapToInt(Integer::intValue).sum();

        // count updates
        // update sizes

        int updatesCount = updateCountByDid.values().stream().mapToInt(i -> i).sum();

        List<Integer> updateSizes = new ArrayList<>();
        lastSidecarUpdates.stream().distinct().forEach(x -> updateSizes.add(toJson(x).length()));

        // count updates by beacon types
        // count updates by update operation

        int updateCountCas = updateCountByCohort.entrySet().stream().filter(e -> e.getKey().contains("cas")).mapToInt(Map.Entry::getValue).sum();
        int updateCountSmt = updateCountByCohort.entrySet().stream().filter(e -> e.getKey().contains("smt")).mapToInt(Map.Entry::getValue).sum();

        final Map<String, Integer> updatesByBeaconTypes = Map.of(
                "SingletonBeacon", updatesCount - updateCountCas - updateCountSmt,
                "CASBeacon", updateCountCas,
                "SMTBeacon", updateCountSmt
        );

        Map<String, Integer> updateOperations = new HashMap<>();

        for (Map<String, Object> lastUpdatedResolutionOptionsSidecarUpdate : lastSidecarUpdates) {
            List<Map<String, Object>> patches = (List<Map<String, Object>>) lastUpdatedResolutionOptionsSidecarUpdate.get("patch");
            Set<String> patchOps = new HashSet<>();
            for (Map<String, Object> patch : patches) patchOps.add((String) patch.get("op"));
            for (String patchOp : patchOps) {
                updateOperations.putIfAbsent(patchOp, 0);
                updateOperations.computeIfPresent(patchOp, (k, v) -> v+1);
            }
        }

        // output

        System.out.println("Number of DIDs: " + didsCount);
        System.out.println("Number of update rounds: " + updateRoundsCount);
        System.out.println("Total beacons: " + beaconsByTypeCounts);
        System.out.println("Total beacon services: " + beaconServicesByTypeCounts);
        System.out.println("Total aggregation cohort types and sizes: " + cohortTypesAndSizes);
        System.out.println("Update probabilities per round per DID: " + "---");
        System.out.println("Actual update count: " + updatesCount);
        System.out.println("Actual update counts per round: " + updateCountPerRound.values().stream().map(x -> Integer.toString(x)).collect(Collectors.joining(",")));
        System.out.println("Beacon type probabilities per update: " + "---");
        System.out.println("Actual updates by beacon types: " + updatesByBeaconTypes);
        System.out.println("Actual update operations: " + updateOperations);
        System.out.println("Actual update sizes: " + updateSizes.stream().map(i -> Integer.toString(i)).collect(Collectors.joining(",")));
        System.out.println("Actual updates per round per DID: ");
        for (String createdDid : createdDids) {
            System.out.println(createdDid + " -> " + isUpdateByRoundByDid.get(createdDid).values().stream().map(b -> b ? "O" : ".").collect(Collectors.joining(" ")) + " (" + updateCountByDid.get(createdDid) + ")");
        }
        System.out.println("Actual updates per round per cohort: ");
        for (String cohort : cohorts.stream().sorted().toList()) {
            System.out.println(String.format("%-35s", cohort) + " -> " + updateCountByRoundByCohort.get(cohort).values().stream().map(i -> Integer.toString(i)).collect(Collectors.joining(" ")) + " (" + updateCountByCohort.get(cohort) + ")");
        }
        System.out.println("Resolution times per round per DID: ");
        System.out.println("versionId, totalResolutionTime, resolutionTime, casAnnouncementCount, smtProofCount, genesisDocumentCidCount, updateCidCount, casAnnouncementCidCount");
        for (String createdDid : createdDids) {
            for (int round : new TreeSet<>(versionIdByRoundByDid.get(createdDid).keySet())) {
                int totalResolutionTIme =
                        resolutionTimeByRoundByDid.get(createdDid).get(round) +
                                TOTAL_RESOLUTION_TIME_PER_VERSIONID * (
                                        versionIdByRoundByDid.get(createdDid).get(round)
                                ) +
                                TOTAL_RESOLUTION_TIME_PER_CID * (
                                        genesisDocumentCidCountByRoundByDid.get(createdDid).get(round) +
                                                updateCidCountByRoundByDid.get(createdDid).get(round) +
                                                casAnnouncementCidCountByRoundByDid.get(createdDid).get(round)
                                );
                System.out.println(createdDid +
                        "," + versionIdByRoundByDid.get(createdDid).get(round) +
                        "," + totalResolutionTIme +
                        "," + resolutionTimeByRoundByDid.get(createdDid).get(round) +
                        "," + casAnnouncementCountByRoundByDid.get(createdDid).get(round) +
                        "," + smtProofCountByRoundByDid.get(createdDid).get(round) +
                        "," + genesisDocumentCidCountByRoundByDid.get(createdDid).get(round) +
                        "," + updateCidCountByRoundByDid.get(createdDid).get(round) +
                        "," + casAnnouncementCidCountByRoundByDid.get(createdDid).get(round));
            }
        }
        System.out.println("Sidecar/CAS genesis documents: " + totalLastSidecarGenesisDocumentCount + ":" + totalLastGenesisDocumentCidCount);
        System.out.println("Sidecar/CAS updates: " + totalLastSidecarUpdatesCount + ":" + totalLastUpdateCidCount);
        System.out.println("Sidecar/CAS CAS announcements: " + totalLastSidecarCasUpdatesCount + ":" + totalLastCasAnnouncementCidCount);
        System.out.println("Sidecar SMT proofs: " + totalLastSidecarSmtProofsCount);
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
        int resolveRound = 0;
        while (true) {
            File file = new File(path + "resolve-out-" + resolveRound + ".jsonl");
            if (! file.exists()) break;
            resolvedDidsRounds.add(new ArrayList<>());
            resolvedResolveResults.add(new HashMap<>());
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String line;
            while ((line=bufferedReader.readLine()) != null) {
                if (line.startsWith("did:"))
                    resolvedDidsRounds.get(resolveRound).add(line);
                else
                    resolvedResolveResults.get(resolveRound).put(resolvedDidsRounds.get(resolveRound).getLast(), ResolveResult.fromJson(line));
            }
            bufferedReader.close();
            resolveRound++;
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
