package fr.cnrs.liris.jpugetgil.sparqltosql.sparql;

public class SPARQLOccurrence {
    private SPARQLPositionType type;
    private Integer position;

    private SPARQLContextType sparqlContextType;

    public SPARQLOccurrence(SPARQLPositionType type, Integer position, SPARQLContextType sparqlContextType) {
        this.type = type;
        this.position = position;
        this.sparqlContextType = sparqlContextType;
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
        return sparqlContextType;
    }

    public void setContextType(SPARQLContextType sparqlContextType) {
        this.sparqlContextType = sparqlContextType;
    }
}
