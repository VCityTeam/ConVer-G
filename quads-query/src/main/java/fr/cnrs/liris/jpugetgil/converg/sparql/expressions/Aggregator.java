package fr.cnrs.liris.jpugetgil.converg.sparql.expressions;

import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.aggregator.*;
import fr.cnrs.liris.jpugetgil.converg.sparql.transformer.FilterConfiguration;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.expr.aggregate.*;

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
    public String toSQLString() {
        ExprAggregator expAggr = this.getJenaExpr();
        return switch (expAggr.getAggregator()) {
            case AggAvg aggAvg -> new Avg(aggAvg, expAggr.getVar()).toSQLString();
            case AggAvgDistinct aggAvgDistinct -> new AvgDistinct(aggAvgDistinct, expAggr.getVar()).toSQLString();
            case AggMedian aggMedian -> new Median(aggMedian, expAggr.getVar()).toSQLString();
            case AggMedianDistinct aggMedianDistinct ->
                    new MedianDistinct(aggMedianDistinct, expAggr.getVar()).toSQLString();
            case AggCount aggCount -> new Count(aggCount, expAggr.getVar()).toSQLString();
            case AggCountDistinct aggCountDistinct ->
                    new CountDistinct(aggCountDistinct, expAggr.getVar()).toSQLString();
            case AggCountVar aggCountVar -> new CountVar(aggCountVar, expAggr.getVar()).toSQLString();
            case AggCountVarDistinct aggCountVarDistinct ->
                    new CountVarDistinct(aggCountVarDistinct, expAggr.getVar()).toSQLString();
            case AggCustom aggCustom -> new Custom(aggCustom, expAggr.getVar()).toSQLString();
            case AggGroupConcat aggGroupConcat -> new GroupConcat(aggGroupConcat, expAggr.getVar()).toSQLString();
            case AggGroupConcatDistinct aggGroupConcatDistinct ->
                    new GroupConcatDistinct(aggGroupConcatDistinct, expAggr.getVar()).toSQLString();
            case AggMax aggMax -> new Max(aggMax, expAggr.getVar()).toSQLString();
            case AggMaxDistinct aggMaxDistinct -> new MaxDistinct(aggMaxDistinct, expAggr.getVar()).toSQLString();
            case AggMin aggMin -> new Min(aggMin, expAggr.getVar()).toSQLString();
            case AggMinDistinct aggMinDistinct -> new MinDistinct(aggMinDistinct, expAggr.getVar()).toSQLString();
            case AggMode aggMode -> new Mode(aggMode, expAggr.getVar()).toSQLString();
            case AggModeDistinct aggModeDistinct -> new ModeDistinct(aggModeDistinct, expAggr.getVar()).toSQLString();
            case AggSample aggSample -> new Sample(aggSample, expAggr.getVar()).toSQLString();
            case AggSampleDistinct aggSampleDistinct ->
                    new SampleDistinct(aggSampleDistinct, expAggr.getVar()).toSQLString();
            case AggNull aggNull -> new Null(aggNull, expAggr.getVar()).toSQLString();
            case AggSum aggSum -> new Sum(aggSum, expAggr.getVar()).toSQLString();
            case AggSumDistinct aggSumDistinct -> new SumDistinct(aggSumDistinct, expAggr.getVar()).toSQLString();
            default -> throw new IllegalStateException("Unexpected value: " + this.getJenaExpr().getAggregator());
        };
    }
}
