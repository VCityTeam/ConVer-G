package fr.cnrs.liris.jpugetgil.sparqltosql.sql;

public class SQLClause {
    public String clause;

    public SQLClause(SQLClauseBuilder sqlClauseBuilder) {
        this.clause = sqlClauseBuilder.clause;
    }

    public static class SQLClauseBuilder {
        private String clause;

        public SQLClauseBuilder() {
            this.clause = "";
        }


        public SQLClauseBuilder and(String clause) {
            if (this.clause.isEmpty()) {
                this.clause = clause;
                return this;
            }

            if (clause.isEmpty()) {
                return this;
            }

            this.clause += " AND " + clause;
            return this;
        }

        public SQLClauseBuilder or(String clause) {
            if (this.clause.isEmpty()) {
                this.clause = clause;
                return this;
            }

            if (clause.isEmpty()) {
                return this;
            }

            this.clause += " OR " + clause;
            return this;
        }

        public SQLClauseBuilder parenthesis() {
            if (!this.clause.isEmpty()) {
                this.clause += "(" + this.clause + ")";
                return this;
            }
            return this;
        }

        public SQLClause build() {
            return new SQLClause(this);
        }
    }
}
