package fr.miage.orleans.m2.interop.tp.authentification.controleur;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.function.Function;
import org.hibernate.validator.constraints.Length;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/auth")
public class ControleurAuth {
  protected static final Logger logger = LoggerFactory.getLogger(ControleurAuth.class);
  private final PasswordEncoder passwordEncoder;
  private final Function<UserDetails, String> genereTokenFunction;
  private final UserDetailsManager users;

  public ControleurAuth(
      PasswordEncoder passwordEncoder,
      Function<UserDetails, String> genereTokenFunction,
      UserDetailsManager users) {
    this.passwordEncoder = passwordEncoder;
    this.genereTokenFunction = genereTokenFunction;
    this.users = users;
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req)
      throws UsernameNotFoundException {
    if (users.userExists(req.username())) {
      // l'utilisateur existe : on teste son mot de passe
      String name = req.username();
      UserDetails user = users.loadUserByUsername(name);
      logger.info("Nouveau user connecter : {}", user.getUsername());
      if (passwordEncoder.matches(req.password(), user.getPassword())) {
        String token = genereTokenFunction.apply(user);
        // (optionnel) expose TTL si tu veux
        return ResponseEntity.ok(new LoginResponse(token, "Bearer"));
      }
    }
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
  }

  @PostMapping("/utilisateurs")
  @PreAuthorize("hasRole('ENSEIGNANT')")
  public ResponseEntity<UserDetails> inscrire(
      @RequestBody @Valid RegisterRequest req, UriComponentsBuilder base) {
    if (users.userExists(req.username())) {
      // l'utilisateur existe déjà : conflit
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
    UserDetails user =
        User.builder()
            .username(req.username())
            .password(passwordEncoder.encode(req.password()))
            .roles(req.roles().split(","))
            .build();
    users.createUser(user);
    URI location =
        base.path("/api/utilisateurs/{username}").buildAndExpand(user.getUsername()).toUri();
    return ResponseEntity.created(location).body(user);
  }

  @GetMapping("/utilisateurs/{username}")
  @PreAuthorize("hasRole('ENSEIGNANT') or #username == authentication.name")
  public ResponseEntity<UserDetails> getProfile(@PathVariable String username) {
    UserDetails user = this.users.loadUserByUsername(username);
    return ResponseEntity.ok(user);
  }

  // demonstration d'API, sur un autre serveur
  @GetMapping("/hello")
  @PreAuthorize("hasRole('ETUDIANT')")
  public ResponseEntity<String> demo() {
    return ResponseEntity.ok("Hello World");
  }

  @GetMapping("/hellochef")
  @PreAuthorize("hasRole('ENSEIGNANT')")
  public ResponseEntity<String> demoAdmin() {
    return ResponseEntity.ok("CHEF oui CHEF !");
  }

  record LoginRequest(String username, String password) {}

  record LoginResponse(String accessToken, String tokenType) {}

  record RegisterRequest(
      @Length(min = 4, max = 25) String username,
      @Length(min = 4, max = 25) String password,
      @NotNull String roles) {}
}
