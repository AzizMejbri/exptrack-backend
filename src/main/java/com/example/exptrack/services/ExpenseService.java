package com.example.exptrack.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.exptrack.models.Expense;
import com.example.exptrack.repositories.ExpenseRepository;

@Service
public class ExpenseService {

  @Autowired
  private ExpenseRepository expenseRep;

  @Autowired
  private UserService userService;

  @Transactional
  public List<Expense> getExpensesByUserId(Long userId) {
    return expenseRep.findByUser(userService.findById(userId));
  }

  @Transactional
  public Expense saveExpense(Expense expense) {
    return expenseRep.save(expense);
  }

  @Transactional
  public Expense getExpenseById(Long expenseId) {
    return expenseRep
        .findById(expenseId)
        .orElseThrow(() -> new RuntimeException("Transaction Invalid!"));
  }
}
