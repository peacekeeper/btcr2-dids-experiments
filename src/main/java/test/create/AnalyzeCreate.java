package test.create;

import com.fasterxml.jackson.databind.json.JsonMapper;
import foundation.identity.did.DIDDocumentV1_1;
import foundation.identity.did.Service;
import foundation.identity.jsonld.JsonLDUtils;
import uniregistrar.openapi.model.CreateState;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalyzeCreate {

    static int FUNDING = 1000;

    static JsonMapper jsonMapper = JsonMapper.builder().build();

    public static void main(String[] args) throws Exception {

        // read input

        List<String> inputDids = new ArrayList<>();
        Map<String, Map<String, Object>> inputResolutionOptions = new HashMap<>();
        readCreateOut(inputDids, inputResolutionOptions);
        System.out.println("inputDids: " + inputDids);

        // analyze

        Map<String, List<String>> casBeacons = new HashMap<>();
        Map<String, List<String>> smtBeacons = new HashMap<>();
        Map<String, List<String>> beacons = new HashMap<>();

        inputResolutionOptions.forEach((did, resolutionOptions) -> {
            DIDDocumentV1_1 didDocumentV11 = DIDDocumentV1_1.fromMap((Map<String, Object>) ((Map<String, Object>) resolutionOptions.get("sidecar")).get("genesisDocument"));
            for (Service service : didDocumentV11.getServices()) {
                String type = service.getType();
                String serviceEndpoint = JsonLDUtils.uriToString((URI) service.getServiceEndpoint());
                if (! serviceEndpoint.startsWith("bitcoin:")) continue;
                String address = serviceEndpoint.substring("bitcoin:".length());
                if ("CASBeacon".equals(type)) casBeacons.computeIfAbsent(address, s -> new ArrayList<>()).add(did);
                if ("SMTBeacon".equals(type)) smtBeacons.computeIfAbsent(address, s -> new ArrayList<>()).add(did);
            }
        });
        beacons.putAll(casBeacons);
        beacons.putAll(smtBeacons);

        // print result

        casBeacons.forEach((s, dids) -> System.out.println("CASBeacon " + s + " (" + dids.size() + "): " + dids));
        smtBeacons.forEach((s, dids) -> System.out.println("SMTBeacon " + s + " (" + dids.size() + "): " + dids));

        // write result to file

        PrintWriter printWriter = new PrintWriter(new FileWriter("./analyze-out-fund.csv"));
        for (String beacon : beacons.keySet()) {
            printWriter.println(beacon + "," + FUNDING + "," + beacon);
        }
        printWriter.close();
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
}
