package com.david.booking_app.user;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Users")
public class User {

  @Id()
  @GeneratedValue
  private Long id;

  @Column(nullable=false, unique=true)
  private String email;

  @Column(nullable=false)
  private String name;

  /** Identifier from the OAuth provider, e.g. "github:12345". */
  @JsonIgnore
  @Column(unique=true)
  private String oauthSubject;

  private String avatarUrl;

  protected User() {
    //jpa
  }

  public User(String oauthSubject, String name, String email, String avatarUrl) {
    this.oauthSubject = oauthSubject;
    this.name = name;
    this.email = email;
    this.avatarUrl = avatarUrl;
  }

  public void updateProfile(String name, String email, String avatarUrl) {
    this.name = name;
    this.email = email;
    this.avatarUrl = avatarUrl;
  }

  public String getEmail() {
      return email;
  }

  public Long getId() {
      return id;
  }

  public String getName() {
      return name;
  }

  public String getAvatarUrl() {
      return avatarUrl;
  }

  public String getOauthSubject() {
      return oauthSubject;
  }
}
