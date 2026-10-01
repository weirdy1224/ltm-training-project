package com.examly.springapp.service;

import com.examly.springapp.model.Course;
import com.examly.springapp.model.Customer;
import com.examly.springapp.model.Orders;

import com.examly.springapp.repository.CourseRepo;
import com.examly.springapp.repository.CustomerRepo;
import com.examly.springapp.repository.OrderRepo;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepo orderRepo;
    private final CustomerRepo customerRepo;
    private final CourseRepo courseRepo;

    public OrderServiceImpl(
            OrderRepo orderRepo,
            CustomerRepo customerRepo,
            CourseRepo courseRepo) {

        this.orderRepo = orderRepo;
        this.customerRepo = customerRepo;
        this.courseRepo = courseRepo;
    }

    @Override
    public Orders addOrder(Orders order) {

        if (order.getCustomer() != null
                && order.getCustomer().getCustomerId() != null) {

            Long customerId =
                    order.getCustomer().getCustomerId();

            Customer customer =
                    customerRepo.findById(customerId)
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Customer not found"
                                    )
                            );

            order.setCustomer(customer);
        }

        if (order.getCourses() != null) {

            List<Course> courses = new ArrayList<>();

            for (Course course : order.getCourses()) {

                Course existingCourse =
                        courseRepo.findById(course.getCourseId())
                                .orElseThrow(
                                        () -> new RuntimeException(
                                                "Course not found"
                                        )
                                );

                courses.add(existingCourse);
            }

            order.setCourses(courses);
        }

        return orderRepo.save(order);
    }

    @Override
    public List<Orders> getAllOrders() {
        return orderRepo.findAll();
    }

    @Override
    public Optional<Orders> getOrderById(Long orderId) {
        return orderRepo.findById(orderId);
    }

    @Override
    public List<Orders> getOrdersByCustomerId(Long customerId) {
        return orderRepo.findByCustomerCustomerId(customerId);
    }
}