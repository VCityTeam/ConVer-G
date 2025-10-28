package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SQLOperator {
    protected static final Logger log = LoggerFactory.getLogger(SQLOperator.class);

    protected SQLOperator() {
        log.info("Created {} SQL operator.", this.getClass().getSimpleName());
    }

    public abstract SQLQuery buildSQLQuery();

    protected abstract String buildSelect();

    protected abstract String buildFrom();

    protected abstract String buildWhere();
}
