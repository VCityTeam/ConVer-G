package fr.vcity;

public enum AnnotationType {
    RELATIONAL("relational"),
    THEORETICAL("theoretical");

    private final String label;

    AnnotationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }
}
