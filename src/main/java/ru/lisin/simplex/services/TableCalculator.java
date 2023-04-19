package ru.lisin.simplex.services;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class TableCalculator {
    @Autowired
    private ExcelService excelService;

    public void calculateFirstVTypeTable(
            Map<String, Double> v1,
            Map<String, Double> v2,
            Map<String, Double> v3,
            Map<String, Double> v
    ) {

        double resultB = getResultColumnCell(v, v1, v2, v3, "b");
        double resultX1 = getResultColumnCell(v, v1, v2, v3, "x1");
        double resultX2 = getResultColumnCell(v, v1, v2, v3, "x2");
        double resultX3 = getResultColumnCell(v, v1, v2, v3, "x3");
        double resultX4 = getResultColumnCell(v, v1, v2, v3, "x4");
        double resultX5 = getResultColumnCell(v, v1, v2, v3, "x5");

        Map<String, Double> resultVRow = new HashMap<>() {{
            put("b", resultB);
            put("x1", resultX1);
            put("x2", resultX2);
            put("x3", resultX3);
            put("x4", resultX4);
            put("x5", resultX5);
        }};

        excelService.fillFirstVTable(v1, v2, v3, v, resultVRow);
    }

    public void calculateNextTable() {
        Workbook workbook = excelService.getWorkbook();
        Sheet sheet = cloneSheet(workbook);

        int resolvingColumnIndex = findResolvingColumnIndex(sheet);
        int resolvingRowIndex = findResolvingRowIndex(sheet, resolvingColumnIndex);

        double lambda = getLambda(sheet, resolvingRowIndex, resolvingColumnIndex);
        sheet.getRow(resolvingRowIndex + 1).getCell(resolvingColumnIndex).setCellValue(lambda);

        fillResolvingColumn(sheet, resolvingColumnIndex, lambda);

        excelService.saveExcelFile();

    }

    public Sheet cloneSheet(Workbook workbook) {
        int numberOfSheets = workbook.getNumberOfSheets();
        int numberOfClonedSheet = numberOfSheets - 1;
        workbook.cloneSheet(numberOfClonedSheet);

        return numberOfSheets == 1 ? workbook.getSheetAt(numberOfSheets) : workbook.getSheetAt(numberOfClonedSheet);
    }

    public void fillResolvingColumn(Sheet sheet, int resolvingColumnIndex, double lambda) {
        for (int i = 3; i < 10; i += 2) {
            Cell cell = sheet.getRow(i).getCell(resolvingColumnIndex);
            double numericCellValue = cell.getNumericCellValue();
            if (numericCellValue == 0) {
                double result = (-lambda) * sheet.getRow(i - 1).getCell(resolvingColumnIndex).getNumericCellValue();
                cell.setCellValue(result);
            }
        }
    }

    public double getLambda(Sheet sheet, int resolvingRowIndex, int resolvingColumnIndex) {
        Row resolvingRow = sheet.getRow(resolvingRowIndex);
        Cell resolvingCell = resolvingRow.getCell(resolvingColumnIndex);
        return 1 / resolvingCell.getNumericCellValue();
    }

    public int findResolvingColumnIndex(Sheet sheet) {
        Row row8 = sheet.getRow(8);
        double cell8Value = 0;
        int resolvingColumnIndex = 0;

        for (int i = 3; i < 8; ++i) {
            Cell row8Cell = row8.getCell(i);
            double numericCellValue = row8Cell.getNumericCellValue();

            if (numericCellValue > 0 && numericCellValue > cell8Value) {
                resolvingColumnIndex = i;
            }
        }

        return resolvingColumnIndex;
    }

    public int findResolvingRowIndex(Sheet sheet, int resolvingColumnIndex) {
        Row row2 = sheet.getRow(2);
        Row row4 = sheet.getRow(4);
        Row row6 = sheet.getRow(6);

        Cell row2Cell = row2.getCell(resolvingColumnIndex);
        Cell row4Cell = row4.getCell(resolvingColumnIndex);
        Cell row6Cell = row6.getCell(resolvingColumnIndex);

        Cell row2B = row2.getCell(2);
        Cell row4B = row4.getCell(2);
        Cell row6B = row6.getCell(2);

        double row2Result = row2B.getNumericCellValue() / row2Cell.getNumericCellValue();
        double row4Result = row4B.getNumericCellValue() / row4Cell.getNumericCellValue();
        double row6Result = row6B.getNumericCellValue() / row6Cell.getNumericCellValue();

        double mainResult = Math.min(Math.min(row2Result, row4Result), Math.min(row4Result, row6Result));

        if (mainResult == row2Result) {
            return 2;
        } else if (mainResult == row4Result) {
            return 4;
        } else {
            return 6;
        }
    }


    private double getResultColumnCell(Map<String, Double> v, Map<String, Double> v1, Map<String, Double> v2, Map<String, Double> v3, String key) {
        return ((v.get("v1") * v1.get(key)) + (v.get("v2") * v2.get(key)) + (v.get("v3") * v3.get(key))) - v.get("coeff" + key);
    }
}
