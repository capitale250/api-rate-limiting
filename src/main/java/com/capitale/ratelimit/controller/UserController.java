package com.capitale.ratelimit.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

	@GetMapping("/v1/secure")
	public String getSecureUser() {
		return "Hello, Secure User";
	}

	@GetMapping("/v2/open")
	public String getOpenUser() {
		return "Hello, Open User";
	}
}
