package com.example.springbootwebflux.controller;

import com.example.springbootwebflux.models.documents.Categoria;
import com.example.springbootwebflux.models.documents.Producto;
import com.example.springbootwebflux.models.services.ProductoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.thymeleaf.spring6.context.webflux.ReactiveDataDriverContextVariable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;

@SessionAttributes("producto")
@Controller
public class ProductoController {
    @Autowired
    private ProductoService productoService;

    @Value("${config.uploads.path}")
    private String path;

    private static final Logger log = Logger.getLogger(ProductoController.class.getName());

    @ModelAttribute("categorias")
    private Flux<Categoria> categorias() {
        return productoService.findAllCategoria();
    }

    @GetMapping("/uploads/img/{nombReFoto:.+}")
    public Mono<ResponseEntity<Resource>> verFoto(@PathVariable String nombreFoto) throws MalformedURLException {
        Path ruta = Paths.get(path).resolve(nombreFoto).toAbsolutePath();
        Resource imagen = new UrlResource(ruta.toUri());
        return Mono.just(ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + imagen.getFilename() + "\"")
                .body(imagen));
    }

    @GetMapping("/ver/{id}")
    public Mono<String> ver(@PathVariable String id, Model model) {
        return productoService.findById(id)
                .doOnNext(prod -> {
                    log.info("Producto: " + prod.getNombre());
                    model.addAttribute("producto", prod);
                    model.addAttribute("titulo", "Detalle producto");
                })
                .defaultIfEmpty(new Producto())
                .flatMap(prod -> {
                    if (prod.getId() == null) {
                        return Mono.error(new InterruptedException("No existe el producto"));
                    }
                    return Mono.just(prod);
                })
                .then(Mono.just("ver"))
                .onErrorResume(ex -> Mono.just("redirect:/listar?error=no+existe+el+producto"));
    }

    @GetMapping({"/listar", "/"})
    public Mono<String> listar(Model model) {
        Flux<Producto> productos = productoService.findAllNombreUpperCase();
        productos.subscribe(prod -> log.info(prod.getNombre()));

        model.addAttribute("productos", productos);
        model.addAttribute("titulo", "Listado de productos");
        return Mono.just("listar");
    }

    @GetMapping("/form")
    public Mono<String> crear(Model model) {
        model.addAttribute("producto", new Producto());
        model.addAttribute("titulo", "Formulario de productos");
        model.addAttribute("boton", "Crear");

        return Mono.just("form");
    }

    @GetMapping("/form/{id}")
    public Mono<String> editar(@PathVariable String id, Model model) {
        Mono<Producto> producto = productoService.findById(id)
                .doOnNext(prod -> log.info("Producto: " + prod.getNombre()))
                .defaultIfEmpty(new Producto());
        model.addAttribute("producto", producto);
        model.addAttribute("titulo", "Editar producto");
        model.addAttribute("boton", "Editar");
        return Mono.just("form");
    }

    @GetMapping("/form-v2/{id}")
    public Mono<String> editarV2(@PathVariable String id, Model model) {

        return productoService.findById(id)
                .doOnNext(prod -> {
                    log.info("Producto: " + prod.getNombre());
                    model.addAttribute("producto", prod);
                    model.addAttribute("titulo", "Editar producto");
                })
                .flatMap(p -> {
                    if (p == null) {
                        return Mono.error(new InterruptedException("No existe el producto"));
                    }
                    return Mono.just(p);
                })
                .then(Mono.just("form"))
                .onErrorResume(ex -> Mono.just("redirect:/listar?error=no+existe+el+producto"));
    }

    @PostMapping("/form")
    public Mono<String> guardar(@Valid Producto producto, BindingResult bindingResult, Model model, @RequestPart FilePart file, SessionStatus status) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("titulo", "Errores en formulario de productos");
            model.addAttribute("boton", "Guardar");
            return Mono.just("form");
        } else {
            status.setComplete();
            Mono<Categoria> categoria = productoService.findCategoriaById(producto.getCategoria().getId());
            return categoria.flatMap(c -> {
                        if (producto.getCreateAt() == null) {
                            producto.setCreateAt(new Date());
                        }
                        if (!file.filename().isEmpty()) {
                            producto.setFoto(UUID.randomUUID().toString() + "-" + file.filename()
                                    .replace(" ", "")
                                    .replace(":", "")
                                    .replace("\\", ""));
                        }
                        producto.setCategoria(c);
                        return productoService.save(producto);
                    }).doOnNext(p -> {
                        log.info("Categoria asignada: " + p.getCategoria().getNombre() + " ID: " + p.getCategoria().getId());
                        log.info("Producto guardado: " + p.getNombre() + " ID: " + p.getId());
                    }).flatMap(p -> {
                        if (!file.filename().isEmpty()) {
                            return file.transferTo(new java.io.File(path + p.getFoto()));
                        } else {
                            return Mono.empty();
                        }
                    })
                    .thenReturn("redirect:/listar?sucess=producto+guardado+con+exito");
        }
    }

    @GetMapping("/eliminar/{id}")
    public Mono<String> eliminar(@PathVariable String id) {
        return productoService.findById(id)
                .defaultIfEmpty(new Producto())
                .flatMap(p -> {
                    if (p.getId() == null) {
                        return Mono.error(new InterruptedException("No existe el producto a eliminar"));
                    }
                    return Mono.just(p);
                })
                .flatMap(p -> {
                    log.info("Eliminando producto: " + p.getNombre() + " ID: " + p.getId());
                    return productoService.delete(p);
                })
                .then(Mono.just("redirect:/listar?success=producto+eliminado+con+exito"))
                .onErrorResume(ex -> Mono.just("redirect:/listar?error=no+existe+el+producto+a+eliminar"));
    }

    // * DATA DRIVER -> Permite sacar del flujo una parte de los productos aunque tenga delay
    @GetMapping("/listar-datadriver")
    public String listarDataDriver(Model model) {
        Flux<Producto> productos = productoService.findAllNombreUpperCase().delayElements(Duration.ofSeconds(1));
        productos.subscribe(prod -> log.info(prod.getNombre()));

        model.addAttribute("productos", new ReactiveDataDriverContextVariable(productos, 2));
        model.addAttribute("titulo", "Listado de productos");
        return "listar";
    }

    // * FULL
    // * Hay un parametro TTFB (Time to first byte) significa cuanto tiempo tiene que estar el navegador esperando hasta que llega la primera respuesta
    @GetMapping("/listar-full")
    public String listarFull(Model model) {
        Flux<Producto> productos = productoService.findAllNombreUpperCaseRepeat();

        model.addAttribute("productos", new ReactiveDataDriverContextVariable(productos, 2));
        model.addAttribute("titulo", "Listado de productos");
        return "listar";
    }

    // * CHUNKED -> Configuracion para contrapresion (Es mejor que el full ya que este esta sin configurar)
    @GetMapping("/listar-chunked")
    public String listarChunked(Model model) {
        Flux<Producto> productos = productoService.findAllNombreUpperCaseRepeat();

        model.addAttribute("productos", new ReactiveDataDriverContextVariable(productos, 2));
        model.addAttribute("titulo", "Listado de productos");
        return "listar-chunked";
    }
}
