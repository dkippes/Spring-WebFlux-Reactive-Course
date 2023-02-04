package com.example.springbootreactor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Flux;


@SpringBootApplication
public class SpringBootReactorApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SpringBootReactorApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringBootReactorApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // Creamos nuestro observable que va a estar subcripto a Fluximplementa un Publisher
        Flux<String> nombres = Flux.just("Andres", "Pedro") // Si se pone "" da un error
                .doOnNext(elemento -> {
                    if (elemento.isEmpty()) {
                        throw new RuntimeException("Nombres no puede ser vacio"); // Con esto no termina el proceso
                    }
                    System.out.println(elemento);
                });

        // Ahora si se ejecuta
        nombres.subscribe(
                e -> log.info(e),
                error -> log.error(error.getMessage()),
                new Runnable() {
                    @Override
                    public void run() {
                        log.info("Ha finalizado");
                    }
                }
        );
    }
}
