package com.examly.springapp.service;

import com.examly.springapp.config.JwtUtil;
import com.examly.springapp.dto.CourseVideoDTO;
import com.examly.springapp.dto.VideoUpdateRequest;
import com.examly.springapp.exception.DatabaseOperationException;
import com.examly.springapp.exception.InvalidRequestException;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.Course;
import com.examly.springapp.model.CourseVideo;
import com.examly.springapp.model.Customer;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.CourseRepo;
import com.examly.springapp.repository.CourseVideoRepo;
import com.examly.springapp.repository.CustomerRepo;
import com.examly.springapp.repository.OrderRepo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Video lesson rules. The file goes to the video folder first and the database row second; if the row cannot be saved,
 * the file is removed again so no orphan files stay behind. Repository failures are wrapped into DatabaseOperationException.
 *
 * @author Lokesh
 */
@Service
public class CourseVideoServiceImpl implements CourseVideoService {
    private static final Logger log = LoggerFactory.getLogger(CourseVideoServiceImpl.class);
    private static final int MAX_TITLE = 150;
    private static final int MAX_DESCRIPTION = 1000;

    private final CourseVideoRepo videoRepo;
    private final CourseRepo courseRepo;
    private final OrderRepo orderRepo;
    private final CustomerRepo customerRepo;
    private final VideoStorageService storage;
    private final AccessService access;
    private final JwtUtil jwtUtil;
    private final long tokenMinutes;

    public CourseVideoServiceImpl(CourseVideoRepo videoRepo, CourseRepo courseRepo, OrderRepo orderRepo,
                                  CustomerRepo customerRepo, VideoStorageService storage, AccessService access,
                                  JwtUtil jwtUtil,
                                  @Value("${app.video.stream-token-minutes:120}") long tokenMinutes) {
        this.videoRepo = videoRepo;
        this.courseRepo = courseRepo;
        this.orderRepo = orderRepo;
        this.customerRepo = customerRepo;
        this.storage = storage;
        this.access = access;
        this.jwtUtil = jwtUtil;
        this.tokenMinutes = tokenMinutes;
    }

    @Override
    public CourseVideoDTO upload(Long courseId, MultipartFile file, String title, String description, Integer orderIndex) {
        log.trace("upload entered for courseId={}", courseId);
        Course course = loadCourse(courseId);
        access.requireCourseManager(course);   // admin, or the instructor who owns the course

        String cleanTitle = title == null ? "" : title.trim();
        if (cleanTitle.isEmpty()) {
            throw new InvalidRequestException("Title is required");
        }
        if (cleanTitle.length() > MAX_TITLE) {
            throw new InvalidRequestException("Title is too long (maximum " + MAX_TITLE + " characters)");
        }
        String cleanDescription = description == null ? null : description.trim();
        if (cleanDescription != null && cleanDescription.length() > MAX_DESCRIPTION) {
            throw new InvalidRequestException("Description is too long (maximum " + MAX_DESCRIPTION + " characters)");
        }

        VideoStorageService.StoredVideo stored = storage.store(file, courseId);   // 1. the file goes to the folder
        try {
            int position = orderIndex != null && orderIndex > 0 ? orderIndex : nextPosition(courseId);
            CourseVideo video = new CourseVideo(course, cleanTitle, cleanDescription, position,
                    safeFileName(file.getOriginalFilename()), stored.relativePath(), stored.contentType(), stored.size(),
                    access.currentUser());
            CourseVideo saved = DatabaseOperationException.guard("saving a video", () -> videoRepo.save(video));   // 2. its path goes to the database
            log.info("Video {} added to course {}", saved.getVideoId(), courseId);
            return CourseVideoDTO.from(saved, true);
        } catch (RuntimeException e) {
            storage.delete(stored.relativePath());   // no database row, so no orphan file either
            throw e;
        }
    }

    @Override
    public List<CourseVideoDTO> listForCourse(Long courseId) {
        log.trace("listForCourse entered for courseId={}", courseId);
        Course course = loadCourse(courseId);
        boolean watchable = canWatch(course);
        List<CourseVideo> videos = DatabaseOperationException.guard("loading videos",
                () -> videoRepo.findByCourse_CourseIdOrderByOrderIndexAscVideoIdAsc(courseId));
        List<CourseVideoDTO> result = new ArrayList<>();
        for (CourseVideo v : videos) {
            result.add(CourseVideoDTO.from(v, watchable));
        }
        log.debug("listForCourse: {} lesson(s), canWatch={}", result.size(), watchable);
        return result;
    }

    @Override
    public CourseVideoDTO update(Long videoId, VideoUpdateRequest request) {
        log.trace("update entered for videoId={}", videoId);
        CourseVideo video = loadVideo(videoId);
        access.requireCourseManager(video.getCourse());
        video.setTitle(request.getTitle().trim());
        video.setDescription(request.getDescription() == null ? null : request.getDescription().trim());
        if (request.getOrderIndex() != null) {
            video.setOrderIndex(request.getOrderIndex());
        }
        CourseVideo saved = DatabaseOperationException.guard("updating a video", () -> videoRepo.save(video));
        log.info("Video {} updated", videoId);
        return CourseVideoDTO.from(saved, true);
    }

    @Override
    public void delete(Long videoId) {
        log.trace("delete entered for videoId={}", videoId);
        CourseVideo video = loadVideo(videoId);
        access.requireCourseManager(video.getCourse());
        DatabaseOperationException.guardVoid("deleting a video", () -> videoRepo.delete(video));
        storage.delete(video.getFilePath());
        log.info("Video {} deleted", videoId);
    }

    @Override
    public void deleteAllForCourse(Long courseId) {
        log.trace("deleteAllForCourse entered for courseId={}", courseId);
        List<CourseVideo> videos = DatabaseOperationException.guard("loading videos", () -> videoRepo.findByCourse_CourseId(courseId));
        if (videos.isEmpty()) {
            return;
        }
        DatabaseOperationException.guardVoid("deleting videos", () -> videoRepo.deleteAll(videos));
        for (CourseVideo v : videos) {
            storage.delete(v.getFilePath());
        }
        log.debug("deleteAllForCourse: removed {} lesson(s) of course {}", videos.size(), courseId);
    }

    @Override
    public String createStreamToken(Long videoId) {
        log.trace("createStreamToken entered for videoId={}", videoId);
        CourseVideo video = loadVideo(videoId);
        if (!canWatch(video.getCourse())) {
            log.warn("Stream token refused for video {}: caller has no access", videoId);
            throw new AccessDeniedException("Enroll in this course to watch its videos");
        }
        return jwtUtil.generateStreamToken(videoId, access.currentUser().getUserId(), tokenMinutes);
    }

    @Override
    public VideoFile openForStreaming(Long videoId, String token) {
        log.trace("openForStreaming entered for videoId={}", videoId);
        if (token == null || !jwtUtil.isValidStreamToken(token, videoId)) {
            throw new AccessDeniedException("This video link is invalid or has expired");
        }
        CourseVideo video = loadVideo(videoId);
        Path path = storage.resolve(video.getFilePath());
        if (!Files.isReadable(path)) {
            log.error("Video file of video {} is missing on the server", videoId);
            throw new ResourceNotFoundException("The video file is not available");
        }
        return new VideoFile(path, video.getContentType() == null ? "video/mp4" : video.getContentType(), video.getSizeBytes());
    }

    /** Admin: always. Owner instructor: yes. Customer: only after buying the course. Any other instructor: no. */
    private boolean canWatch(Course course) {
        if (access.isAdmin()) {
            return true;
        }
        User me = access.currentUser();
        if ("INSTRUCTOR".equals(me.getRole())) {
            return course.getInstructor() != null && course.getInstructor().getUserId().equals(me.getUserId());
        }
        Optional<Customer> customer = DatabaseOperationException.guard("loading a customer",
                () -> customerRepo.findByUser_UserId(me.getUserId()));
        return customer.isPresent() && DatabaseOperationException.guard("checking enrollment",
                () -> orderRepo.existsByCustomer_CustomerIdAndCourses_CourseId(customer.get().getCustomerId(), course.getCourseId()));
    }

    private int nextPosition(Long courseId) {
        List<CourseVideo> existing = DatabaseOperationException.guard("loading videos", () -> videoRepo.findByCourse_CourseId(courseId));
        return existing.stream().mapToInt(CourseVideo::getOrderIndex).max().orElse(0) + 1;
    }

    private Course loadCourse(Long courseId) {
        return DatabaseOperationException.guard("loading a course", () -> courseRepo.findById(courseId))
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id " + courseId));
    }

    private CourseVideo loadVideo(Long videoId) {
        return DatabaseOperationException.guard("loading a video", () -> videoRepo.findById(videoId))
                .orElseThrow(() -> new ResourceNotFoundException("Video not found with id " + videoId));
    }

    /** Keeps only the file name (no folders), at most 255 characters. */
    private String safeFileName(String name) {
        if (name == null || name.isBlank()) {
            return "video";
        }
        String clean = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        return clean.length() > 255 ? clean.substring(0, 255) : clean;
    }
}
