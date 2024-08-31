package com.ambrosia.backup.sql;

import com.ambrosia.backup.Ambrosia;
import com.ambrosia.backup.config.AmbrosiaDatabaseConfig;
import com.ambrosia.backup.google.GoogleService;
import com.ambrosia.backup.google.SheetsService;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.opencsv.CSVWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ExportDatabase {

    public static final int FETCH_SIZE = 1000;

    public static void export(AmbrosiaDatabaseConfig config) throws SQLException, IOException {
        Connection conn = config.getConnection();
        conn.setAutoCommit(false);
        List<TableName> tableNames = getTableNames(config, conn);
        Spreadsheet spreadsheet = GoogleService.createTableSpreadsheet(config, tableNames);
        for (TableName tableName : tableNames) {
            Ambrosia.logger().info("Exporting {}.{}", config.getType(), tableName);
            File file = exportTable(conn, tableName);
            SheetsService.upload(spreadsheet, tableName, file);
            file.delete();
        }
        Ambrosia.logger().info("Finished exporting {}", config.getType());
    }

    private static File exportTable(Connection conn, TableName tableName) throws SQLException, IOException {
        File tempFile = File.createTempFile("ambrosia-backup", ".csv.tmp");
        try (CSVWriter writer = new CSVWriter(Files.newBufferedWriter(tempFile.toPath()))) {
            try (Statement stmt = conn.createStatement()) {
                stmt.setFetchSize(FETCH_SIZE);

                ResultSet result = stmt.executeQuery("SELECT * FROM " + tableName.sql());
                int columnCount = result.getMetaData().getColumnCount();
                String[] header = new String[columnCount];
                for (int i = 0; i < columnCount; i++) {
                    String columnName = result.getMetaData().getColumnName(i + 1);
                    header[i] = columnName;
                }
                writer.writeNext(header);
                while (result.next()) {
                    String[] row = new String[columnCount];
                    for (int i = 0; i < columnCount; i++) {
                        Object object = result.getObject(i + 1);
                        if (object instanceof String) {
                            row[i] = "\"" + object + "\"";
                        } else {
                            row[i] = Objects.toString(object);
                        }
                        row[i] = row[i].replace("\n", " \\n ");
                    }
                    writer.writeNext(row);
                }
                writer.flush();
            }
        }
        return tempFile;
    }

    private static List<TableName> getTableNames(AmbrosiaDatabaseConfig config, Connection conn) throws SQLException {
        List<TableName> tableNames = new ArrayList<>();
        try (Statement statement = conn.createStatement()) {
            ResultSet result = statement
                .executeQuery("""
                    SELECT table_schema, table_name
                    FROM information_schema.tables
                    WHERE table_schema = '%s'""".formatted(config.getSchema()));
            while (result.next()) {
                String schema = result.getString("table_schema");
                String name = result.getString("table_name");
                tableNames.add(new TableName(schema, name));
            }
        }
        return tableNames;
    }
}
