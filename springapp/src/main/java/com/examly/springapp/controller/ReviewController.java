package com.examly.springapp.controller;

import com.examly.springapp.model.Review;
import com.examly.springapp.service.ReviewService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(
            ReviewService reviewService) {

        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<Review> addReview(
            @RequestBody Review review) {

        Review savedReview =
                reviewService.addReview(review);

        return new ResponseEntity<>(
                savedReview,
                HttpStatus.CREATED
        );
    }

    @GetMapping
    public ResponseEntity<List<Review>> getAllReviews() {

        return ResponseEntity.ok(
                reviewService.getAllReviews()
        );
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<Review> getReviewById(
            @PathVariable Long reviewId) {

        return reviewService
                .getReviewById(reviewId)
                .map(ResponseEntity::ok)
                .orElseGet(
                        () -> ResponseEntity.notFound().build()
                );
    }
}