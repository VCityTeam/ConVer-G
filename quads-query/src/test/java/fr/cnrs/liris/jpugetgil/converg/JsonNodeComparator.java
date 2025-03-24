package fr.cnrs.liris.jpugetgil.converg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class JsonNodeComparator implements Comparator<JsonNode> {

    @Override
    public int compare(JsonNode node1, JsonNode node2) {
        if (node1 == null && node2 == null) {
            return 0;
        }
        if (node1 == null) {
            return -1;
        }
        if (node2 == null) {
            return 1;
        }

        if (node1.getNodeType() != node2.getNodeType()) {
            return Integer.compare(node1.getNodeType().ordinal(), node2.getNodeType().ordinal());
        }

        switch (node1.getNodeType()) {
            case NULL:
                return 0;
            case BOOLEAN:
                return Boolean.compare(node1.asBoolean(), node2.asBoolean());
            case NUMBER:
                if (node1.isIntegralNumber() && node2.isIntegralNumber()) {
                    return Long.compare(node1.asLong(), node2.asLong());
                } else {
                    return Double.compare(node1.asDouble(), node2.asDouble());
                }
            case STRING:
                return formatToCanonicalString(node1).compareTo(formatToCanonicalString(node2));
            case ARRAY:
                return compareArrays((ArrayNode) node1, (ArrayNode) node2);
            case OBJECT:
                return compareObjects((ObjectNode) node1, (ObjectNode) node2);
            default:
                throw new IllegalArgumentException("Unsupported JsonNode type: " + node1.getNodeType());
        }
    }

    private String formatToCanonicalString(JsonNode node){
        String nodeString = node.asText();
        try {
            return String.valueOf((float) Integer.parseInt(nodeString));
        } catch(NumberFormatException ignore) {}

        return nodeString;
    }

    private int compareArrays(ArrayNode array1, ArrayNode array2) {
        int sizeComparison = Integer.compare(array1.size(), array2.size());
        if (sizeComparison != 0) {
            return sizeComparison;
        }

        for (int i = 0; i < array1.size(); i++) {
            int elementComparison = compare(array1.get(i), array2.get(i));
            if (elementComparison != 0) {
                return elementComparison;
            }
        }
        return 0;
    }

    private int compareObjects(ObjectNode object1, ObjectNode object2) {
        int keySetComparison = compareIterables(object1.fieldNames(), object2.fieldNames());
        if (keySetComparison != 0) {
            return keySetComparison;
        }

        Iterator<String> keys1 = object1.fieldNames();
        while (keys1.hasNext()) {
            String key = keys1.next();
            if (key.equals("datatype")) {
                continue;
            }
            int valueComparison = compare(object1.get(key), object2.get(key));
            if (valueComparison != 0) {
                return valueComparison;
            }
        }
        return 0;

    }

    private <T extends Comparable<T>> int compareIterables(Iterator<T> it1, Iterator<T> it2) {
        List<String> fields1 = new ArrayList<>();
        it1.forEachRemaining(field -> {
            if (!field.equals("datatype")) {
                fields1.add(field.toString());
            }
        });
        List<String> sortedFields1 = fields1.stream().sorted().toList();

        List<String> fields2 = new ArrayList<>();
        it2.forEachRemaining(field -> {
            if (!field.equals("datatype")) {
                fields2.add(field.toString());
            }
        });

        List<String> sortedFields2 = fields2.stream().sorted().toList();

        if (sortedFields1.size() != sortedFields2.size()) {
            return sortedFields1.size() - sortedFields2.size();
        }

        for (int index = 0; index < sortedFields1.size(); index++) {
            int elementComparison = sortedFields1.get(index).compareTo(sortedFields2.get(index));
            if (elementComparison != 0) {
                return elementComparison;
            }
        }

        return 0;
    }
}
