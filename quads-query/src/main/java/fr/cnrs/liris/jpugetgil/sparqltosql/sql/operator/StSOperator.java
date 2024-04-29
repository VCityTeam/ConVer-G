package fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator;

import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public abstract class StSOperator {
    private static final Logger log = LoggerFactory.getLogger(StSOperator.class);

    public StSOperator() {
        log.info("Found {}.", this.getClass().getSimpleName());
    }

    List<SQLVariable> sqlVariables = new ArrayList<>();

    public abstract SQLQuery buildSQLQuery();
}
