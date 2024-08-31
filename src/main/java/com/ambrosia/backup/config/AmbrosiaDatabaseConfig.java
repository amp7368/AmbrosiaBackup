package com.ambrosia.backup.config;

import com.ambrosia.backup.Ambrosia;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

public class AmbrosiaDatabaseConfig {

    public static final CronParser PARSER = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));

    protected String schema = "public";
    protected List<String> ignoredTables = new ArrayList<>();
    protected String fileName = "Name";
    protected String folderId = "None";
    protected String cronDefinition = "0 0 * * 1";
    protected AmbrosiaDatabaseType type;
    protected Instant lastUpdated = Instant.EPOCH;
    @Nullable
    protected Instant nextUpdate = Instant.EPOCH;

    protected String username = "username";
    protected String password = "password";
    protected String connectionUrl = "jdbc:mysql://localhost:3306/ambrosia";

    protected transient Cron parsed;

    public AmbrosiaDatabaseConfig(AmbrosiaDatabaseType type) {
        this.type = type;
    }

    public AmbrosiaDatabaseConfig() {
    }

    public void init() {
        parsed = PARSER.parse(cronDefinition);
        resetNextUpdate();
    }

    private void resetNextUpdate() {
        ExecutionTime executionTime = ExecutionTime.forCron(parsed);
        ZonedDateTime now = ZonedDateTime.now();
        Optional<ZonedDateTime> shouldHaveRan = executionTime.lastExecution(now);
        if (shouldHaveRan.isPresent()) {
            ZonedDateTime lastUpdatedZoned = lastUpdated.atZone(Ambrosia.DATE_ZONE);
            if (shouldHaveRan.get().isAfter(lastUpdatedZoned)) {
                nextUpdate = Instant.now();
                return;
            }
        }
        nextUpdate = executionTime.nextExecution(now).map(ZonedDateTime::toInstant).orElse(null);
    }

    public void setUpdated() {
        lastUpdated = Instant.now();
        resetNextUpdate();
    }

    @Nullable
    public Instant getNextUpdate() {
        if (nextUpdate == null) Ambrosia.logger().error("Next update for {} is null", type);
        return nextUpdate;
    }

    public boolean canRunNext() {
        if (nextUpdate == null) return false;
        return Instant.now().isAfter(nextUpdate);
    }

    public String getName() {
        String date = DateTimeFormatter.ISO_DATE.format(ZonedDateTime.now());
        return "%s-%s".formatted(fileName, date);
    }

    public String getFolderId() {
        return folderId;
    }

    public String getSchema() {
        return schema;
    }

    public AmbrosiaDatabaseType getType() {
        return this.type;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(connectionUrl, username, password);
    }

    public boolean isIgnored(String name) {
        return this.ignoredTables.stream().anyMatch(name::equalsIgnoreCase);
    }
}
