package fr.orleans.miage.m2.aar.authentification;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// TODO refaire les tests
@SpringBootTest
@AutoConfigureMockMvc
class AuthentificationApplicationTests {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PasswordEncoder passwordEncoder;

    @MockitoBean
    Function<UserDetails, String> genereTokenFunction;

    @MockitoBean(name = "users")
    UserDetailsManager userDetailsManager;


    @Autowired
    ObjectMapper objectMapper;

    //@Test
    void testLogin() throws Exception {

        String email = "test@univ-orleans.fr";
        String rawPassword = "passwordtestclair";
        String encodedPassword = "passwordtestencode";

        LoginRequest loginDTO = new LoginRequest(email, rawPassword);

        UserDetails userDetails = User.withUsername(email).password(encodedPassword).roles("ETUDIANT").build();

        when(userDetailsManager.userExists(email)).thenReturn(true);
        when(userDetailsManager.loadUserByUsername(email)).thenReturn(userDetails);
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
        when(genereTokenFunction.apply(userDetails)).thenReturn("tokenValue");

        mvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("tokenValue"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    //@Test
    void testLoginUtilisateurInexistant() throws Exception {

        String email = "test@univ-orleans.fr";
        String rawPassword = "passwordtestclair";

        LoginRequest loginDTO = new LoginRequest(email, rawPassword);

        when(userDetailsManager.userExists(email)).thenReturn(false);

        mvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isUnauthorized());
    }

    //@Test
    void testLoginMotDePasseInvalide() throws Exception {

        String email = "test@univ-orleans.fr";
        String rawPassword = "passwordtestclair";
        String encodedPassword = "passwordtestencode";

        LoginRequest loginDTO = new LoginRequest(email, rawPassword);

        UserDetails userDetails = User.withUsername(email).password(encodedPassword).roles("ETUDIANT").build();

        when(userDetailsManager.userExists(email)).thenReturn(true);
        when(userDetailsManager.loadUserByUsername(email)).thenReturn(userDetails);
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(false);

        mvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isUnauthorized());
    }

    record LoginRequest(String username, String password) {}
}
