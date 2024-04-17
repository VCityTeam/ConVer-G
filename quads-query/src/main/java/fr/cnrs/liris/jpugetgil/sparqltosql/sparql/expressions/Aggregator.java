package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.aggregator.*;
import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.transformer.FilterConfiguration;
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
        return switch (this.getJenaExpr().getAggregator()) {
            case AggAvg aggAvg -> new Avg(aggAvg).toSQLString();
            case AggAvgDistinct aggAvgDistinct -> new AvgDistinct(aggAvgDistinct).toSQLString();
            case AggMedian aggMedian -> new Median(aggMedian).toSQLString();
            case AggMedianDistinct aggMedianDistinct -> new MedianDistinct(aggMedianDistinct).toSQLString();
            case AggCount aggCount -> new Count(aggCount).toSQLString();
            case AggCountDistinct aggCountDistinct -> new CountDistinct(aggCountDistinct).toSQLString();
            case AggCountVar aggCountVar -> new CountVar(aggCountVar).toSQLString();
            case AggCountVarDistinct aggCountVarDistinct -> new CountVarDistinct(aggCountVarDistinct).toSQLString();
            case AggCustom aggCustom -> new Custom(aggCustom).toSQLString();
            case AggGroupConcat aggGroupConcat -> new GroupConcat(aggGroupConcat).toSQLString();
            case AggGroupConcatDistinct aggGroupConcatDistinct -> new GroupConcatDistinct(aggGroupConcatDistinct).toSQLString();
            case AggMax aggMax -> new Max(aggMax).toSQLString();
            case AggMaxDistinct aggMaxDistinct -> new MaxDistinct(aggMaxDistinct).toSQLString();
            case AggMin aggMin -> new Min(aggMin).toSQLString();
            case AggMinDistinct aggMinDistinct -> new MinDistinct(aggMinDistinct).toSQLString();
            case AggMode aggMode -> new Mode(aggMode).toSQLString();
            case AggModeDistinct aggModeDistinct -> new ModeDistinct(aggModeDistinct).toSQLString();
            case AggSample aggSample -> new Sample(aggSample).toSQLString();
            case AggSampleDistinct aggSampleDistinct -> new SampleDistinct(aggSampleDistinct).toSQLString();
            case AggNull aggNull -> new Null(aggNull).toSQLString();
            case AggSum aggSum -> new Sum(aggSum).toSQLString();
            case AggSumDistinct aggSumDistinct -> new SumDistinct(aggSumDistinct).toSQLString();
            default -> throw new IllegalStateException("Unexpected value: " + this.getJenaExpr().getAggregator());
        };
    }
}
