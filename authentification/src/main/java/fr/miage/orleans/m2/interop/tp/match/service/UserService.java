package fr.miage.orleans.m2.interop.tp.authentification.service;

import fr.miage.orleans.m2.interop.tp.authentification.dao.UserDao;
import fr.miage.orleans.m2.interop.tp.authentification.model.User;
import fr.miage.orleans.m2.interop.tp.authentification.model.exception.UserInexistantException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class UserService {

    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    @Transactional
    public void createUser(String mail, String passwordEncoded) {
        User newUser = new User(mail, passwordEncoded);
        this.userDao.save(newUser);
    }

    @Transactional
    public User updateUser(Long idU, String mail, String passwordEncoded) throws UserInexistantException {
        User newUser = new User(idU, mail, passwordEncoded);
        this.userDao.save(newUser);
        return this.getUser(idU);
    }

    @Transactional
    public void deleteUser(Long idU) throws UserInexistantException {
        this.userDao.deleteById(idU);
    }

    public List<User> getAllUser() {
        return StreamSupport
                .stream(userDao.findAll().spliterator(), false)
                .collect(Collectors.toList());
    }

    public User getUser(Long idU) throws UserInexistantException {
        return userDao.findById(idU)
                .orElseThrow();
    }
}
