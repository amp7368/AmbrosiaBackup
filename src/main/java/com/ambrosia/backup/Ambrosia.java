package com.ambrosia.backup;

import apple.utilities.util.FileFormatting;
import com.ambrosia.backup.config.AmbrosiaConfig;
import com.ambrosia.backup.google.GoogleService;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.ZoneId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Ambrosia {


    public static final ZoneId DATE_ZONE = ZoneId.systemDefault();
    private static final Logger LOGGER = LogManager.getLogger("Ambrosia");

    public static Logger logger() {
        return LOGGER;
    }

    public static void main(String[] args) throws IOException, GeneralSecurityException {
        initSQLDriver();
        AmbrosiaConfig.load();
        GoogleService.load();
        new Scheduler();
    }

    private static void initSQLDriver() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static File getFile(String... children) {
        File root = FileFormatting.getDBFolder(Ambrosia.class);
        return FileFormatting.fileWithChildren(root, children);
    }
}