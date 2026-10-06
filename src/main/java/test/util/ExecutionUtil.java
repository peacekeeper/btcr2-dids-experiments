package test.util;

import com.danubetech.kms.clientkeyinterface.ClientKeyInterface;
import com.danubetech.kms.clientstateinterface.ClientStateInterface;
import com.danubetech.uniregistrar.local.internalsecretmode.InternalSecretMode;
import uniregistrar.openapi.model.CreateRequest;
import uniregistrar.openapi.model.RegistrarRequest;
import uniregistrar.openapi.model.RegistrarState;
import uniregistrar.openapi.model.UpdateRequest;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ExecutionUtil {

    private static final Random random = new Random();

    public static <R extends RegistrarRequest, S extends RegistrarState> void submitIteration(ScheduledExecutorService executorService, ClientKeyInterface<?> clientKeyInterface, ClientStateInterface clientStateInterface, final Object task, final int iteration, final R registrarRequest, final CompletableFuture<S> future) throws Exception {

        System.out.println("Submitting iteration: " + task + "/" + iteration);

        executorService.schedule(() -> {

            try {

                S registrarState = (S) switch (registrarRequest) {
                    case CreateRequest createRequest -> ClientUtil.create(createRequest);
                    case UpdateRequest updateRequest -> ClientUtil.update(updateRequest);
                    default -> throw new IllegalStateException("Unexpected value: " + registrarRequest);
                };
                System.out.println(task + "/" + iteration + ": " + StateUtil.stateToString(registrarState));
                System.out.println(StateUtil.stateStats(task, registrarState));

                Optional<RegistrarRequest> nextRequest = InternalSecretMode.handleState(registrarRequest, registrarState, clientKeyInterface, clientStateInterface);

                if ("finished".equals(registrarState.getDidState().getState())) {
                    future.complete(registrarState);
                    return;
                }

                if (nextRequest == null || nextRequest.isEmpty()) {
                    nextRequest = Optional.of(registrarRequest);
                }

                ExecutionUtil.submitIteration(executorService, clientKeyInterface, clientStateInterface, task, iteration+1, nextRequest.get(), future);
            } catch (Exception ex) {
                future.completeExceptionally(new RuntimeException(task + "/" + iteration + ": " + ex.getMessage(), ex));
            }
        }, random.nextInt(100), TimeUnit.MILLISECONDS);
    }
}
