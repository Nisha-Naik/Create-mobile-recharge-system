package com.rechargeapp;

import com.rechargeapp.http.RechargeHttpServer;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        Path projectRoot = Paths.get("").toAbsolutePath().normalize();
        Path webRoot = projectRoot.resolve("web");
        String historyOverride = System.getenv("RECHARGE_HISTORY_FILE");
        Path historyFile = historyOverride == null || historyOverride.isBlank()
                ? projectRoot.resolve("data").resolve("recharge-history.csv")
                : Paths.get(historyOverride).toAbsolutePath().normalize();

        RechargeHttpServer server = new RechargeHttpServer(port, webRoot, historyFile);
        server.start();

        System.out.println("Mobile Recharge System is running");
        System.out.println("Open http://localhost:" + port);
        System.out.println("Press Ctrl+C to stop the server");
    }
}
