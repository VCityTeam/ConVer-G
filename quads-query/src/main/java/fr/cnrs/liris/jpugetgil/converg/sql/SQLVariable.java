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

    private SQLVarType sqlVarType;

    private String sqlVarName;

    private boolean isValue = false;

    private boolean isGraphNameOrMetadata;

    public SQLVariable(SQLVarType sqlVarType, String sqlVarName, boolean isGraphNameOrMetadata) {
        this.sqlVarType = sqlVarType;
        this.sqlVarName = sqlVarName;
        this.isGraphNameOrMetadata = isGraphNameOrMetadata;
    }

    public SQLVariable(SQLVarType sqlVarType, String sqlVarName, boolean isGraphNameOrMetadata, boolean isValue) {
        this.sqlVarType = sqlVarType;
        this.sqlVarName = sqlVarName;
        this.isValue = isValue;
        this.isGraphNameOrMetadata = isGraphNameOrMetadata;
    }

    public SQLVarType getSqlVarType() {
        return sqlVarType;
    }

    public String getSqlVarName() {
        return sqlVarName;
    }

    public boolean isValue() {
        return isValue;
    }

    public boolean isGraphNameOrMetadata() {
        return isGraphNameOrMetadata;
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
                case BIT_STRING -> throw new ARQNotImplemented("BITSTRING - DATA join Not supported yet.");
                case AGGREGATED -> throw new ARQNotImplemented("AGGREGATED - DATA join Not supported yet.");
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
                case BIT_STRING -> throw new ARQNotImplemented("VNG - BITSTRING join Not supported yet.");
                case AGGREGATED -> throw new ARQNotImplemented("VNG - AGGREGATED join Not supported yet.");
            };
            case BIT_STRING -> switch (rightSQLVar.getSqlVarType()) {
                case DATA, VERSIONED_NAMED_GRAPH, GRAPH_NAME -> throw new ARQNotImplemented("BITSTRING - DATA/VNG/GRAPHNAME join Not supported yet.");
                case BIT_STRING -> new NotEqualToOperator()
                        .buildComparisonOperatorSQL(
                                "bit_count(" + leftTableName + ".bs$" + this.sqlVarName + " & " + rightTableName + ".bs$" + rightSQLVar.getSqlVarName() + ")",
                                "0"
                        );
                case AGGREGATED -> throw new ARQNotImplemented("BITSTRING - AGGREGATED join Not supported yet.");
            };
            case GRAPH_NAME -> switch (rightSQLVar.getSqlVarType()) {
                case BIT_STRING -> throw new ARQNotImplemented("GRAPHNAME - BISTRING join Not supported yet.");
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
                case AGGREGATED -> throw new ARQNotImplemented("GRAPHNAME - AGGREGATED join Not supported yet.");
            };
            case AGGREGATED -> throw new ARQNotImplemented("AGGREGATED - * join Not supported yet.");
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
