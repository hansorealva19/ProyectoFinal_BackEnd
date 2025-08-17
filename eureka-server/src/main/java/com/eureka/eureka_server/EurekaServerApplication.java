package com.eureka.eureka_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.ConfigurableApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

	private static final Logger logger = LoggerFactory.getLogger(EurekaServerApplication.class);

	public static void main(String[] args) {
		logger.info("Access the dashboard at http://localhost:8761");
		logger.info("Use this server for service registration and discovery");

		try {
			System.setProperty("spring.application.name", "eureka-server");
			System.setProperty("server.port", "8761");

			logger.info("Eureka Server is running on port 8761");
			ConfigurableApplicationContext context = SpringApplication.run(EurekaServerApplication.class, args);

			context.registerShutdownHook();
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				logger.info("Shutting down Eureka Server...");
				logger.info("Registered services at shutdown: {}",
						context.getBean("eurekaServerContext") != null ? "Disponible" : "No disponible"
				);
				context.close();
				logger.info("Eureka Server has been shut down.");
			}));

			logger.info("Eureka Server started successfully");
		} catch (Exception e) {
			logger.error("Error starting Eureka Server", e);
			logger.error("Puerto ya en uso o configuración incorrecta. Verifique el puerto 8761.");
			System.exit(1);
		}
	}
}