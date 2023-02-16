package com.example.springbootwebfluxapirest.models.dao;

import com.example.springbootwebfluxapirest.models.documents.Producto;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface ProductoDao extends ReactiveMongoRepository<Producto, String> {
}
