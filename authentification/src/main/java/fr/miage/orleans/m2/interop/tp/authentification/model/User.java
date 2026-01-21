package fr.miage.orleans.m2.interop.tp.authentification.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(schema = "USER")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @Column(name = "ID_USER")
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long idUser;
    @Column(name = "MAIL")
    private String mail;
    @Column(name = "PASSWORD")
    @JsonIgnore
    private String password;

    public User(String mail, String password) {
        this.mail = mail;
        this.password = password;
    }

}
