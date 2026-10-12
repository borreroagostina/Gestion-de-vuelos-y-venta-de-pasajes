package com.aerolinea.gestionvuelos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GestionVuelosApplication {

  public static void main(String[] args) {
    SpringApplication.run(GestionVuelosApplication.class, args);
  }
}
