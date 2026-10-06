package com.sliit.helpdesk.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Form-login sessions keep the user from the moment they signed in.
 * This reloads the account so a soft-delete or hard-delete takes effect on the next request.
 */
public class DisabledAccountFilter extends OncePerRequestFilter {

    private final UserDetailsService userDetailsService;

    public DisabledAccountFilter(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getName() != null
                && !"anonymousUser".equals(authentication.getName())) {
            try {
                if (!userDetailsService.loadUserByUsername(authentication.getName()).isEnabled()) {
                    new SecurityContextLogoutHandler().logout(request, response, authentication);
                }
            } catch (UsernameNotFoundException ex) {
                new SecurityContextLogoutHandler().logout(request, response, authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}
