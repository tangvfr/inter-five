package fr.miage.orleans.m2.interop.tp.authentification.controleur;

import fr.miage.orleans.m2.interop.tp.authentification.model.User;
import fr.miage.orleans.m2.interop.tp.authentification.model.exception.PasswordIncorrectException;
import fr.miage.orleans.m2.interop.tp.authentification.model.exception.UserInexistantException;
import fr.miage.orleans.m2.interop.tp.authentification.service.UserService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/auth")
public class ControleurAuth {
  protected static final Logger logger = LoggerFactory.getLogger(ControleurAuth.class);
  private final PasswordEncoder passwordEncoder;
  private final Function<User, String> genereTokenFunction;
  private final UserService userService;

  public ControleurAuth(
      PasswordEncoder passwordEncoder,
      Function<User, String> genereTokenFunction,
      UserService userService) {
    this.passwordEncoder = passwordEncoder;
    this.genereTokenFunction = genereTokenFunction;
    this.userService = userService;
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req)
      throws UserInexistantException, PasswordIncorrectException {

    User user = userService.connection(req.username, req.password);
    String token = genereTokenFunction.apply(user);
    logger.info("Nouveau user connecter : {}", user.getMail());
    return ResponseEntity.ok(new LoginResponse(token, "Bearer"));
  }

  @PostMapping("/register")
  public ResponseEntity<User> inscrire(
      @RequestBody @Valid RegisterRequest req, UriComponentsBuilder base) {

    if (userService.userExists(req.username())) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
    User user = new User(req.username, req.password);
    userService.createUser(user);
    logger.info("Nouveau user créer : {}", user.getMail());
    URI location = base.path("/utilisateurs/{id}").buildAndExpand(user.getIdUser()).toUri();
    return ResponseEntity.created(location).body(user);
  }

  @GetMapping("/profil/{id}")
  @PreAuthorize("#id == authentication.id")
  public ResponseEntity<User> getProfile(@PathVariable Long id) throws UserInexistantException {

    User user = this.userService.getUser(id);
    return ResponseEntity.ok(user);
  }

  record LoginRequest(String username, String password) {}

  record LoginResponse(String accessToken, String tokenType) {}

  record RegisterRequest(
      @Length(min = 4, max = 25) String username,
      @Length(min = 4, max = 25) String password,
      @NotNull String roles) {}
}
