package com.ambrosia.backup.google;

import static com.ambrosia.backup.google.GoogleService.sheets;

import com.ambrosia.backup.sql.TableName;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.opencsv.CSVParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SheetsService {

    private static final int ROWS_PER_UPDATE = 2000;


    public static void upload(Spreadsheet spreadsheet, TableName tableName, File file) throws IOException {
        Sheet sheetForTable = getSheetForTable(spreadsheet, tableName);
        if (sheetForTable == null)
            throw new IllegalStateException("Sheet for table " + tableName + " not found");

        CSVParser csvParser = new CSVParser();
        try (BufferedReader br = Files.newBufferedReader(file.toPath())) {
            String line = br.readLine();
            int rowIndex = 0;
            while (line != null) {
                List<List<Object>> rows = new ArrayList<>();
                for (int i = 0; i < ROWS_PER_UPDATE && line != null; i++) {
                    String[] abc = csvParser.parseLine(line);
                    List<Object> columns = Arrays.<Object>stream(abc).toList();
                    rows.add(columns);
                    line = br.readLine();
                }
                String table = tableName.table();
                String range = "%s!A%d".formatted(table, rowIndex + 1);
                ValueRange values = new ValueRange()
                    .setRange(range)
                    .setValues(rows)
                    .setMajorDimension("ROWS");
                sheets().values().append(spreadsheet.getSpreadsheetId(), range, values)
                    .setValueInputOption("USER_ENTERED")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute();
                rowIndex += ROWS_PER_UPDATE;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static Sheet getSheetForTable(Spreadsheet spreadsheet, TableName tableName) {
        for (Sheet sheet : spreadsheet.getSheets()) {
            if (sheet.getProperties().getTitle().equals(tableName.table())) {
                return sheet;
            }
        }
        return null;
    }

}
