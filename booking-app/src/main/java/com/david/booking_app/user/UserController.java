package com.david.booking_app.user;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.david.booking_app.common.NotFoundException;
import com.david.booking_app.security.CurrentUserService;

@RestController
@RequestMapping("/users")
public class UserController {

  private final UserRepository userRepository;
  private final CurrentUserService currentUser;

  public UserController(UserRepository userRepository, CurrentUserService currentUser) {
    this.userRepository = userRepository;
    this.currentUser = currentUser;
  }

  @GetMapping
  public List<User> getAll() {
    return userRepository.findAll();
  }

  @GetMapping("/me")
  public User me(@AuthenticationPrincipal OAuth2User principal) {
    return currentUser.get(principal);
  }

  @GetMapping("/{id}")
  public User getUser(@PathVariable Long id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("User " + id + " not found"));
  }
}
