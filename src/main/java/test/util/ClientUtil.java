package test.util;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.util.Timeout;
import uniregistrar.RegistrationException;
import uniregistrar.client.ClientUniRegistrar;
import uniregistrar.openapi.model.CreateRequest;
import uniregistrar.openapi.model.CreateState;
import uniregistrar.openapi.model.UpdateRequest;
import uniregistrar.openapi.model.UpdateState;

import java.net.URI;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class ClientUtil {

    //private static final URI BASE_URI = URI.create("https://api.dev.godiddy.com/1.0.0/universal-registrar");
    //private static final String API_KEY = "c4aa5dd6-4a08-4014-8510-17eb99eddb79";
    //private static final URI BASE_URI = URI.create("https://api.godiddy.com/1.0.0/universal-registrar");
    //private static final String API_KEY = "06c9d23a-589d-46f4-90e4-bc707b0252a1";
    private static final URI BASE_URI = URI.create("http://localhost:9080/1.0/");
    private static final String API_KEY = null;

    private static Semaphore semaphore = new Semaphore(1);

    private static ClientUniRegistrar clientUniRegistrar() {

        ClientUniRegistrar clientUniRegistrar;
        clientUniRegistrar = ClientUniRegistrar.create(BASE_URI);
        clientUniRegistrar.setHttpClient(HttpClients.custom()
                .setConnectionManager(
                        PoolingHttpClientConnectionManagerBuilder
                                .create()
                                .setMaxConnTotal(1)
                                .setMaxConnPerRoute(1)
                                .setConnPoolPolicy(PoolReusePolicy.FIFO)
                                .setDefaultConnectionConfig(
                                        ConnectionConfig.custom()
                                                .setConnectTimeout(Timeout.ofSeconds(10))
                                                .setSocketTimeout(Timeout.ofSeconds(10))
                                                .build()
                                )
                                .build())
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(Timeout.ofSeconds(10))
                        .setResponseTimeout(Timeout.ofSeconds(10))
                        .build())
                .build());
        if (API_KEY != null) clientUniRegistrar.setHttpHeaders(Map.of("Authorization", "Bearer " + API_KEY));
        return clientUniRegistrar;
    }

    public static synchronized CreateState create(CreateRequest createRequest) throws InterruptedException, RegistrationException {
        while (true) {
            try {
                return clientUniRegistrar().create("btcr2", createRequest);
            } catch (RegistrationException ex) {
                throw ex;
            } catch (Exception ex) {
                System.out.println("Exception during create(): " + ex.getMessage());
                Thread.sleep(new Random().nextInt(1000));
            }
        }
    }

    public static synchronized UpdateState update(UpdateRequest updateRequest) throws InterruptedException, RegistrationException {
        while (true) {
            try {
                return clientUniRegistrar().update("btcr2", updateRequest);
            } catch (RegistrationException ex) {
                throw ex;
            } catch (Exception ex) {
                System.out.println("Exception during update(): " + ex.getMessage());
                Thread.sleep(new Random().nextInt(1000));
            }
        }
    }
}
