package com.david.booking_app.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      @Value("${app.frontend-url}") String frontendUrl) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/error").permitAll()
            .anyRequest().authenticated())
        // The SPA calls the API with fetch, so answer 401 instead of redirecting to GitHub
        .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .oauth2Login(oauth -> oauth
            .defaultSuccessUrl(frontendUrl + "/", true)
            .failureUrl(frontendUrl + "/?loginError"))
        .logout(logout -> logout.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()))
        // XSRF-TOKEN cookie, sent back by the frontend as the X-XSRF-TOKEN header
        .csrf(csrf -> csrf.spa());

    return http.build();
  }

  /** Creates/updates the application user every time someone logs in. */
  @Bean
  OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService(CurrentUserService currentUser) {
    DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    return request -> {
      OAuth2User user = delegate.loadUser(request);
      currentUser.sync(user);
      return user;
    };
  }
}
