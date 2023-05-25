package ru.lisin.simplex.services;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

@Service
@Slf4j
public class ExcelService {
    private final static String SIMPLEX_TABLE_PATH = "simplex_tables" + File.separator + "ResultSimplexTable.xlsx";

    @Getter
    private File resultExcelFile = new File(SIMPLEX_TABLE_PATH);

    @Getter
    private Workbook workbook = new XSSFWorkbook();

    @Getter
    private CellStyle cellStyle;

    public ExcelService() {
        this.cellStyle = workbook.createCellStyle();
        this.cellStyle.setDataFormat(workbook.createDataFormat().getFormat("0.0000"));
    }

    @PostConstruct
    private void createExcelFile() {
        resultExcelFile.getParentFile().mkdirs();
        try {
            resultExcelFile.createNewFile();
        } catch (IOException e) {
            log.error("Excel file creation failed", e);
        }
    }

    public void createRows(Sheet sheet, int rowNumber) {
        for (int i = 0; i < rowNumber; ++i) {
            sheet.createRow(i);
        }
    }

    public void saveExcelFile() {
        try {
            workbook.write(new FileOutputStream(resultExcelFile));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void mergeCells(Sheet sheet) {
        sheet.addMergedRegion(new CellRangeAddress(2, 3, 1, 1));
        sheet.addMergedRegion(new CellRangeAddress(4, 5, 1, 1));
        sheet.addMergedRegion(new CellRangeAddress(6, 7, 1, 1));
        sheet.addMergedRegion(new CellRangeAddress(8, 9, 1, 1));

        sheet.addMergedRegion(new CellRangeAddress(2, 3, 0, 0));
        sheet.addMergedRegion(new CellRangeAddress(4, 5, 0, 0));
        sheet.addMergedRegion(new CellRangeAddress(6, 7, 0, 0));
        sheet.addMergedRegion(new CellRangeAddress(8, 9, 0, 0));
    }

    public void setCoefficients0(Row row) {
        for (int i = 2; i < 8; ++i) {
            row.getCell(i).setCellValue(doubleToString(0.00));
        }
    }

    public void setVX1X5(Row row) {
        int xValue = 1;
        for (int i = 3; i < 8; ++i) {
            row.getCell(i).setCellValue("X" + xValue);
            ++xValue;
        }
    }

    public void setVX1X5Values(Row row, Map<String, Double> v) {
        row.getCell(2).setCellValue(doubleToString(v.get("b")));

        int xValue = 1;
        for (int i = 3; i < 8; ++i) {
            row.getCell(i).setCellValue(doubleToString(v.get("x" + xValue)));
            ++xValue;
        }
    }

    public void createCells(Sheet sheet) {
        Iterator<Row> iterator = sheet.iterator();
        while (iterator.hasNext()) {
            Row row = iterator.next();

            for (int i = 0; i < 8; ++i) {
                Cell cell = row.createCell(i);
                //cell.setCellStyle(cellStyle);
            }
        }
    }

    public void fillFirstVTable(
            Map<String, Double> v1,
            Map<String, Double> v2,
            Map<String, Double> v3,
            Map<String, Double> v,
            Map<String, Double> resultVRow
    ) {
        if (workbook.getSheet("Sheet1") == null) {
            Sheet sheet1 = workbook.createSheet("Sheet1");
            createRows(sheet1, 10);
            createCells(sheet1);
            mergeCells(sheet1);
        }
        Sheet sheet1 = workbook.getSheet("Sheet1");
        Row row0 = sheet1.getRow(0);
        row0.getCell(0).setCellValue("C");
        setCoefficients0(row0);

        Row row1 = sheet1.getRow(1);
        row1.getCell(2).setCellValue("b");
        setVX1X5(row1);

        Row row2 = sheet1.getRow(2);
        row2.getCell(0).setCellValue(doubleToString(1.00));
        row2.getCell(1).setCellValue("V1");
        setVX1X5Values(row2, v1);

        Row row4 = sheet1.getRow(4);
        row4.getCell(0).setCellValue(doubleToString(1.00));
        row4.getCell(1).setCellValue("V2");
        setVX1X5Values(row4, v2);

        Row row6 = sheet1.getRow(6);
        row6.getCell(0).setCellValue(doubleToString(1.00));
        row6.getCell(1).setCellValue("V3");
        setVX1X5Values(row6, v3);

        Row row8 = sheet1.getRow(8);
        row8.getCell(1).setCellValue("V");
        setVX1X5Values(row8, resultVRow);

        saveExcelFile();
    }

    public static String doubleToString(double number) {
        return String.valueOf(number);
    }

    public static double stringToDouble(String string) {
        if (string != null && !string.isEmpty()) {
            return Double.parseDouble(string);
        }
        return 0;
    }

}
