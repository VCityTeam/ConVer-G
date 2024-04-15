package fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator;

import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLVariable;

import java.util.ArrayList;
import java.util.List;

public abstract class StSOperator {
    List<SQLVariable> sqlVariables = new ArrayList<>();

    public abstract SQLQuery buildSQLQuery();
}
