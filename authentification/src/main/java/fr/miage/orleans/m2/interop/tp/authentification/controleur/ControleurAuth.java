package fr.miage.orleans.m2.interop.tp.authentification.controleur;

import fr.miage.orleans.m2.interop.tp.authentification.model.User;
import fr.miage.orleans.m2.interop.tp.authentification.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import java.util.function.Function;
import org.apache.http.auth.InvalidCredentialsException;
import org.hibernate.validator.constraints.Length;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/auth")
public class ControleurAuth {
  protected static final Logger logger = LoggerFactory.getLogger(ControleurAuth.class);
  private final Function<User, String> generateTokenFunction;
  private final UserService userService;

  public ControleurAuth(Function<User, String> generateTokenFunction, UserService userService) {
    this.generateTokenFunction = generateTokenFunction;
    this.userService = userService;
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req)
      throws InvalidCredentialsException {

    logger.debug("Login attempt for user: {}", req.username);
    User user = userService.login(req.username, req.password);
    String token = generateTokenFunction.apply(user);
    logger.info("Nouvel utilisateur connecté : {}", user.getMail());
    return ResponseEntity.ok(new LoginResponse(token, "Bearer"));
  }

  @PostMapping("/register")
  public ResponseEntity<User> register(
      @RequestBody @Valid RegisterRequest req, UriComponentsBuilder base)
      throws InvalidCredentialsException {

    logger.debug("Registration attempt for user: {}", req.username);
    User user = userService.createUser(req.username(), req.password());
    logger.info("Nouvel utilisateur créé : {}", user.getMail());
    URI location = base.path("/auth/profil/{id}").buildAndExpand(user.getIdUser()).toUri();
    return ResponseEntity.created(location).body(user);
  }

  @GetMapping("/profil/{id}")
  @PreAuthorize("#id == authentication.principal.claims['idUtilisateur']")
  public ResponseEntity<User> getProfile(@PathVariable Long id) throws InvalidCredentialsException {

    logger.debug("Profile access attempt for user ID: {}", id);
    User user = this.userService.getUser(id);
    return ResponseEntity.ok(user);
  }

  @GetMapping("/profil")
  @PreAuthorize("hasRole('ENSEIGNANT')")
  public ResponseEntity<List<User>> getAllProfiles() {
    logger.debug("All profiles access attempt by teacher");
    return ResponseEntity.ok(this.userService.getAllUsers());
  }

  @PutMapping("/profil/{id}")
  @PreAuthorize("#id == authentication.principal.claims['idUtilisateur']")
  public ResponseEntity<User> updateProfile(
      @PathVariable Long id, @RequestBody UpdateProfileRequest req)
      throws InvalidCredentialsException {

    logger.debug("Profile update attempt for user ID: {}", id);
    User existingUser = this.userService.getUser(id);
    existingUser.setMail(req.mail());
    this.userService.updateUser(existingUser);
    return ResponseEntity.ok(existingUser);
  }

  @PutMapping("/profil/{id}/changePassword")
  @PreAuthorize("#id == authentication.principal.claims['idUtilisateur']")
  public ResponseEntity updatePassword(
      @PathVariable Long id, @RequestBody ChangePasswordRequest req)
      throws InvalidCredentialsException {

    logger.debug("Password change attempt for user ID: {}", id);
    this.userService.changePassword(id, req.oldPassword(), req.newPassword());
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/profil/{id}")
  @PreAuthorize("#id == authentication.principal.claims['idUtilisateur']")
  public ResponseEntity<String> deleteProfile(@PathVariable Long id)
      throws InvalidCredentialsException {
    logger.debug("Profile deletion attempt for user ID: {}", id);
    this.userService.deleteUser(id);
    return ResponseEntity.noContent().build();
  }

  record LoginRequest(String username, String password) {}

  record LoginResponse(String accessToken, String tokenType) {}

  record RegisterRequest(
      @Length(min = 4, max = 25) String username,
      @Length(min = 4, max = 25) String password,
      @NotNull String roles) {}

  record UpdateProfileRequest(@Length(min = 4, max = 50) String mail) {}

  record ChangePasswordRequest(
      @Length(min = 4, max = 25) String oldPassword,
      @Length(min = 4, max = 25) String newPassword) {}
}
