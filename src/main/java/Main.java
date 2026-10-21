import db.Database;
import io.javalin.Javalin;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class Main {
    public static void main(String[] args) throws SQLException {
        Javalin backend = Javalin.create();

        backend.get("/backend", ctx -> {
            ctx.result("Backend");
        });

        backend.post("/post", ctx -> {
            ctx.result(ctx.body());
        });

        backend.start(9090);

        Javalin proxy = Javalin.create();

        HttpClient httpClient = HttpClient.newHttpClient();

        proxy.get("/*", ctx -> {
            String method = ctx.method().toString();
            String path = ctx.path();
            String clientIP = ctx.ip();
            System.out.println("Method: " + method + " Path: " + path + " IP: " + clientIP);
            ctx.result("Proxy is working");

            HttpRequest httpRequest = HttpRequest.newBuilder().uri(URI.create("http://localhost:9090" + ctx.path())).timeout(Duration.ofSeconds(4)).GET().build();

            ctx.future(() ->
                    httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofByteArray())
                            .thenAccept(response -> {
                                ctx.status(response.statusCode());
                                ctx.result(response.body());
                            })
                            .exceptionally(exception -> {
                                ctx.result("Error. Report at 24052671@kiit.ac.in");
                                return null;
                            })
            );

        });

        proxy.post("/*", ctx -> {
            System.out.println("Method: POST Path: " + ctx.path() + " IP: " + ctx.ip());

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:9090" + ctx.path()))
                    .timeout(Duration.ofSeconds(3))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(ctx.bodyAsBytes()))
                    .build();

            ctx.future(() ->
                    httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofByteArray())
                            .thenAccept(response -> {
                                ctx.status(response.statusCode());
                                ctx.result(response.body());
                            })
                            .exceptionally(e -> {
                                ctx.result("Error. Report at 24052671@kiit.ac.in");
                                return null;
                            })
            );
        });


        proxy.start(8080);
    }
}