package com.examly.springapp.service;

import com.examly.springapp.model.Course;

import java.util.List;
import java.util.Optional;

public interface CourseService {

    Course addCourse(Course course);

    List<Course> getAllCourses();

    Optional<Course> getCourseById(Long courseId);
}
