package fr.cnrs.liris.jpugetgil.converg.sparql;

import fr.cnrs.liris.jpugetgil.converg.sql.SQLVariable;

import java.util.Objects;

public class SPARQLOccurrence {
    private SPARQLPositionType type;
    private Integer position;
    private SPARQLContextType sparqlContextType;
    private SQLVariable sqlVariable;

    public SPARQLOccurrence(SPARQLPositionType type, Integer position, SPARQLContextType sparqlContextType, SQLVariable sqlVariable) {
        this.type = type;
        this.position = position;
        this.sparqlContextType = sparqlContextType;
        this.sqlVariable = sqlVariable;
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

    public SQLVariable getSqlVariable() {
        return sqlVariable;
    }

    public void setSqlVariable(SQLVariable sqlVariable) {
        this.sqlVariable = sqlVariable;
    }

    public SPARQLContextType getContextType() {
        return sparqlContextType;
    }

    public void setContextType(SPARQLContextType sparqlContextType) {
        this.sparqlContextType = sparqlContextType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SPARQLOccurrence that = (SPARQLOccurrence) o;
        return type == that.type && Objects.equals(position, that.position) && sparqlContextType == that.sparqlContextType && Objects.equals(sqlVariable, that.sqlVariable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, position, sparqlContextType, sqlVariable);
    }
}
