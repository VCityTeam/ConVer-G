package fr.cnrs.liris.jpugetgil.converg.sql;

import fr.cnrs.liris.jpugetgil.converg.sql.comparison.EqualToOperator;
import fr.cnrs.liris.jpugetgil.converg.sql.comparison.NotEqualToOperator;
import org.apache.jena.sparql.ARQNotImplemented;

import java.util.Objects;

/**
 * This class represents a SQL variable.
 * A SQL variable is a variable that is used in a SQL query.
 * It is composed of a type and a name.
 * The type is an enumeration of SQLVarType.
 * The name is a string.
 */
public class SQLVariable {

    private final SQLVarType sqlVarType;

    private final String sqlVarName;

    public SQLVariable(SQLVarType sqlVarType, String sqlVarName) {
        this.sqlVarType = sqlVarType;
        this.sqlVarName = sqlVarName;
    }

    public SQLVarType getSqlVarType() {
        return sqlVarType;
    }

    public String getSqlVarName() {
        return sqlVarName;
    }

    public static String join(SQLVariable leftSQLVar, SQLVariable rightSQLVar, String leftTableName, String rightTableName) {
        return switch (leftSQLVar.getSqlVarType()) {
            case VALUE -> switch (rightSQLVar.getSqlVarType()) {
                case VALUE -> new EqualToOperator()
                        .buildComparisonOperatorSQL(
                                leftTableName + ".v$" + leftSQLVar.getSqlVarName(),
                                rightTableName + ".v$" + rightSQLVar.getSqlVarName()
                        );
                case ID -> new EqualToOperator()
                        .buildComparisonOperatorSQL(
                                leftTableName + ".v$" + leftSQLVar.getSqlVarName(),
                                rightTableName + ".vng$" + rightSQLVar.getSqlVarName()
                        );
                case CONDENSED -> new EqualToOperator()
                        .buildComparisonOperatorSQL(
                                leftTableName + ".v$" + leftSQLVar.getSqlVarName(),
                                rightTableName + ".ng$" + rightSQLVar.getSqlVarName()
                        );
                case UNBOUND_GRAPH -> throw new ARQNotImplemented(leftSQLVar.getSqlVarType() + "-" + rightSQLVar.getSqlVarType() + " join Not supported yet.");
            };
            case ID -> switch (rightSQLVar.getSqlVarType()) {
                case VALUE -> new EqualToOperator()
                        .buildComparisonOperatorSQL(
                                leftTableName + ".vng$" + leftSQLVar.getSqlVarName(),
                                rightTableName + ".v$" + rightSQLVar.getSqlVarName()
                        );
                case ID -> new EqualToOperator()
                        .buildComparisonOperatorSQL(
                                leftTableName + ".vng$" + leftSQLVar.getSqlVarName(),
                                rightTableName + ".vng$" + rightSQLVar.getSqlVarName()
                        );
                case CONDENSED -> new EqualToOperator()
                        .buildComparisonOperatorSQL(
                                leftTableName + ".vng$" + leftSQLVar.getSqlVarName(),
                                rightTableName + ".ng$" + rightSQLVar.getSqlVarName()
                        );
                case UNBOUND_GRAPH -> throw new ARQNotImplemented(leftSQLVar.getSqlVarType() + "-" + rightSQLVar.getSqlVarType() + " join Not supported yet.");
            };
            case CONDENSED -> switch (rightSQLVar.getSqlVarType()) {
                case VALUE, UNBOUND_GRAPH -> throw new ARQNotImplemented(leftSQLVar.getSqlVarType() + "-" + rightSQLVar.getSqlVarType() + " join Not supported yet.");
                case ID -> new EqualToOperator()
                        .buildComparisonOperatorSQL(
                                leftTableName + ".vng$" + leftSQLVar.getSqlVarName(),
                                rightTableName + ".ng$" + rightSQLVar.getSqlVarName()
                        );
                case CONDENSED -> new NotEqualToOperator()
                        .buildComparisonOperatorSQL(
                                "bit_count(" + leftTableName + ".bs$" + leftSQLVar.getSqlVarName() + " & " + rightTableName + ".bs$" + rightSQLVar.getSqlVarName() + ")",
                                "0"
                        );
            };
            case UNBOUND_GRAPH -> throw new ARQNotImplemented(leftSQLVar.getSqlVarType() + "-" + rightSQLVar.getSqlVarType() + " join Not supported yet.");
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
