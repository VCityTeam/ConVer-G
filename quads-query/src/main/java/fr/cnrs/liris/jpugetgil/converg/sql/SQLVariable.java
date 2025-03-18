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

    private String sqlVarName;

    private boolean isOptional = false;

    public SQLVariable(SQLVarType sqlVarType, String sqlVarName) {
        this.sqlVarType = sqlVarType;
        this.sqlVarName = sqlVarName;
    }

    public boolean isOptional() {
        return isOptional;
    }

    public void setOptional(boolean optional) {
        isOptional = optional;
    }

    public SQLVarType getSqlVarType() {
        return sqlVarType;
    }

    public String getSqlVarName() {
        return sqlVarName;
    }

    public void setSqlVarName(String sqlVarName) {
        this.sqlVarName = sqlVarName;
    }

    public String getSelect() {
        return switch (this.sqlVarType) {
            case VALUE, ID -> "v$" + this.sqlVarName;
            case CONDENSED -> "ng$" + this.sqlVarName + ", bs$" + this.sqlVarName;
            case UNBOUND_GRAPH -> null;
        };
    }

    public String getSelect(String tableName) {
        return switch (this.sqlVarType) {
            case VALUE, ID -> tableName + ".v$" + this.sqlVarName;
            case CONDENSED -> tableName + ".ng$" + this.sqlVarName + ", " + tableName + ".bs$" + this.sqlVarName;
            case UNBOUND_GRAPH -> null;
        };
    }

    /**
     * Join the two SQL variables
     *
     * @param rightSQLVar    the right SQL variable
     * @param leftTableName  the left table name
     * @param rightTableName the right table name
     * @return the join SQL projections
     */
    public String joinProjections(SQLVariable rightSQLVar, String leftTableName, String rightTableName) {
        return joinProjections(this, rightSQLVar, leftTableName, rightTableName);
    }

    /**
     * Left Join the two SQL variables
     *
     * @param rightSQLVar    the right SQL variable
     * @param leftTableName  the left table name
     * @param rightTableName the right table name
     * @return the left join SQL projections
     */
    public String leftJoinProjections(SQLVariable rightSQLVar, String leftTableName, String rightTableName) {
        return leftJoinProjections(this, rightSQLVar, leftTableName, rightTableName);
    }

    /**
     * Join the two SQL variables
     *
     * @param rightSQLVar    the right SQL variable
     * @param leftTableName  the left table name
     * @param rightTableName the right table name
     * @return the join SQL join
     */
    public String joinJoin(SQLVariable rightSQLVar, String leftTableName, String rightTableName) {
        return joinJoin(this, rightSQLVar, leftTableName, rightTableName);
    }

    /**
     * Left Join the two SQL variables
     *
     * @param rightSQLVar    the right SQL variable
     * @param leftTableName  the left table name
     * @param rightTableName the right table name
     * @return the left join SQL join
     */
    public String joinLeftJoin(SQLVariable rightSQLVar, String leftTableName, String rightTableName) {
        return joinLeftJoin(this, rightSQLVar, leftTableName, rightTableName);
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


    /**
     * Identify the SQL variable (SELECT)
     *
     * @return the identified SQL variable Select part
     */
    public String getSelectIdentifyVariable() {
        return "rl.name as v$" + this.sqlVarName;
    }

    /**
     * Flatten the SQL variable (SELECT)
     *
     * @return the flatten SQL select part
     */
    public String getSelectFlattenVariable() {
        if (this.sqlVarType != SQLVarType.CONDENSED) {
            throw new RuntimeException("The variable must be at " + SQLVarType.CONDENSED + "level to be flattened.");
        }
        return getVNGName() + ".id_versioned_named_graph AS v$" + this.sqlVarName;
    }

    /**
     * Identify the SQL variable (JOIN)
     *
     * @return the identified SQL variable JOIN part
     */
    public String fromIdentifiedVariable(String identifyTableName) {
        if (this.sqlVarType != SQLVarType.ID) {
            throw new RuntimeException("The variable must be at " + SQLVarType.ID + "level to be identified.");
        }

        return " JOIN resource_or_literal rl ON " + this.getSelect(identifyTableName) + " = rl.id_resource_or_literal";
    }

    /**
     * Flatten the SQL variable (FROM)
     *
     * @param tableName the table name
     * @return the flatten SQL from part
     */
    public String fromFlattenVariable(String tableName) {
        if (this.sqlVarType != SQLVarType.CONDENSED) {
            throw new RuntimeException("The variable must be at " + SQLVarType.CONDENSED + "level to be flattened.");
        }

        SQLClause.SQLClauseBuilder sqlClauseBuilder = new SQLClause.SQLClauseBuilder();
        return "JOIN versioned_named_graph " + getVNGName() + " ON " + sqlClauseBuilder.and(new EqualToOperator()
                        .buildComparisonOperatorSQL(
                                tableName + ".ng$" + this.sqlVarName,
                                getVNGName() + ".id_named_graph"
                        ))
                .and(
                        new EqualToOperator()
                                .buildComparisonOperatorSQL(
                                        "get_bit(" + tableName + ".bs$" + this.sqlVarName + ", " + getVNGName() + ".index_version - 1)",
                                        "1"
                                )
                ).build().clause;
    }

    /**
     * Join the two SQL variables
     *
     * @param leftSQLVar  the left SQL variable
     * @param rightSQLVar the right SQL variable
     * @return the join SQL projections
     */
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
                case ID -> rightTableName + ".id_versioned_named_graph AS v$" + rightSQLVar.getSqlVarName();
                case CONDENSED -> leftTableName + ".bs$" + leftSQLVar.getSqlVarName() + " & " +
                        rightTableName + ".bs$" + rightSQLVar.getSqlVarName() + " AS bs$" + leftSQLVar.getSqlVarName() + ", " +
                        leftTableName + ".ng$" + leftSQLVar.getSqlVarName();
            };
            case UNBOUND_GRAPH ->
                    throw new ARQNotImplemented(leftSQLVar.getSqlVarType() + "-" + rightSQLVar.getSqlVarType() + " join Not supported yet.");
        };
    }

    /**
     * Left Join the two SQL variables
     *
     * @param leftSQLVar  the left SQL variable
     * @param rightSQLVar the right SQL variable
     * @return the left join SQL projections
     */
    private String leftJoinProjections(SQLVariable leftSQLVar, SQLVariable rightSQLVar, String leftTableName, String rightTableName) {
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
                case ID -> rightTableName + ".id_versioned_named_graph AS v$" + rightSQLVar.getSqlVarName();
                case CONDENSED ->
                        "COALESCE(" + leftTableName + ".bs$" + leftSQLVar.getSqlVarName() + " & " + rightTableName + ".bs$"
                                + rightSQLVar.getSqlVarName() + "," + leftTableName + ".bs$" + leftSQLVar.getSqlVarName() + ") AS bs$"
                                + leftSQLVar.getSqlVarName() + ", " + leftTableName + ".ng$" + leftSQLVar.getSqlVarName();
            };
            case UNBOUND_GRAPH ->
                    throw new ARQNotImplemented(leftSQLVar.getSqlVarType() + "-" + rightSQLVar.getSqlVarType() + " join Not supported yet.");
        };
    }

    /**
     * Join the two SQL variables
     *
     * @param leftSQLVar  the left SQL variable
     * @param rightSQLVar the right SQL variable
     * @return the join SQL
     */
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
                case ID -> new EqualToOperator()
                        .buildComparisonOperatorSQL(
                                leftSQLVar.getSelect(leftTableName),
                                rightSQLVar.getSelect(rightTableName)
                        );
                case CONDENSED ->
                        "JOIN versioned_named_graph " + getVNGName() + " ON " + sqlClauseBuilder.and(new EqualToOperator()
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
                case VALUE, UNBOUND_GRAPH ->
                        throw new ARQNotImplemented(leftSQLVar.getSqlVarType() + "-" + rightSQLVar.getSqlVarType() + " join Not supported yet.");
            };
            case CONDENSED -> switch (rightSQLVar.getSqlVarType()) {
                case VALUE, UNBOUND_GRAPH ->
                        throw new ARQNotImplemented(leftSQLVar.getSqlVarType() + "-" + rightSQLVar.getSqlVarType() + " join Not supported yet.");
                case ID ->
                        "JOIN versioned_named_graph " + getVNGName() + " ON " + sqlClauseBuilder.and(new EqualToOperator()
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

    /**
     * Left Join the two SQL variables
     *
     * @param leftSQLVar  the left SQL variable
     * @param rightSQLVar the right SQL variable
     * @return the left join SQL
     */
    private String joinLeftJoin(SQLVariable leftSQLVar, SQLVariable rightSQLVar, String leftTableName, String rightTableName) {
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
                case ID -> new EqualToOperator()
                        .buildComparisonOperatorSQL(
                                leftSQLVar.getSelect(leftTableName),
                                rightSQLVar.getSelect(rightTableName)
                        );
                case CONDENSED ->
                        "JOIN versioned_named_graph " + getVNGName() + " ON " + sqlClauseBuilder.and(new EqualToOperator()
                                        .buildComparisonOperatorSQL(
                                                rightTableName + ".ng$" + rightSQLVar.getSqlVarName(),
                                                leftTableName + ".id_named_graph"
                                        ))
                                .build().clause;
                case VALUE, UNBOUND_GRAPH ->
                        throw new ARQNotImplemented(leftSQLVar.getSqlVarType() + "-" + rightSQLVar.getSqlVarType() + " join Not supported yet.");
            };
            case CONDENSED -> switch (rightSQLVar.getSqlVarType()) {
                case VALUE, UNBOUND_GRAPH ->
                        throw new ARQNotImplemented(leftSQLVar.getSqlVarType() + "-" + rightSQLVar.getSqlVarType() + " join Not supported yet.");
                case ID ->
                        "JOIN versioned_named_graph " + getVNGName() + " ON " + sqlClauseBuilder.and(new EqualToOperator()
                                .buildComparisonOperatorSQL(
                                        leftTableName + ".ng$" + leftSQLVar.getSqlVarName(),
                                        rightTableName + ".id_named_graph"
                                )).build().clause;
                case CONDENSED -> sqlClauseBuilder
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

    private String getVNGName() {
        return "vng";
    }
}
