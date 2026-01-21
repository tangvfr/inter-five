package fr.miage.orleans.m2.interop.tp.authentification.service;

import fr.miage.orleans.m2.interop.tp.authentification.dao.UserDao;
import fr.miage.orleans.m2.interop.tp.authentification.model.User;
import fr.miage.orleans.m2.interop.tp.authentification.model.exception.PasswordIncorrectException;
import fr.miage.orleans.m2.interop.tp.authentification.model.exception.UserInexistantException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
    if (passwordEncoder.matches(user.getPassword(), password)) {
      return user;
    }
    throw new PasswordIncorrectException("Le mot de passe est incorrect pour ce username");
  }

  @Transactional
  public void createUser(User user) {
    this.userDao.save(user);
  }

  @Transactional
  public User updateUser(User newUser) throws UserInexistantException {
    this.userDao.save(newUser);
    return this.getUser(newUser.getIdUser());
  }

  @Transactional
  public void deleteUser(Long idU) throws UserInexistantException {
    this.userDao.deleteById(idU);
  }

  public void changePassword(String oldPassword, String newPassword) {}

  public boolean userExists(String username) {
    return userDao.findUserByMail(username).isPresent();
  }

  public List<User> getAllUser() {
    return StreamSupport.stream(userDao.findAll().spliterator(), false)
        .collect(Collectors.toList());
  }

  public User getUser(Long idU) throws UserInexistantException {
    return userDao.findById(idU).orElseThrow();
  }

  public User loadUserByUsername(String username) throws UserInexistantException {
    return userDao.findUserByMail(username).orElseThrow(UserInexistantException);
  }
}
