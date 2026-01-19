package fr.miage.orleans.m2.interop.tp.authentification.controleur;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/profil")
public class ControleurProfil {

    private final UserDetailsManager users;

    public ControleurProfil(UserDetailsManager users) {
        this.users = users;
    }


    @GetMapping("")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<List<UserDetails>> getAllProfil() {
        return ResponseEntity.ok(users.toString());
    }

    @GetMapping("/{id}")
    // @PreAuthorize("hasRole('ENSEIGNANT')")
    public ResponseEntity<UserDetails> getProfil(@PathVariable Long id) {
        return ResponseEntity.ok(users.loadUserByUsername(id));

    }
}
