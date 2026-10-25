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

        String query = ctx.req().getQueryString();
        String target = backendBase + ctx.path();
        String method = ctx.req().getMethod();

        target  = (query != null) ? target += "?" + query : target;

        HttpRequest.Builder builder = HttpRequest.newBuilder().header("Secret", "KIIT")
                        .uri(URI.create(target))
                                . timeout(Duration.ofSeconds(3));


        (method.equalsIgnoreCase("POST") ?
                builder.POST(HttpRequest.BodyPublishers.ofByteArray(ctx.bodyAsBytes())):
                builder.GET()).
                build();

        HttpRequest httpRequest = builder.build();

        ctx.future(() -> client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofByteArray())
                .thenAccept(response -> {
                    ctx.status(response.statusCode());
                    ctx.result(response.body());
                }).exceptionally(e -> {
                    if (e.getCause() instanceof HttpConnectTimeoutException) {
                        ctx.status(504);
                        ctx.result("Timeout");
                    }
                    else {
                        ctx.status(502);
                        ctx.result("Bad Gateway");
                    }
                    return null;
                })
        );
    }
}
