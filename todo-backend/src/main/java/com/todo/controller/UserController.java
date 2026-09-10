package com.todo.controller;

import com.todo.dto.UserDTO;
import com.todo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody UserDTO dto) {
        userService.register(dto);
        return ResponseEntity.ok(Map.of("message", "registered"));
    }

    @PostMapping("/login")
    public Map<String, String> login(@Valid @RequestBody UserDTO dto) {
        String token = userService.login(dto);
        return Map.of("token", token);
    }
}
