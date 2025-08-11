package com.eureka.eureka_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.ConfigurableApplicationContext;
//  funciones
//  - registro y descubrimiento de servicios
//  - health check de servicios usando Eureka y actuator
// - dashboard de Eureka
// - load balancing
// - puerto: 8761  (por defecto)
// - url: http://localhost:8761

@SpringBootApplication
@EnableEurekaServer // Habilita el servidor Eureka
public class EurekaServerApplication {

	// Logger para tracking detallado
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EurekaServerApplication.class);

	public static void main(String[] args) {

//    logging de inicio detallado
		logger.info("Access the dashboard at http://localhost:8761");
		logger.info("Use this server for service registration and discovery");

		try {
			//    configurar propiedades del sistema via codigo
			System.setProperty("spring.application.name", "eureka-server");
			System.setProperty("server.port", "8761");

			logger.info("Eureka Server is running on port 8761");
			ConfigurableApplicationContext context = SpringApplication.run(EurekaServerApplication.class, args);

			//    configurar el shutdown hook
			context.registerShutdownHook();
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				logger.info("Shutting down Eureka Server...");

				//    logear el servicio registrado al cierre
				logger.info("Registered services at shutdown: {}",
						context.getBean("eurekaServerContext") != null ? "Disponible" : "No disponible"
				);

				context.close();
				logger.info("Eureka Server has been shut down.");
			}));

			// Contexto de la aplicación iniciado correctamente
			logger.info("Eureka Server started successfully");
		} catch (Exception e) {
			// Manejo de excepciones durante el inicio
			logger.error("Error starting Eureka Server", e);
			logger.error("Puerto ya en uso o configuración incorrecta. Verifique el puerto 8761.");
			System.exit(1); // Salir con código de error
		}


	}

}
