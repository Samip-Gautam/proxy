import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ProxyHandler implements Handler {
    private final String backendBase;
    private final HttpClient client;

    ProxyHandler(String backendBase, HttpClient client) {
        this.backendBase = backendBase;
        this.client = client;
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        System.out.println("Method: " + ctx.method());
        System.out.println("Path: " + ctx.path());
        System.out.println("IP: " + ctx.ip());

        HttpRequest httpRequest = HttpRequest.newBuilder().uri(URI.create(backendBase + ctx.path())).timeout(Duration.ofSeconds(4)).build();

        ctx.future(() -> client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofByteArray())
                .thenAccept(response -> {
                    ctx.status(response.statusCode());
                    ctx.result(response.body());
                }).exceptionally(e -> {
                    if (e.getCause() instanceof HttpConnectTimeoutException) {
                        ctx.status(504);
                        ctx.result("Timeout");
                        return null;
                    }
                    else {
                        ctx.status(502);
                        ctx.result("Bad Gateway");
                        return null;
                    }
                })
        );
    }
}
