import com.bank.web.WebServer;

/** Starts the web version: browser UI + JSON API. Usage: WebMain [port] (default 8080). */
public class WebMain {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        WebServer server = new WebServer(port);
        server.start();
        System.out.println("Banking web app running at http://localhost:" + server.getPort());
        System.out.println("Press Ctrl+C to stop.");
    }
}
