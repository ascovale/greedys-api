package com.application.admin.service.authentication;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.security.jwt.JwtUtil;
import com.application.common.web.dto.get.RUserDTO;
import com.application.common.web.dto.post.AuthResponseDTO;
import com.application.restaurant.RUserAuthenticationDetails;
import com.application.restaurant.persistence.dao.RUserDAO;
import com.application.restaurant.persistence.model.user.RUser;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AdminRUserAuthenticationService {

    private final RUserDAO RUserDAO;
    private final JwtUtil jwtUtil;

    /**
     * Admin login to restaurant user - allows admin to authenticate as a restaurant user
     * @param RUserId The ID of the restaurant user to authenticate as
     * @param request The HTTP servlet request
     * @return AuthResponseDTO containing JWT token and user details
     */
    public AuthResponseDTO adminLoginToRUser(Long RUserId, HttpServletRequest request) {
        RUser user = RUserDAO.findById(RUserId)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with ID: " + RUserId));

        // Creazione di un token di autenticazione con bypass della password
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                user.getUsername(), null);
        authToken.setDetails(
                new RUserAuthenticationDetails(true, user.getRestaurant().getId(), user.getEmail()));

        // Autenticazione senza password
        SecurityContextHolder.getContext().setAuthentication(authToken);

        final String jwt = jwtUtil.generateToken(user);
        return new AuthResponseDTO(jwt, new RUserDTO(user));
    }
}
