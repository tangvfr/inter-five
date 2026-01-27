package fr.miage.orleans.m2.interop.tp.match.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/match")
public class MatchController {

    @GetMapping("{idMatch}")
    public ResponseEntity<?> createMatch() {
        return ResponseEntity.ok().build();
    }

}
