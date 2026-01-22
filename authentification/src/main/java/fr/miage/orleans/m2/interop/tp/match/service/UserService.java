package fr.miage.orleans.m2.interop.tp.authentification.service;

import fr.miage.orleans.m2.interop.tp.authentification.dao.UserDao;
import fr.miage.orleans.m2.interop.tp.authentification.model.Role;
import fr.miage.orleans.m2.interop.tp.authentification.model.User;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.auth.InvalidCredentialsException;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service pour la gestion des utilisateurs dans le système d'authentification.
 * Fournit des opérations CRUD pour les utilisateurs, la validation des mots de passe,
 * et la gestion des rôles.
 */
@Slf4j
@Service
public class UserService {

  private final UserDao userDao;
  private final PasswordEncoder passwordEncoder;
  private final MessageSource messageSource;

  public UserService(UserDao userDao, PasswordEncoder passwordEncoder, MessageSource messageSource) {
    this.userDao = userDao;
    this.passwordEncoder = passwordEncoder;
    this.messageSource = messageSource;
  }

  /**
   * Authentifie un utilisateur avec son nom d'utilisateur et mot de passe.
   *
   * @param username le nom d'utilisateur (email)
   * @param password le mot de passe en clair
   * @return l'utilisateur authentifié
   * @throws InvalidCredentialsException si les identifiants sont incorrects
   */
  public User login(String username, String password)
      throws InvalidCredentialsException {

    User user = this.loadUserByUsername(username);
    if (passwordEncoder.matches(password, user.getPassword())) {
      return user;
    }
    log.debug("Failed login attempt for user: {}", username);
    log.debug(this.messageSource.getMessage("conflictUsername", new Object[]{username}, Locale.getDefault())); // ToDo make sure the link works well
    throw new InvalidCredentialsException(messageSource.getMessage("invalidPassword", null, Locale.getDefault()));
  }

  /**
   * Crée un nouvel utilisateur dans le système.
   * Valide que l'email n'existe pas et est valide.
   *
   * @param username l'email de l'utilisateur
   * @param password le mot de passe en clair
   * @return l'utilisateur créé
   * @throws InvalidCredentialsException si l'utilisateur existe déjà ou l'email est invalide
   */
  @Transactional
  public User createUser(String username, String password) throws InvalidCredentialsException {

    if (this.userExists(username) || !this.isEmailValid(username)) {
      log.debug("Attempt to create user with existing or invalid username: {}", username);
      log.debug("[US] - Username {} déjà pris dans l'application", username);
      throw new InvalidCredentialsException(messageSource.getMessage("usernameTakenOrInvalidEmail", null, Locale.getDefault()));
    }
    String passwordEncoded = passwordEncoder.encode(password);
    User newUser = new User(username, passwordEncoded);
    newUser.addRole(Role.FIVE);
    this.userDao.save(newUser);
    return this.loadUserByUsername(username);
  }

  /**
   * Met à jour tous les éléments de l'utilisateur (sauf son mot de passe)
   *
   * @param updatedUser
   * @return
   * @throws InvalidCredentialsException
   */
  @Transactional
  public User updateUser(User updatedUser) throws InvalidCredentialsException {

    // ToDo allow modifying everything except roles or password
    this.userDao.save(updatedUser);
    return this.getUser(updatedUser.getIdUser());
  }

  @Transactional
  public void deleteUser(Long userId) throws InvalidCredentialsException {
    this.userDao.delete(this.getUser(userId));
  }

  /**
   * Change le mot de passe d'un utilisateur après validation de l'ancien mot de passe.
   *
   * @param userId l'ID de l'utilisateur
   * @param oldPassword l'ancien mot de passe
   * @param newPassword le nouveau mot de passe
   * @throws InvalidCredentialsException si l'ancien mot de passe est incorrect
   */
  @Transactional
  public void changePassword(Long userId, String oldPassword, String newPassword)
      throws InvalidCredentialsException {
    User user = this.getUser(userId);
    if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
      log.debug("Failed password change for user ID: {}", userId);
      throw new InvalidCredentialsException(messageSource.getMessage("incorrectPassword", null, Locale.getDefault()));
    }
    String newPasswordEncoded = this.passwordEncoder.encode(newPassword);
    user.setPassword(newPasswordEncoded);
    this.userDao.save(user);
  }

  /**
   * Vérifie si un utilisateur existe avec le nom d'utilisateur donné.
   *
   * @param username le nom d'utilisateur à vérifier
   * @return true si l'utilisateur existe, false sinon
   */
  public boolean userExists(String username) {
    return userDao.findUserByMail(username).isPresent();
  }

  /**
   * Récupère tous les utilisateurs du système.
   *
   * @return la liste de tous les utilisateurs
   */
  public List<User> getAllUsers() {
    return StreamSupport.stream(userDao.findAll().spliterator(), false)
        .collect(Collectors.toList());
  }

  /**
   * Récupère un utilisateur par son ID.
   *
   * @param userId l'ID de l'utilisateur
   * @return l'utilisateur trouvé
   * @throws InvalidCredentialsException si l'utilisateur n'existe pas
   */
  public User getUser(Long userId) throws InvalidCredentialsException {
    return userDao
        .findById(userId)
        .orElseThrow(() -> {
          log.debug("User not found with ID: {}", userId);
          return new InvalidCredentialsException(messageSource.getMessage("userNotFoundById", new Object[]{userId}, Locale.getDefault()));
        });
  }

  /**
   * Charge un utilisateur par son nom d'utilisateur (email).
   *
   * @param username le nom d'utilisateur
   * @return l'utilisateur trouvé
   * @throws InvalidCredentialsException si l'utilisateur n'existe pas
   */
  public User loadUserByUsername(String username) throws InvalidCredentialsException {
    return userDao
        .findUserByMail(username)
        .orElseThrow(
            () -> {
              log.debug("User not found with username: {}", username);
              return new InvalidCredentialsException(
                  messageSource.getMessage("userNotFoundByUsername", new Object[]{username}, Locale.getDefault()));
            });
  }

  /**
   * Valide le format d'une adresse email.
   *
   * @param mail l'adresse email à valider
   * @return true si le format est valide, false sinon
   */
  private boolean isEmailValid(String mail) {
    return true; // mail.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\\\.[a-zA-Z]{2,}$");
  }
}
