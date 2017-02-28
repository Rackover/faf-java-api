package com.faforever.api.data.domain;

import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "login")
@Setter
public class User extends Login {

  private String password;
  private UserGroup userGroup;

  @Column(name = "password")
  public String getPassword() {
    return password;
  }

  @OneToOne(mappedBy = "user")
  public UserGroup getUserGroup() {
    return userGroup;
  }

  @Override
  public String toString() {
    return "User(" + getId() + ", " + getLogin() + ")";
  }
}
