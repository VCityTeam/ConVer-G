package fr.cnrs.liris.jpugetgil.sparqltosql.sql;

public enum SQLVarType {
    DATA,
    VERSIONED_NAMED_GRAPH,
    BIT_STRING,
    GRAPH_NAME;

    public String getAttributeName(String varName) {
        switch (this) {
            case DATA:
                return "v$" + varName;
            case VERSIONED_NAMED_GRAPH:
                return "vng$" + varName;
            case GRAPH_NAME:
                return "ng$" + varName;
            case BIT_STRING:
                return "bs$" + varName;
        }
    }
}
