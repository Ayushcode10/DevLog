package com.devlog.devlog_backend.config;

import com.devlog.devlog_backend.security.JwtAuthFilter;
import com.devlog.devlog_backend.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

// WHY @Configuration: tells Spring "this class contains bean definitions".
// Spring scans it at startup and registers all @Bean methods into the
// application context (the container that manages all your objects).

// WHY @EnableWebSecurity: activates Spring Security for this application.
// Without this, Spring Boot would apply its default security config
// which locks down everything with a generated password — not what we want.

// WHY @RequiredArgsConstructor: Lombok generates a constructor that
// injects all 'final' fields automatically. This is constructor injection,
// which is preferred over @Autowired field injection because:
// 1. Dependencies are explicit and visible
// 2. The class can't be instantiated without its dependencies
// 3. Easier to unit test (you can pass mocks via constructor)
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // WHY these two dependencies:
    // JwtAuthFilter — our custom filter that reads and validates the JWT
    //                 on every incoming request before any controller runs.
    // UserDetailsServiceImpl — tells Spring Security HOW to load a user
    //                          from our database given an email address.
    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    // ─────────────────────────────────────────────────────────────────
    // BEAN 1: SecurityFilterChain
    // This is the CORE security configuration. It answers the question:
    // "What rules apply to every HTTP request that hits our server?"
    // ─────────────────────────────────────────────────────────────────
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ── CSRF ──────────────────────────────────────────────────
                // WHY disable CSRF:
                // CSRF (Cross-Site Request Forgery) attacks work by tricking
                // a browser into making requests using the victim's COOKIES.
                // We are NOT using cookies — we use Bearer tokens in the
                // Authorization header. Browsers don't automatically send
                // headers cross-site, so CSRF is not a threat here.
                // Disabling it removes unnecessary complexity.
                .csrf(csrf -> csrf.disable())

                // ── CORS ──────────────────────────────────────────────────
                // WHY configure CORS here and NOT just with @CrossOrigin:
                // Spring Security runs its filter chain BEFORE your controllers.
                // When a browser makes a cross-origin request (React on port 5173
                // calling Spring on port 8080), it first sends a "preflight"
                // OPTIONS request to ask "is this origin allowed?".
                // If CORS isn't configured at the Security level, Spring Security
                // blocks that OPTIONS request with a 403 BEFORE it ever reaches
                // your @CrossOrigin annotation — so @CrossOrigin alone doesn't work.
                // Configuring CORS here ensures preflight requests are handled correctly.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ── SESSION MANAGEMENT ────────────────────────────────────
                // WHY STATELESS:
                // Traditional web apps store session data on the server
                // (who is logged in, what's in their cart, etc.).
                // With JWT, we don't need this — the token itself carries
                // the user's identity on every request.
                // STATELESS tells Spring: "never create or use an HttpSession".
                // Benefits:
                // - Server uses less memory (no session storage)
                // - Works perfectly with multiple server instances (horizontal scaling)
                // - Every request is fully self-contained
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ── AUTHORIZATION RULES ───────────────────────────────────
                // WHY requestMatchers before anyRequest:
                // Rules are evaluated top-to-bottom. More specific rules must
                // come first. 'anyRequest().authenticated()' is the catch-all.
                //
                // /api/auth/** → permitAll():
                //   These are the register and login endpoints. They MUST be
                //   public — users can't send a token before they've logged in.
                //   The ** wildcard matches any sub-path:
                //   /api/auth/login, /api/auth/register, etc.
                //
                // anyRequest().authenticated():
                //   Every other endpoint (/api/entries, /api/tags, etc.)
                //   requires a valid JWT token. If no token is present,
                //   Spring Security returns 401 Unauthorized automatically.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated()
                )

                // ── AUTHENTICATION PROVIDER ───────────────────────────────
                // WHY wire authenticationProvider here:
                // This tells the SecurityFilterChain which provider to use
                // when verifying credentials (during login).
                // Our DaoAuthenticationProvider knows:
                //   - How to load a user (via UserDetailsServiceImpl)
                //   - How to verify a password (via BCryptPasswordEncoder)
                // Without this line, Spring would use a default in-memory provider
                // that knows nothing about our database or BCrypt.
                .authenticationProvider(authenticationProvider())

                // ── JWT FILTER PLACEMENT ──────────────────────────────────
                // WHY addFilterBefore:
                // Spring Security has its own default filter chain. We need
                // our JwtAuthFilter to run BEFORE the standard
                // UsernamePasswordAuthenticationFilter.
                //
                // The order matters:
                // 1. Request arrives
                // 2. JwtAuthFilter runs → extracts token → validates it →
                //    sets the authenticated user in SecurityContext
                // 3. UsernamePasswordAuthenticationFilter runs → sees that
                //    authentication is already set → skips its own logic
                // 4. Request reaches the controller with identity established
                //
                // If our filter ran AFTER, Spring would try its own auth
                // first, find no username/password form data, and reject it.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    // ─────────────────────────────────────────────────────────────────
    // BEAN 2: PasswordEncoder
    // Defines HOW passwords are hashed before storing in the database.
    // ─────────────────────────────────────────────────────────────────
    @Bean
    public PasswordEncoder passwordEncoder() {
        // WHY BCrypt specifically:
        // BCrypt is a password hashing algorithm with two key properties:
        //
        // 1. ONE-WAY: you cannot reverse a BCrypt hash back to the
        //    original password. To verify, you hash the input and
        //    compare hashes — you never decrypt.
        //
        // 2. SALTED: BCrypt automatically generates a random "salt"
        //    (random data mixed into the hash) for every password.
        //    This means two users with the same password "secret123"
        //    get completely different hashes — preventing rainbow table attacks.
        //
        // 3. SLOW BY DESIGN: BCrypt has a "cost factor" (default 10).
        //    It performs 2^10 = 1024 rounds of hashing on purpose.
        //    This makes brute-force attacks impractical — even if an
        //    attacker gets your database, cracking passwords takes too long.
        //
        // NEVER use: MD5, SHA1, SHA256 for passwords — they're too fast.
        // A modern GPU can try billions of MD5 hashes per second.
        // BCrypt limits attackers to a few hundred attempts per second.
        return new BCryptPasswordEncoder();
    }


    // ─────────────────────────────────────────────────────────────────
    // BEAN 3: AuthenticationManager
    // The entry point for triggering authentication programmatically.
    // ─────────────────────────────────────────────────────────────────
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        // WHY we need this bean:
        // In AuthService.login(), we call:
        //   authenticationManager.authenticate(
        //       new UsernamePasswordAuthenticationToken(email, password)
        //   )
        // This triggers the full Spring Security authentication process:
        //   1. Calls UserDetailsService.loadUserByUsername(email)
        //   2. Compares the provided password with the stored BCrypt hash
        //   3. Throws BadCredentialsException if they don't match
        //
        // Without exposing this as a @Bean, we can't inject it into AuthService.
        // Spring creates the actual implementation — we just expose it.
        return config.getAuthenticationManager();
    }


    // ─────────────────────────────────────────────────────────────────
    // BEAN 4: DaoAuthenticationProvider
    // Connects our UserDetailsService and PasswordEncoder into one
    // cohesive authentication strategy.
    // ─────────────────────────────────────────────────────────────────
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        // WHY Spring Boot 3.2+ uses the constructor approach:
        // Older versions used:
        //   new DaoAuthenticationProvider()       ← no-arg constructor
        //   provider.setUserDetailsService(...)   ← setter
        // Spring Boot 3.2 removed the no-arg constructor and setter
        // to enforce that PasswordEncoder is always provided upfront.
        // The provider is never in an "incomplete" state this way.
        //
        // WHY pass passwordEncoder() into the constructor:
        // DaoAuthenticationProvider needs to know WHICH hashing algorithm
        // to use when comparing the login password with the stored hash.
        // We pass our BCryptPasswordEncoder, so it knows to BCrypt-hash
        // the incoming password before comparison.
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        // WHY setUserDetailsService:
        // This tells the provider: "when you need to look up a user
        // by email, use THIS service (which queries our MySQL database)."
        // The provider calls userDetailsService.loadUserByUsername(email)
        // internally during authentication.
//        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }


    // ─────────────────────────────────────────────────────────────────
    // BEAN 5: CorsConfigurationSource
    // Defines exactly which cross-origin requests are allowed.
    // ─────────────────────────────────────────────────────────────────
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // WHY setAllowedOrigins (not setAllowedOriginPatterns):
        // We specify the exact origin of our React dev server.
        // In production, you'd change this to your deployed frontend URL.
        // Never use "*" (allow all) with allowCredentials=true —
        // browsers reject that combination for security reasons.
        config.setAllowedOrigins(List.of("http://localhost:5173"));

        // WHY include OPTIONS:
        // Browsers send an OPTIONS "preflight" request before any
        // cross-origin POST/PUT/DELETE to ask if it's allowed.
        // If OPTIONS isn't listed here, preflight requests fail
        // and the actual request never gets sent.
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // WHY allowedHeaders "*":
        // We need to allow the Authorization header (for our Bearer token)
        // and Content-Type (for JSON bodies). Using "*" allows all headers
        // which is fine for a development/learning project.
        config.setAllowedHeaders(List.of("*"));

        // WHY setAllowCredentials(true):
        // This allows the browser to include credentials (like auth headers)
        // in cross-origin requests. Required for our Authorization header to work.
        config.setAllowCredentials(true);

        // WHY UrlBasedCorsConfigurationSource:
        // This applies our CORS config to ALL endpoints ("/**").
        // You could apply different rules to different paths if needed.
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}