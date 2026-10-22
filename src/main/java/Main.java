import io.javalin.Javalin;
import java.net.http.HttpClient;
import java.sql.SQLException;

public class Main {
    public static String BACKEND_BASE = "http://localhost:9090";
    public static void main(String[] args) throws SQLException {
        Javalin backend = Javalin.create();

        backend.get("/index", ctx -> {
            ctx.result("Hello");
        });

        backend.post("/backend", ctx -> {
            ctx.result(ctx.body());
        }).start(9090);

        HttpClient httpClient = HttpClient.newHttpClient();
        ProxyHandler proxyHandler = new ProxyHandler(BACKEND_BASE, httpClient);

        Javalin proxy = Javalin.create();
        proxy.get("/*", proxyHandler);
        proxy.post("/*", proxyHandler);

        proxy.start(8080);
    }
}