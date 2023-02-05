package com.example.springbootreactor;

import com.example.springbootreactor.models.Comentarios;
import com.example.springbootreactor.models.Usuario;
import com.example.springbootreactor.models.UsuarioComentarios;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.ui.Model;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;


@SpringBootApplication
public class SpringBootReactorApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SpringBootReactorApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringBootReactorApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // * Creamos nuestro observable que va a estar subcripto a Fluximplementa un Publisher
        Flux<String> nombres = Flux.just("Andres", "Pedro", "Maria", "Diego") // Si se pone "" da un error
                .doOnNext(elemento -> {
                    if (elemento.isEmpty()) {
                        throw new RuntimeException("Nombres no puede ser vacio"); // Con esto no termina el proceso
                    }
                    System.out.println(elemento);
                });

        // * Ahora si se ejecuta
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

        // * OPERADOR MAP
        nombres.map(nombre -> {
                    return new Usuario(nombre.toUpperCase(), "Un apellido");
                })
                .subscribe(e -> System.out.println("OPERADOR MAP => " + e.toString()));

        // * OPERADOR FILTER - Cada vez que se ejecuta un nuevo operador se crea nuevas instancias con su propio estado modificado
        nombres.map(nombre -> {
                    return new Usuario(nombre.toUpperCase(), "Un apellido");
                })
                .filter(usuario -> usuario.getNombre().equalsIgnoreCase("Andres"))
                .subscribe(e -> System.out.println("OPERADOR FILTER => " + e.toString()));

        // * CREANDO UN FLUX (OBSERVABLE) A PARTIR DE UN LIST O ITERABLE
        List<String> usuariosList = new ArrayList<>();
        usuariosList.add("Andres");
        usuariosList.add("Diego");
        usuariosList.add("Maria");

        Flux<String> nombresIterables = Flux.fromIterable(usuariosList);
        nombresIterables.subscribe(e -> System.out.println("OPERADOR ITERABLE => " + e.toString()));

        // * SQL, MYSQL, ORACLE, POSTGRES, SQLSERVER NO SON REACTIVAS PERO AL RECUPERAR LOS DATOS SI SE PUEDEN CONVERTIR A FLUX
        // * FLUX -> VARIOS DATOS
        // * MONO -> UN SOLO DATO
        // * MONGODB SI ES REACTIVO -> ReactivePersonRepository extends ReactiveSortingRepository<Person, Long>

        // * OPERADOR FLATMAP -> APLANA UNA LISTA DE LISTAS
        nombresIterables.map(nombre -> new Usuario(nombre.toUpperCase(), "Un apellido"))
                .flatMap(usuario -> {
                    if (usuario.getNombre().equalsIgnoreCase("Andres")) {
                        return Mono.just(usuario);
                    } else {
                        return Mono.empty();
                    }
                })
                .subscribe(e -> System.out.println("OPERADOR FLATMAP => " + e.toString()));

        // * CONVERTIR UN FLUX A MONO
        List<String> usuariosFlux = Arrays.asList("Andres", "Diego", "Maria");
        Flux.fromIterable(usuariosFlux)
                .collectList()
                .subscribe(list -> {
                    System.out.println("FLUX A MONO => ");
                    list.forEach(System.out::println);
                });

        // * OTRO EJEMPLO USANDO FLATMAP
        ejemploUsuarioComentariosFlatMap();

        // * OPERADOR ZIPWITH -> COMBINAR DOS FLUX
        ejemploUsuarioComentariosZipWith();

        // * OPERADOR RANGE
        ejemploZipWithRange();

        // * INTERVALOS DE TIEMPOS CON OPERADOR INTERVAL Y ZIPWITH
        ejemploInterval();
        ejemploDelayElements();

        // * INTERVALOS DE TIEMPOS INFINITO
        ejemploIntervaloInfinito();

        // * CREANDO OPERADOR PROPIO FLUX
        ejemploIntervaloDesdeCreate();

        // * MANEJANDO CONTRAPRESION
        ejemploContraPresion();
    }

    public void ejemploContraPresion() throws InterruptedException {
        Flux.range(1, 10)
                .log()
                // * .limitRate(5) CANTIDAD DE ELEMNTOS QUE QUEREMOS RECIBIR
                .subscribe(new Subscriber<Integer>() {

                    private Subscription s;
                    private Integer limite = 2;
                    private Integer consumido = 0;

                    @Override
                    public void onSubscribe(Subscription subscription) {
                        this.s = subscription;
                        s.request(limite); // Envia todos los elementos de 1 sola vez
                    }

                    @Override
                    public void onNext(Integer integer) {
                        System.out.println("CONTRAPRESION => " + integer);
                        consumido++;
                        if (consumido == limite) {
                            consumido = 0;
                            s.request(limite);
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void ejemploIntervaloDesdeCreate() throws InterruptedException {
        Flux.create(emitter -> {
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        private Integer contador = 0;

                        @Override
                        public void run() {
                            emitter.next(++contador);
                            if (contador == 10) {
                                timer.cancel();
                                emitter.complete();
                            }
                            if (contador == 5) {
                                timer.cancel();
                                emitter.error(new InterruptedException("Solo hasta 5"));
                            }
                        }
                    }, 1000, 1000);
                })
                .doOnNext(next -> System.out.println(next))
                .doOnComplete(() -> System.out.println("Ha finalizado la ejecucion"))
                .doOnError(error -> System.out.println("Ha ocurrido un error: " + error.getMessage()))
                .subscribe();
    }

    public void ejemploIntervaloInfinito() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Flux.interval(Duration.ofSeconds(1))
                .doOnTerminate(() -> latch.countDown())
                .flatMap(i -> {
                    if (i >= 5) {
                        return Flux.error(new InterruptedException("Solo hasta 5"));
                    }
                    return Flux.just(i);
                })
                .map(i -> "Hola " + i)
                //.doOnNext(s -> System.out.println(s))
                .retry(2)
                .subscribe(s -> System.out.println(s), error -> log.error(error.getMessage()));

        latch.await();
    }

    public void ejemploDelayElements() throws InterruptedException {
        System.out.println("EJEMPLO DELAY ELEMENTS");
        Flux<Integer> rangos = Flux.range(1, 12)
                .delayElements(Duration.ofSeconds(1))
                .doOnNext(i -> System.out.println("Interval: " + i));

        rangos.blockLast();
    }

    public void ejemploInterval() {
        System.out.println("EJEMPLO INTERVAL");
        Flux<Integer> rangos = Flux.range(1, 12);
        Flux<Long> retraso = Flux.interval(Duration.ofSeconds(1));
        rangos.zipWith(retraso, (rango, retrazo) -> rango)
                .doOnNext(i -> System.out.println("Interval: " + i))
                .blockLast(); // * TAMBIEN SE SUSCRIBE PERO BLOCKEA EL PROCESO - ES SYNCRONICO - NO ES RECOMENDABLE
    }

    public void ejemploZipWithRange() {
        System.out.println("EJEMPLO ZIPWITH RANGE");
        Flux<Integer> rangos = Flux.range(1, 4);
        Flux.just(1, 2, 3, 4)
                .map(i -> i * 2)
                .zipWith(rangos, (uno, dos) -> String.format("Primer Flux: %d, Segundo Flux: %d", uno, dos))
                .subscribe(System.out::println);
    }

    public void ejemploUsuarioComentariosFlatMap() {
        Mono<Usuario> usuarioMono = Mono.fromCallable(() -> crearUsuario());
        Mono<Comentarios> comentariosUsuarioMono = Mono.fromCallable(() -> {
            Comentarios comentarios = new Comentarios();
            comentarios.addComentario("Hola que tal");
            comentarios.addComentario("Que tal");
            return comentarios;
        });

        usuarioMono
                .flatMap(u -> comentariosUsuarioMono.map(c -> new UsuarioComentarios(u, c)))
                .subscribe(uc -> System.out.println("EJEMPLO FLATMAP => " + uc.toString()));
    }

    public void ejemploUsuarioComentariosZipWith() {
        Mono<Usuario> usuarioMono = Mono.fromCallable(() -> crearUsuario());
        Mono<Comentarios> comentariosUsuarioMono = Mono.fromCallable(() -> {
            Comentarios comentarios = new Comentarios();
            comentarios.addComentario("Hola que tal");
            comentarios.addComentario("Que tal");
            return comentarios;
        });

        usuarioMono
                .zipWith(comentariosUsuarioMono, (usuario, comentarios) -> new UsuarioComentarios(usuario, comentarios))
                .subscribe(uc -> System.out.println("EJEMPLO ZIPWITH => " + uc.toString()));
    }

    private Usuario crearUsuario() {
        return new Usuario("Diego", "Garcia");
    }
}
