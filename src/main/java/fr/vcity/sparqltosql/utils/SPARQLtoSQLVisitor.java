package fr.vcity.sparqltosql.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.jena.sparql.algebra.OpVisitor;
import org.apache.jena.sparql.algebra.op.*;

@Slf4j
public class SPARQLtoSQLVisitor implements OpVisitor {

    private static final String VALIDITY_PATTERN = "/Validity#";
    private static final String VERSION_PATTERN = "/Version#";

    /**
     * gets the list of triple from the Basic Graph Pattern
     *
     * @param opBGP the Basic Graph Pattern operator
     */
    @Override
    public void visit(OpBGP opBGP) {
        log.debug("Visiting OpBGP size: {}", opBGP.getPattern().size());
        opBGP
                .getPattern()
                .getList()
                .forEach(triple -> log.debug("Triple: {}", triple.toString()));
        log.debug("-----------------");
    }

    /**
     * @param quadPattern
     */
    @Override
    public void visit(OpQuadPattern quadPattern) {
        log.debug("Visiting OpQuadPattern");
        log.debug("-----------------");
    }

    /**
     * @param quadBlock
     */
    @Override
    public void visit(OpQuadBlock quadBlock) {
        log.debug("Visiting OpQuadBlock");
        log.debug("-----------------");
    }

    /**
     * @param opTriple
     */
    @Override
    public void visit(OpTriple opTriple) {
        log.debug("Visiting OpTriple");
        log.debug("-----------------");
    }

    /**
     * @param opQuad
     */
    @Override
    public void visit(OpQuad opQuad) {
        log.debug("Visiting OpQuad");
        log.debug("-----------------");
    }

    /**
     * @param opPath
     */
    @Override
    public void visit(OpPath opPath) {
        log.debug("Visiting OpPath");
        log.debug("-----------------");
    }

    /**
     * @param opTable
     */
    @Override
    public void visit(OpTable opTable) {
        log.debug("Visiting OpTable");
        log.debug("-----------------");
    }

    /**
     * @param opNull
     */
    @Override
    public void visit(OpNull opNull) {
        log.debug("Visiting OpNull");
        log.debug("-----------------");
    }

    /**
     * @param opProc
     */
    @Override
    public void visit(OpProcedure opProc) {
        log.debug("Visiting OpProcedure");
        log.debug("-----------------");
    }

    /**
     * @param opPropFunc
     */
    @Override
    public void visit(OpPropFunc opPropFunc) {
        log.debug("Visiting OpPropFunc");
        log.debug("-----------------");
    }

    /**
     * @param opFilter
     */
    @Override
    public void visit(OpFilter opFilter) {
        // FIXME: Visit and evaluate expression?
        log.debug("Visiting OpFilter Expr size: {}", opFilter.getExprs().size());
        opFilter
                .getExprs()
                .forEach(expr -> log.debug("Expr: {}", expr.toString()));
        log.debug("-----------------");
    }

    /**
     * gets the validity String from graph name
     *
     * @param opGraph the Graph Operator
     */
    @Override
    public void visit(OpGraph opGraph) {
        String nodeString = opGraph.getNode().toString();
        log.debug("Visiting OpGraph: {}", nodeString);

        if (nodeString.contains(VALIDITY_PATTERN) || nodeString.contains(VERSION_PATTERN)) {
            log.debug("V: {}", getAnchorValueFromURI(nodeString));
        }

        log.debug("-----------------");
    }

    /**
     * @param opService
     */
    @Override
    public void visit(OpService opService) {
        log.debug("Visiting OpService");
        log.debug("-----------------");
    }

    /**
     * @param dsNames
     */
    @Override
    public void visit(OpDatasetNames dsNames) {
        log.debug("Visiting OpDatasetNames");
        log.debug("-----------------");
    }

    /**
     * @param opLabel
     */
    @Override
    public void visit(OpLabel opLabel) {
        log.debug("Visiting OpLabel");
        log.debug("-----------------");
    }

    /**
     * @param opAssign
     */
    @Override
    public void visit(OpAssign opAssign) {
        log.debug("Visiting OpAssign");
        log.debug("-----------------");
    }

    /**
     * @param opExtend
     */
    @Override
    public void visit(OpExtend opExtend) {
        log.debug("Visiting OpExtend");
        log.debug("-----------------");
    }

    /**
     * @param opJoin
     */
    @Override
    public void visit(OpJoin opJoin) {
        // FIXME: Visit and evaluate expression?
        log.debug("Visiting OpJoin");
        log.debug("opJoin Left: {}", opJoin.getLeft().toString());
        log.debug(opJoin.getLeft().getName());
        log.debug("opJoin Right: {}", opJoin.getRight().toString());
        log.debug(opJoin.getRight().getName());
        log.debug("-----------------");
    }

    /**
     * @param opLeftJoin
     */
    @Override
    public void visit(OpLeftJoin opLeftJoin) {
        log.debug("Visiting OpLeftJoin");
        log.debug("-----------------");
    }

    /**
     * @param opUnion
     */
    @Override
    public void visit(OpUnion opUnion) {
        log.debug("Visiting OpUnion");
        log.debug("-----------------");
    }

    /**
     * @param opDiff
     */
    @Override
    public void visit(OpDiff opDiff) {
        log.debug("Visiting OpDiff");
        log.debug("-----------------");
    }

    /**
     * @param opMinus
     */
    @Override
    public void visit(OpMinus opMinus) {
        log.debug("Visiting OpMinus");
        log.debug("-----------------");
    }

    /**
     * @param opLateral
     */
    @Override
    public void visit(OpLateral opLateral) {
        log.debug("Visiting OpLateral");
        log.debug("-----------------");
    }

    /**
     * @param opCondition
     */
    @Override
    public void visit(OpConditional opCondition) {
        log.debug("Visiting OpConditional");
        log.debug("-----------------");
    }

    /**
     * @param opSequence
     */
    @Override
    public void visit(OpSequence opSequence) {
        log.debug("Visiting OpSequence");
        log.debug("-----------------");
    }

    /**
     * @param opDisjunction
     */
    @Override
    public void visit(OpDisjunction opDisjunction) {
        log.debug("Visiting OpDisjunction");
        log.debug("-----------------");
    }

    /**
     * @param opList
     */
    @Override
    public void visit(OpList opList) {
        log.debug("Visiting OpList");
        log.debug("-----------------");
    }

    /**
     * @param opOrder
     */
    @Override
    public void visit(OpOrder opOrder) {
        log.debug("Visiting OpOrder");
        log.debug("-----------------");
    }

    /**
     * gets the variable list from the SELECT statement
     *
     * @param opProject The project operator
     */
    @Override
    public void visit(OpProject opProject) {
        log.debug("Visiting OpProject size: {}", opProject.getVars().size());
        opProject
                .getVars()
                .forEach(var -> log.debug("Var: {}", var.getVarName()));
        log.debug("-----------------");
    }

    /**
     * @param opReduced
     */
    @Override
    public void visit(OpReduced opReduced) {
        log.debug("Visiting OpReduced");
        log.debug("-----------------");
    }

    /**
     * @param opDistinct
     */
    @Override
    public void visit(OpDistinct opDistinct) {
        log.debug("Visiting OpDistinct");
        log.debug("-----------------");
    }

    /**
     * @param opSlice
     */
    @Override
    public void visit(OpSlice opSlice) {
        log.debug("Visiting OpSlice");
        log.debug("-----------------");
    }

    /**
     * gets the aggregator and var list
     *
     * @param opGroup the group operator
     */
    @Override
    public void visit(OpGroup opGroup) {
        // FIXME: Visit and evaluate expression?
        log.debug("Visiting OpGroup var size: {}", opGroup.getGroupVars().size());
        opGroup
                .getGroupVars()
                .getVars()
                .forEach(var -> log.debug("Var: {}", var.getVarName()));
        log.debug("Visiting OpGroup aggregator size: {}", opGroup.getAggregators().size());
        opGroup
                .getAggregators()
                .forEach(exprAggregator -> {
                    log.debug("Aggregator: {} ", exprAggregator.getAggregator().getName());
                    log.debug("Associated aggregator expression size: {}", exprAggregator.getAggregator().getExprList().size());
                    exprAggregator
                            .getAggregator()
                            .getExprList()
                            .forEach(expr -> log.debug("Agg {} Expression: {}", exprAggregator.getAggregator().getName(), expr.getVarName()));
                });
        log.debug("-----------------");
    }

    /**
     * @param opTopN
     */
    @Override
    public void visit(OpTopN opTopN) {
        log.debug("Visiting OpTopN");
        log.debug("-----------------");
    }

    private static String getAnchorValueFromURI(String uri) {
        return uri.substring(uri.indexOf('#') + 1);
    }
}
