package com.application.controller.pub;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.application.persistence.model.customer.Customer;
import com.application.persistence.model.restaurant.user.RestaurantPrivilege;
import com.application.persistence.model.restaurant.user.RestaurantUser;
import com.application.persistence.model.restaurant.user.RestaurantUserVerificationToken;
import com.application.service.RestaurantUserService;
import com.application.web.util.GenericResponse;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/public/register/user")
public class RegistrationRestaurantUserController {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private RestaurantUserService restaurantUserService;

    @Autowired
    private MessageSource messages;

    @Autowired
    private Environment env;

    public RegistrationRestaurantUserController() {
        super();
    }

    @RequestMapping(value = "/confirm_restaurant_user", method = RequestMethod.GET)
    public String confirmRestaurantUserRegistration(final HttpServletRequest request, final Model model, @RequestParam final String token) throws UnsupportedEncodingException {
        Locale locale = request.getLocale();
        final String result = restaurantUserService.validateVerificationToken(token);
        if (result.equals("valid")) {
            final RestaurantUser user = restaurantUserService.getRestaurantUser(token);
            // if (user.isUsing2FA()) {
            // model.addAttribute("qr", userService.generateQRUrl(user));
            // return "redirect:/qrcode.html?lang=" + locale.getLanguage();
            // }
            authWithoutPassword(user);
            model.addAttribute("message", messages.getMessage("message.accountVerified", null, locale));
            return "redirect:/console.html?lang=" + locale.getLanguage();
        }

        model.addAttribute("message", messages.getMessage("auth.message." + result, null, locale));
        model.addAttribute("expired", "expired".equals(result));
        model.addAttribute("token", token);
        return "redirect:/badUser.html?lang=" + locale.getLanguage();
    }

    // user activation - verification

    @RequestMapping(value = "/resendToken", method = RequestMethod.GET)
    @ResponseBody
    public GenericResponse resendRegistrationToken(final HttpServletRequest request, @RequestParam("token") final String existingToken) {
        final RestaurantUserVerificationToken newToken = restaurantUserService.generateNewVerificationToken(existingToken);
		restaurantUserService.getRestaurantUser(newToken.getToken());
        //TODO: rimettere l'invio della mail
        //mailSender.send(constructResendVerificationTokenEmail(getAppUrl(request), request.getLocale(), newToken, user));
        return new GenericResponse(messages.getMessage("message.resendToken", null, request.getLocale()));
    }


/*
    @RequestMapping(value = "/user/update/2fa", method = RequestMethod.POST)
    @ResponseBody
    public GenericResponse modifyUser2FA(@RequestParam("use2FA") final boolean use2FA) throws UnsupportedEncodingException {
        final User user = userService.updateUser2FA(use2FA);
        if (use2FA) {
            return new GenericResponse(userService.generateQRUrl(user));
        }
        return null;
    }*/

    // ============== NON-API ============
/*
    @SuppressWarnings("unused")
	private SimpleMailMessage constructResendVerificationTokenEmail(final String contextPath, final Locale locale, final VerificationToken newToken, final User user) {
        final String confirmationUrl = contextPath + "/registrationConfirm.html?token=" + newToken.getToken();
        final String message = messages.getMessage("message.resendToken", null, locale);
        return constructEmail("Resend Registration Token", message + " \r\n" + confirmationUrl, user);
    }*/

    @SuppressWarnings("unused")
	private SimpleMailMessage constructResetTokenEmail(final String contextPath, final Locale locale, final String token, final Customer user) {
        final String url = contextPath + "/user/changePassword?id=" + user.getId() + "&token=" + token;
        final String message = messages.getMessage("message.resetPassword", null, locale);
        return constructEmail("Reset Password", message + " \r\n" + url, user);
    }

    private SimpleMailMessage constructEmail(String subject, String body, Customer user) {
        final SimpleMailMessage email = new SimpleMailMessage();
        email.setSubject(subject);
        email.setText(body);
        email.setTo(user.getEmail());
        email.setFrom(env.getProperty("support.email"));
        return email;
    }

    public void authWithHttpServletRequest(HttpServletRequest request, String username, String password) {
        try {
            request.login(username, password);
        } catch (ServletException e) {
            LOGGER.error("Error while login ", e);
        }
    }

    /*

    public void authWithAuthManager(HttpServletRequest request, String username, String password) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password);
        authToken.setDetails(new WebAuthenticationDetails(request));
        Authentication authentication = authenticationManager.authenticate(authToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        // request.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());
    }
        */

    public void authWithoutPassword(RestaurantUser user) {
        List<RestaurantPrivilege> privileges = user.getRoles().stream().map(role -> role.getPrivileges()).flatMap(list -> list.stream()).distinct().collect(Collectors.toList());
        List<GrantedAuthority> authorities = privileges.stream().map(p -> new SimpleGrantedAuthority(p.getName())).collect(Collectors.toList());

        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
	



}
