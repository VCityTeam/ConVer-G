package fr.cnrs.liris.jpugetgil.converg;

import fr.cnrs.liris.jpugetgil.converg.utils.PgUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.postgresql.jdbc.PgResultSet;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;

public class BindingIterator implements Iterator<Binding> {
    private final PgResultSet pgResultSet;
    private final ResultSetMetaData rsmd;
    private final List<String> allVariables = new ArrayList<>();
    private final List<String> variables = new ArrayList<>();
    private final Set<Var> vars = new HashSet<>();
    private boolean hasNextValue;

    public BindingIterator(PgResultSet pgResultSet) throws SQLException {
        this.pgResultSet = pgResultSet;
        this.rsmd = pgResultSet.getMetaData();
        buildAllVariablesAndVariables();
        hasNextValue = pgResultSet.next();
    }

    public Set<Var> getVars() {
        return vars;
    }

    @Override
    public Binding next() {
        if (!hasNextValue) {
            try {
                if(pgResultSet.isClosed()) {
                    throw new NoSuchElementException("No more elements and result set is closed.");
                }
            } catch(SQLException e) {
                throw new RuntimeException(e);
            }
        }
        if (!hasNextValue) {
            throw new NoSuchElementException("No more elements.");
        }

        try {
            BindingBuilder bindingBuilder = Binding.builder();

            for (String v : variables) {
                Var variable = Var.alloc(v);
                Node variableValue = null;

                try {
                    if (PgUtils.hasColumn(pgResultSet, "name$" + v) && pgResultSet.getString("name$" + v) != null) {
                        String value = pgResultSet.getString("name$" + v);
                        String valueType;
                        if (allVariables.contains("type$" + v)) {
                            valueType = pgResultSet.getString("type$" + v);
                        } else {
                            valueType = PgUtils.getAssociatedRDFType(rsmd.getColumnType(pgResultSet.findColumn("name$" + v)));
                        }
                        variableValue = valueType == null ?
                                NodeFactory.createURI(value) : NodeFactory.createLiteral(value, NodeFactory.getType(valueType));
                    } else if (PgUtils.hasColumn(pgResultSet, v) && pgResultSet.getString(v) != null) {
                        String value = pgResultSet.getString(v);
                        String valueType = PgUtils.getAssociatedRDFType(rsmd.getColumnType(pgResultSet.findColumn(v)));
                        variableValue = NodeFactory.createLiteral(value, NodeFactory.getType(valueType));
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                if (variableValue != null) {
                    bindingBuilder.add(variable, variableValue);
                }
            }

            hasNextValue = pgResultSet.next();
            return bindingBuilder.build();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasNext() {
        return hasNextValue;
    }

    @Override
    public void remove() {
        Iterator.super.remove();
    }

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