package com.volunteer.platform.user.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("user-service", "UP");
    }
}
