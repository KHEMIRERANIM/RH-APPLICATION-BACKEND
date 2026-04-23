package tn.esprit.rh_rse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableMongoAuditing
@SpringBootApplication
public class RhRseApplication {

    public static void main(String[] args) {
        SpringApplication.run(RhRseApplication.class, args);
    }

}


