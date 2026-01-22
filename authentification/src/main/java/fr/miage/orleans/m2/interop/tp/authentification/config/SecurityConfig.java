package fr.miage.orleans.m2.interop.tp.authentification.config;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import fr.miage.orleans.m2.interop.tp.authentification.model.Role;
import fr.miage.orleans.m2.interop.tp.authentification.model.User;
import java.time.Instant;
import java.util.Set;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final KeyPairManager keyPairManager;

    @Value("${security.jwt.issuer:auth-service}")
    String issuer;

    @Value("${security.jwt.expiration-minutes}")
    int expirationMinutes;

    public SecurityConfig(KeyPairManager keyPairManager) {
        this.keyPairManager = keyPairManager;
        log.info("SecurityConfig initialisé avec KeyPairManager");
    }

    private static String[] getRoles(Set<Role> roles) {
        return roles.stream().map(Role::name).toArray(String[]::new);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    // Removed JDBC connection creation to use only JPA

    @Bean
    SecurityFilterChain api(HttpSecurity http, JwtDecoder decoder, JwtAuthenticationConverter jac)
            throws Exception {
        http.securityMatcher("/auth/**")
                // .csrf(csrf -> csrf.disable())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        reg ->
                                reg.requestMatchers(HttpMethod.GET, "/actuator/health")
                                        .permitAll() // health check de consul
                                        .requestMatchers(HttpMethod.POST, "/auth/register")
                                        .permitAll() // inscription
                                        .requestMatchers(HttpMethod.POST, "/auth/login")
                                        .permitAll() // connexion
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(
                        oauth2 -> oauth2.jwt(jwt -> jwt.decoder(decoder).jwtAuthenticationConverter(jac)));
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(this.keyPairManager.getPublicKey()).build();
    }

    @Bean
    JwtEncoder jwtEncoder() {
        JWK jwk =
                new RSAKey.Builder(this.keyPairManager.getPublicKey())
                        .privateKey(this.keyPairManager.getPrivateKey())
                        .build();
        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwks);
    }

    // Convertit la claim "roles" en autorités ROLE_*
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        var conv = new JwtGrantedAuthoritiesConverter();
        conv.setAuthoritiesClaimName("roles");
        conv.setAuthorityPrefix("ROLE_");
        var jwtConv = new JwtAuthenticationConverter();
        jwtConv.setJwtGrantedAuthoritiesConverter(conv);
        return jwtConv;
    }

    @Bean
    Function<User, String> generateTokenFunction(JwtEncoder jwtEncoder) {
        return user -> {
            Instant now = Instant.now();
            String[] roles = getRoles(user.getRoles());
            JwtClaimsSet claims =
                    JwtClaimsSet.builder()
                            .issuer(issuer)
                            .issuedAt(now)
                            .expiresAt(now.plusSeconds(expirationMinutes * 60L))
                            .subject(user.getMail())
                            .claim("roles", roles)
                            // .claim("scope", roles)
                            .claim("idUtilisateur", String.valueOf(user.getIdUser()))
                            .build();
            return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        };
    }
}
