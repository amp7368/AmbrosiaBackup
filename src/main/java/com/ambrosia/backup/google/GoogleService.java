package com.ambrosia.backup.google;

import com.ambrosia.backup.Ambrosia;
import com.ambrosia.backup.config.AmbrosiaDatabaseConfig;
import com.ambrosia.backup.sql.TableName;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.Sheets.Builder;
import com.google.api.services.sheets.v4.Sheets.Spreadsheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.List;

public class GoogleService {

    private static final String APPLICATION_NAME = "Ambrosia Backups";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart. If modifying these scopes, delete your previously saved tokens/
     * folder.
     */
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
        return SHEETS.spreadsheets();
    }

    private static Credential getCredentials(NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        if (!Files.exists(CREDENTIALS_FILE_PATH)) {
            throw new FileNotFoundException(
                "Please include file: %s for Google Credentials".formatted(CREDENTIALS_FILE_PATH.toAbsolutePath()));
        }
        BufferedReader in = Files.newBufferedReader(CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, in);

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
            .setDataStoreFactory(new FileDataStoreFactory(Ambrosia.getFile(TOKENS_DIRECTORY_PATH)))
            .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public static void load() throws IOException, GeneralSecurityException {
        NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Credential credentials = getCredentials(HTTP_TRANSPORT);
        SHEETS = new Builder(HTTP_TRANSPORT, JSON_FACTORY, credentials)
            .setApplicationName(APPLICATION_NAME)
            .build();
        NetHttpTransport DRIVE_HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        DRIVE = new Drive.Builder(DRIVE_HTTP_TRANSPORT, JSON_FACTORY, getCredentials(DRIVE_HTTP_TRANSPORT))
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
