package com.devlog.devlog_backend.security;

import com.devlog.devlog_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// WHY this class exists: Spring Security doesn't know about YOUR User entity.
// It only works with its own UserDetails interface.
// This bridge tells it: "to find a user, query our UserRepository by email."
@Service
@RequiredArgsConstructor  // Lombok: generates constructor injection for final fields
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Spring Security calls this during authentication.
        // Our User entity already implements UserDetails, so we can return it directly.
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with email: " + email)
                );
    }
}