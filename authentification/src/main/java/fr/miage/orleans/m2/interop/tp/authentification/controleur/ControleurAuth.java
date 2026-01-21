package fr.miage.orleans.m2.interop.tp.authentification.controleur;

import fr.miage.orleans.m2.interop.tp.authentification.model.Role;
import fr.miage.orleans.m2.interop.tp.authentification.model.User;
import fr.miage.orleans.m2.interop.tp.authentification.model.exception.PasswordIncorrectException;
import fr.miage.orleans.m2.interop.tp.authentification.model.exception.UserInexistantException;
import fr.miage.orleans.m2.interop.tp.authentification.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.function.Function;

@RestController
@RequestMapping("/api/auth")
public class ControleurAuth {
    protected static final Logger logger = LoggerFactory.getLogger(ControleurAuth.class);
    private final Function<User, String> genereTokenFunction;
    private final UserService userService;

    public ControleurAuth(
            Function<User, String> genereTokenFunction,
            UserService userService) {
        this.genereTokenFunction = genereTokenFunction;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req)
            throws UserInexistantException, PasswordIncorrectException {

        User user = userService.connection(req.username, req.password);
        String token = genereTokenFunction.apply(user);
        logger.info("Nouvel utilisateur connecté : {}", user.getMail());
        return ResponseEntity.ok(new LoginResponse(token, "Bearer"));
    }

    @PostMapping("/register")
    public ResponseEntity<User> inscrire(
            @RequestBody @Valid RegisterRequest req, UriComponentsBuilder base) {

        if (userService.userExists(req.username())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        User user = new User(req.username(), req.password());
        
        // Gérer les rôles depuis la chaîne de caractères
        if (req.roles() != null && !req.roles().trim().isEmpty()) {
            String[] roleNames = req.roles().split(",");
            for (String roleName : roleNames) {
                try {
                    Role role = Role.valueOf(roleName.trim().toUpperCase());
                    user.addRole(role);
                } catch (IllegalArgumentException e) {
                    // Ignorer les rôles invalides
                }
            }
        }
        
        userService.createUser(user);
        logger.info("Nouvel utilisateur créé : {}", user.getMail());
        URI location = base.path("/api/auth/profil/{id}").buildAndExpand(user.getIdUser()).toUri();
        return ResponseEntity.created(location).body(user);
    }

    @GetMapping("/profil/{id}")
    @PreAuthorize("#id == authentication.id")
    public ResponseEntity<User> getProfile(@PathVariable Long id) throws UserInexistantException {

        User user = this.userService.getUser(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/profil")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<List<User>> getAllProfil() {
        return ResponseEntity.ok(this.userService.getAllUser());
    }
    @PutMapping("/profil/{id}")
    @PreAuthorize("#id == authentication.id")
    public ResponseEntity<User> updateProfil(
            @PathVariable Long id, @RequestBody UpdateProfileRequest req) throws UserInexistantException {

        User existingUser = this.userService.getUser(id);
        existingUser.setMail(req.mail());
        this.userService.updateUser(existingUser);
        return ResponseEntity.ok(existingUser);
    }
    @PutMapping("/profil/{id}/changePassword")
    @PreAuthorize("#id == authentication.id")
    public ResponseEntity updatePassword(
            @PathVariable Long id, @RequestBody ChangePasswordRequest req) throws UserInexistantException, PasswordIncorrectException {

        this.userService.changePassword(id, req.oldPassword(), req.newPassword());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/profil/{id}")
    @PreAuthorize("#id == authentication.id")
    public ResponseEntity<String> deleteProfil(@PathVariable Long id) throws UserInexistantException {
        this.userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    record LoginRequest(String username, String password) {
    }

    record LoginResponse(String accessToken, String tokenType) {
    }

    record RegisterRequest(
            @Length(min = 4, max = 25) String username,
            @Length(min = 4, max = 25) String password,
            @NotNull String roles) {
    }

    record UpdateProfileRequest(
            @Length(min = 4, max = 50) String mail) {
    }

    record ChangePasswordRequest(
            @Length(min = 4, max = 25) String oldPassword,
            @Length(min = 4, max = 25) String newPassword) {
    }
}
