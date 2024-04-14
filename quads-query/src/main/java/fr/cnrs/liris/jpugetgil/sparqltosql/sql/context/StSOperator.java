package fr.cnrs.liris.jpugetgil.sparqltosql.sql.context;

import fr.cnrs.liris.jpugetgil.sparqltosql.SQLQuery;
import fr.cnrs.liris.jpugetgil.sparqltosql.SQLVariable;

import java.util.ArrayList;
import java.util.List;

public abstract class StSOperator {
    List<SQLVariable> sqlVariables = new ArrayList<>();

    public abstract SQLQuery buildSQLQuery();
}
