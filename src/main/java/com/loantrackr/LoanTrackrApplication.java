package com.loantrackr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class LoanTrackrApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanTrackrApplication.class, args);
    }

}
