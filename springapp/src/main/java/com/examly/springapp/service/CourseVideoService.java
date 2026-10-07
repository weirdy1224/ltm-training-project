package com.examly.springapp.service;

import com.examly.springapp.dto.CourseVideoDTO;
import com.examly.springapp.dto.VideoUpdateRequest;
import java.nio.file.Path;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * Video lessons of courses: upload (file in a folder, details in MySQL), list, edit, delete and secure streaming.
 * Who may do what: admins and the owner instructor manage a course's videos; admins, the owner instructor and
 * customers who bought the course may watch them.
 *
 * @author Lokesh
 */
public interface CourseVideoService {
    /** Where a streamed file is and how to describe it to the browser. */
    record VideoFile(Path path, String contentType, long size) {
    }

    /** Saves the file and a database row. AccessDeniedException when the caller does not own the course. */
    CourseVideoDTO upload(Long courseId, MultipartFile file, String title, String description, Integer orderIndex);

    /** The lessons of a course in playing order, each flagged with whether the caller may watch it. */
    List<CourseVideoDTO> listForCourse(Long courseId);

    /** Renames a lesson or changes its description / position. */
    CourseVideoDTO update(Long videoId, VideoUpdateRequest request);

    /** Removes the database row and the file. */
    void delete(Long videoId);

    /** Removes every lesson of a course (used when the course itself is deleted). No access check: the caller already did it. */
    void deleteAllForCourse(Long courseId);

    /** A short-lived signed token that lets the caller stream this one video. AccessDeniedException when not allowed. */
    String createStreamToken(Long videoId);

    /** Checks the token and finds the file to stream. */
    VideoFile openForStreaming(Long videoId, String token);
}
