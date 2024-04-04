package fr.cnrs.liris.jpugetgil.sparqltosql;

public class Occurrence {
    private String type;
    private Integer position;

    private ContextType contextType;

    public Occurrence(String type, Integer position, ContextType contextType) {
        this.type = type;
        this.position = position;
        this.contextType = contextType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public ContextType getContextType() {
        return contextType;
    }

    public void setContextType(ContextType contextType) {
        this.contextType = contextType;
    }
}
