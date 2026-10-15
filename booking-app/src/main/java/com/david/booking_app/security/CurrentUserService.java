package com.david.booking_app.security;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.david.booking_app.user.User;
import com.david.booking_app.user.UserRepository;

/**
 * Maps a GitHub login to an application {@link User}, creating or refreshing
 * the user record from the GitHub profile.
 */
@Service
public class CurrentUserService {

  private final UserRepository users;

  public CurrentUserService(UserRepository users) {
    this.users = users;
  }

  public static String subjectOf(OAuth2User principal) {
    return "github:" + principal.getAttribute("id");
  }

  @Transactional
  public User sync(OAuth2User principal) {
    String subject = subjectOf(principal);
    String login = principal.getAttribute("login");
    String avatarUrl = principal.getAttribute("avatar_url");
    String name = orElse(principal.getAttribute("name"), login);
    // GitHub hides private emails; fall back to the user's noreply address
    String email = orElse(
        principal.getAttribute("email"),
        principal.getAttribute("id") + "+" + login + "@users.noreply.github.com");

    User user = users.findByOauthSubject(subject)
        .or(() -> users.findByEmailIgnoreCase(email).filter(u -> u.getOauthSubject() == null))
        .orElse(null);

    if (user == null) {
      return users.save(new User(subject, name, email, avatarUrl));
    }
    user.updateProfile(name, email, avatarUrl);
    return user;
  }

  private static String orElse(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  public User get(OAuth2User principal) {
    return users.findByOauthSubject(subjectOf(principal))
        .orElseGet(() -> sync(principal));
  }
}
