package com.examly.springapp.repository;

import com.examly.springapp.model.CourseVideo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data access for the video lessons of courses.
 *
 * @author Lokesh
 */
@Repository
public interface CourseVideoRepo extends JpaRepository<CourseVideo, Long> {
    /** Lessons of a course in playing order. */
    List<CourseVideo> findByCourse_CourseIdOrderByOrderIndexAscVideoIdAsc(Long courseId);

    List<CourseVideo> findByCourse_CourseId(Long courseId);

    long countByCourse_Instructor_UserId(Long userId);
}
