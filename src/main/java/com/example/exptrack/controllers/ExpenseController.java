package com.example.exptrack.controllers;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.exptrack.dtos.ExpenseRequest;
import com.example.exptrack.dtos.RevenueRequest;
import com.example.exptrack.models.Expense;
import com.example.exptrack.models.Revenue;
import com.example.exptrack.models.User;
import com.example.exptrack.services.ExpenseService;
import com.example.exptrack.services.RevenueService;
import com.example.exptrack.services.UserService;

@RestController
@RequestMapping("/api/public")
public class ExpenseController {

  @GetMapping("/")
  public String home() {
    return "Expense Tracker API is running!";
  }

  @GetMapping("/expenses")
  public String getExpenses() {
    return "<p>Expense list will be here</p>";
  }

  @GetMapping("/health")
  public String health() {
    return "API is healthy!";
  }

  @Autowired
  private UserService userService = new UserService();

  @GetMapping("/test/users")
  public List<User> getUsers() {
    return userService.getUsers();
  }

  @PostMapping("/test/users")
  public User postUser(@RequestBody User user) {
    return userService.saveUser(user);
  }

  @DeleteMapping("/test/users/{userId}")
  public User deleteUser(@PathVariable Long userId) {
    User user = userService.findById(userId);
    userService.deleteById(userId);
    return user;
  }

  @Autowired
  private ExpenseService expenseService = new ExpenseService();

  @Autowired
  private RevenueService revenueService = new RevenueService();

  @GetMapping("/test/{userId}/expenses")
  public List<Expense> getExepenses(@PathVariable Long userId) {
    return expenseService.getExpensesByUserId(userId);
  }

  @GetMapping("/test/{userId}/revenues")
  public List<Revenue> getRevenues(@PathVariable Long userId) {
    return revenueService.getRevenuesByUserId(userId);
  }

  @PostMapping("/test/{userId}/expenses")
  public Expense postExpense(@PathVariable Long userId, @RequestBody ExpenseRequest expenseReq) {
    Expense expense = new Expense(
        expenseReq.getAmount(),
        userService.findById(userId),
        new Date(),
        new Date(),
        expenseReq.getCategory());
    return expenseService.saveExpense(expense);
  }

  @PostMapping("/test/{userId}/revenues")
  public Revenue postRevenue(@PathVariable Long userId, @RequestBody RevenueRequest revenueReq) {
    Revenue revenue = new Revenue(
        revenueReq.getAmount(),
        userService.findById(userId),
        new Date(),
        new Date(),
        revenueReq.getSource());
    return revenueService.saveRevenue(revenue);
  }

  @PutMapping("/test/{userId}/expenses/{expenseId}")
  public Expense putExpense(@PathVariable Long userId,
      @PathVariable Long expenseId,
      @RequestBody ExpenseRequest expenseReq) {
    Expense expense = expenseService
        .getExpenseById(expenseId);
    Double newAmount = expenseReq.getAmount();
    String newCategory = expenseReq.getCategory();
    if (newAmount != null) {
      expense.setAmount(newAmount);
    }
    if (newCategory != null) {
      expense.setCategory(newCategory);
    }

    return expenseService.saveExpense(expense);
  }

  @PutMapping("/test/{userId}/revenues/{revenueId}")
  public Revenue putRevenue(@PathVariable Long userId,
      @PathVariable Long revenueId,
      @RequestBody RevenueRequest revenueRequest) {
    Revenue revenue = revenueService
        .getRevenueById(revenueId);
    Double newAmount = revenueRequest.getAmount();
    String newSource = revenueRequest.getSource();
    if (newAmount != null) {
      revenue.setAmount(newAmount);
    }
    if (newSource != null) {
      revenue.setSource(newSource);
    }

    return revenueService.saveRevenue(revenue);
  }

}
