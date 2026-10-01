package com.examly.springapp.controller;

import com.examly.springapp.model.Course;
import com.examly.springapp.service.CourseService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/course")
public class CourseController {

    private final CourseService courseService;

    public CourseController(
            CourseService courseService) {

        this.courseService = courseService;
    }

    @PostMapping
    public ResponseEntity<Course> addCourse(
            @RequestBody Course course) {

        Course savedCourse =
                courseService.addCourse(course);

        return new ResponseEntity<>(
                savedCourse,
                HttpStatus.CREATED
        );
    }

    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {

        return ResponseEntity.ok(
                courseService.getAllCourses()
        );
    }

    @GetMapping("/courses/{courseId}")
    public ResponseEntity<Course> getCourseById(
            @PathVariable Long courseId) {

        return courseService
                .getCourseById(courseId)
                .map(ResponseEntity::ok)
                .orElseGet(
                        () -> ResponseEntity.notFound().build()
                );
    }
}