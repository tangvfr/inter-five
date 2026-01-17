package fr.miage.orleans.m2.interop.tp.authentification.config;

import static org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType.H2;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import fr.orleans.miage.m2.aar.authentification.model.Role;
import java.time.Instant;
import java.util.function.Function;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.jdbc.JdbcDaoImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final KeyPairManager keyPairManager;

    @Value("${security.jwt.issuer:auth-service}")
    String issuer;

    @Value("${security.jwt.expiration-minutes:60}")
    int expirationMinutes;

    public SecurityConfig(KeyPairManager keyPairManager) {
        this.keyPairManager = keyPairManager;
        log.info("SecurityConfig initialisé avec KeyPairManager");
    }

    private static String[] getRoles(String email) {
        String domain = (email.split("@"))[1];

        return switch (domain) {
            case "etu.univ-orleans.fr" -> new String[]{
                    Role.ETUDIANT.name()
            };
            case "univ-orleans.fr" -> new String[]{
                    Role.ETUDIANT.name(),
                    Role.ENSEIGNANT.name()
            };
            default -> new String[0];
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(H2)
                .addScript(JdbcDaoImpl.DEFAULT_USER_SCHEMA_DDL_LOCATION)
                .build();
    }

    // initialise la base et ajoute un utilisateur admin
    @Bean
    UserDetailsManager users(DataSource dataSource, PasswordEncoder passwordEncoder) {
        UserDetails admin = User.builder()
                .username("admin@univ-orleans.fr")
                .password(passwordEncoder.encode("admin"))
                .roles(Role.ENSEIGNANT.name())
                .build();
        JdbcUserDetailsManager users = new JdbcUserDetailsManager(dataSource);
        users.createUser(admin);
        return users;
    }

    @Bean
    SecurityFilterChain api(HttpSecurity http, JwtDecoder decoder, JwtAuthenticationConverter jac) throws Exception {
        http
                .securityMatcher("/api/**")
                //.csrf(csrf -> csrf.disable())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reg -> reg
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll() // health check de consul
                        .requestMatchers(HttpMethod.POST, "/api/utilisateurs").permitAll() // inscription
                        .requestMatchers(HttpMethod.POST, "/api/login").permitAll() // connexion
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(decoder).jwtAuthenticationConverter(jac)));
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(this.keyPairManager.getPublicKey()).build();
    }

    @Bean
    JwtEncoder jwtEncoder() {
        JWK jwk = new RSAKey.Builder(this.keyPairManager.getPublicKey()).privateKey(this.keyPairManager.getPrivateKey()).build();
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
    Function<UserDetails, String> genereTokenFunction(JwtEncoder jwtEncoder) {
        return user -> {
            Instant now = Instant.now();
            String[] roles = getRoles(user.getUsername());
            JwtClaimsSet claims = JwtClaimsSet.builder()
                    .issuer(issuer)
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(expirationMinutes * 60L))
                    .subject(user.getUsername())
                    .claim("roles", roles)
                    .claim("scope", roles)
                    .claim("idUtilisateur", String.valueOf(user.getUsername()))
                    .build();
            return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        };
    }
}
