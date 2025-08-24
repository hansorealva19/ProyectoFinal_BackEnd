package order;


//Aplicacion principal para microservicios de pedidos

// Principios aplicados:
// - SRP: La clase tiene una única responsabilidad, que es iniciar la aplicación del servicio de pedidos.
// - Microservices Communication: La aplicación está diseñada para funcionar como un microservicio independiente.

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients // habilita Feign para comunicacion entre microservicios
@EnableScheduling
public class OrderServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(OrderServiceApplication.class, args);
    System.out.println("Order Service is running... in port http://localhost:8084");
  }

}
