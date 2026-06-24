package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLOccurrence;
import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.Expression;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLClause;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLUtils;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVarType;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVariable;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.algebra.op.OpLeftJoin;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class LeftJoinSQLOperator extends JoinSQLOperator {

    private final OpLeftJoin opLeftJoin;
    private final String LEFT_TABLE_NAME = "left_table";
    private final String RIGHT_TABLE_NAME = "right_table";

    public LeftJoinSQLOperator(
            OpLeftJoin opLeftJoin,
            SQLQuery leftSQLQuery,
            SQLQuery rightSQLQuery
    ) {
        super(leftSQLQuery, rightSQLQuery);
        this.mergedMapOccurrences = SQLUtils.mergeMapOccurrencesLeftJoin(
                leftQuery.getContext().sparqlVarOccurrences(),
                rightQuery.getContext().sparqlVarOccurrences()
        );
        this.opLeftJoin = opLeftJoin;
    }

    /**
     * @return the select part of the Join SQL Operator
     */
    @Override
    protected String buildSelect() {
        return "SELECT " + mergedMapOccurrences
                .keySet()
                .stream()
                .map((node) -> SQLUtils.generateLeftJoinNodeProjectionByListSPARQLOccurrences(
                        leftQuery.getContext().sparqlVarOccurrences().get(node),
                        rightQuery.getContext().sparqlVarOccurrences().get(node)
                ))
                .collect(Collectors.joining(", ")) + "\n";
    }

    private String buildDifferenceGroupBy() {
        return "GROUP BY " + leftQuery.getContext().sparqlVarOccurrences()
                .keySet()
                .stream()
                .map((node) -> SQLUtils.getMaxSPARQLOccurrence(leftQuery.getContext().sparqlVarOccurrences().get(node)))
                .map(sparqlOccurrence -> sparqlOccurrence.getSqlVariable().getSelect(LEFT_TABLE_NAME))
                .collect(Collectors.joining(", "));
    }

    /**
     * Build the SELECT of one slice of the Product-Set Difference (Lemma 5.1).
     * Every shared condensed coordinate is projected according to its role in the
     * given slice (blocked / difference / full); all other variables keep their
     * usual difference projection (left value, or NULL for the optional side).
     *
     * @param orderedZeta the shared condensed coordinates in a fixed order
     * @param sliceIndex  the index of the coordinate carrying the {@code A_i \ B_i} factor
     * @return the SELECT clause of the slice
     */
    private String buildDifferenceSliceSelect(List<Node> orderedZeta, int sliceIndex) {
        return "SELECT " + mergedMapOccurrences
                .keySet()
                .stream()
                .map((node) -> {
                    int zetaIndex = orderedZeta.indexOf(node);
                    if (zetaIndex < 0) {
                        return SQLUtils.generateDifferenceLeftJoinNodeProjectionByListSPARQLOccurrences(
                                leftQuery.getContext().sparqlVarOccurrences().get(node),
                                rightQuery.getContext().sparqlVarOccurrences().get(node)
                        );
                    }
                    SQLVariable leftVar = SQLUtils.getMaxSPARQLOccurrence(
                            leftQuery.getContext().sparqlVarOccurrences().get(node)).getSqlVariable();
                    SQLVariable rightVar = SQLUtils.getMaxSPARQLOccurrence(
                            rightQuery.getContext().sparqlVarOccurrences().get(node)).getSqlVariable();
                    SQLVariable.DifferenceRole role = zetaIndex < sliceIndex
                            ? SQLVariable.DifferenceRole.BLOCKED
                            : zetaIndex == sliceIndex
                            ? SQLVariable.DifferenceRole.DIFFERENCE
                            : SQLVariable.DifferenceRole.FULL;
                    return leftVar.leftJoinDifferenceSliceProjection(rightVar, LEFT_TABLE_NAME, RIGHT_TABLE_NAME, role);
                })
                .collect(Collectors.joining(", ")) + "\n";
    }

    /**
     * The shared condensed (ζ) coordinates of the left join, in a fixed (name) order.
     * These are exactly the coordinates that carry a version-set difference; the order
     * fixes the slicing of Lemma 5.1.
     *
     * @return the ordered list of shared condensed variable nodes
     */
    private List<Node> orderedSharedCondensedNodes() {
        return mergedMapOccurrences.keySet()
                .stream()
                .filter(this::isSharedCondensed)
                .sorted(Comparator.comparing(Node::getName))
                .collect(Collectors.toList());
    }

    private boolean isSharedCondensed(Node node) {
        List<SPARQLOccurrence> left = leftQuery.getContext().sparqlVarOccurrences().get(node);
        List<SPARQLOccurrence> right = rightQuery.getContext().sparqlVarOccurrences().get(node);
        return left != null && right != null
                && SQLUtils.getMaxSPARQLOccurrence(left).getSqlVariable().getSqlVarType() == SQLVarType.CONDENSED;
    }

    /**
     * @return the from part of the Join SQL Operator
     */
    @Override
    protected String buildFrom() {
        SQLClause.SQLClauseBuilder sqlJoinClauseBuilder = new SQLClause.SQLClauseBuilder();

        if (commonVariables.isEmpty()) {
            sqlJoinClauseBuilder.and("1 = 1");
        } else {
            commonVariables.forEach(sqlVariablePair -> sqlJoinClauseBuilder.and(
                    sqlVariablePair.getLeft().joinLeftJoin(
                            sqlVariablePair.getRight(),
                            LEFT_TABLE_NAME,
                            RIGHT_TABLE_NAME
                    )
            ));
        }

        return " FROM " + LEFT_TABLE_NAME + " LEFT JOIN " + RIGHT_TABLE_NAME + " ON " + sqlJoinClauseBuilder.build().clause;
    }

    /**
     * @return the from difference part of the Join SQL Operator
     */
    protected String buildDifferenceFrom() {
        SQLClause.SQLClauseBuilder sqlJoinClauseBuilder = new SQLClause.SQLClauseBuilder();

        if (commonVariables.isEmpty()) {
            sqlJoinClauseBuilder.and("1 = 1");
        } else {
            commonVariables.forEach(sqlVariablePair -> sqlJoinClauseBuilder.and(
                    sqlVariablePair.getLeft().joinLeftJoin(
                            sqlVariablePair.getRight(),
                            LEFT_TABLE_NAME,
                            RIGHT_TABLE_NAME
                    )
            ));
        }

        return " FROM " + LEFT_TABLE_NAME + " JOIN " + RIGHT_TABLE_NAME + " ON " + sqlJoinClauseBuilder.build().clause;
    }

    /**
     * @return the where part of the Join SQL Operator
     */
    @Override
    protected String buildWhere() {
        if (opLeftJoin.getExprs() == null) {
            return "";
        }

        return opLeftJoin.getExprs().getList().stream()
                .map(Expression::fromJenaExpr)
                .map(Expression::toSQLString)
                .collect(Collectors.joining(" AND "));
    }

    /**
     * @return then new SQLQuery containing the join of the two subqueries
     */
    @Override
    public SQLQuery buildSQLQuery() {
        joinSubQueries();

        // similar or right part not existing
        String selectSimOrNotExist = buildSelect();
        String fromSimOrNotExist = buildFrom();
        String whereSimOrNotExist = buildWhere();

        String sql = "WITH " + LEFT_TABLE_NAME + " AS (" + leftQuery.getSql() + "),\n" +
                RIGHT_TABLE_NAME + " AS (" + rightQuery.getSql() + ")\n" +
                selectSimOrNotExist + fromSimOrNotExist + whereSimOrNotExist;

        if (leftQuery.getContext().condensedMode() && hasCondensedCommonVariable()) {
            String differenceFrom = buildDifferenceFrom();
            String groupByDifferences = buildDifferenceGroupBy();

            List<Node> orderedZeta = orderedSharedCondensedNodes();
            for (int sliceIndex = 0; sliceIndex < orderedZeta.size(); sliceIndex++) {
                sql = sql + "\n UNION \n "
                        + buildDifferenceSliceSelect(orderedZeta, sliceIndex)
                        + differenceFrom + whereSimOrNotExist + "\n" + groupByDifferences;
            }
        }

        return new SQLQuery(
                sql,
                new SQLContext(
                        mergedMapOccurrences,
                        leftQuery.getContext().condensedMode(),
                        null,
                        null
                ));
    }

    /**
     * The condensed "difference" branch computes a per-version bitstring difference
     * (left.bs &amp; ~right.bs) and only makes sense when the OPTIONAL is correlated
     * through a shared CONDENSED (graph) variable. When the two sides are joined only
     * through ID variables (e.g. two distinct graph variables connected by a shared
     * subject), the plain LEFT JOIN already yields the correct OPTIONAL semantics and
     * the difference branch would be both redundant and malformed.
     *
     * @return true if at least one common variable is in the CONDENSED representation
     */
    private boolean hasCondensedCommonVariable() {
        return commonVariables.stream()
                .anyMatch(pair -> pair.getLeft().getSqlVarType() == SQLVarType.CONDENSED
                        || pair.getRight().getSqlVarType() == SQLVarType.CONDENSED);
    }
}
