package com.payment.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.GetMapping;

@SpringBootTest
class GatewayApplicationTests {

	public static void main(String[] args) {
        SpringApplication.run(GatewayApplicationTests.class, args);
    }

    @GetMapping("/")
    public String helloRender() {
        return "🟢 Success! Spring Boot (via Gradle) is running on Render.";
    }
}
