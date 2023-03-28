package ru.lisin.simplex.services;

import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@Service
public class TableGenerator {
    private final static String SIMPLEX_TABLE_PATH = "simplex_tables" + File.separator + "ResultSimplexTable.xlsx";

    public File createExcelFile() throws IOException {
        return Files.createFile(Paths.get(SIMPLEX_TABLE_PATH)).toFile();
    }

    public void createFirstTableVType(
            Map<String, Double> v1,
            Map<String, Double> v2,
            Map<String, Double> v3,
            Map<String, Double> v
    ) throws IOException {
        File simplexTableFile = createExcelFile();

        double resultB = getResultColumnCell(v, v1, v2, v3, "b");
        double resultX1 = getResultColumnCell(v, v1, v2, v3, "x1");
        double resultX2 = getResultColumnCell(v, v1, v2, v3, "x2");
        double resultX3 = getResultColumnCell(v, v1, v2, v3, "x3");
        double resultX4 = getResultColumnCell(v, v1, v2, v3, "x4");
        double resultX5= getResultColumnCell(v, v1, v2, v3, "x5");

        // TODO: create a sheet with the first table
    }

    private double getResultColumnCell(Map<String, Double> v, Map<String, Double> v1, Map<String, Double> v2, Map<String, Double> v3, String key) {
        return ((v.get("v1") * v1.get(key)) + (v.get("v2") * v2.get(key)) + (v.get("v3") * v3.get(key))) - v.get("coeff" + key);
    }

}
