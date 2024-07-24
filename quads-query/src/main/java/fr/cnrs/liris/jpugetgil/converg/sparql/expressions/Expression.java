package fr.cnrs.liris.jpugetgil.converg.sparql.expressions;

import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.constants.*;
import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.op.*;
import fr.cnrs.liris.jpugetgil.converg.sparql.transformer.FilterConfiguration;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.expr.nodevalue.*;

public interface Expression {

    static Expression fromJenaExpr(Expr expr) {
        return switch (expr) {
            case E_Add add -> new Add(add);
            case E_Bound bound -> new Bound(bound);
            case E_Divide divide -> new Divide(divide);
            case E_Equals equals -> new Equals(equals);
            case E_Exists exists -> new Exists(exists);
            case E_Function ef -> new FunctionApp(ef);
            case E_GreaterThan gt -> new GreaterThan(gt);
            case E_GreaterThanOrEqual gte -> new GreaterThanOrEqual(gte);
            case E_LessThan lt -> new LessThan(lt);
            case E_LessThanOrEqual lte -> new LessThanOrEqual(lte);
            case E_LogicalAnd la -> new LogicalAnd(la);
            case E_LogicalNot ln -> new LogicalNot(ln);
            case E_LogicalOr lo -> new LogicalOr(lo);
            case E_Multiply m -> new Multiply(m);
            case E_NotEquals ne -> new NotEquals(ne);
            case E_NotExists nex -> new NotExists(nex);
            case E_Now eNow -> new Now(eNow);
            case E_NumAbs numAbs -> new NumAbs(numAbs);
            case E_NumCeiling eNumCeiling -> new DirectApplication1(eNumCeiling, true, "ceil");
            case E_NumFloor eNumFloor -> new DirectApplication1(eNumFloor, true, "floor");
            case E_NumRound eNumRound -> new DirectApplication1(eNumRound, true, "round");
            case E_OpNumericIntegerDivide eOpNumericIntegerDivide ->
                    new OpNumericIntegerDivide(eOpNumericIntegerDivide);
            case E_OpNumericMod eOpNumericMod -> new OpNumericMod(eOpNumericMod);
            case E_Str eStr -> new Str(eStr);
            case E_StrAfter eStrAfter -> new StrAfter(eStrAfter);
            case E_StrBefore eStrBefore -> new StrBefore(eStrBefore);
            case E_StrContains eStrContains -> new StrContains(eStrContains);
            case E_StrEndsWith eStrEndsWith -> new StrEndsWith(eStrEndsWith);
            case E_StrLength eStrLength -> new DirectApplication1(eStrLength, true, "character_length");
            case E_StrLowerCase eStrLowerCase -> new DirectApplication1(eStrLowerCase, true, "lower");
            case E_StrStartsWith eStrStartsWith -> new DirectApplication2(eStrStartsWith, true, "starts_with");
            case E_StrSubstring eStrSubstring -> new DirectApplicationN(eStrSubstring, true, "substr");
            case E_StrUpperCase eStrUpperCase -> new DirectApplication1(eStrUpperCase, true, "upper");
            case E_Subtract eSubtract -> new Subtract(eSubtract);
            case E_UnaryMinus eUnaryMinus -> new UnaryMinus(eUnaryMinus);
            case E_UnaryPlus eUnaryPlus -> new UnaryPlus(eUnaryPlus);
            case ExprAggregator eAggregator -> new Aggregator(eAggregator);
            case ExprNone eNone -> new None(eNone);
            case ExprVar eVar -> new Var(eVar);
            case NodeValueBoolean nodeValueBoolean -> new BooleanConstant(nodeValueBoolean);
            case NodeValueDateTime nodeValueDateTime -> new DateTimeConstant(nodeValueDateTime);
            case NodeValueDecimal nodeValueDecimal -> new DecimalConstant(nodeValueDecimal);
            case NodeValueDouble nodeValueDouble -> new DoubleConstant(nodeValueDouble);
            case NodeValueDuration nodeValueDuration -> new DurationConstant(nodeValueDuration);
            case NodeValueFloat nodeValueFloat -> new FloatConstant(nodeValueFloat);
            case NodeValueInteger nodeValueInteger -> new IntegerConstant(nodeValueInteger);
            case NodeValueNode nodeValueNode -> new NodeConstant(nodeValueNode);
            case NodeValueString nvs -> new StringConstant(nvs);
            // No priority on these types:
            // NodeValueSortKey,
            // E_AdjustToTimezone, E_Call, E_Cast, E_Coalesce, E_Conditional, E_Datatype,
            // E_DateTimeDay, E_DateTimeHours, E_DateTimeMinutes, E_DateTimeMonth,
            // E_DateTimeSeconds, E_DateTimeTimezone, E_DateTimeTZ, E_DateTimeYear,
            // E_FunctionDynamic, E_IRI, E_IRI2, E_Lang, E_LangMatches, E_MD5,
            // E_NotOneOf, E_OneOf, E_OneOfBase, E_Random, E_Regex, E_SameTerm,
            // E_SHA1, E_SHA224, E_SHA256, E_SHA384, E_SHA512, E_StrDatatype,
            // E_StrEncodeForURI, E_StrLang, E_StrReplace, E_StrUUID,
            // E_TripleFn, E_TripleObject, E_TriplePredicate, E_TripleSubject,
            // E_URI, E_URI2, E_UUID, E_Version, ExprTripleTerm, NodeValueLang,
            //
            // Typing boolean functions:
            // E_IsBlank, E_IsIRI, E_IsLiteral, E_IsNumeric, E_IsTriple, E_IsURI,
            //
            // Abstract classes in hierarchy:
            // ExprFunction, ExprFunction0, ExprFunction1,
            // ExprFunction2, ExprFunction3, ExprFunctionN, ExprFunctionOp,
            // ExprDigest, ExprNode, ExprSystem, NodeValue
            default -> throw new UnsupportedExpressionException(expr);
        };
    }

    /**
     * The expression used to build this object. It is represented in jena algebra.
     *
     * @return expression represented in jena algebra
     */
    Expr getJenaExpr();

    default String toSQLString() {
        throw new IllegalArgumentException("This expression does not have a SQL representation.");
    }

    default String toSQLStringAgg() {
        throw new IllegalArgumentException("This expression does not have a SQL representation.");
    }

    /**
     * Updates the filter configuration to take this expression's variables into account
     *
     * @param configuration the configuration to update
     * @param requiresValue true if the value is required, false if the int representation is sufficient
     */
    void updateFilterConfiguration(FilterConfiguration configuration, boolean requiresValue);
}
