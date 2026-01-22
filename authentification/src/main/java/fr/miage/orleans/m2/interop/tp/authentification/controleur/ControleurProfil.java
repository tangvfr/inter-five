package fr.miage.orleans.m2.interop.tp.authentification.controleur;

import fr.miage.orleans.m2.interop.tp.authentification.model.User;
import fr.miage.orleans.m2.interop.tp.authentification.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profil")
public class ControleurProfil {

    private final UserService userService;

    public ControleurProfil(UserService userService) {
        this.userService = userService;
    }


    @GetMapping("")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<List<User>> getAllProfil() {
        return ResponseEntity.ok(this.userService.getAllUser());
    }

    @GetMapping("/{id}")
    @PreAuthorize("#id == authentication.name")
    public ResponseEntity<User> getProfil(@PathVariable Long id) {
        return ResponseEntity.ok(this.userService.getUser(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("#id == authentication.name")
    public ResponseEntity<User> updateProfil(@PathVariable Long id, @RequestBody ControleurAuth.LoginRequest req) {
        return ResponseEntity.ok(this.userService.updateUser(id, req.username(), req.password()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("#id == authentication.name")
    public ResponseEntity<String> deleteProfil(@PathVariable Long id) {
        this.userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
