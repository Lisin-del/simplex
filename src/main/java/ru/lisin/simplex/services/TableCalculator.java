package ru.lisin.simplex.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.*;

@Service
@Slf4j
public class TableCalculator {
    private DecimalFormat decimalFormat = new DecimalFormat("0.00");
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

    public void generateCellsAgain() {
        Workbook workbook = excelService.getWorkbook();
        Sheet sheet = workbook.getSheetAt(workbook.getNumberOfSheets() - 1);

        for (int rowNumber : emptyRowNumbers) {
            Row row = sheet.getRow(rowNumber);
            for (int i = 2; i < 8; ++i) {
                Cell cell = row.createCell(i);
                cell.setCellStyle(excelService.getCellStyle());
            }
        }
    }

    public void calculateNextTable() {
        Workbook workbook = excelService.getWorkbook();
        Sheet sheet = cloneSheet(workbook);

        List<Integer> resolvingColumnIndexes = findResolvingColumnIndex(sheet);

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
        sheet.getRow(resolvingRowIndex + 1).getCell(resolvingColumnIndex).setCellValue(lambda);

        fillResolvingColumn(sheet, resolvingColumnIndex, resolvingRowIndex, lambda);
        fillResolvingRow(sheet, resolvingRowIndex, lambda);

        calculateOtherCells(resolvingColumnIndex, resolvingRowIndex);

        Sheet nextResultVTableSheet = getNextResultVTable(resolvingColumnIndex, resolvingRowIndex);
        validateTable(nextResultVTableSheet);
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

    public void calculateOtherCells(int resolvingColumnIndex, int resolvingRowIndex) {
        Workbook workbook = excelService.getWorkbook();
        Sheet sheet = cloneSheet(workbook);
        Row resolvingRow = sheet.getRow(resolvingRowIndex);

        List<Integer> sortedRowNumbers = emptyRowNumbers.stream().filter(number -> number != resolvingRowIndex + 1).toList();

        for (int rowNumber : sortedRowNumbers) {
            Row row = sheet.getRow(rowNumber);

            for (int i = 2; i < 8; ++i) {
                Cell cell = row.getCell(i);

                if (i == resolvingColumnIndex) {
                    continue;
                }

                double result = resolvingRow.getCell(cell.getColumnIndex()).getNumericCellValue() *
                        row.getCell(resolvingColumnIndex).getNumericCellValue();
                cell.setCellValue(result);
            }
        }
    }

    public boolean isFinishCalculatingVTables() {
        Workbook workbook = excelService.getWorkbook();
        Sheet sheet = workbook.getSheetAt(workbook.getNumberOfSheets() - 1);

        Row rowForCheck = sheet.getRow(8);
        for (int i = 3; i < 8; ++i) {
            double cellValueForCheck = rowForCheck.getCell(i).getNumericCellValue();
            String formattedCellForCheck = decimalFormat.format(cellValueForCheck);
            double aDouble = Double.parseDouble(formattedCellForCheck);

            if (aDouble > 0) {
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

    public void validateTable(Sheet sheet) {
        List<Integer> rowIndexes = new ArrayList<>() {{
            add(2);
            add(4);
            add(6);
        }};

        double result = 0;
        for (int i = 2; i < 8; ++i) {
            for (int rowIndex : rowIndexes) {
                Row row = sheet.getRow(rowIndex);
                result += row.getCell(i).getNumericCellValue() * row.getCell(0).getNumericCellValue();
            }
            double vValue = sheet.getRow(8).getCell(i).getNumericCellValue();
            result = result - sheet.getRow(0).getCell(i).getNumericCellValue();

            if (vValue != result) {
                log.error("Result value in row V is {}, but expected {}", vValue, result);
            }
            result = 0;
        }

    }

    public Sheet getNextResultVTable(int resolvingColumnIndex, int resolvingRowIndex) {
        Workbook workbook = excelService.getWorkbook();
        Sheet sheet = cloneSheet(workbook);

        Row resolvingRow = sheet.getRow(resolvingRowIndex);
        String basisNameValue = resolvingRow.getCell(1).getStringCellValue();

        Cell resolvingCell = sheet.getRow(1).getCell(resolvingColumnIndex);
        String freeMemberName = resolvingCell.getStringCellValue();

        resolvingRow.getCell(1).setCellValue(freeMemberName);
        resolvingCell.setCellValue(basisNameValue);

        double basisCoefficient = resolvingRow.getCell(0).getNumericCellValue();
        double freeMemberCoefficient = sheet.getRow(0).getCell(resolvingColumnIndex).getNumericCellValue();

        resolvingRow.getCell(0).setCellValue(freeMemberCoefficient);
        sheet.getRow(0).getCell(resolvingColumnIndex).setCellValue(basisCoefficient);

        Row rowToMoveToResolvingRow = sheet.getRow(resolvingRowIndex + 1);

        for (int i = 2; i < 8; ++i) {
            Cell rowToMoveToResolvingRowCell = rowToMoveToResolvingRow.getCell(i);
            resolvingRow.getCell(i).setCellValue(rowToMoveToResolvingRowCell.getNumericCellValue());
            rowToMoveToResolvingRow.removeCell(rowToMoveToResolvingRowCell);
        }

        List<Integer> sortedRowNumbers = emptyRowNumbers.stream().filter(number -> number != resolvingRowIndex + 1).toList();
        for (int rowNumber : sortedRowNumbers) {
            Cell cell = sheet.getRow(rowNumber).getCell(resolvingColumnIndex);
            sheet.getRow(rowNumber - 1).getCell(resolvingColumnIndex).setCellValue(cell.getNumericCellValue());
            sheet.getRow(rowNumber).removeCell(cell);
        }

        for (int rowNumber : sortedRowNumbers) {
            Row notResolvingRow = sheet.getRow(rowNumber);

            for (int i = 2; i < 8; ++i) {
                if (i == resolvingColumnIndex) {
                    continue;
                }
                double resultCellValue = notResolvingRow.getCell(i).getNumericCellValue() + sheet.getRow(rowNumber - 1).getCell(i).getNumericCellValue();
                sheet.getRow(rowNumber - 1).getCell(i).setCellValue(resultCellValue);
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

    public void fillResolvingRow(Sheet sheet, int resolvingRowIndex, double lambda) {
        Row filledRow = sheet.getRow(resolvingRowIndex);
        Row emptyRow = sheet.getRow(resolvingRowIndex + 1);
        for (int i = 2; i < 8; ++i) {
            Cell cell = filledRow.getCell(i);
            double numericCellValue = cell.getNumericCellValue();
            double resultValue = numericCellValue * lambda;
            Cell emptyRowCell = emptyRow.getCell(i);

            if (emptyRowCell.getNumericCellValue() == 0) {
                emptyRowCell.setCellValue(resultValue);
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

    public List<Integer> findResolvingColumnIndex(Sheet sheet) {
        List<Integer> resolvingColumnIndexes = new ArrayList<>();
        Row row8 = sheet.getRow(8);
        double cell8Value = 0;
        int resolvingColumnIndex = 0;

        for (int i = 3; i < 8; ++i) {
            Cell row8Cell = row8.getCell(i);
            double numericCellValue = row8Cell.getNumericCellValue();

//            if (numericCellValue == 13.40) {
//                resolvingColumnIndex = i;
//                return resolvingColumnIndex;
//            }

            if (numericCellValue > 0 && numericCellValue > cell8Value) {
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

        if (row2Cell.getNumericCellValue() > 0) {
            double row2Result = row2B.getNumericCellValue() / row2Cell.getNumericCellValue();
            numbers.add(row2Result);
            numbersToReturn.put(row2Result, 2);
        }

        if (row4Cell.getNumericCellValue() > 0) {
            double row4Result = row4B.getNumericCellValue() / row4Cell.getNumericCellValue();
            numbers.add(row4Result);
            numbersToReturn.put(row4Result, 4);
        }

        if (row6Cell.getNumericCellValue() > 0) {
            double row6Result = row6B.getNumericCellValue() / row6Cell.getNumericCellValue();
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
