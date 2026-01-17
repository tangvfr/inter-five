package fr.miage.orleans.m2.interop.tp.authentification.config;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.kv.model.PutParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Component
public class KeyPairManager {

    private final ConsulClient consulClient;

    @Value("${jwt.key.private-path:/app/keys/private_key.pem}")
    private String privateKeyPath;

    @Value("${jwt.key.public-path:/app/keys/public_key.pem}")
    private String publicKeyPath;

    @Value("${jwt.key.consul-path:config/jwt/public-key}")
    private String consulKeyPath;

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    public KeyPairManager(ConsulClient consulClient) {
        this.consulClient = consulClient;
    }

    @PostConstruct
    public void initialize() throws Exception {
        log.info("=== Initialisation du gestionnaire de clés JWT ===");

        // Attendre que Consul soit disponible
        waitForConsul();

        // Créer le répertoire des clés s'il n'existe pas
        Path keysDir = Paths.get(privateKeyPath).getParent();
        if (keysDir != null && !Files.exists(keysDir)) {
            Files.createDirectories(keysDir);
            log.info("Répertoire de clés créé : {}", keysDir);
        }

        // Vérifier si les clés existent déjà
        if (Files.exists(Paths.get(privateKeyPath)) && Files.exists(Paths.get(publicKeyPath))) {
            log.info("Chargement des clés existantes...");
            loadExistingKeys();
        } else {
            log.info("Génération d'une nouvelle paire de clés RSA 2048 bits...");
            generateNewKeyPair();
        }

        // Publier la clé publique dans Consul
        publishPublicKeyToConsul();

        log.info("=== Initialisation terminée avec succès ===");
    }

    private void waitForConsul() {
        int maxAttempts = 10;
        int attemptDelay = 2000; // 2 secondes

        for (int i = 0; i < maxAttempts; i++) {
            try {
                consulClient.getStatusLeader();
                log.info("✓ Consul est disponible");
                return;
            } catch (Exception e) {
                log.warn("Tentative {}/{}: En attente de Consul...", i + 1, maxAttempts);
                try {
                    Thread.sleep(attemptDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Attente de Consul interrompue", ie);
                }
            }
        }
        throw new RuntimeException(
                "Consul n'est pas disponible après " + maxAttempts + " tentatives");
    }

    private void generateNewKeyPair() throws Exception {
        // Générer la paire de clés RSA 2048 bits
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048, new SecureRandom());
        KeyPair keyPair = keyGen.generateKeyPair();

        this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
        this.publicKey = (RSAPublicKey) keyPair.getPublic();

        // Sauvegarder les clés sur le disque
        savePrivateKey(this.privateKey);
        savePublicKey(this.publicKey);

        log.info("✓ Nouvelle paire de clés générée et sauvegardée");
        log.info("  - Clé privée : {}", privateKeyPath);
        log.info("  - Clé publique : {}", publicKeyPath);
    }

    private void loadExistingKeys() throws Exception {
        this.privateKey = loadPrivateKeyFromFile(privateKeyPath);
        this.publicKey = loadPublicKeyFromFile(publicKeyPath);
        log.info("✓ Clés existantes chargées avec succès");
    }

    private void savePrivateKey(RSAPrivateKey key) throws IOException {
        String privateKeyPEM = convertToPEM(key.getEncoded(), "PRIVATE KEY");
        Files.writeString(Paths.get(privateKeyPath), privateKeyPEM);

        // Sécuriser les permissions (Linux uniquement)
        try {
            Runtime.getRuntime().exec("chmod 600 " + privateKeyPath);
            log.debug("Permissions 600 appliquées sur la clé privée");
        } catch (Exception e) {
            log.debug("Impossible de modifier les permissions (probablement Windows)");
        }
    }

    private void savePublicKey(RSAPublicKey key) throws IOException {
        String publicKeyPEM = convertToPEM(key.getEncoded(), "PUBLIC KEY");
        Files.writeString(Paths.get(publicKeyPath), publicKeyPEM);
    }

    private void publishPublicKeyToConsul() {
        try {
            String publicKeyPEM = convertToPEM(publicKey.getEncoded(), "PUBLIC KEY");

            // Publier dans Consul
            PutParams putParams = new PutParams();
            consulClient.setKVValue(consulKeyPath, publicKeyPEM, putParams);

            log.info("✓ Clé publique publiée dans Consul");
            log.info("  - Chemin Consul : {}", consulKeyPath);
            log.info("  - Taille : {} caractères", publicKeyPEM.length());

        } catch (Exception e) {
            log.error("✗ Erreur lors de la publication de la clé publique dans Consul", e);
            throw new RuntimeException("Impossible de publier la clé publique", e);
        }
    }

    private String convertToPEM(byte[] keyBytes, String type) {
        String base64 = Base64.getEncoder().encodeToString(keyBytes);
        StringBuilder pem = new StringBuilder();
        pem.append("-----BEGIN ").append(type).append("-----\n");

        // Ajouter des retours à la ligne tous les 64 caractères (standard PEM)
        for (int i = 0; i < base64.length(); i += 64) {
            pem.append(base64, i, Math.min(i + 64, base64.length())).append("\n");
        }

        pem.append("-----END ").append(type).append("-----\n");
        return pem.toString();
    }

    private RSAPrivateKey loadPrivateKeyFromFile(String path) throws Exception {
        String keyPEM = Files.readString(Paths.get(path));
        keyPEM = keyPEM.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(keyPEM);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) kf.generatePrivate(spec);
    }

    private RSAPublicKey loadPublicKeyFromFile(String path) throws Exception {
        String keyPEM = Files.readString(Paths.get(path));
        keyPEM = keyPEM.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(keyPEM);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(spec);
    }

    public RSAPrivateKey getPrivateKey() {
        if (privateKey == null) {
            throw new IllegalStateException("La clé privée n'est pas initialisée");
        }
        return privateKey;
    }

    public RSAPublicKey getPublicKey() {
        if (publicKey == null) {
            throw new IllegalStateException("La clé publique n'est pas initialisée");
        }
        return publicKey;
    }
}
