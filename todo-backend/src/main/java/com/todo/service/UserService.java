package com.todo.service;

import com.todo.auth.JwtUtil;
import com.todo.dto.UserDTO;
import com.todo.entity.User;
import com.todo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public void register(UserDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        userRepository.save(user);
    }

    public String login(UserDTO dto) {
        User user = userRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        return jwtUtil.generate(user.getId(), user.getUsername());
    }
}
