package fr.cnrs.liris.jpugetgil.converg.utils;

import org.apache.jena.datatypes.xsd.XSDDatatype;

import java.sql.SQLException;
import java.sql.Types;

public class PgUtils {
    public static boolean hasColumn(java.sql.ResultSet rs, String columnName) {
        try {
            rs.findColumn(columnName);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public static String getAssociatedRDFType(int sqlType) {
        // Implement this method to map SQL types to RDF types
        return switch (sqlType) {
            case Types.INTEGER -> XSDDatatype.XSDinteger.getURI();
            case Types.VARCHAR -> XSDDatatype.XSDstring.getURI();
            case Types.BOOLEAN -> XSDDatatype.XSDboolean.getURI();
            case Types.DOUBLE -> XSDDatatype.XSDdouble.getURI();
            case Types.FLOAT -> XSDDatatype.XSDfloat.getURI();
            case Types.DECIMAL -> XSDDatatype.XSDdecimal.getURI();
            case Types.TIMESTAMP -> XSDDatatype.XSDdateTime.getURI();
            case Types.DATE -> XSDDatatype.XSDdate.getURI();
            case Types.TIME -> XSDDatatype.XSDtime.getURI();
            case Types.BIGINT -> XSDDatatype.XSDlong.getURI();
            // Add more cases as needed
            default -> null; // or a default type
        };
    }
}
