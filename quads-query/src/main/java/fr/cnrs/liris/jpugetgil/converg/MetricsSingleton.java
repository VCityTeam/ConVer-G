package fr.cnrs.liris.jpugetgil.converg;

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Summary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class MetricsSingleton {

    private static final Logger log = LoggerFactory.getLogger(MetricsSingleton.class);

    private static MetricsSingleton metricsSingleton;

    public Counter selectQueryCounter;

    public Summary queryTranslationDuration;

    public Summary queryExecutionDuration;

    /**
     * Constructor method in order to create db connection & statement
     */
    private MetricsSingleton() {
        selectQueryCounter = Counter.builder()
                .name("select_query_counter")
                .help("Number of SELECT queries asked to be translated")
                .register();

        queryTranslationDuration = Summary.builder()
                .name("query_translation_duration")
                .labelNames("query")
                .help("Duration of the query translation")
                .register();

        queryExecutionDuration = Summary.builder()
                .name("query_execution_duration")
                .labelNames("query")
                .help("Duration of the query execution")
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