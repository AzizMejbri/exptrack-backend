package com.example.exptrack.repositories;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.exptrack.dtos.CategorySummaryDTO;
import com.example.exptrack.models.Revenue;
import com.example.exptrack.models.User;

@Repository
public interface RevenueRepository extends JpaRepository<Revenue, Long> {
  public List<Revenue> findByUser(User user);

  List<Revenue> findByUserIdOrderByCreationDateDesc(Long userId);

  List<Revenue> findByUserIdAndCreationDateBetweenOrderByCreationDateDesc(
      Long userId, Date startDate, Date endDate);

  @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Revenue r WHERE r.user.id = :userId AND r.creationDate BETWEEN :startDate AND :endDate")
  Double sumRevenueByUserAndDateRange(
      @Param("userId") Long userId,
      @Param("startDate") Date startDate,
      @Param("endDate") Date endDate);

  @Query("SELECT COUNT(r) FROM Revenue r WHERE r.user.id = :userId AND r.creationDate BETWEEN :startDate AND :endDate")
  Long countRevenueByUserAndDateRange(
      @Param("userId") Long userId,
      @Param("startDate") Date startDate,
      @Param("endDate") Date endDate);

  @Query("SELECT new com.example.exptrack.dtos.CategorySummaryDTO(" +
      "r.source, " +
      "COALESCE(SUM(r.amount), 0), " +
      "'revenue', " +
      "0.0) " +
      "FROM Revenue r " +
      "WHERE r.user.id = :userId " +
      "AND r.creationDate BETWEEN :startDate AND :endDate " +
      "GROUP BY r.source")
  List<CategorySummaryDTO> getRevenueCategorySummary(
      @Param("userId") Long userId,
      @Param("startDate") Date startDate,
      @Param("endDate") Date endDate);
}
