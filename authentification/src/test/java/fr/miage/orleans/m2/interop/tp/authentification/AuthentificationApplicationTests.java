package fr.miage.orleans.m2.interop.tp.authentification;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthentificationApplicationTests {

  @Autowired MockMvc mvc;

  @MockitoBean PasswordEncoder passwordEncoder;

  @MockitoBean Function<UserDetails, String> generateTokenFunction;

  @MockitoBean(name = "users")
  UserDetailsManager userDetailsManager;

  @Test
  @WithMockUser(
      username = "test@univ-orleans.fr",
      roles = {"ETUDIANT"})
  void testGetProfileUnauthorized() throws Exception {
    // Attempt to access another user's profile
    mvc.perform(
            get("/auth/profil/2") // Assuming current user has ID 1
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden()); // Should be denied by @PreAuthorize
  }

  @Test
  @WithMockUser(
      username = "teacher@univ-orleans.fr",
      roles = {"ENSEIGNANT"})
  void testGetAllProfilesAsTeacher() throws Exception {
    mvc.perform(get("/auth/profil").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }

  @Test
  void testGetAllProfilesUnauthorized() throws Exception {
    // No auth
    mvc.perform(get("/auth/profil").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(
      username = "student@univ-orleans.fr",
      roles = {"ETUDIANT"})
  void testGetAllProfilesAsStudent() throws Exception {
    // Student should not access all profiles
    mvc.perform(get("/auth/profil").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }
}
