package com.mblm.userservice.config.startup;

import com.mblm.userservice.model.Role;
import com.mblm.userservice.model.User;
import com.mblm.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Injection des valeurs depuis application.properties
    @Value("${admin.default.username}")
    private String adminUsername;

    @Value("${admin.default.email}")
    private String adminEmail;

    @Value("${admin.default.password}")
    private String adminPassword;

    // Remplacement de @RequiredArgsConstructor par un constructeur explicite
    public AdminUserInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Cette méthode sera exécutée par Spring Boot au démarrage de l'application.
     */
    @Override
    public void run(String... args) throws Exception {
        // On vérifie si un utilisateur avec le nom d'admin existe déjà
        if (userRepository.findByUsername(adminUsername).isPresent()) {
            logger.info("L'utilisateur administrateur existe déjà. Aucune action n'est requise.");
        } else {
            // S'il n'existe pas, on le crée
            logger.info("Aucun utilisateur administrateur trouvé. Création de l'utilisateur par défaut.");

            User adminUser = User.builder()
                    .username(adminUsername)
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword)) // On n'oublie pas de hacher le mot de passe !
                    .role(Role.ADMIN) // On lui assigne le rôle ADMIN
                    .build();

            userRepository.save(adminUser);
            logger.warn("*****************************************************************");
            logger.warn("Utilisateur ADMIN créé : username='{}', password='{}'", adminUsername, adminPassword);
            logger.warn("VEUILLEZ CHANGER CE MOT DE PASSE DANS UN ENVIRONNEMENT DE PRODUCTION");
            logger.warn("*****************************************************************");
        }
    }
}