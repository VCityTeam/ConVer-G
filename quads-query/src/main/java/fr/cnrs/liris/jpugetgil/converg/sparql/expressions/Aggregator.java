package fr.cnrs.liris.jpugetgil.converg.sparql.expressions;

import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.aggregator.*;
import fr.cnrs.liris.jpugetgil.converg.sparql.transformer.FilterConfiguration;
import org.apache.jena.sparql.ARQException;
import org.apache.jena.sparql.algebra.op.OpGroup;
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

    public boolean isCountable() {
        return switch (this.getJenaExpr().getAggregator()) {
            case AggCount ignored -> true; // Done
//            case AggCountDistinct ignored -> true;
            case AggCountVar ignored -> true;
//            case AggCountVarDistinct ignored -> true;
            case AggAvg ignored -> true; // TODO: to test
//            case AggAvgDistinct ignored -> true;
            case AggMax ignored -> true;
//            case AggMaxDistinct ignored -> true;
            case AggMin ignored -> true;
//            case AggMinDistinct ignored -> true;
            case AggSum ignored -> true;
//            case AggSumDistinct ignored -> true;
            default -> false;
        };
    }

    public AbstractAggregator<?> getAggregator() {
        ExprAggregator expAggr = this.getJenaExpr();
        return switch (expAggr.getAggregator()) {
            case AggAvg aggAvg -> new Avg(aggAvg, expAggr.getVar());
            case AggAvgDistinct aggAvgDistinct -> new AvgDistinct(aggAvgDistinct, expAggr.getVar());
            case AggMedian aggMedian -> new Median(aggMedian, expAggr.getVar());
            case AggMedianDistinct aggMedianDistinct ->
                    new MedianDistinct(aggMedianDistinct, expAggr.getVar());
            case AggCount aggCount -> new Count(aggCount, expAggr.getVar());
            case AggCountDistinct aggCountDistinct ->
                    new CountDistinct(aggCountDistinct, expAggr.getVar());
            case AggCountVar aggCountVar -> new CountVar(aggCountVar, expAggr.getVar());
            case AggCountVarDistinct aggCountVarDistinct ->
                    new CountVarDistinct(aggCountVarDistinct, expAggr.getVar());
            case AggCustom aggCustom -> new Custom(aggCustom, expAggr.getVar());
            case AggGroupConcat aggGroupConcat -> new GroupConcat(aggGroupConcat, expAggr.getVar());
            case AggGroupConcatDistinct aggGroupConcatDistinct ->
                    new GroupConcatDistinct(aggGroupConcatDistinct, expAggr.getVar());
            case AggMax aggMax -> new Max(aggMax, expAggr.getVar());
            case AggMaxDistinct aggMaxDistinct -> new MaxDistinct(aggMaxDistinct, expAggr.getVar());
            case AggMin aggMin -> new Min(aggMin, expAggr.getVar());
            case AggMinDistinct aggMinDistinct -> new MinDistinct(aggMinDistinct, expAggr.getVar());
            case AggMode aggMode -> new Mode(aggMode, expAggr.getVar());
            case AggModeDistinct aggModeDistinct -> new ModeDistinct(aggModeDistinct, expAggr.getVar());
            case AggSample aggSample -> new Sample(aggSample, expAggr.getVar());
            case AggSampleDistinct aggSampleDistinct ->
                    new SampleDistinct(aggSampleDistinct, expAggr.getVar());
            case AggNull aggNull -> new Null(aggNull, expAggr.getVar());
            case AggSum aggSum -> new Sum(aggSum, expAggr.getVar());
            case AggSumDistinct aggSumDistinct -> new SumDistinct(aggSumDistinct, expAggr.getVar());
            default -> throw new ARQException("Aggregation - Unexpected value: " + this.getJenaExpr().getAggregator());
        };
    }

    @Override
    public void updateFilterConfiguration(FilterConfiguration configuration, boolean requiresValue) {
        throw new ARQException("updateFilterConfiguration should not be called on an aggregate value");
    }

    @Override
    public String toSQLString() {
        return getAggregator().toSQLString();
    }

    public String toSQLString(OpGroup opGroup, String alias) {
        return getAggregator().toSQLString(opGroup, alias);
    }
}
