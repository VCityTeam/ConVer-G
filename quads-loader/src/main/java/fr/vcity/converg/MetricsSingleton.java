package fr.vcity.converg;

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Summary;
import io.prometheus.metrics.model.snapshots.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class MetricsSingleton {

    private static final Logger log = LoggerFactory.getLogger(MetricsSingleton.class);

    private static MetricsSingleton metricsSingleton;

    public Counter counterVersionTotal;

    public Summary summaryVersionBatchingDuration;

    public Summary summaryVersionCatalogDuration;

    public Summary summaryVersionCondensingDuration;

    /**
     * Constructor method in order to create db connection & statement
     */
    private MetricsSingleton() {
        counterVersionTotal =
                Counter.builder()
                        .name("version_count_total")
                        .help("number of version that have been imported in the database")
                        .register();

        summaryVersionBatchingDuration =
                Summary.builder()
                        .name("file_at_version_batching_duration_seconds")
                        .help("duration of the file batching in seconds at a certain version")
                        .unit(Unit.SECONDS)
                        .labelNames("filename", "version")
                        .register();

        summaryVersionCatalogDuration =
                Summary.builder()
                        .name("file_at_version_catalog_duration_seconds")
                        .help("duration of the file catalog in seconds at a certain version")
                        .unit(Unit.SECONDS)
                        .labelNames("filename", "version")
                        .register();

        summaryVersionCondensingDuration =
                Summary.builder()
                        .name("file_at_version_condensing_duration_seconds")
                        .help("duration of the file condensing in seconds at a certain version")
                        .unit(Unit.SECONDS)
                        .labelNames("filename", "version")
                        .register();
    }

    /**
     * Create the instance of {@link MetricsSingleton} object if it is not created yet and guarantee that there is only one single instance is created for this class.
     *
     * @return MetricsSingleton created single instance
     */
    public static MetricsSingleton getInstance() {
        log.info("MetricsSingleton.getInstance()");

        if (Objects.isNull(metricsSingleton)) {
            metricsSingleton = new MetricsSingleton();
        }

        return metricsSingleton;
    }
}