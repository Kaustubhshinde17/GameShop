
package gameshop_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .cors(Customizer.withDefaults())

            .csrf(csrf -> csrf
                .csrfTokenRepository(
                    CookieCsrfTokenRepository.withHttpOnlyFalse()
                )
                .csrfTokenRequestHandler(
                    new CsrfTokenRequestAttributeHandler()
                )
            )

            .sessionManagement(session -> session
                .sessionCreationPolicy(
                    SessionCreationPolicy.IF_REQUIRED
                )
            )

            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/auth/csrf"
                ).permitAll()

                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/logout"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/stations",
                    "/api/stations/availability"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/bookings"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/bookings"
                ).hasRole("ADMIN")

                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/bookings/*/confirm",
                    "/api/bookings/*/cancel"
                ).hasRole("ADMIN")
                .requestMatchers("/api/admin/stations/**").hasRole("ADMIN")

                .anyRequest().authenticated()
            )

            .formLogin(form -> form
                .loginProcessingUrl("/api/auth/login")
                .successHandler((request, response, authentication) -> {
                    response.setStatus(200);
                    response.setContentType(
                        MediaType.APPLICATION_JSON_VALUE
                    );
                    response.getWriter().write(
                        "{\"message\":\"Login successful\"}"
                    );
                })
                .failureHandler((request, response, exception) -> {
                    response.setStatus(401);
                    response.setContentType(
                        MediaType.APPLICATION_JSON_VALUE
                    );
                    response.getWriter().write(
                        "{\"error\":\"Invalid username or password\"}"
                    );
                })
                .permitAll()
            )

            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(200);
                    response.setContentType(
                        MediaType.APPLICATION_JSON_VALUE
                    );
                    response.getWriter().write(
                        "{\"message\":\"Logged out successfully\"}"
                    );
                })
                .permitAll()
            );

        return http.build();
    }
}
