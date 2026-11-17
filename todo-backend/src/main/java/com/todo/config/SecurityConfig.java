package com.todo.config;

import com.todo.auth.JwtAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** ponytail: 用简单的 MVC 风格过滤器注册，而非完整的 Spring Security 过滤链——对 JWT 演示足够。 */
    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtFilter(JwtAuthFilter filter) {
        FilterRegistrationBean<JwtAuthFilter> reg = new FilterRegistrationBean<>(filter);
        reg.addUrlPatterns("/api/*");
        reg.setOrder(1);
        return reg;
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
            }

            /** 提供上传图片的访问。ponytail: 公开静态服务（无鉴权）——<img> 标签
                无法携带 JWT 请求头；URL 是不可猜测的 UUID。若在意隐私，
                升级方向是改用短时效签名 URL。 */
            @Override
            public void addResourceHandlers(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry registry) {
                // 结尾斜杠必不可少：Spring 会原样拼接 location + 文件名
                String dir = java.nio.file.Path.of(System.getProperty("user.dir"), "uploads").toUri().toString();
                if (!dir.endsWith("/")) dir += "/";
                registry.addResourceHandler("/uploads/**").addResourceLocations(dir);
            }
        };
    }
}
