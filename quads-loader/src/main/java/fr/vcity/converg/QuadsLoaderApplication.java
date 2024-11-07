package fr.vcity.converg;

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.model.snapshots.Unit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class QuadsLoaderApplication {

    public static void main(String[] args) throws IOException, InterruptedException {
        SpringApplication.run(QuadsLoaderApplication.class, args);

        JvmMetrics.builder().register();

        HTTPServer server = HTTPServer.builder()
                .port(9400)
                .buildAndStart();

        Counter counter =
                Counter.builder()
                        .name("uptime_seconds_total")
                        .help("total number of seconds since this application was started")
                        .unit(Unit.SECONDS)
                        .register();

        System.out.println(
                "HTTPServer listening on port:" + server.getPort() + "/metrics");

        while (true) {
            Thread.sleep(1000);
            counter.inc();
        }
    }
}
