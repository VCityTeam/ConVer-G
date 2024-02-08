package fr.cnrs.liris.jpugetgil.sparqltosql;

public class Occurrence {
    private String type;
    private Integer position;

    public Occurrence(String type, Integer position) {
        this.type = type;
        this.position = position;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }
}
