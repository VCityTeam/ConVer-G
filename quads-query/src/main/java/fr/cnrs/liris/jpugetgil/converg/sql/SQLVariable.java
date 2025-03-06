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

    public String getSelect(String tableName) {
        return switch (this.sqlVarType) {
            case VALUE, ID -> tableName + ".v$" + this.sqlVarName;
            case CONDENSED -> tableName + ".ng$" + this.sqlVarName + ", " + tableName + ".bs$" + this.sqlVarName;
            case UNBOUND_GRAPH -> null;
        };
    }

    public String joinProjections(SQLVariable rightSQLVar, String leftTableName, String rightTableName) {
        return joinProjections(this, rightSQLVar, leftTableName, rightTableName);
    }

    public String joinJoin(SQLVariable rightSQLVar, String leftTableName, String rightTableName) {
        return joinJoin(this, rightSQLVar, leftTableName, rightTableName);
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

    private String joinProjections(SQLVariable leftSQLVar, SQLVariable rightSQLVar, String leftTableName, String rightTableName) {
        return switch (leftSQLVar.getSqlVarType()) {
            case VALUE -> switch (rightSQLVar.getSqlVarType()) {
                case VALUE -> leftSQLVar.getSelect(leftTableName);
                case ID, CONDENSED, UNBOUND_GRAPH ->
                        throw new ARQNotImplemented(leftSQLVar.getSqlVarType() + "-" + rightSQLVar.getSqlVarType() + " join Not supported yet.");
            };
            case ID -> switch (rightSQLVar.getSqlVarType()) {
                case ID -> leftSQLVar.getSelect(leftTableName);
                case CONDENSED -> leftTableName + ".id_versioned_named_graph AS v$" + leftSQLVar.getSqlVarName();
                case VALUE, UNBOUND_GRAPH ->
                        throw new ARQNotImplemented(leftSQLVar.getSqlVarType() + "-" + rightSQLVar.getSqlVarType() + " join Not supported yet.");
            };
            case CONDENSED -> switch (rightSQLVar.getSqlVarType()) {
                case VALUE, UNBOUND_GRAPH ->
                        throw new ARQNotImplemented(leftSQLVar.getSqlVarType() + "-" + rightSQLVar.getSqlVarType() + " join Not supported yet.");
                case ID -> rightTableName + ".id_versioned_named_graph AS v$" + leftSQLVar.getSqlVarName();
                case CONDENSED -> leftTableName + ".bs$" + leftSQLVar.getSqlVarName() + " & " +
                        rightTableName + ".bs$" + rightSQLVar.getSqlVarName() + " AS bs$" + leftSQLVar.getSqlVarName() + ", " +
                        leftTableName + ".ng$" + leftSQLVar.getSqlVarName();
            };
            case UNBOUND_GRAPH ->
                    throw new ARQNotImplemented(leftSQLVar.getSqlVarType() + "-" + rightSQLVar.getSqlVarType() + " join Not supported yet.");
        };
    }

    private String joinJoin(SQLVariable leftSQLVar, SQLVariable rightSQLVar, String leftTableName, String rightTableName) {
        SQLClause.SQLClauseBuilder sqlClauseBuilder = new SQLClause.SQLClauseBuilder();

        return switch (leftSQLVar.getSqlVarType()) {
            case VALUE -> switch (rightSQLVar.getSqlVarType()) {
                case VALUE -> new EqualToOperator()
                        .buildComparisonOperatorSQL(
                                leftSQLVar.getSelect(leftTableName),
                                rightSQLVar.getSelect(rightTableName)
                        );
                case ID -> leftSQLVar.getSelect(leftTableName);
                case CONDENSED, UNBOUND_GRAPH ->
                        throw new ARQNotImplemented(leftSQLVar.getSqlVarType() + "-" + rightSQLVar.getSqlVarType() + " join Not supported yet.");
            };
            case ID -> switch (rightSQLVar.getSqlVarType()) {
                case VALUE ->
                        throw new ARQNotImplemented(leftSQLVar.getSqlVarType() + "-" + rightSQLVar.getSqlVarType() + " join Not supported yet.");
                case ID -> new EqualToOperator()
                        .buildComparisonOperatorSQL(
                                leftSQLVar.getSelect(leftTableName),
                                rightSQLVar.getSelect(rightTableName)
                        );
                case CONDENSED -> "JOIN versioned_named_graph vng ON " + sqlClauseBuilder.and(new EqualToOperator()
                                .buildComparisonOperatorSQL(
                                        rightTableName + ".ng$" + rightSQLVar.getSqlVarName(),
                                        leftTableName + ".id_named_graph"
                                ))
                        .and(
                                new EqualToOperator()
                                        .buildComparisonOperatorSQL(
                                                "get_bit(" + rightTableName + ".bs$" + rightSQLVar.getSqlVarName() + ", " + leftTableName + ".index_version - 1)",
                                                "1"
                                        )
                        ).build().clause;
                case UNBOUND_GRAPH ->
                        throw new ARQNotImplemented(leftSQLVar.getSqlVarType() + "-" + rightSQLVar.getSqlVarType() + " join Not supported yet.");
            };
            case CONDENSED -> switch (rightSQLVar.getSqlVarType()) {
                case VALUE, UNBOUND_GRAPH ->
                        throw new ARQNotImplemented(leftSQLVar.getSqlVarType() + "-" + rightSQLVar.getSqlVarType() + " join Not supported yet.");
                case ID -> "JOIN versioned_named_graph vng ON " + sqlClauseBuilder.and(new EqualToOperator()
                        .buildComparisonOperatorSQL(
                                leftTableName + ".ng$" + leftSQLVar.getSqlVarName(),
                                rightTableName + ".id_named_graph"
                        ))
                        .and(
                                new EqualToOperator()
                                        .buildComparisonOperatorSQL(
                                                "get_bit(" + leftTableName + ".bs$" + leftSQLVar.getSqlVarName() + ", " + rightTableName + ".index_version - 1)",
                                                "1"
                                        )
                        ).build().clause;
                case CONDENSED -> sqlClauseBuilder
                        .and(new NotEqualToOperator()
                                .buildComparisonOperatorSQL(
                                        "bit_count(" + leftTableName + ".bs$" + leftSQLVar.getSqlVarName() + " & " + rightTableName + ".bs$" + rightSQLVar.getSqlVarName() + ")",
                                        "0"
                                ))
                        .and(new EqualToOperator()
                                .buildComparisonOperatorSQL(
                                        leftTableName + ".ng$" + leftSQLVar.getSqlVarName(),
                                        rightTableName + ".ng$" + rightSQLVar.getSqlVarName()
                                ))
                        .build()
                        .clause;
            };
            case UNBOUND_GRAPH ->
                    throw new ARQNotImplemented(leftSQLVar.getSqlVarType() + "-" + rightSQLVar.getSqlVarType() + " join Not supported yet.");
        };
    }
}
