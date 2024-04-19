package fr.cnrs.liris.jpugetgil.sparqltosql.sql;

import fr.cnrs.liris.jpugetgil.sparqltosql.sql.comparison.EqualToOperator;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.comparison.NotEqualToOperator;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Objects;

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

    public String getSQLValue(String tableName) {
        return tableName + "." + getSQLAttributeName();
    }

    public String getSQLAttributeName() {
        return sqlVarType.getAttributeName(sqlVarName);
    }

    public String join(SQLVariable rightSQLVar, String leftTableName, String rightTableName) {
        return switch (this.sqlVarType) {
            case DATA -> switch (rightSQLVar.getSqlVarType()) {
                case DATA -> new EqualToOperator()
                        .buildComparisonOperatorSQL(
                                leftTableName + ".v$" + this.sqlVarName,
                                rightTableName + ".v$" + rightSQLVar.getSqlVarName()
                        );
                case GRAPH_NAME -> new EqualToOperator()
                        .buildComparisonOperatorSQL(
                                leftTableName + ".v$" + this.sqlVarName,
                                rightTableName + ".ng$" + rightSQLVar.getSqlVarName()
                        );
                case VERSIONED_NAMED_GRAPH -> new EqualToOperator()
                        .buildComparisonOperatorSQL(
                                leftTableName + ".v$" + this.sqlVarName,
                                rightTableName + ".vng$" + rightSQLVar.getSqlVarName()
                        );
                case BIT_STRING -> throw new NotImplementedException("Not supported yet.");
            };
            case VERSIONED_NAMED_GRAPH -> switch (rightSQLVar.getSqlVarType()) {
                case VERSIONED_NAMED_GRAPH -> new EqualToOperator()
                        .buildComparisonOperatorSQL(
                                leftTableName + ".vng$" + this.sqlVarName,
                                rightTableName + ".vng$" + rightSQLVar.getSqlVarName()
                        );
                case GRAPH_NAME -> new EqualToOperator()
                        .buildComparisonOperatorSQL(
                                leftTableName + ".vng$" + this.sqlVarName,
                                rightTableName + ".ng$" + rightSQLVar.getSqlVarName()
                        );
                case DATA -> new EqualToOperator()
                        .buildComparisonOperatorSQL(
                                leftTableName + ".vng$" + this.sqlVarName,
                                rightTableName + ".v$" + rightSQLVar.getSqlVarName()
                        );
                case BIT_STRING -> throw new NotImplementedException("Not supported yet.");
            };
            case BIT_STRING -> switch (rightSQLVar.getSqlVarType()) {
                case DATA, VERSIONED_NAMED_GRAPH, GRAPH_NAME -> throw new NotImplementedException("Not supported yet.");
                case BIT_STRING -> new NotEqualToOperator()
                        .buildComparisonOperatorSQL(
                                "bit_count(" + leftTableName + ".bs$" + this.sqlVarName + " & " + rightTableName +
                                        ".bs$" + rightSQLVar.getSqlVarName() + ")",
                                "0"
                        );
            };
            case GRAPH_NAME -> switch (rightSQLVar.getSqlVarType()) {
                case BIT_STRING -> throw new NotImplementedException("Not supported yet.");
                case VERSIONED_NAMED_GRAPH -> new EqualToOperator()
                        .buildComparisonOperatorSQL(
                                leftTableName + ".ng$" + this.sqlVarName,
                                rightTableName + ".vng$" + rightSQLVar.getSqlVarName()
                        );
                case DATA -> new EqualToOperator()
                        .buildComparisonOperatorSQL(
                                leftTableName + ".ng$" + this.sqlVarName,
                                rightTableName + ".v$" + rightSQLVar.getSqlVarName()
                        );
                case GRAPH_NAME -> new EqualToOperator()
                        .buildComparisonOperatorSQL(
                                leftTableName + ".ng$" + this.sqlVarName,
                                rightTableName + ".ng$" + rightSQLVar.getSqlVarName()
                        );
            };
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SQLVariable that = (SQLVariable) o;
        return sqlVarType == that.sqlVarType && Objects.equals(sqlVarName, that.sqlVarName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sqlVarType, sqlVarName);
    }
}
