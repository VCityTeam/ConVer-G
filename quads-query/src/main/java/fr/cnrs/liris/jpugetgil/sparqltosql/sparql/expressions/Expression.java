package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions;

import org.apache.jena.sparql.expr.E_Add;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.nodevalue.NodeValueSortKey;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;

public interface Expression {
    public static Expression fromJenaExpr(Expr expr) {
        return switch (expr) {
            case NodeValueString nvs -> new StringConstant(nvs);
//            E_Add, E_AdjustToTimezone, E_Bound, E_Call, E_Cast, E_Coalesce, E_Conditional, E_Datatype, E_DateTimeDay, E_DateTimeHours, E_DateTimeMinutes, E_DateTimeMonth, E_DateTimeSeconds, E_DateTimeTimezone, E_DateTimeTZ, E_DateTimeYear, E_Divide, E_Equals, E_Exists, E_Function, E_FunctionDynamic, E_GreaterThan, E_GreaterThanOrEqual, E_IRI, E_IRI2, E_IsBlank, E_IsIRI, E_IsLiteral, E_IsNumeric, E_IsTriple, E_IsURI, E_Lang, E_LangMatches, E_LessThan, E_LessThanOrEqual, E_LogicalAnd, E_LogicalNot, E_LogicalOr, E_MD5, E_Multiply, E_NotEquals, E_NotExists, E_NotOneOf, E_Now, E_NumAbs, E_NumCeiling, E_NumFloor, E_NumRound, E_OneOf, E_OneOfBase, E_OpNumericIntegerDivide, E_OpNumericMod, E_Random, E_Regex, E_SameTerm, E_SHA1, E_SHA224, E_SHA256, E_SHA384, E_SHA512, E_Str, E_StrAfter, E_StrBefore, E_StrConcat, E_StrContains, E_StrDatatype, E_StrEncodeForURI, E_StrEndsWith, E_StrLang, E_StrLength, E_StrLowerCase, E_StrReplace, E_StrStartsWith, E_StrSubstring, E_StrUpperCase, E_StrUUID, E_Subtract, E_TripleFn, E_TripleObject, E_TriplePredicate, E_TripleSubject, E_UnaryMinus, E_UnaryPlus, E_URI, E_URI2, E_UUID, E_Version,
//            ExprAggregator, ExprDigest, ExprFunction, ExprFunction0, ExprFunction1, ExprFunction2, ExprFunction3, ExprFunctionN, ExprFunctionOp, ExprNode, ExprNone, ExprSystem, ExprTripleTerm, ExprVar, NodeValue, NodeValueBoolean, NodeValueDateTime, NodeValueDecimal, NodeValueDouble, NodeValueDuration, NodeValueFloat, NodeValueInteger, NodeValueLang, NodeValueNode,

            case NodeValueSortKey nvsk -> throw new UnsupportedExpressionException(expr);
            case E_Add add -> throw new UnsupportedExpressionException(expr);
            default -> throw new UnsupportedExpressionException(expr);
        };
    }

    /**
     * The expression used to build this object. It is represented in jena algebra.
     *
     * @return expression represented in jena algebra
     */
    Expr getJenaExpr();
}
