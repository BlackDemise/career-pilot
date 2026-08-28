package blackdemise.cp.security;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import blackdemise.cp.user.User;
import blackdemise.cp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Seeds the single dev/basic-auth user on first startup (MVP has no registration flow yet).
@Slf4j
@Component
@RequiredArgsConstructor
public class DevUserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DevUserProperties devUserProperties;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        User devUser = new User();
        devUser.setUsername(devUserProperties.username());
        devUser.setPassword(passwordEncoder.encode(devUserProperties.password()));
        userRepository.save(devUser);

        log.info("Created default dev user '{}'", devUserProperties.username());
    }
}
