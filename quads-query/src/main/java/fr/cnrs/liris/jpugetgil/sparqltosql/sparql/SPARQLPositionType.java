package fr.cnrs.liris.jpugetgil.sparqltosql.sparql;

public enum SPARQLPositionType {
    SUBJECT,
    PROPERTY,
    OBJECT,
    GRAPH_NAME;


    /**
     * Return the column name of the SQL query according to the occurrence type
     *
     * @return the column name of the versioned quad table
     */
    public String getSQLColumn() {
        return switch (this) {
            case SUBJECT -> "id_subject";
            case PROPERTY -> "id_property";
            case OBJECT -> "id_object";
            default -> throw new IllegalArgumentException();
        };
    }
    }
