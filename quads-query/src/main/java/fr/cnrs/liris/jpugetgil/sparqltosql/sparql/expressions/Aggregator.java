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
        return switch (this.getJenaExpr().getAggregator()) {
            case AggAvg aggAvg -> new Avg(aggAvg).toSQLString(sqlVariables);
            case AggAvgDistinct aggAvgDistinct -> new AvgDistinct(aggAvgDistinct).toSQLString(sqlVariables);
            case AggMedian aggMedian -> new Median(aggMedian).toSQLString(sqlVariables);
            case AggMedianDistinct aggMedianDistinct -> new MedianDistinct(aggMedianDistinct).toSQLString(sqlVariables);
            case AggCount aggCount -> new Count(aggCount).toSQLString(sqlVariables);
            case AggCountDistinct aggCountDistinct -> new CountDistinct(aggCountDistinct).toSQLString(sqlVariables);
            case AggCountVar aggCountVar -> new CountVar(aggCountVar).toSQLString(sqlVariables);
            case AggCountVarDistinct aggCountVarDistinct -> new CountVarDistinct(aggCountVarDistinct).toSQLString(sqlVariables);
            case AggCustom aggCustom -> new Custom(aggCustom).toSQLString(sqlVariables);
            case AggGroupConcat aggGroupConcat -> new GroupConcat(aggGroupConcat).toSQLString(sqlVariables);
            case AggGroupConcatDistinct aggGroupConcatDistinct -> new GroupConcatDistinct(aggGroupConcatDistinct).toSQLString(sqlVariables);
            case AggMax aggMax -> new Max(aggMax).toSQLString(sqlVariables);
            case AggMaxDistinct aggMaxDistinct -> new MaxDistinct(aggMaxDistinct).toSQLString(sqlVariables);
            case AggMin aggMin -> new Min(aggMin).toSQLString(sqlVariables);
            case AggMinDistinct aggMinDistinct -> new MinDistinct(aggMinDistinct).toSQLString(sqlVariables);
            case AggMode aggMode -> new Mode(aggMode).toSQLString(sqlVariables);
            case AggModeDistinct aggModeDistinct -> new ModeDistinct(aggModeDistinct).toSQLString(sqlVariables);
            case AggSample aggSample -> new Sample(aggSample).toSQLString(sqlVariables);
            case AggSampleDistinct aggSampleDistinct -> new SampleDistinct(aggSampleDistinct).toSQLString(sqlVariables);
            case AggNull aggNull -> new Null(aggNull).toSQLString(sqlVariables);
            case AggSum aggSum -> new Sum(aggSum).toSQLString(sqlVariables);
            case AggSumDistinct aggSumDistinct -> new SumDistinct(aggSumDistinct).toSQLString(sqlVariables);
            default -> throw new IllegalStateException("Unexpected value: " + this.getJenaExpr().getAggregator());
        };
    }
}
