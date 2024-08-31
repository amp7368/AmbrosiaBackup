package com.ambrosia.backup.shutdown;

import com.ambrosia.backup.HandleShutdown;
import com.ambrosia.backup.google.GoogleService;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import java.io.File;
import java.io.IOException;
import javax.annotation.Nullable;

public class ShutdownExports implements HandleShutdown {

    private static final ShutdownExports INSTANCE = new ShutdownExports();
    @Nullable
    private Spreadsheet activeExport;
    private File activeFile;

    public static ShutdownExports get() {
        return INSTANCE;
    }

    public void setActiveExport(@Nullable Spreadsheet activeExport) {
        synchronized (this) {
            this.activeExport = activeExport;
        }
    }

    @Override
    public String name() {
        return "Shutdown Exports";
    }

    @Override
    public void endShutdown() throws IOException {
        synchronized (this) {
            if (activeExport != null) {
                GoogleService.drive().delete(activeExport.getSpreadsheetId()).execute();
            }
            if (this.activeFile != null && this.activeFile.exists()) {
                this.activeFile.delete();
            }
        }
    }

    public void setActiveFile(File file) {
        synchronized (this) {
            this.activeFile = file;
        }
    }
}
