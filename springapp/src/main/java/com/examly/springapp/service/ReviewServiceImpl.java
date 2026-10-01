package com.examly.springapp.service;

import com.examly.springapp.model.Customer;
import com.examly.springapp.model.Review;

import com.examly.springapp.repository.CustomerRepo;
import com.examly.springapp.repository.ReviewRepo;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepo reviewRepo;
    private final CustomerRepo customerRepo;

    public ReviewServiceImpl(
            ReviewRepo reviewRepo,
            CustomerRepo customerRepo) {

        this.reviewRepo = reviewRepo;
        this.customerRepo = customerRepo;
    }

    @Override
    public Review addReview(Review review) {

        if (review.getCustomer() != null
                && review.getCustomer().getCustomerId() != null) {

            Long customerId =
                    review.getCustomer().getCustomerId();

            Customer customer =
                    customerRepo.findById(customerId)
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Customer not found"
                                    )
                            );

            review.setCustomer(customer);
        }

        return reviewRepo.save(review);
    }

    @Override
    public List<Review> getAllReviews() {
        return reviewRepo.findAll();
    }

    @Override
    public Optional<Review> getReviewById(Long reviewId) {
        return reviewRepo.findById(reviewId);
    }
}