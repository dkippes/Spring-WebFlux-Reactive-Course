package com.example.springbootwebflux.models.dao;

import com.example.springbootwebflux.models.documents.Producto;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface ProductoDao extends ReactiveMongoRepository<Producto, String> {
}
