package fr.cnrs.liris.jpugetgil.converg;

import fr.cnrs.liris.jpugetgil.converg.utils.PgUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
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
                    if (PgUtils.hasColumn(resultSet, "name$" + v) && resultSet.getString("name$" + v) != null) {
                        String value = resultSet.getString("name$" + v);
                        String valueType;
                        if (allVariables.contains("type$" + v)) {
                            valueType = resultSet.getString("type$" + v);
                        } else {
                            valueType = PgUtils.getAssociatedRDFType(rsmd.getColumnType(resultSet.findColumn("name$" + v)));
                        }
                        variableValue = valueType == null ?
                                NodeFactory.createURI(value) : NodeFactory.createLiteral(value, NodeFactory.getType(valueType));
                    } else if (PgUtils.hasColumn(resultSet, v) && resultSet.getString(v) != null) {
                        String value = resultSet.getString(v);
                        String valueType = PgUtils.getAssociatedRDFType(rsmd.getColumnType(resultSet.findColumn(v)));
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
}
