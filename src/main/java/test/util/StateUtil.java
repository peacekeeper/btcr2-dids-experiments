package test.util;

import uniregistrar.openapi.model.DidStateAction;
import uniregistrar.openapi.model.DidStateWait;
import uniregistrar.openapi.model.RegistrarState;

import java.util.*;

public class StateUtil {

    private static Map<Object, String> stateMap = Collections.synchronizedMap(new HashMap<>());

    public static String stateStats(Object key, RegistrarState registrarState) {
        stateMap.put(key, stateString(registrarState, false));
        Set<String> valueSet = new HashSet<>(stateMap.values());
        StringBuilder stringBuilder = new StringBuilder();
        valueSet.forEach(value -> {
            stringBuilder.append(value);
            stringBuilder.append(":");
            stringBuilder.append(stateMap.entrySet().stream().filter(x -> value.equals(x.getValue())).count());
            stringBuilder.append(", ");
        });
        return stringBuilder.toString();
    }

    public static String stateToString(RegistrarState registrarState) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(registrarState.getDidState().getDid());
        stringBuilder.append(" -> ");
        stringBuilder.append(stateString(registrarState, true));
        return stringBuilder.toString();
    }

    private static String stateString(RegistrarState registrarState, boolean details) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(registrarState.getDidState().getState());
        stringBuilder.append("(");
        switch (registrarState.getDidState().getState()) {
            case "finished" -> {
                stringBuilder.append(")");
                if (details) {
                    stringBuilder.append(": ");
                    stringBuilder.append(registrarState.getDidState().getDid());
                }
            }
            case "action" -> {
                stringBuilder.append(((DidStateAction) registrarState.getDidState()).getAction());
                stringBuilder.append(")");
                if (details) {
                    switch (((DidStateAction) registrarState.getDidState()).getAction()) {
                        case "getVerificationMethod" -> {
                            stringBuilder.append(": ");
                            stringBuilder.append(((DidStateAction) registrarState.getDidState()).getVerificationMethodTemplate().stream().map(x -> x.getId() + " " + x.getPublicKeyJwk()).toList());
                        }
                        case "signPayload" -> {
                            stringBuilder.append(": ");
                            stringBuilder.append(((DidStateAction) registrarState.getDidState()).getSigningRequest().values().stream().map(x -> x.getKid() + " " + x.getAlg()).toList());
                        }
                        case "fundAddress" -> {
                            stringBuilder.append(": ");
                            stringBuilder.append(registrarState.getDidState().getAdditionalProperty("address") + " " + registrarState.getDidState().getAdditionalProperty("minimumValue"));
                        }
                    }
                }
            }
            case "wait" -> {
                stringBuilder.append(((DidStateWait) registrarState.getDidState()).getWait());
                stringBuilder.append(")");
                if (details) {
                    switch (((DidStateWait) registrarState.getDidState()).getWait()) {
                        case "completeAggregationCohort" -> {
                            stringBuilder.append(": ");
                            stringBuilder.append(registrarState.getDidState().getAdditionalProperty("aggregationCohorts"));
                        }
                        case "completeAggregationUpdates" -> {
                            stringBuilder.append(": ");
                            stringBuilder.append(registrarState.getDidState().getAdditionalProperty("aggregationCohort"));
                        }
                        case "completeAggregationSignatures" -> {
                            stringBuilder.append(": ");
                            stringBuilder.append(registrarState.getDidState().getAdditionalProperty("aggregationCohort"));
                        }
                        case null, default -> {
                            stringBuilder.append(": ");
                            stringBuilder.append(((DidStateWait) registrarState.getDidState()).getWaitTime());
                        }
                    }
                }
            }
        }
        return stringBuilder.toString();
    }
}
