package fr.cnrs.liris.jpugetgil.converg.inference;

import fr.cnrs.liris.jpugetgil.converg.entailment.EntailmentRegime;

import java.util.StringJoiner;

/**
 * Which inference sources a query should saturate over: an RDFS/OWL entailment
 * regime and/or the server's verified SWRL rules.
 * <p>
 * The server is configured with a <em>default</em> (from the {@code ENTAILMENT_REGIME}
 * and {@code SWRL_RULES} environment variables). Each query may override it with the
 * {@code ?infer=} HTTP parameter so a user can include or exclude inferred knowledge
 * per query without a restart.
 *
 * @param regime the RDFS/OWL regime to apply ({@link EntailmentRegime#NONE} for none)
 * @param swrl   whether the verified SWRL rules should be applied
 */
public record InferenceConfig(EntailmentRegime regime, boolean swrl) {

    public static final InferenceConfig NONE = new InferenceConfig(EntailmentRegime.NONE, false);

    /**
     * @return true when at least one inference source is selected.
     */
    public boolean isEnabled() {
        return regime != EntailmentRegime.NONE || swrl;
    }

    /**
     * A compact description of the active inference sources, e.g. {@code "RDFS"},
     * {@code "SWRL"}, {@code "RDFS+SWRL"} or the empty string when nothing is active.
     */
    public String describe() {
        StringJoiner joiner = new StringJoiner("+");
        if (regime != EntailmentRegime.NONE) {
            joiner.add(regime.name());
        }
        if (swrl) {
            joiner.add("SWRL");
        }
        return joiner.toString();
    }

    /**
     * Resolve the effective configuration for a query.
     *
     * @param inferParam    the raw {@code ?infer=} value, or {@code null} when absent
     * @param serverDefault the configuration to fall back to when the parameter is absent
     * @param swrlAvailable whether the server has verified SWRL rules loaded
     * @return the configuration to apply to this query
     */
    public static InferenceConfig resolve(String inferParam, InferenceConfig serverDefault, boolean swrlAvailable) {
        if (inferParam == null || inferParam.isBlank()) {
            return serverDefault;
        }

        EntailmentRegime regime = EntailmentRegime.NONE;
        boolean swrl = false;

        for (String rawToken : inferParam.trim().toLowerCase().split("[+,\\s]+")) {
            switch (rawToken) {
                case "", "off", "none", "false", "no" -> {
                    // explicit opt-out: leave everything disabled
                }
                case "rdfs" -> regime = EntailmentRegime.RDFS;
                case "owl", "owl_lite", "owllite" -> regime = EntailmentRegime.OWL_LITE;
                case "swrl" -> swrl = swrlAvailable;
                case "all", "on", "true", "yes" -> {
                    regime = serverDefault.regime() != EntailmentRegime.NONE
                            ? serverDefault.regime()
                            : EntailmentRegime.RDFS;
                    swrl = swrlAvailable;
                }
                default -> {
                    // Unknown token: ignore, keeping whatever has been recognised so far
                }
            }
        }

        return new InferenceConfig(regime, swrl);
    }
}
