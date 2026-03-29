package tn.esprit.rh_rse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@EnableMongoAuditing
@SpringBootApplication
public class RhRseApplication {

    public static void main(String[] args) {
        SpringApplication.run(RhRseApplication.class, args);
    }

}


