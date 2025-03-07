package fr.cnrs.liris.jpugetgil.converg;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.function.Consumer;

public class BindingIterator implements Iterator<Binding> {
    private final ResultSet resultSet;
    private final ResultSetMetaData rsmd;
    private final List<String> allVariables = new ArrayList<>();
    private final List<String> variables = new ArrayList<>();
    private final Set<Var> vars = new HashSet<>();

    public BindingIterator(ResultSet resultSet) throws SQLException {
        this.resultSet = resultSet;
        this.rsmd = resultSet.getMetaData();
        buildAllVariablesAndVariables();
    }

    public Set<Var> getVars() {
        return vars;
    }

    @Override
    public Binding next() {
        try {
            resultSet.next();

            BindingBuilder bindingBuilder = Binding.builder();

            for (String v : variables) {
                Var variable = Var.alloc(v);
                Node variableValue;

                try {
                    if (hasColumn(resultSet, "name$" + v) && resultSet.getString("name$" + v) != null) {
                        String value = resultSet.getString("name$" + v);
                        String valueType;
                        if (allVariables.contains("type$" + v)) {
                            valueType = resultSet.getString("type$" + v);
                        } else {
                            valueType = getAssociatedRDFType(rsmd.getColumnType(resultSet.findColumn("name$" + v)));
                        }
                        variableValue = valueType == null ?
                                NodeFactory.createURI(value) : NodeFactory.createLiteral(value, NodeFactory.getType(valueType));
                    } else if (hasColumn(resultSet, v) && resultSet.getString(v) != null) {
                        String value = resultSet.getString(v);
                        String valueType = getAssociatedRDFType(rsmd.getColumnType(resultSet.findColumn(v)));
                        variableValue = NodeFactory.createLiteral(value, NodeFactory.getType(valueType));
                    } else {
                        variableValue = null;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                if (variableValue != null) {
                    bindingBuilder.add(variable, variableValue);
                }
            }

            return bindingBuilder.build();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return true if the iteration has more elements
     */
    @Override
    public boolean hasNext() {
        try {
            boolean hasNext = !resultSet.isLast();

            if (!hasNext) {
                resultSet.close();
            }
            return hasNext;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 
     */
    @Override
    public void remove() {
        Iterator.super.remove();
    }

    /**
     * @param action The action to be performed for each element 
     */
    @Override
    public void forEachRemaining(Consumer<? super Binding> action) {
        while (hasNext()) {
            action.accept(next());
        }
    }

    private void buildAllVariablesAndVariables() throws SQLException {
        int nbColumns = rsmd.getColumnCount();
        for (int i = 1; i <= nbColumns; i++) {
            String columnName = rsmd.getColumnName(i);
            allVariables.add(columnName);

            if (columnName.startsWith("name$")) {
                String variableName = columnName.substring(5);
                variables.add(variableName);
                Var var = Var.alloc(variableName);
                vars.add(var);
            } else if (!columnName.startsWith("type$")) {
                variables.add(columnName);
                Var var = Var.alloc(columnName);
                vars.add(var);
            }
        }
    }

    private boolean hasColumn(java.sql.ResultSet rs, String columnName) {
        try {
            rs.findColumn(columnName);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private String getAssociatedRDFType(int sqlType) {
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
