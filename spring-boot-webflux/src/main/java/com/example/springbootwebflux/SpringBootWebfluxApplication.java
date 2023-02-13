package com.example.springbootwebflux;

import com.example.springbootwebflux.models.documents.Categoria;
import com.example.springbootwebflux.models.documents.Producto;
import com.example.springbootwebflux.models.services.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;

import java.util.Date;

@SpringBootApplication
public class SpringBootWebfluxApplication implements CommandLineRunner {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SpringBootWebfluxApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringBootWebfluxApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        mongoTemplate.dropCollection("productos").subscribe();
        mongoTemplate.dropCollection("categorias").subscribe();

        Categoria electrónica = new Categoria("Electrónica");
        Categoria deportes = new Categoria("Deportes");
        Categoria jardín = new Categoria("Jardín");
        Categoria muebles = new Categoria("Muebles");

        Flux.just(electrónica, deportes, jardín, muebles)
                .flatMap(productoService::saveCategoria)
                .doOnNext(categoria -> {
                    log.info("Categoria creada: " + categoria.getNombre() + " Id: " + categoria.getId());
                }).thenMany(
                        Flux.just(
                                new Producto("TV Panasonic Pantalla LCD", 456.89, electrónica),
                                new Producto("Sony Camara HD Digital", 177.89, electrónica),
                                new Producto("Apple iPod", 46.89, electrónica),
                                new Producto("Sony Notebook", 846.89, electrónica),
                                new Producto("Hewlett Packard Multifuncional", 200.89, jardín),
                                new Producto("Bianchi Bicicleta", 70.89, deportes),
                                new Producto("HP Notebook Omen 17", 2500.89, electrónica),
                                new Producto("Mica Cómoda 5 Cajones", 150.89, muebles),
                                new Producto("TV Sony Bravia OLED 4K Ultra HD", 2255.89, electrónica),
                                new Producto("Apple Watch Series 4 GPS", 1846.89, electrónica)
                        )
                ).flatMap(producto -> {
                    producto.setCreateAt(new Date());
                    return productoService.save(producto);
                }).subscribe(producto -> log.info("Insert: " + producto.getId() + " " + producto.getNombre()));
    }
}
