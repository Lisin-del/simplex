package ru.lisin.simplex.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.*;

import static ru.lisin.simplex.services.ExcelService.*;

@Service
@Slf4j
public class TableCalculator {
//    private DecimalFormat decimalFormat = new DecimalFormat("0.0000");
    private final static int CHECK_VALUE = 1001;
    @Autowired
    private ExcelService excelService;
    private List<Integer> emptyRowNumbers = new ArrayList<>() {{
        add(3);
        add(5);
        add(7);
        add(9);
    }};

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

    public void generateCellsAgain(int columnNumber) {
        Workbook workbook = excelService.getWorkbook();
        Sheet sheet = workbook.getSheetAt(workbook.getNumberOfSheets() - 1);

        for (int rowNumber : emptyRowNumbers) {
            Row row = sheet.getRow(rowNumber);
            for (int i = 2; i < columnNumber; ++i) {
                Cell cell = row.createCell(i);
                //cell.setCellStyle(excelService.getCellStyle());
            }
        }
    }

    public void calculateNextTable(int columnNumber) {
        Workbook workbook = excelService.getWorkbook();
        Sheet sheet = cloneSheet(workbook);

        List<Integer> resolvingColumnIndexes = findResolvingColumnIndex(sheet, columnNumber);

        int resolvingRowIndex = CHECK_VALUE;
        int resolvingColumnIndex = CHECK_VALUE;

        for (int colIndex : resolvingColumnIndexes) {
            resolvingRowIndex = findResolvingRowIndex(sheet, colIndex);

            if (resolvingRowIndex != CHECK_VALUE) {
                resolvingColumnIndex = colIndex;
                break;
            }
        }

        if (resolvingRowIndex == CHECK_VALUE) {
            log.error("The current task doesn't have a solution");
            System.exit(0);
        }

        double lambda = getLambda(sheet, resolvingRowIndex, resolvingColumnIndex);

        //set lambda under a resolving cell
        sheet.getRow(resolvingRowIndex + 1).getCell(resolvingColumnIndex).setCellValue(doubleToString(lambda));

        fillResolvingColumn(sheet, resolvingColumnIndex, resolvingRowIndex, lambda);
        fillResolvingRow(sheet, resolvingRowIndex, lambda, columnNumber);

        calculateOtherCells(resolvingColumnIndex, resolvingRowIndex, columnNumber);

        Sheet nextResultVTableSheet = getNextResultVTable(resolvingColumnIndex, resolvingRowIndex, columnNumber);
        validateTable(nextResultVTableSheet, columnNumber);
        excelService.saveExcelFile();
    }

    public void deleteVRowsColumns() {
        Workbook workbook = excelService.getWorkbook();
        Sheet sheet = cloneSheet(workbook);
        List<String> vNames = new ArrayList<>() {{
            add("V1");
            add("V2");
            add("V3");
        }};
        List<Integer> columnIndexesToDelete = new ArrayList<>();
        Row row1 = sheet.getRow(1);
        for (int i = 0; i < 8; ++i) {
            Cell cell = row1.getCell(i);
            vNames.forEach(vName -> {
                if (vName.equalsIgnoreCase(cell.getStringCellValue())) {
                    columnIndexesToDelete.add(cell.getColumnIndex());
                }
            });
        }
        log.info("V Column indexes to delete: {}", columnIndexesToDelete);

        for (int i = 0; i < 8; ++i) {
            Row row = sheet.getRow(i);
            for (int columnIndex : columnIndexesToDelete) {
                Cell cellToDelete = row.getCell(columnIndex);
                if (cellToDelete != null) {
                    row.removeCell(cellToDelete);
                }
            }
        }

        Row vRowToDelete = sheet.getRow(8);
        for (int i = 0; i < 8; ++i) {
            Cell cellToDelete = vRowToDelete.getCell(i);
            if (cellToDelete != null) {
                vRowToDelete.removeCell(cellToDelete);
            }
        }
        excelService.saveExcelFile();
    }

    public void moveColumns() {
        Workbook workbook = excelService.getWorkbook();
        Sheet sheet = cloneSheet(workbook);

        List<Integer> emptyColumnIndexes = new ArrayList<>();
        List<Integer> filledColumnIndexes = new ArrayList<>();
        List<Integer> columnsMustBeFilled = new ArrayList<>() {{
            add(3);
            add(4);
        }};

        Row row1 = sheet.getRow(1);
        for (int i = 3; i < 8; ++i) {
            Cell cell = row1.getCell(i);
            if (cell == null) {
                emptyColumnIndexes.add(i);
            } else {
                filledColumnIndexes.add(i);
            }
        }

        for (int index : columnsMustBeFilled) {
            if (emptyColumnIndexes.contains(index)) {
                int filledColumnIndex = filledColumnIndexes.get(0);
                filledColumnIndexes.remove(0);

                for (int i = 0; i < 8; ++i) {
                    Row row = sheet.getRow(i);
                    Cell cellToStore = row.getCell(filledColumnIndex);
                    if (cellToStore != null) {
                        CellType cellType = cellToStore.getCellType();
                        Cell cellMustBeFilled = row.getCell(index);
                        if (cellMustBeFilled == null) {
                            row.createCell(index);
                            cellMustBeFilled = row.getCell(index);
                            //cellMustBeFilled.setCellStyle(excelService.getCellStyle());
                        }
                        switch (cellType) {
                            case NUMERIC -> cellMustBeFilled.setCellValue(cellToStore.getStringCellValue());
                            case STRING -> cellMustBeFilled.setCellValue(cellToStore.getStringCellValue());
                        }
                        row.removeCell(cellToStore);
                    }
                }
            }
        }
        excelService.saveExcelFile();
    }

    public void setTargetFunctionCoefficients(Map<String, Double> targetFunctionMap) {
        Workbook workbook = excelService.getWorkbook();
        Sheet sheet = cloneSheet(workbook);

        Row row0 = sheet.getRow(0);
        Row row1 = sheet.getRow(1);

        for (int i = 3; i < 5; ++i) {
            Cell row0Cell = row0.getCell(i);
            Cell row1Cell = row1.getCell(i);

            if (row0Cell != null && row1Cell != null) {
                Double targetFunctionXValue = targetFunctionMap.get(row1Cell.getStringCellValue());
                row0Cell.setCellValue(doubleToString(targetFunctionXValue));
            }
        }

        List<Integer> rowIndexes = new ArrayList<>() {{
            add(2);
            add(4);
            add(6);
        }};

        for (int rowIndex : rowIndexes) {
            Row row = sheet.getRow(rowIndex);
            Cell cell0 = row.getCell(0);
            Cell cell1 = row.getCell(1);

            if (cell0 != null && cell1 != null) {
                Double targetFunctionXValue = targetFunctionMap.get(cell1.getStringCellValue());
                cell0.setCellValue(doubleToString(targetFunctionXValue));
            }
        }

        excelService.saveExcelFile();
    }

    public void createMainTaskFirstTable() {
        Workbook workbook = excelService.getWorkbook();
        Sheet sheet = cloneSheet(workbook);

        List<Integer> rowIndexes = new ArrayList<>() {{
            add(2);
            add(4);
            add(6);
        }};

        Map<Integer, Double> results = new HashMap<>();


        for (int i = 2; i < 5; ++i) {
            double result = 0;
            for (int rowIndex : rowIndexes) {
                Row row = sheet.getRow(rowIndex);
                result += stringToDouble(row.getCell(i).getStringCellValue()) * stringToDouble(row.getCell(0).getStringCellValue());
            }
            result = result - stringToDouble(sheet.getRow(0).getCell(i).getStringCellValue());
            results.put(i, result);
        }

        Row row8 = sheet.getRow(8);
        for (int i = 0; i < 5; ++i) {
            row8.createCell(i);
        }

        row8.getCell(1).setCellValue("L");
        for (Map.Entry<Integer, Double> res : results.entrySet()) {
            Cell cell = row8.getCell(res.getKey());
            //cell.setCellStyle(excelService.getCellStyle());
            cell.setCellValue(doubleToString(res.getValue()));
        }
        excelService.saveExcelFile();
    }

    public void calculateOtherCells(int resolvingColumnIndex, int resolvingRowIndex, int columnNumber) {
        Workbook workbook = excelService.getWorkbook();
        Sheet sheet = cloneSheet(workbook);
        Row resolvingRow = sheet.getRow(resolvingRowIndex);

        List<Integer> sortedRowNumbers = emptyRowNumbers.stream().filter(number -> number != resolvingRowIndex + 1).toList();

        for (int rowNumber : sortedRowNumbers) {
            Row row = sheet.getRow(rowNumber);

            for (int i = 2; i < columnNumber; ++i) {
                Cell cell = row.getCell(i);

                if (i == resolvingColumnIndex) {
                    continue;
                }

                double result = stringToDouble(resolvingRow.getCell(cell.getColumnIndex()).getStringCellValue()) *
                        stringToDouble(row.getCell(resolvingColumnIndex).getStringCellValue());
                cell.setCellValue(doubleToString(result));
            }
        }
    }

    public boolean isFinishCalculatingTables(int columnNumber) {
        Workbook workbook = excelService.getWorkbook();
        Sheet sheet = workbook.getSheetAt(workbook.getNumberOfSheets() - 1);

        Row rowForCheck = sheet.getRow(8);
        for (int i = 3; i < columnNumber; ++i) {
            double cellValueForCheck = stringToDouble(rowForCheck.getCell(i).getStringCellValue());
            //String formattedCellForCheck = decimalFormat.format(cellValueForCheck);
            //double aDouble = Double.parseDouble(formattedCellForCheck);

            if (cellValueForCheck > 0) {
                return false;
            }
        }
        return true;
    }

    public boolean areVsMovedToFreeVars() {
        Workbook workbook = excelService.getWorkbook();
        Sheet sheet = workbook.getSheetAt(workbook.getNumberOfSheets() - 1);

        Row row = sheet.getRow(1);
        Iterator<Cell> cellIterator = row.iterator();
        List<String> cellLetters = new ArrayList<>();

        while (cellIterator.hasNext()) {
            cellLetters.add(cellIterator.next().getStringCellValue());
        }

        List<String> sortedLetters = cellLetters.stream()
                .filter(letter -> letter.equals("V1") || letter.equals("V2") || letter.equals("V3"))
                .toList();

        return sortedLetters.size() == 3;
    }

    public void validateTable(Sheet sheet, int columnNumber) {
        List<Integer> rowIndexes = new ArrayList<>() {{
            add(2);
            add(4);
            add(6);
        }};

        double result = 0.0;
        for (int i = 2; i < columnNumber; ++i) {
            for (int rowIndex : rowIndexes) {
                Row row = sheet.getRow(rowIndex);
                result += stringToDouble(row.getCell(i).getStringCellValue()) * stringToDouble(row.getCell(0).getStringCellValue());
            }
            double vValue = stringToDouble(sheet.getRow(8).getCell(i).getStringCellValue());
            result = result - stringToDouble(sheet.getRow(0).getCell(i).getStringCellValue());

            if (vValue != result) {
                log.error("Result value in row V is {}, but expected {}", vValue, result);
            }
            result = 0;
        }

    }

    public Sheet getNextResultVTable(int resolvingColumnIndex, int resolvingRowIndex, int columnNumber) {
        Workbook workbook = excelService.getWorkbook();
        Sheet sheet = cloneSheet(workbook);

        Row resolvingRow = sheet.getRow(resolvingRowIndex);
        String basisNameValue = resolvingRow.getCell(1).getStringCellValue();

        Cell resolvingCell = sheet.getRow(1).getCell(resolvingColumnIndex);
        String freeMemberName = resolvingCell.getStringCellValue();

        resolvingRow.getCell(1).setCellValue(freeMemberName);
        resolvingCell.setCellValue(basisNameValue);

        double basisCoefficient = stringToDouble(resolvingRow.getCell(0).getStringCellValue());
        double freeMemberCoefficient = stringToDouble(sheet.getRow(0).getCell(resolvingColumnIndex).getStringCellValue());

        resolvingRow.getCell(0).setCellValue(doubleToString(freeMemberCoefficient));
        sheet.getRow(0).getCell(resolvingColumnIndex).setCellValue(doubleToString(basisCoefficient));

        Row rowToMoveToResolvingRow = sheet.getRow(resolvingRowIndex + 1);

        for (int i = 2; i < columnNumber; ++i) {
            Cell rowToMoveToResolvingRowCell = rowToMoveToResolvingRow.getCell(i);
            resolvingRow.getCell(i).setCellValue(rowToMoveToResolvingRowCell.getStringCellValue());
            rowToMoveToResolvingRow.removeCell(rowToMoveToResolvingRowCell);
        }

        List<Integer> sortedRowNumbers = emptyRowNumbers.stream().filter(number -> number != resolvingRowIndex + 1).toList();
        for (int rowNumber : sortedRowNumbers) {
            Cell cell = sheet.getRow(rowNumber).getCell(resolvingColumnIndex);
            sheet.getRow(rowNumber - 1).getCell(resolvingColumnIndex).setCellValue(cell.getStringCellValue());
            sheet.getRow(rowNumber).removeCell(cell);
        }

        for (int rowNumber : sortedRowNumbers) {
            Row notResolvingRow = sheet.getRow(rowNumber);

            for (int i = 2; i < columnNumber; ++i) {
                if (i == resolvingColumnIndex) {
                    continue;
                }
                double resultCellValue = stringToDouble(notResolvingRow.getCell(i).getStringCellValue()) + stringToDouble(sheet.getRow(rowNumber - 1).getCell(i).getStringCellValue());
                sheet.getRow(rowNumber - 1).getCell(i).setCellValue(doubleToString(resultCellValue));
                notResolvingRow.removeCell(notResolvingRow.getCell(i));
            }
        }

        return sheet;
    }

    public Sheet cloneSheet(Workbook workbook) {
        int numberOfSheets = workbook.getNumberOfSheets();
        int indexOfClonedSheet = numberOfSheets - 1;
        workbook.cloneSheet(indexOfClonedSheet);

        return workbook.getSheetAt(indexOfClonedSheet + 1);
    }

    public void fillResolvingRow(Sheet sheet, int resolvingRowIndex, double lambda, int columnNumber) {
        Row filledRow = sheet.getRow(resolvingRowIndex);
        Row emptyRow = sheet.getRow(resolvingRowIndex + 1);
        for (int i = 2; i < columnNumber; ++i) {
            Cell cell = filledRow.getCell(i);
            double numericCellValue = stringToDouble(cell.getStringCellValue());
            double resultValue = numericCellValue * lambda;
            Cell emptyRowCell = emptyRow.getCell(i);

            if (stringToDouble(emptyRowCell.getStringCellValue()) == 0) {
                emptyRowCell.setCellValue(doubleToString(resultValue));
            }
        }
    }

    public void fillResolvingColumn(Sheet sheet, int resolvingColumnIndex, int resolvingRowIndex, double lambda) {
        for (int i = 3; i < 10; i += 2) {

            if (resolvingRowIndex == (i - 1)) {
                continue;
            }

            Row row = sheet.getRow(i);
            Cell cell = row.getCell(resolvingColumnIndex);

            double numericCellValue = stringToDouble(cell.getStringCellValue());
            if (numericCellValue == 0) {
                double result = (-lambda) * stringToDouble(sheet.getRow(i - 1).getCell(resolvingColumnIndex).getStringCellValue());
                cell.setCellValue(doubleToString(result));
            }
        }
    }

    public double getLambda(Sheet sheet, int resolvingRowIndex, int resolvingColumnIndex) {
        Row resolvingRow = sheet.getRow(resolvingRowIndex);
        Cell resolvingCell = resolvingRow.getCell(resolvingColumnIndex);

        double numericCellValue = stringToDouble(resolvingCell.getStringCellValue());
        //String formattedCellForCheck = decimalFormat.format(numericCellValue);
        //double aDouble = Double.parseDouble(formattedCellForCheck);

        return 1.00 / numericCellValue;
    }

    public List<Integer> findResolvingColumnIndex(Sheet sheet, int columnNumber) {
        List<Integer> resolvingColumnIndexes = new ArrayList<>();
        Row row8 = sheet.getRow(8);
        double cell8Value = 0;
        int resolvingColumnIndex = 0;

        for (int i = 3; i < columnNumber; ++i) {
            Cell row8Cell = row8.getCell(i);
            double numericCellValue = stringToDouble(row8Cell.getStringCellValue());

//            if (numericCellValue == 13.40) {
//                resolvingColumnIndex = i;
//                return resolvingColumnIndex;
//            }

            if (numericCellValue > 0) {
                cell8Value = numericCellValue;
                resolvingColumnIndex = i;
                resolvingColumnIndexes.add(i);
                log.info("Resolving column index [{}] was added", i);
            }
        }

        if (resolvingColumnIndex == 0) {
            log.error("Can not find a resolving column!");
        }

        return resolvingColumnIndexes;
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

        List<Double> numbers = new ArrayList<>();
        Map<Double, Integer> numbersToReturn = new HashMap<>();

        if (stringToDouble(row2Cell.getStringCellValue()) > 0) {
            double row2Result = stringToDouble(row2B.getStringCellValue()) / stringToDouble(row2Cell.getStringCellValue());
            numbers.add(row2Result);
            numbersToReturn.put(row2Result, 2);
        }

        if (stringToDouble(row4Cell.getStringCellValue()) > 0) {
            double row4Result = stringToDouble(row4B.getStringCellValue()) / stringToDouble(row4Cell.getStringCellValue());
            numbers.add(row4Result);
            numbersToReturn.put(row4Result, 4);
        }

        if (stringToDouble(row6Cell.getStringCellValue()) > 0) {
            double row6Result = stringToDouble(row6B.getStringCellValue()) / stringToDouble(row6Cell.getStringCellValue());
            numbers.add(row6Result);
            numbersToReturn.put(row6Result, 6);
        }

        if (numbers.isEmpty()) {
            return CHECK_VALUE;
        }

        return numbersToReturn.get(Collections.min(numbers));
    }

    private double getResultColumnCell(Map<String, Double> v, Map<String, Double> v1, Map<String, Double> v2, Map<String, Double> v3, String key) {
        return ((v.get("v1") * v1.get(key)) + (v.get("v2") * v2.get(key)) + (v.get("v3") * v3.get(key))) - v.get("coeff" + key);
    }
}
