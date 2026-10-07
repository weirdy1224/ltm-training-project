package com.examly.springapp.repository;

import com.examly.springapp.model.Orders;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Data access for orders, including the counts and sums used by the dashboards.
 *
 * @author Tanvi
 * @author Meruva Lokesh (dashboard queries)
 */
@Repository
public interface OrderRepo extends JpaRepository<Orders, Long> {
    List<Orders> findByCustomer_CustomerId(Long customerId);
    List<Orders> findByCourses_CourseId(Long courseId);
    /** True when the customer bought the course (used to unlock its video lessons). */
    boolean existsByCustomer_CustomerIdAndCourses_CourseId(Long customerId, Long courseId);
    /** Enrollments in an instructor's courses (one per course per order). */
    @Query("SELECT COUNT(c) FROM Orders o JOIN o.courses c WHERE c.instructor.userId = :instructorId")
    long countEnrollmentsByInstructorId(@Param("instructorId") Long instructorId);
    /** Different learners who bought at least one course of the instructor. */
    @Query("SELECT COUNT(DISTINCT o.customer.customerId) FROM Orders o JOIN o.courses c WHERE c.instructor.userId = :instructorId")
    long countLearnersByInstructorId(@Param("instructorId") Long instructorId);
    /** Sum of the prices of the instructor's courses over all enrollments, or null when there are none. */
    @Query("SELECT SUM(c.coursePrice) FROM Orders o JOIN o.courses c WHERE c.instructor.userId = :instructorId")
    Double sumRevenueByInstructorId(@Param("instructorId") Long instructorId);

    /** Number of orders a customer has placed. */
    long countByCustomer_CustomerId(Long customerId);

    /** Number of different courses a customer bought (the same course in two orders counts once). */
    @Query("SELECT COUNT(DISTINCT c.courseId) FROM Orders o JOIN o.courses c WHERE o.customer.customerId = :customerId")
    long countDistinctCoursesByCustomerId(@Param("customerId") Long customerId);

    /** Total money a customer spent, or null when they have no orders. */
    @Query("SELECT SUM(o.orderPrice) FROM Orders o WHERE o.customer.customerId = :customerId")
    Double sumOrderPriceByCustomerId(@Param("customerId") Long customerId);

    /** Revenue of the whole platform, or null when there are no orders. */
    @Query("SELECT SUM(o.orderPrice) FROM Orders o")
    Double sumAllOrderPrice();
}