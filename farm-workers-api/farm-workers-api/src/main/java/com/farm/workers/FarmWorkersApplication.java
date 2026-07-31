package com.farm.workers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  .NET → Java: Application Entry Point                    ║
 * ║  Program.cs / Startup.cs  →  This class                 ║
 * ║  WebApplication.CreateBuilder()  →  SpringApplication   ║
 * ║  builder.Services.Add*()  →  @Bean / @Component scan    ║
 * ║  app.UseAuthentication()  →  SecurityConfig (separate)  ║
 * ╚══════════════════════════════════════════════════════════╝
 *
 * @SpringBootApplication is a composite of:
 *   - @Configuration       (like a DI container setup class)
 *   - @EnableAutoConfiguration (convention-over-config wiring)
 *   - @ComponentScan       (scans this package + sub-packages for beans)
 *
 * Beans (≈ DI services) are registered via:
 *   - @Component / @Service / @Repository / @Controller (auto-scan)
 *   - @Bean methods inside @Configuration classes (explicit registration)
 */
@SpringBootApplication
@EnableAsync    // Enables @Async methods  ≈  Task.Run() / async void in .NET
@EnableScheduling // Enables @Scheduled methods  ≈  IHostedService / BackgroundService
public class FarmWorkersApplication {

    public static void main(String[] args) {
        // ≈ WebApplication.CreateBuilder(args).Build().Run()
        SpringApplication.run(FarmWorkersApplication.class, args);
    }
}
