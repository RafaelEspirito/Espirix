package br.com.espiritolabs.espirix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EspirixApplication {
    public static void main(String[] args) {
        SpringApplication.run(EspirixApplication.class, args);
    }
}
