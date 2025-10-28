package fr.cnrs.liris.jpugetgil.converg.sql;

public enum SQLVarType {
    VALUE(3),
    ID(2),
    CONDENSED(1),
    UNBOUND_GRAPH(0);

    public final int level;

    SQLVarType(int level) {
        this.level = level;
    }

    public boolean isLower(SQLVarType sqlVarType2) {
        return this.level < sqlVarType2.level;
    }

    public boolean isEqual(SQLVarType sqlVarType2) {
        return this.level == sqlVarType2.level;
    }

    public boolean isHigher(SQLVarType sqlVarType2) {
        return this.level > sqlVarType2.level;
    }
}
