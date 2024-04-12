package fr.cnrs.liris.jpugetgil.sparqltosql;

public class Occurrence {
    private SPARQLPositionType type;
    private Integer position;

    private ContextType contextType;

    public Occurrence(SPARQLPositionType type, Integer position, ContextType contextType) {
        this.type = type;
        this.position = position;
        this.contextType = contextType;
    }

    public SPARQLPositionType getType() {
        return type;
    }

    public void setType(SPARQLPositionType type) {
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
