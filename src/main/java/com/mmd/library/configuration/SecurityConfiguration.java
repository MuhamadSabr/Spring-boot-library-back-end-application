package com.mmd.library.configuration;

import com.mmd.library.constant.RoleConstants;
import com.mmd.library.constant.UrlConstants;
import com.mmd.library.exception.EmailNotConfirmedException;
import com.mmd.library.filter.JWTTokenGeneratorFilter;
import com.mmd.library.filter.JWTTokenRefresher;
import com.mmd.library.filter.JWTTokenValidatorFilter;
import com.mmd.library.service.LibraryUserDetailsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfiguration {



    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("api/admin/**", "api/messages/search/findByClosed").hasRole(RoleConstants.ADMIN.name())
                        .requestMatchers(
                                "/api/books/checkout/**", "/api/books/currentCheckedOutCountByUser", "/api/books/currentLoans",
                                "/api/books/isCheckedOutByUser/**", "/api/reviews/add", "/api/reviews/hasUserLeftReview/**",
                                "/api/books/returnCheckedOutBook/**", "/api/books/renewCheckedOutBook/**", "api/messages/**", "api/payment/**"
                        ).authenticated()
                        .requestMatchers("/signup", "/login", "/logout", "/api/books/**", "/api/reviews/**", "/verify-email/**").permitAll()
                        .anyRequest().authenticated())
                .cors(cors-> cors.configurationSource(corsConfigurationSource()));

        http.httpBasic(httpSecurityHttpBasicConfigurer -> httpSecurityHttpBasicConfigurer.authenticationEntryPoint(authenticationEntryPoint()));

        http.addFilterAfter(new JWTTokenGeneratorFilter(), BasicAuthenticationFilter.class);
        http.addFilterAfter(new JWTTokenRefresher(), BasicAuthenticationFilter.class);
        http.addFilterBefore(new JWTTokenValidatorFilter(), BasicAuthenticationFilter.class);

        return http.build();
    }


    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.applyPermitDefaultValues();
        configuration.setAllowedOrigins(List.of(UrlConstants.FRONTEND_URL)); // Allow all origins
        configuration.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS")); // Allow all methods
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));//Exposing header from backend to frontend
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }


    @Bean
    AuthenticationEntryPoint authenticationEntryPoint() {
        return new LibraryAuthenticationEntryPoint();
    }

    static class LibraryAuthenticationEntryPoint implements AuthenticationEntryPoint{// For handling failures of basic auth
        @Override
        public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
            Throwable cause = authException.getCause();
            if(cause instanceof EmailNotConfirmedException){
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("Email not confirmed: " + authException.getMessage());
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
            }
        }
    }


    @Bean
    PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
