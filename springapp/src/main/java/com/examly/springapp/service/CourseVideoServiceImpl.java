package com.examly.springapp.service;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    private final CourseVideoRepo videoRepo;
    private final CourseRepo courseRepo;
    private final OrderRepo orderRepo;
    private final CustomerRepo customerRepo;
    private final CloudinaryVideoStorageService storage;
    private final AccessService access;

    public CourseVideoServiceImpl(CourseVideoRepo videoRepo, CourseRepo courseRepo, OrderRepo orderRepo,
                                  CustomerRepo customerRepo, CloudinaryVideoStorageService storage, AccessService access) {
        this.videoRepo = videoRepo;
        this.courseRepo = courseRepo;
        this.orderRepo = orderRepo;
        this.customerRepo = customerRepo;
        this.storage = storage;
        this.access = access;
    }

    @Override
    public CourseVideoDTO upload(
            Long courseId,
            MultipartFile file,
            String title,
            String description,
            Integer orderIndex) {
    
        Course course = loadCourse(courseId);
    
        access.requireCourseManager(course);
    
        String cleanTitle =
                title == null ? "" : title.trim();
    
        if (cleanTitle.isEmpty()) {
            throw new InvalidRequestException(
                    "Title is required"
            );
        }
    
        String cleanDescription =
                description == null
                        ? null
                        : description.trim();
    
        CloudinaryVideoStorageService.StoredVideo stored =
                storage.store(file, courseId);
    
        try {
    
            int position =
                    orderIndex != null && orderIndex > 0
                            ? orderIndex
                            : nextPosition(courseId);
    
            CourseVideo video = new CourseVideo();
    
            video.setCourse(course);
            video.setTitle(cleanTitle);
            video.setDescription(cleanDescription);
            video.setOrderIndex(position);
    
            video.setOriginalFileName(
                    safeFileName(
                            file.getOriginalFilename()
                    )
            );
    
            video.setContentType(
                    stored.contentType()
            );
    
            video.setSizeBytes(
                    stored.size()
            );
    
            video.setCloudinaryPublicId(
                    stored.publicId()
            );
    
            video.setVideoUrl(
                    stored.url()
            );
    
            video.setUploadedBy(
                    access.currentUser()
            );
    
            video.setUploadedAt(
                    java.time.LocalDateTime.now()
            );
    
            CourseVideo saved =
                    videoRepo.save(video);
    
            return CourseVideoDTO.from(
                    saved,
                    true
            );
    
        } catch (Exception ex) {
    
            storage.delete(
                    stored.publicId()
            );
    
            throw ex;
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
    
        CourseVideo video =
                loadVideo(videoId);
    
        access.requireCourseManager(
                video.getCourse()
        );
    
        videoRepo.delete(video);
    
        storage.delete(
                video.getCloudinaryPublicId()
        );
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
            storage.delete(
                    v.getCloudinaryPublicId()
            );
        }
        log.debug("deleteAllForCourse: removed {} lesson(s) of course {}", videos.size(), courseId);
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
