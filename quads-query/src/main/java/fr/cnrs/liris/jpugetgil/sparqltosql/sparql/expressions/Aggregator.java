package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.aggregator.*;
import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.transformer.FilterConfiguration;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLVariable;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.expr.aggregate.*;

import java.util.List;

public class Aggregator extends AbstractExpression<ExprAggregator> {
    /**
     * Builds an expression from a jena expr
     *
     * @param expr the source jena expr
     */
    public Aggregator(ExprAggregator expr) {
        super(expr);
    }

    @Override
    public void updateFilterConfiguration(FilterConfiguration configuration, boolean requiresValue) {
        throw new IllegalStateException("updateFilterConfiguration should not be called on an aggregate value");
    }

    @Override
    public String toSQLString(List<SQLVariable> sqlVariables) {
        ExprAggregator expAggr = this.getJenaExpr();
        return switch (expAggr.getAggregator()) {
            case AggAvg aggAvg -> new Avg(aggAvg, expAggr.getVar()).toSQLString(sqlVariables);
            case AggAvgDistinct aggAvgDistinct -> new AvgDistinct(aggAvgDistinct, expAggr.getVar()).toSQLString(sqlVariables);
            case AggMedian aggMedian -> new Median(aggMedian, expAggr.getVar()).toSQLString(sqlVariables);
            case AggMedianDistinct aggMedianDistinct -> new MedianDistinct(aggMedianDistinct, expAggr.getVar()).toSQLString(sqlVariables);
            case AggCount aggCount -> new Count(aggCount, expAggr.getVar()).toSQLString(sqlVariables);
            case AggCountDistinct aggCountDistinct -> new CountDistinct(aggCountDistinct, expAggr.getVar()).toSQLString(sqlVariables);
            case AggCountVar aggCountVar -> new CountVar(aggCountVar, expAggr.getVar()).toSQLString(sqlVariables);
            case AggCountVarDistinct aggCountVarDistinct -> new CountVarDistinct(aggCountVarDistinct, expAggr.getVar()).toSQLString(sqlVariables);
            case AggCustom aggCustom -> new Custom(aggCustom, expAggr.getVar()).toSQLString(sqlVariables);
            case AggGroupConcat aggGroupConcat -> new GroupConcat(aggGroupConcat, expAggr.getVar()).toSQLString(sqlVariables);
            case AggGroupConcatDistinct aggGroupConcatDistinct -> new GroupConcatDistinct(aggGroupConcatDistinct, expAggr.getVar()).toSQLString(sqlVariables);
            case AggMax aggMax -> new Max(aggMax, expAggr.getVar()).toSQLString(sqlVariables);
            case AggMaxDistinct aggMaxDistinct -> new MaxDistinct(aggMaxDistinct, expAggr.getVar()).toSQLString(sqlVariables);
            case AggMin aggMin -> new Min(aggMin, expAggr.getVar()).toSQLString(sqlVariables);
            case AggMinDistinct aggMinDistinct -> new MinDistinct(aggMinDistinct, expAggr.getVar()).toSQLString(sqlVariables);
            case AggMode aggMode -> new Mode(aggMode, expAggr.getVar()).toSQLString(sqlVariables);
            case AggModeDistinct aggModeDistinct -> new ModeDistinct(aggModeDistinct, expAggr.getVar()).toSQLString(sqlVariables);
            case AggSample aggSample -> new Sample(aggSample, expAggr.getVar()).toSQLString(sqlVariables);
            case AggSampleDistinct aggSampleDistinct -> new SampleDistinct(aggSampleDistinct, expAggr.getVar()).toSQLString(sqlVariables);
            case AggNull aggNull -> new Null(aggNull, expAggr.getVar()).toSQLString(sqlVariables);
            case AggSum aggSum -> new Sum(aggSum, expAggr.getVar()).toSQLString(sqlVariables);
            case AggSumDistinct aggSumDistinct -> new SumDistinct(aggSumDistinct, expAggr.getVar()).toSQLString(sqlVariables);
            default -> throw new IllegalStateException("Unexpected value: " + this.getJenaExpr().getAggregator());
        };
    }
}
