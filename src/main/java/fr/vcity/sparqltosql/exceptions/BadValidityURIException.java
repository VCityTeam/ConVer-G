package fr.vcity.sparqltosql.exceptions;

public class BadValidityURIException extends Exception {
    public BadValidityURIException(String uri) {
        super("Bad validity pattern. The URI musts contain \"/Validity#\". Provided URI: " + uri);
    }
}
