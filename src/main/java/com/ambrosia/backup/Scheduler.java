package com.ambrosia.backup;

import com.ambrosia.backup.config.AmbrosiaConfig;
import com.ambrosia.backup.config.AmbrosiaDatabaseConfig;
import com.ambrosia.backup.sql.ExportDatabase;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Scheduler {

    private static final ScheduledThreadPoolExecutor SCHEDULER = new ScheduledThreadPoolExecutor(1);

    public Scheduler() {
        schedule();
    }

    private void schedule() {
        Instant nextRun = AmbrosiaConfig.get().getNextRun();
        if (nextRun == null) {
            Ambrosia.logger().warn("Could not find next run. Defaulting to 1 hour.");
            nextRun = Instant.now().plus(Duration.ofHours(1));
        }
        // add 1 second to make sure we get at least the ciel when rounding
        Duration delay = Duration.between(Instant.now(), nextRun).plusSeconds(1);
        AmbrosiaConfig.get().listDelays();
        SCHEDULER.schedule(this::run, delay.getSeconds(), TimeUnit.SECONDS);
    }


    private void run() {
        Ambrosia.logger().info("Checking for tasks...");
        List<AmbrosiaDatabaseConfig> services = AmbrosiaConfig.get().getRunnableServices();
        try {
            for (AmbrosiaDatabaseConfig service : services) {
                Ambrosia.logger().info("Starting export: {}", service.getName());
                ExportDatabase.export(service);
                Ambrosia.logger().info("Finished export: {}", service.getName());
            }
        } catch (Exception e) {
            String failedServices = services.stream().map(AmbrosiaDatabaseConfig::getName).collect(
                Collectors.joining(","));
            Ambrosia.logger().error("Failed to run services: {}", failedServices, e);
        } finally {
            services.forEach(AmbrosiaDatabaseConfig::setUpdated);
            AmbrosiaConfig.get().save();
            schedule();
        }

    }
}
