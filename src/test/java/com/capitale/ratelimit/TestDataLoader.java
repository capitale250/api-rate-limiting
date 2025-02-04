package com.capitale.ratelimit;

import com.capitale.ratelimit.model.User;
import com.capitale.ratelimit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class TestDataLoader implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) {
        userRepository.save(new User(1,"client123", 5));  // Rate limit: 5 requests/min
    }
}

