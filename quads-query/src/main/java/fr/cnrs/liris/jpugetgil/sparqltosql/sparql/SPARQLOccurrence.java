package fr.cnrs.liris.jpugetgil.sparqltosql.sparql;

public class SPARQLOccurrence {
    private SPARQLPositionType type;
    private Integer position;

    private SPARQLContextType SPARQLContextType;

    public SPARQLOccurrence(SPARQLPositionType type, Integer position, SPARQLContextType SPARQLContextType) {
        this.type = type;
        this.position = position;
        this.SPARQLContextType = SPARQLContextType;
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

    public SPARQLContextType getContextType() {
        return SPARQLContextType;
    }

    public void setContextType(SPARQLContextType SPARQLContextType) {
        this.SPARQLContextType = SPARQLContextType;
    }
}
