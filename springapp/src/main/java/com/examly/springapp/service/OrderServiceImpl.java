package com.examly.springapp.service;

import com.examly.springapp.exception.DatabaseOperationException;
import com.examly.springapp.exception.DuplicateResourceException;
import com.examly.springapp.exception.InvalidRequestException;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.Course;
import com.examly.springapp.model.Customer;
import com.examly.springapp.model.Orders;
import com.examly.springapp.repository.CourseRepo;
import com.examly.springapp.repository.CustomerRepo;
import com.examly.springapp.repository.OrderRepo;
import com.examly.springapp.model.Enrollment;
import com.examly.springapp.model.NotificationType;
import com.examly.springapp.repository.EnrollmentRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Order rules: an order needs a customer and at least one existing course, and
 * its price is always
 * computed on the server. Repository failures are wrapped into
 * DatabaseOperationException.
 *
 * @author Tanvi
 */
@Service
public class OrderServiceImpl implements OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    private final OrderRepo orderRepo;
    private final CustomerRepo customerRepo;
    private final CourseRepo courseRepo;
    private final NotificationService notificationService;
    private final EnrollmentRepository enrollmentRepository;

    public OrderServiceImpl(OrderRepo orderRepo, CustomerRepo customerRepo, CourseRepo courseRepo,
                            NotificationService notificationService, EnrollmentRepository enrollmentRepository) {
        this.orderRepo = orderRepo;
        this.customerRepo = customerRepo;
        this.courseRepo = courseRepo;
        this.notificationService = notificationService;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    public Orders addOrder(Orders order) {
        log.trace("addOrder entered");
        if (order.getCustomer() == null || order.getCustomer().getCustomerId() == null) {
            throw new InvalidRequestException("A customer id is required");
        }
        if (order.getCourses() == null || order.getCourses().isEmpty()) {
            throw new InvalidRequestException("An order needs at least one course");
        }
        Long customerId = order.getCustomer().getCustomerId();
        Customer customer = DatabaseOperationException
                .guard("loading a customer", () -> customerRepo.findById(customerId))
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + customerId));
        // NEW: a course can be bought only once. Collect what this customer has already
        // bought.
        Set<Long> alreadyBought = new HashSet<>();
        DatabaseOperationException.guard("loading orders", () -> orderRepo.findByCustomer_CustomerId(customerId))
                .forEach(o -> o.getCourses().forEach(c -> alreadyBought.add(c.getCourseId())));
        Set<Long> inThisOrder = new HashSet<>();
        List<Course> courses = new ArrayList<>();
        double total = 0;
        for (Course c : order.getCourses()) {
            if (c == null || c.getCourseId() == null) {
                throw new InvalidRequestException("Each course must have a courseId");
            }
            Long courseId = c.getCourseId();
            Course found = DatabaseOperationException.guard("loading a course", () -> courseRepo.findById(courseId))
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found with id " + courseId));
            // NEW: refuse a course the customer already owns, and the same course twice in
            // one order
            if (alreadyBought.contains(courseId) || !inThisOrder.add(courseId)) {
                throw new DuplicateResourceException("You are already enrolled in " + found.getCourseType());
            }
            courses.add(found);
            total += found.getCoursePrice() == null ? 0 : found.getCoursePrice();
        }
        log.debug("addOrder: customerId={}, courses={}, computed total={}", customerId, courses.size(), total);
        order.setOrderId(null);
        order.setCustomer(customer);
        order.setCourses(courses);
        order.setOrderPrice(total); // price is always computed on the server
        Orders saved = DatabaseOperationException.guard("saving an order", () -> orderRepo.save(order));
        log.info("Order {} created for customer {}", saved.getOrderId(), customerId);

        // Notify student of course purchase & notify instructors of new student enrollment
        for (Course c : courses) {
            try {
                // Secondary enrollment table record
                Enrollment enrollment = new Enrollment();
                enrollment.setUserId(customer.getUser() != null ? customer.getUser().getUserId() : customerId);
                enrollment.setCourseId(c.getCourseId());
                enrollment.setPurchaseDate(LocalDateTime.now());
                enrollmentRepository.save(enrollment);
            } catch (Exception ex) {
                log.warn("Failed to create secondary enrollment row: {}", ex.getMessage());
            }

            try {
                // 1. Notification for student: course purchase
                if (customer.getUser() != null && customer.getUser().getUserId() != null) {
                    notificationService.createNotification(
                            customer.getUser().getUserId(),
                            "Course Enrolled",
                            "You have successfully purchased and enrolled in " + c.getCourseType() + ".",
                            NotificationType.COURSE_PURCHASE,
                            "/watch-course/" + c.getCourseId()
                    );
                }
            } catch (Exception ex) {
                log.warn("Failed to send student course purchase notification: {}", ex.getMessage());
            }

            try {
                // 2. Notification for instructor: new student enrollment
                if (c.getInstructor() != null && c.getInstructor().getUserId() != null) {
                    notificationService.createNotification(
                            c.getInstructor().getUserId(),
                            "New Student Enrollment",
                            customer.getCustomerName() + " has enrolled in your course \"" + c.getCourseType() + "\".",
                            NotificationType.NEW_STUDENT_ENROLLMENT,
                            "/instructor/courses"
                    );
                }
            } catch (Exception ex) {
                log.warn("Failed to send instructor enrollment notification: {}", ex.getMessage());
            }
        }

        return saved;
    }

    @Override
    public List<Orders> getAllOrders() {
        log.trace("getAllOrders entered");
        List<Orders> orders = DatabaseOperationException.guard("loading orders", () -> orderRepo.findAll());
        log.debug("getAllOrders returned {} order(s)", orders.size());
        return orders;
    }

    @Override
    public Orders getOrderById(Long orderId) {
        log.trace("getOrderById entered for orderId={}", orderId);
        return DatabaseOperationException.guard("loading an order", () -> orderRepo.findById(orderId))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id " + orderId));
    }

    @Override
    public List<Orders> getOrdersByCustomerId(Long customerId) {
        log.trace("getOrdersByCustomerId entered for customerId={}", customerId);
        List<Orders> orders = DatabaseOperationException.guard("loading orders",
                () -> orderRepo.findByCustomer_CustomerId(customerId));
        log.debug("getOrdersByCustomerId: {} order(s) for customer {}", orders.size(), customerId);
        if (orders.isEmpty()) {
            throw new ResourceNotFoundException("No orders found for customer id " + customerId);
        }
        return orders;
    }

    @Override
    public Orders deleteOrder(Long orderId) {
        log.trace("deleteOrder entered for orderId={}", orderId);
        Orders order = getOrderById(orderId);
        DatabaseOperationException.guardVoid("deleting an order", () -> orderRepo.delete(order));
        log.info("Order {} deleted", orderId);
        return order;
    }
}
