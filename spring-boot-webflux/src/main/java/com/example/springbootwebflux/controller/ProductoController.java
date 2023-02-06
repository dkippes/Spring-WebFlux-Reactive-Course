package com.example.springbootwebflux.controller;

import com.example.springbootwebflux.models.dao.ProductoDao;
import com.example.springbootwebflux.models.documents.Producto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.thymeleaf.spring6.context.webflux.ReactiveDataDriverContextVariable;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.logging.Logger;

@Controller
public class ProductoController {
    @Autowired
    private ProductoDao dao;

    private static final Logger log = Logger.getLogger(ProductoController.class.getName());

    @GetMapping({"/listar", "/"})
    public String listar(Model model) {
        Flux<Producto> productos = dao.findAll()
                .map(producto -> {
                    producto.setNombre(producto.getNombre().toUpperCase());
                    return producto;
                });
        productos.subscribe(prod -> log.info(prod.getNombre()));

        model.addAttribute("productos", productos);
        model.addAttribute("titulo", "Listado de productos");
        return "listar";
    }

    // * DATA DRIVER -> Permite sacar del flujo una parte de los productos aunque tenga delay
    @GetMapping("/listar-datadriver")
    public String listarDataDriver(Model model) {
        Flux<Producto> productos = dao.findAll()
                .map(producto -> {
                    producto.setNombre(producto.getNombre().toUpperCase());
                    return producto;
                }).delayElements(Duration.ofSeconds(1));
        productos.subscribe(prod -> log.info(prod.getNombre()));

        model.addAttribute("productos", new ReactiveDataDriverContextVariable(productos, 2));
        model.addAttribute("titulo", "Listado de productos");
        return "listar";
    }

    // * FULL
    // * Hay un parametro TTFB (Time to first byte) significa cuanto tiempo tiene que estar el navegador esperando hasta que llega la primera respuesta
    @GetMapping("/listar-full")
    public String listarFull(Model model) {
        Flux<Producto> productos = dao.findAll()
                .map(producto -> {
                    producto.setNombre(producto.getNombre().toUpperCase());
                    return producto;
                }).repeat(5000);

        model.addAttribute("productos", new ReactiveDataDriverContextVariable(productos, 2));
        model.addAttribute("titulo", "Listado de productos");
        return "listar";
    }

    // * CHUNKED -> Configuracion para contrapresion (Es mejor que el full ya que este esta sin configurar)
    @GetMapping("/listar-chunked")
    public String listarChunked(Model model) {
        Flux<Producto> productos = dao.findAll()
                .map(producto -> {
                    producto.setNombre(producto.getNombre().toUpperCase());
                    return producto;
                }).repeat(5000);

        model.addAttribute("productos", new ReactiveDataDriverContextVariable(productos, 2));
        model.addAttribute("titulo", "Listado de productos");
        return "listar-chunked";
    }
}
