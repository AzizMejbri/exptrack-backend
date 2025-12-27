
package com.example.exptrack.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.exptrack.models.Revenue;
import com.example.exptrack.repositories.RevenueRepository;

@Service
public class RevenueService {

  @Autowired
  private RevenueRepository revenueRep;

  @Autowired
  private UserService userService;

  @Transactional
  public List<Revenue> getRevenuesByUserId(Long userId) {
    return revenueRep.findByUser(userService.findById(userId));
  }

  @Transactional
  public Revenue saveRevenue(Revenue revenue) {
    return revenueRep.save(revenue);
  }

  @Transactional
  public Revenue getRevenueById(Long revenueId) {
    return revenueRep
        .findById(revenueId)
        .orElseThrow(() -> new RuntimeException("Invalid Transaction!!"));
  }
}
