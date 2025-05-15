package com.covolt.backend.core.security.service;

import com.covolt.backend.core.model.User;
import com.covolt.backend.core.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        logger.debug("Attempting to load user by email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + email));

        logger.debug("User found: {}. Checking roles.", user.getUsername());

        if (user.getRoles() == null) {
            logger.error("CRITICAL ERROR: User.getRoles() is NULL for user: {}", user.getUsername());
            throw new UsernameNotFoundException("Kullanıcı rolleri bulunamadı: " + email);
        }

        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .toList();

        logger.debug("Loaded {} authorities for user: {}", authorities.size(), user.getUsername());

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(!user.isEnabled())
                .accountLocked(!user.isEnabled())
                .credentialsExpired(!user.isEnabled())
                .disabled(!user.isEnabled())
                .build();
    }
}