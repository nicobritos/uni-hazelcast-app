package ar.edu.itba.client.utils;

import ar.edu.itba.api.queryResults.Query1Result;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.util.Collection;

public class Query1Writer extends QueryWriter {
    private static final String[] HEADERS = {"BARRIO", "ARBOLES_POR_HABITANTE"};

    private final CSVPrinter printer;

    public Query1Writer(String outPath) throws IOException {
        this.printer = this.getPrinter(outPath, 1);
    }

    public void write(Collection<Query1Result> results) throws IOException {
        for (Query1Result result : results) {
            this.printer.printRecord(result.getNeighbourhood(), result.getTreesPerPerson());
        }
    }

    protected final String[] getHeaders() {
        return HEADERS;
    }
}
