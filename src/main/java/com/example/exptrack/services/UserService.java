package com.example.exptrack.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.exptrack.models.User;
import com.example.exptrack.repositories.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class UserService {

  @Autowired
  UserRepository userRep;
  @Autowired
  PasswordEncoder passwordEncoder;

  @Transactional
  public List<User> getUsers() {
    return userRep.findAll();
  }

  @Transactional
  public User saveUser(User user) {
    user.setPassword(passwordEncoder.encode(user.getPassword()));
    return userRep.save(user);
  }

  @Transactional
  public User findById(Long userId) {
    return userRep
        .findById(userId)
        .orElseThrow(() -> new RuntimeException("unkown user"));
  }

  @Transactional
  public User findByEmail(String email) {
    return userRep
        .findByEmail(email)
        .orElseThrow(() -> new RuntimeException("unable to find a user by the given Email!"));
  }

  @Transactional
  public void deleteById(Long id) {
    userRep.deleteById(id);
  }

}
