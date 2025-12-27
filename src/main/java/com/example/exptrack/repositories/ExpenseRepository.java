package com.example.exptrack.repositories;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.exptrack.dtos.CategorySummaryDTO;
import com.example.exptrack.models.Expense;
import com.example.exptrack.models.User;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
  public List<Expense> findByUser(User user);

  List<Expense> findByUserIdOrderByCreationDateDesc(Long userId);

  List<Expense> findByUserIdAndCreationDateBetweenOrderByCreationDateDesc(
      Long userId, Date startDate, Date endDate);

  @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user.id = :userId AND e.creationDate BETWEEN :startDate AND :endDate")
  Double sumExpensesByUserAndDateRange(
      @Param("userId") Long userId,
      @Param("startDate") Date startDate,
      @Param("endDate") Date endDate);

  @Query("SELECT COUNT(e) FROM Expense e WHERE e.user.id = :userId AND e.creationDate BETWEEN :startDate AND :endDate")
  Long countExpensesByUserAndDateRange(
      @Param("userId") Long userId,
      @Param("startDate") Date startDate,
      @Param("endDate") Date endDate);

  @Query("SELECT new com.example.exptrack.dtos.CategorySummaryDTO(" +
      "e.category, " +
      "COALESCE(SUM(e.amount), 0), " +
      "'expense', " +
      "0.0) " +
      "FROM Expense e " +
      "WHERE e.user.id = :userId " +
      "AND e.creationDate BETWEEN :startDate AND :endDate " +
      "GROUP BY e.category")
  List<CategorySummaryDTO> getExpenseCategorySummary(
      @Param("userId") Long userId,
      @Param("startDate") Date startDate,
      @Param("endDate") Date endDate);
}
