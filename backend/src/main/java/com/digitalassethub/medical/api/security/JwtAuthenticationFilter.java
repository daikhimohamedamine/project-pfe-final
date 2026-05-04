package com.digitalassethub.medical.api.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, @org.springframework.context.annotation.Lazy UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);
        if (!jwtService.isValid(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String username = jwtService.extractSubject(jwt);
            var userDetails = userDetailsService.loadUserByUsername(username);
            System.out.println("DEBUG AUTH: User=" + username + ", Authorities=" + userDetails.getAuthorities());
            if (userDetails.getAuthorities().isEmpty()) {
                System.out.println("WARNING AUTH: User has NO authorities!");
            }
            var token = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(token);
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            System.out.println("DEBUG AUTH: JWT token user not found - " + e.getMessage());
        } catch (Exception e) {
            System.out.println("DEBUG AUTH: JWT token error - " + e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
}
