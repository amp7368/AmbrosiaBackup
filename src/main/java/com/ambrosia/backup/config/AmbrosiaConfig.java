package com.ambrosia.backup.config;

import apple.utilities.database.concurrent.ConcurrentAJD;
import apple.utilities.database.concurrent.inst.ConcurrentAJDInst;
import com.ambrosia.backup.Ambrosia;
import com.ambrosia.backup.InstantGsonSerializing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

public class AmbrosiaConfig {

    private static AmbrosiaConfig instance;
    private static ConcurrentAJDInst<AmbrosiaConfig> manager;

    private final Map<AmbrosiaDatabaseType, AmbrosiaDatabaseConfig> services = new HashMap<>();

    public AmbrosiaConfig() {
        instance = this;
    }

    public static AmbrosiaConfig get() {
        return instance;
    }

    public static void load() {
        File file = Ambrosia.getFile("AmbrosiaConfig.json");
        Gson gson = InstantGsonSerializing.registerGson(new GsonBuilder().setPrettyPrinting()).create();
        manager = ConcurrentAJD.createInst(AmbrosiaConfig.class, file, gson);
        manager.loadNow().init();
    }

    public List<AmbrosiaDatabaseConfig> getRunnableServices() {
        synchronized (services) {
            return services.values().stream()
                .filter(AmbrosiaDatabaseConfig::canRunNext)
                .toList();
        }
    }

    @Nullable
    public Instant getNextRun() {
        Optional<Instant> nextRun;
        synchronized (services) {
            nextRun = services.values().stream()
                .map(AmbrosiaDatabaseConfig::getNextUpdate)
                .filter(Objects::nonNull)
                .sorted()
                .findFirst();
        }

        if (nextRun.isEmpty()) {
            System.err.println("There are no tasks to run!");
            return null;
        }
        return nextRun.get();
    }

    private void init() {
        synchronized (services) {
            for (AmbrosiaDatabaseType type : AmbrosiaDatabaseType.values()) {
                services.computeIfAbsent(type, AmbrosiaDatabaseConfig::new)
                    .init();
            }
            save();
        }
    }

    public void save() {
        synchronized (services) {
            manager.saveNow();
        }
    }

    public void listDelays() {
        synchronized (services) {
            Instant now = Instant.now();
            for (AmbrosiaDatabaseConfig config : services.values()) {
                Instant nextUpdate = config.getNextUpdate();
                if (nextUpdate == null) {
                    Ambrosia.logger().warn("Ignoring next update for {}", config.getName());
                    continue;
                }
                double hours = Duration.between(now, nextUpdate).toMinutes() / 60.0;
                String msg = "Set to run %s in %.2f hours".formatted(config.getName(), hours);
                Ambrosia.logger().info(msg);
            }

        }
    }

}
