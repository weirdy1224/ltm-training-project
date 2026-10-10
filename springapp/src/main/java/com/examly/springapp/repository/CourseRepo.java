package com.examly.springapp.repository;

import com.examly.springapp.model.Course;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface CourseRepo extends JpaRepository<Course, Long> {

        @Query("SELECT c FROM Course c WHERE "
                        + "(LOWER(c.courseType) LIKE :pattern ESCAPE '!' OR LOWER(c.courseDetails) LIKE :pattern ESCAPE '!') "
                        + "AND c.coursePrice >= :minPrice AND c.coursePrice <= :maxPrice")
        Page<Course> search(@Param("pattern") String pattern,
                        @Param("minPrice") Double minPrice,
                        @Param("maxPrice") Double maxPrice,
                        Pageable pageable);

        /** The courses owned by one instructor, newest first. */
        List<Course> findByInstructor_UserIdOrderByCourseIdDesc(Long userId);

        /** True when some course still has this type (ignoring case). */
        boolean existsByCourseTypeIgnoreCase(String courseType);
}