package com.example.springbootwebfluxapirest.services;

import com.example.springbootwebfluxapirest.models.documents.Categoria;
import com.example.springbootwebfluxapirest.models.documents.Producto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductoService {
    public Flux<Producto> findAll();
    public Flux<Producto> findAllNombreUpperCase();
    public Flux<Producto> findAllNombreUpperCaseRepeat();
    public Mono<Producto> findById(String id);
    public Mono<Producto> save(Producto producto);
    public Mono<Void> delete(Producto producto);
    public Flux<Categoria> findAllCategoria();
    public Mono<Categoria> findCategoriaById(String id);
    public Mono<Categoria> saveCategoria(Categoria categoria);



}
