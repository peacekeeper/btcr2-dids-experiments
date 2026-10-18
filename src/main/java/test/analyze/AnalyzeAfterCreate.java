package test.analyze;

import com.fasterxml.jackson.databind.json.JsonMapper;
import foundation.identity.did.DIDDocumentV1_1;
import foundation.identity.did.Service;
import foundation.identity.jsonld.JsonLDUtils;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalyzeAfterCreate {

    private static final int FUNDING = 3000;

    private static final JsonMapper jsonMapper = JsonMapper.builder().build();

    public static void main(String[] args) throws Exception {

        // read input

        List<String> createdDids = new ArrayList<>();
        Map<String, Map<String, Object>> createdResolutionOptions = new HashMap<>();
        readCreateOut(createdDids, createdResolutionOptions);
        System.out.println("createdDids: " + createdDids);

        // analyze

        Map<String, List<String>> singletonBeacons = new HashMap<>();
        Map<String, List<String>> casBeacons = new HashMap<>();
        Map<String, List<String>> smtBeacons = new HashMap<>();
        Map<String, List<String>> beacons = new HashMap<>();

        createdResolutionOptions.forEach((did, resolutionOptions) -> {
            DIDDocumentV1_1 didDocumentV11 = DIDDocumentV1_1.fromMap((Map<String, Object>) ((Map<String, Object>) resolutionOptions.get("sidecar")).get("genesisDocument"));
            for (Service service : didDocumentV11.getServices()) {
                String id = service.getId().toString();
                String type = service.getType();
                String serviceEndpoint = JsonLDUtils.uriToString((URI) service.getServiceEndpoint());
                if (! serviceEndpoint.startsWith("bitcoin:")) continue;
                String address = serviceEndpoint.substring("bitcoin:".length());
                if ("SingletonBeacon".equals(type) && "#initialP2PKH".equals(id)) singletonBeacons.computeIfAbsent(address, s -> new ArrayList<>()).add(did);
                if ("CASBeacon".equals(type)) casBeacons.computeIfAbsent(address, s -> new ArrayList<>()).add(did);
                if ("SMTBeacon".equals(type)) smtBeacons.computeIfAbsent(address, s -> new ArrayList<>()).add(did);
            }
        });
        beacons.putAll(singletonBeacons);
        beacons.putAll(casBeacons);
        beacons.putAll(smtBeacons);

        // print result

        singletonBeacons.forEach((s, dids) -> System.out.println("SingletonBeacon " + s + " (" + dids.size() + "): " + dids));
        casBeacons.forEach((s, dids) -> System.out.println("CASBeacon " + s + " (" + dids.size() + "): " + dids));
        smtBeacons.forEach((s, dids) -> System.out.println("SMTBeacon " + s + " (" + dids.size() + "): " + dids));

        // write result to file

        PrintWriter printWriter = new PrintWriter(new FileWriter("./analyze-out-fund.csv"));
        for (String beacon : beacons.keySet()) {
            printWriter.println(beacon + "," + FUNDING + "," + beacon);
        }
        printWriter.close();
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
}
