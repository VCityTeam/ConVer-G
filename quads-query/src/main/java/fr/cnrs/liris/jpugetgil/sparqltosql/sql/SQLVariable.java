package fr.cnrs.liris.jpugetgil.sparqltosql.sql;

/**
 * This class represents a SQL variable.
 * A SQL variable is a variable that is used in a SQL query.
 * It is composed of a type and a name.
 * The type is an enumeration of SQLVarType.
 * The name is a string.
 */
public class SQLVariable {

    private SQLVarType sqlVarType;

    private String sqlVarName;

    public SQLVariable(SQLVarType sqlVarType, String sqlVarName) {
        this.sqlVarType = sqlVarType;
        this.sqlVarName = sqlVarName;
    }

    public SQLVarType getSqlVarType() {
        return sqlVarType;
    }

    public void setSqlVarType(SQLVarType sqlVarType) {
        this.sqlVarType = sqlVarType;
    }

    public String getSqlVarName() {
        return sqlVarName;
    }

    public void setSqlVarName(String sqlVarName) {
        this.sqlVarName = sqlVarName;
    }
}
