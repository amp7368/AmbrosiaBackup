package com.ambrosia.backup.google;

import com.ambrosia.backup.Ambrosia;
import com.ambrosia.backup.config.AmbrosiaDatabaseConfig;
import com.ambrosia.backup.sql.TableName;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.Sheets.Spreadsheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.List;

public class GoogleService {

    private static final String APPLICATION_NAME = "Ambrosia Backups";

    private static final List<String> SCOPES = List.of(SheetsScopes.SPREADSHEETS, DriveScopes.DRIVE_FILE);
    private static final Path CREDENTIALS_FILE_PATH = Ambrosia.getFile("./credentials.json").toPath();
    private static Sheets SHEETS;
    private static Drive DRIVE;

    public static Sheets sheetsService() {
        return SHEETS;
    }

    public static Drive driveService() {
        return DRIVE;
    }

    public static Drive.Files drive() {
        return driveService().files();
    }

    public static Spreadsheets sheets() {
        return sheetsService().spreadsheets();
    }

    private static HttpCredentialsAdapter getCredentials() throws IOException {
        if (!Files.exists(CREDENTIALS_FILE_PATH)) {
            String msg = "Please include file: %s for Google Credentials".formatted(CREDENTIALS_FILE_PATH.toAbsolutePath());
            throw new FileNotFoundException(msg);
        }
        InputStream in = Files.newInputStream(CREDENTIALS_FILE_PATH);
        GoogleCredentials clientSecrets = GoogleCredentials.fromStream(in)
            .createScoped(SCOPES);
        return new HttpCredentialsAdapter(clientSecrets);
    }

    public static void load() throws IOException, GeneralSecurityException {
        NetHttpTransport SHEETS_HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        GsonFactory gson = GsonFactory.getDefaultInstance();
        SHEETS = new Sheets.Builder(SHEETS_HTTP_TRANSPORT, gson, getCredentials())
            .setApplicationName(APPLICATION_NAME)
            .build();
        NetHttpTransport DRIVE_HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        DRIVE = new Drive.Builder(DRIVE_HTTP_TRANSPORT, gson, getCredentials())
            .setApplicationName(APPLICATION_NAME)
            .build();
    }

    public static Spreadsheet createTableSpreadsheet(AmbrosiaDatabaseConfig config, List<TableName> tableNames) throws IOException {
        SpreadsheetProperties properties = new SpreadsheetProperties().setTitle(config.getName());
        Spreadsheet spreadsheet = new Spreadsheet().setProperties(properties);

        List<Sheet> sheets = tableNames.stream()
            .map(TableName::table)
            .map(name -> new Sheet().setProperties(new SheetProperties().setTitle(name)))
            .toList();
        spreadsheet.setSheets(sheets);

        spreadsheet = sheets().create(spreadsheet).execute();
        drive().update(spreadsheet.getSpreadsheetId(), null)
            .setAddParents(config.getFolderId())
            .setFields("id, parents")
            .execute();
        return spreadsheet;
    }
}
