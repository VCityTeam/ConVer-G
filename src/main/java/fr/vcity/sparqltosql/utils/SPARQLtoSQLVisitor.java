package fr.vcity.sparqltosql.utils;

import fr.vcity.sparqltosql.exceptions.BadValidityURIException;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.sparql.algebra.OpVisitor;
import org.apache.jena.sparql.algebra.op.*;

@Slf4j
public class SPARQLtoSQLVisitor implements OpVisitor {

    /**
     * gets the list of triple from the Basic Graph Pattern
     *
     * @param opBGP the Basic Graph Pattern operator
     */
    @Override
    public void visit(OpBGP opBGP) {
        log.info("Visiting OpBGP size: {}", opBGP.getPattern().size());
        opBGP
                .getPattern()
                .getList()
                .forEach(triple -> log.info("Triple: {}", triple.toString()));
        log.info("-----------------");
    }

    /**
     * @param quadPattern
     */
    @Override
    public void visit(OpQuadPattern quadPattern) {
        log.info("Visiting OpQuadPattern");
        log.info("-----------------");
    }

    /**
     * @param quadBlock
     */
    @Override
    public void visit(OpQuadBlock quadBlock) {
        log.info("Visiting OpQuadBlock");
        log.info("-----------------");
    }

    /**
     * @param opTriple
     */
    @Override
    public void visit(OpTriple opTriple) {
        log.info("Visiting OpTriple");
        log.info("-----------------");
    }

    /**
     * @param opQuad
     */
    @Override
    public void visit(OpQuad opQuad) {
        log.info("Visiting OpQuad");
        log.info("-----------------");
    }

    /**
     * @param opPath
     */
    @Override
    public void visit(OpPath opPath) {
        log.info("Visiting OpPath");
        log.info("-----------------");
    }

    /**
     * @param opTable
     */
    @Override
    public void visit(OpTable opTable) {
        log.info("Visiting OpTable");
        log.info("-----------------");
    }

    /**
     * @param opNull
     */
    @Override
    public void visit(OpNull opNull) {
        log.info("Visiting OpNull");
        log.info("-----------------");
    }

    /**
     * @param opProc
     */
    @Override
    public void visit(OpProcedure opProc) {
        log.info("Visiting OpProcedure");
        log.info("-----------------");
    }

    /**
     * @param opPropFunc
     */
    @Override
    public void visit(OpPropFunc opPropFunc) {
        log.info("Visiting OpPropFunc");
        log.info("-----------------");
    }

    /**
     * @param opFilter
     */
    @Override
    public void visit(OpFilter opFilter) {
        // FIXME: Visit and evaluate expression?
        log.info("Visiting OpFilter Expr size: {}", opFilter.getExprs().size());
        opFilter
                .getExprs()
                .forEach(expr -> log.info("Expr: {}", expr.toString()));
        log.info("-----------------");
    }

    /**
     * gets the validity String from graph name
     *
     * @param opGraph the Graph Operator
     */
    @Override
    public void visit(OpGraph opGraph) {
        log.info("Visiting OpGraph: {}", opGraph.getNode().toString());
        try {
            log.info("Validity: {}", getValidityFromURI(opGraph.getNode().toString()));
        } catch (BadValidityURIException e) {
            log.error(e.getMessage());
        }
        log.info("-----------------");
    }

    /**
     * @param opService
     */
    @Override
    public void visit(OpService opService) {
        log.info("Visiting OpService");
        log.info("-----------------");
    }

    /**
     * @param dsNames
     */
    @Override
    public void visit(OpDatasetNames dsNames) {
        log.info("Visiting OpDatasetNames");
        log.info("-----------------");
    }

    /**
     * @param opLabel
     */
    @Override
    public void visit(OpLabel opLabel) {
        log.info("Visiting OpLabel");
        log.info("-----------------");
    }

    /**
     * @param opAssign
     */
    @Override
    public void visit(OpAssign opAssign) {
        log.info("Visiting OpAssign");
        log.info("-----------------");
    }

    /**
     * @param opExtend
     */
    @Override
    public void visit(OpExtend opExtend) {
        log.info("Visiting OpExtend");
        log.info("-----------------");
    }

    /**
     * @param opJoin
     */
    @Override
    public void visit(OpJoin opJoin) {
        // FIXME: Visit and evaluate expression?
        log.info("Visiting OpJoin");
        log.info("opJoin Left: {}", opJoin.getLeft().toString());
        log.info(opJoin.getLeft().getName());
        log.info("opJoin Right: {}", opJoin.getRight().toString());
        log.info(opJoin.getRight().getName());
        log.info("-----------------");
    }

    /**
     * @param opLeftJoin
     */
    @Override
    public void visit(OpLeftJoin opLeftJoin) {
        log.info("Visiting OpLeftJoin");
        log.info("-----------------");
    }

    /**
     * @param opUnion
     */
    @Override
    public void visit(OpUnion opUnion) {
        log.info("Visiting OpUnion");
        log.info("-----------------");
    }

    /**
     * @param opDiff
     */
    @Override
    public void visit(OpDiff opDiff) {
        log.info("Visiting OpDiff");
        log.info("-----------------");
    }

    /**
     * @param opMinus
     */
    @Override
    public void visit(OpMinus opMinus) {
        log.info("Visiting OpMinus");
        log.info("-----------------");
    }

    /**
     * @param opLateral
     */
    @Override
    public void visit(OpLateral opLateral) {
        log.info("Visiting OpLateral");
        log.info("-----------------");
    }

    /**
     * @param opCondition
     */
    @Override
    public void visit(OpConditional opCondition) {
        log.info("Visiting OpConditional");
        log.info("-----------------");
    }

    /**
     * @param opSequence
     */
    @Override
    public void visit(OpSequence opSequence) {
        log.info("Visiting OpSequence");
        log.info("-----------------");
    }

    /**
     * @param opDisjunction
     */
    @Override
    public void visit(OpDisjunction opDisjunction) {
        log.info("Visiting OpDisjunction");
        log.info("-----------------");
    }

    /**
     * @param opList
     */
    @Override
    public void visit(OpList opList) {
        log.info("Visiting OpList");
        log.info("-----------------");
    }

    /**
     * @param opOrder
     */
    @Override
    public void visit(OpOrder opOrder) {
        log.info("Visiting OpOrder");
        log.info("-----------------");
    }

    /**
     * gets the variable list from the SELECT statement
     *
     * @param opProject The project operator
     */
    @Override
    public void visit(OpProject opProject) {
        log.info("Visiting OpProject size: {}", opProject.getVars().size());
        opProject
                .getVars()
                .forEach(var -> log.info("Var: {}", var.getVarName()));
        log.info("-----------------");
    }

    /**
     * @param opReduced
     */
    @Override
    public void visit(OpReduced opReduced) {
        log.info("Visiting OpReduced");
        log.info("-----------------");
    }

    /**
     * @param opDistinct
     */
    @Override
    public void visit(OpDistinct opDistinct) {
        log.info("Visiting OpDistinct");
        log.info("-----------------");
    }

    /**
     * @param opSlice
     */
    @Override
    public void visit(OpSlice opSlice) {
        log.info("Visiting OpSlice");
        log.info("-----------------");
    }

    /**
     * gets the aggregator and var list
     *
     * @param opGroup the group operator
     */
    @Override
    public void visit(OpGroup opGroup) {
        // FIXME: Visit and evaluate expression?
        log.info("Visiting OpGroup var size: {}", opGroup.getGroupVars().size());
        opGroup
                .getGroupVars()
                .getVars()
                .forEach(var -> log.info("Var: {}", var.getVarName()));
        log.info("Visiting OpGroup aggregator size: {}", opGroup.getAggregators().size());
        opGroup
                .getAggregators()
                .forEach(exprAggregator -> {
                    log.info("Aggregator: {} ", exprAggregator.getAggregator().getName());
                    log.info("Associated aggregator expression size: {}", exprAggregator.getAggregator().getExprList().size());
                    exprAggregator
                            .getAggregator()
                            .getExprList()
                            .forEach(expr -> log.info("Agg {} Expression: {}", exprAggregator.getAggregator().getName(), expr.getVarName()));
                });
        log.info("-----------------");
    }

    /**
     * @param opTopN
     */
    @Override
    public void visit(OpTopN opTopN) {
        log.info("Visiting OpTopN");
        log.info("-----------------");
    }

    private static String getValidityFromURI(String uri) throws BadValidityURIException {
        // checking the URI contains validity pattern
        if (!uri.contains("/Validity#")) {
            throw new BadValidityURIException(uri);
        }

        return uri.substring(uri.indexOf('#') + 1);
    }
}
