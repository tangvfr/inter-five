package fr.miage.orleans.m2.interop.tp.authentification.service;

import fr.miage.orleans.m2.interop.tp.authentification.dao.UserDao;
import fr.miage.orleans.m2.interop.tp.authentification.model.User;
import fr.miage.orleans.m2.interop.tp.authentification.model.exception.PasswordIncorrectException;
import fr.miage.orleans.m2.interop.tp.authentification.model.exception.UserInexistantException;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class UserService {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserDao userDao, PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }

    public User connection(String username, String password)
            throws UserInexistantException, PasswordIncorrectException {

        User user = this.loadUserByUsername(username);
        if (passwordEncoder.matches(password, user.getPassword())) {
            return user;
        }
        throw new PasswordIncorrectException("Le mot de passe est incorrect pour ce username");
    }

    @Transactional
    public void createUser(User user) {
        this.userDao.save(user);
    }

    /**
     * Met à jour tous les éléments de l'utilisateur (sauf son mot de passe)
     * @param nouveauUtilisateur
     * @return
     * @throws UserInexistantException
     */
    @Transactional
    public User updateUser(User nouveauUtilisateur) throws UserInexistantException {

        this.userDao.save(nouveauUtilisateur);
        return this.getUser(nouveauUtilisateur.getIdUser());
    }

    @Transactional
    public void deleteUser(Long idU) throws UserInexistantException {
        this.userDao.deleteById(idU);
    }

    @Transactional
    public void changePassword(Long idU, String oldPassword, String newPassword) throws UserInexistantException, PasswordIncorrectException {
        User user = this.getUser(idU);
        if (passwordEncoder.matches(oldPassword, user.getPassword())) {
            user.setPassword(newPassword);
            this.userDao.save(user);
            return;
        }
        throw new PasswordIncorrectException("Le mot de passe est incorrect");
    }

    public boolean userExists(String username) {
        return userDao.findUserByMail(username).isPresent();
    }

    public List<User> getAllUser() {
        return StreamSupport.stream(userDao.findAll().spliterator(), false)
                .collect(Collectors.toList());
    }

    public User getUser(Long idU) throws UserInexistantException {
        return userDao.findById(idU)
                .orElseThrow(() -> new UserInexistantException("Utilisateur non trouvé avec l'ID: " + idU));
    }

    public User loadUserByUsername(String username) throws UserInexistantException {
        return userDao.findUserByMail(username)
                .orElseThrow(() -> new UserInexistantException("Utilisateur non trouvé avec le username: " + username));
    }
}
