package com.examly.springapp.repository;

import com.examly.springapp.model.InstructorProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data access for the profiles instructors fill in while registering.
 *
 * @author Lokesh
 */
@Repository
public interface InstructorProfileRepo extends JpaRepository<InstructorProfile, Long> {
    Optional<InstructorProfile> findByUser_UserId(Long userId);

    void deleteByUser_UserId(Long userId);
}
