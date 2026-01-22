package fr.miage.orleans.m2.interop.tp.authentification.dao;

import fr.miage.orleans.m2.interop.tp.authentification.model.User;
import org.springframework.data.repository.CrudRepository;

public interface UserDao extends CrudRepository<User, Long> {
}
