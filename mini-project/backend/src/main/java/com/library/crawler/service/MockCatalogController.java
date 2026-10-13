package com.library.crawler.service;

import com.library.crawler.model.ExternalBookDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/mock-external-library")
public class MockCatalogController {

    @GetMapping("/books")
    public ResponseEntity<List<ExternalBookDto>> getExternalCatalog() {
        List<ExternalBookDto> books = List.of(
                ExternalBookDto.builder()
                        .isbn("9780132350884")
                        .title("Clean Code: A Handbook of Agile Software Craftsmanship")
                        .authors(List.of("Robert C. Martin"))
                        .categories(List.of("Software Engineering", "Programming"))
                        .publisher("Prentice Hall")
                        .publicationYear(2008)
                        .description("Even bad code can function. But if code isn't clean, it can bring a development organization to its knees.")
                        .coverUrl("https://images-na.ssl-images-amazon.com/images/I/41xShlnTZTL._SX376_BO1,204,203,200_.jpg")
                        .externalRating(4.7)
                        .externalSource("GlobalPublisherNet")
                        .build(),

                ExternalBookDto.builder()
                        .isbn("9780134494166")
                        .title("Clean Architecture: A Craftsman's Guide to Software Structure and Design")
                        .authors(List.of("Robert C. Martin"))
                        .categories(List.of("Software Engineering", "System Design"))
                        .publisher("Prentice Hall")
                        .publicationYear(2017)
                        .description("By applying universal rules of software architecture, you can dramatically improve developer productivity.")
                        .coverUrl("https://images-na.ssl-images-amazon.com/images/I/41-sN-mzwKL._SX381_BO1,204,203,200_.jpg")
                        .externalRating(4.8)
                        .externalSource("GlobalPublisherNet")
                        .build(),

                ExternalBookDto.builder()
                        .isbn("9781449373320")
                        .title("Designing Data-Intensive Applications")
                        .authors(List.of("Martin Kleppmann"))
                        .categories(List.of("Distributed Systems", "Database Design", "System Design"))
                        .publisher("O'Reilly Media")
                        .publicationYear(2017)
                        .description("The definitive guide to architecture of data systems, replication, sharding, transactions, and stream processing.")
                        .coverUrl("https://images-na.ssl-images-amazon.com/images/I/51ZSpMl1-LL._SX379_BO1,204,203,200_.jpg")
                        .externalRating(4.9)
                        .externalSource("GlobalPublisherNet")
                        .build(),

                ExternalBookDto.builder()
                        .isbn("9780262033848")
                        .title("Introduction to Algorithms (CLRS)")
                        .authors(List.of("Thomas H. Cormen", "Charles E. Leiserson", "Ronald L. Rivest", "Clifford Stein"))
                        .categories(List.of("Computer Science", "Algorithms"))
                        .publisher("MIT Press")
                        .publicationYear(2009)
                        .description("Comprehensive textbook covering all foundational and advanced algorithms and data structures.")
                        .coverUrl("https://images-na.ssl-images-amazon.com/images/I/41SNoh5ZhOL._SX379_BO1,204,203,200_.jpg")
                        .externalRating(4.6)
                        .externalSource("GlobalPublisherNet")
                        .build(),

                ExternalBookDto.builder()
                        .isbn("9780131103627")
                        .title("The C Programming Language")
                        .authors(List.of("Brian W. Kernighan", "Dennis M. Ritchie"))
                        .categories(List.of("Programming", "Computer Science"))
                        .publisher("Prentice Hall")
                        .publicationYear(1988)
                        .description("The foundational book on C by its creator, Dennis Ritchie.")
                        .coverUrl("https://images-na.ssl-images-amazon.com/images/I/411mW5kZ2PL._SX379_BO1,204,203,200_.jpg")
                        .externalRating(4.8)
                        .externalSource("GlobalPublisherNet")
                        .build(),

                ExternalBookDto.builder()
                        .isbn("9780321356680")
                        .title("Effective Java")
                        .authors(List.of("Joshua Bloch"))
                        .categories(List.of("Programming", "Java", "Software Engineering"))
                        .publisher("Addison-Wesley")
                        .publicationYear(2018)
                        .description("Best practices for the Java platform, covering generics, enums, lambdas, streams, and concurrency.")
                        .coverUrl("https://images-na.ssl-images-amazon.com/images/I/51X752A6E9L._SX379_BO1,204,203,200_.jpg")
                        .externalRating(4.9)
                        .externalSource("GlobalPublisherNet")
                        .build(),

                ExternalBookDto.builder()
                        .isbn("9781491950357")
                        .title("Building Microservices: Designing Fine-Grained Systems")
                        .authors(List.of("Sam Newman"))
                        .categories(List.of("Distributed Systems", "Cloud Computing", "System Design"))
                        .publisher("O'Reilly Media")
                        .publicationYear(2021)
                        .description("A holistic guide for distributed architectures, deployment models, security, and monitoring.")
                        .coverUrl("https://images-na.ssl-images-amazon.com/images/I/51r-7cEa+vL._SX379_BO1,204,203,200_.jpg")
                        .externalRating(4.7)
                        .externalSource("GlobalPublisherNet")
                        .build()
        );

        return ResponseEntity.ok(books);
    }
}
