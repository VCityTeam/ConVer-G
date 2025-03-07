package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLOccurrence;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLUtils;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVariable;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.algebra.op.OpProject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProjectSQLOperator extends SQLOperator {

    OpProject opProject;

    SQLQuery query;

    public ProjectSQLOperator(OpProject opProject, SQLQuery query) {
        this.opProject = opProject;
        this.query = query;
    }

    /**
     * @return the SQL query of the quad pattern
     */
    @Override
    public SQLQuery buildSQLQuery() {
        this.query.setContext(applyProjection());

        String select = "SELECT " + buildSelect() + "\n";
        String from = "FROM " + buildFrom() + "\n";
        String where = buildWhere();

        String query = !where.isEmpty() ? select + from + "WHERE " + where : select + from;

        return new SQLQuery(
                query,
                this.query.getContext()
        );
    }

    /**
     * @return the select part of a Project
     */
    @Override
    protected String buildSelect() {
        return this.query.getContext().sparqlVarOccurrences().keySet()
                .stream()
                .map(node -> {
                    SQLVariable maxVariable = SQLUtils.getMaxSQLVariableByOccurrences(this.query.getContext().sparqlVarOccurrences().get(node));

                    return switch (maxVariable.getSqlVarType()) {
                        case VALUE, ID:
                            yield "project_table.v$" + maxVariable.getSqlVarName().replace(".", "agg");
                        case CONDENSED:
                            yield "project_table.bs$" + maxVariable.getSqlVarName().replace(".", "agg") + ", " + "project_table.ng$" + maxVariable.getSqlVarName().replace(".", "agg");
                        case UNBOUND_GRAPH:
                            yield null;
                    };
                })
                .collect(Collectors.joining(", "));
    }

    /**
     * @return the from part of a project
     */
    @Override
    protected String buildFrom() {
        return "(" + this.query.getSql() + ") project_table";
    }

    /**
     * @return the where part of a project pattern
     */
    @Override
    protected String buildWhere() {
        return "";
    }

    /**
     * Apply the projection to the SQL context
     *
     * @return the new SQL context with the projection applied
     */
    private SQLContext applyProjection() {
        Map<Node, List<SPARQLOccurrence>> newSparqlVarOccurrences = new HashMap<>();

        for (Node node : opProject.getVars()) {
            List<SPARQLOccurrence> occurrences = this.query.getContext().sparqlVarOccurrences().get(node);
            if (occurrences != null) {
                newSparqlVarOccurrences.put(node, occurrences);
            }
        }

        return this.query.getContext()
                .setVarOccurrences(newSparqlVarOccurrences);
    }
}
