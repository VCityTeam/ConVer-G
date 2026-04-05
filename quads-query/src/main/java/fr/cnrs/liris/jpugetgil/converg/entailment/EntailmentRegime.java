package fr.cnrs.liris.jpugetgil.converg.entailment;

public enum EntailmentRegime {
    NONE,
    RDFS,
    OWL_LITE;

    public static EntailmentRegime fromString(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        return switch (value.trim().toUpperCase()) {
            case "RDFS" -> RDFS;
            case "OWL", "OWL_LITE" -> OWL_LITE;
            default -> NONE;
        };
    }
}
