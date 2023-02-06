package com.example.springbootwebflux.controller;

import com.example.springbootwebflux.models.dao.ProductoDao;
import com.example.springbootwebflux.models.documents.Producto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.logging.Logger;

@RestController
@RequestMapping("/api/productos")
public class ProductoRestController {
    @Autowired
    private ProductoDao dao;

    private static final Logger log = Logger.getLogger(ProductoRestController.class.getName());

    @GetMapping
    public Flux<Producto> index() {
        Flux<Producto> productos = dao.findAll()
                .map(producto -> {
                    producto.setNombre(producto.getNombre().toUpperCase());
                    return producto;
                }).doOnNext(prod -> log.info(prod.getNombre()));
        return productos;
    }

    @GetMapping("/{id}")
    public Mono<Producto> show(@PathVariable String id) {
//        Mono<Producto> productos = dao.findById(id);
        Flux<Producto> productos = dao.findAll();

        Mono<Producto> producto = productos.filter(p -> p.getId().equals(id))
                .next() // * Nos retorna un Mono
                .doOnNext(prod -> log.info(prod.getNombre())); // Imprimir el nombre del producto
        return producto;
    }
}
