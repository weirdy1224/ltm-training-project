package com.examly.springapp.service;

import com.examly.springapp.dto.TicketDtos.FeedbackRequest;
import com.examly.springapp.dto.TicketDtos.TicketView;
import com.examly.springapp.dto.TicketDtos.UserTicketSummary;
import com.examly.springapp.exception.DatabaseOperationException;
import com.examly.springapp.exception.InvalidRequestException;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.NotificationType;
import com.examly.springapp.model.Ticket;
import com.examly.springapp.model.TicketCategory;
import com.examly.springapp.model.TicketStatus;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.TicketRepo;
import com.examly.springapp.repository.UserRepo;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * The support ticket system. A user raises a ticket, an admin answers it once:
 * a written response RESOLVES the ticket, an empty response CLOSES it. Notifications are sent to the user and the admins
 * at each step. The caller (controller) passes the logged-in user, so identity always comes from the JWT.
 */
@Service
public class TicketService {
    private static final Logger log = LoggerFactory.getLogger(TicketService.class);
    private static final String ADMIN_ROLE = "ADMIN";

    private final TicketRepo ticketRepo;
    private final UserRepo userRepo;
    private final NotificationService notifications;
    private final ImageStorageService images;

    public TicketService(TicketRepo ticketRepo, UserRepo userRepo, NotificationService notifications,
            ImageStorageService images) {
        this.ticketRepo = ticketRepo;
        this.userRepo = userRepo;
        this.notifications = notifications;
        this.images = images;
    }

    // ------------------------------------------------------------------ user side

    @Transactional
    public TicketView create(User user, Long claimedUserId, String category, String description, MultipartFile screenshot) {
        if (claimedUserId == null || !claimedUserId.equals(user.getUserId())) {
            throw new InvalidRequestException("The user id does not match your account");
        }
        TicketCategory cat;
        try {
            cat = TicketCategory.valueOf(category == null ? "" : category.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Please choose a category");
        }
        String text = description == null ? "" : description.trim();
        if (text.length() < 10) {
            throw new InvalidRequestException("Please describe the problem in at least 10 characters");
        }
        if (text.length() > 2000) {
            throw new InvalidRequestException("The description is too long (2000 characters at most)");
        }

        Ticket t = new Ticket();
        t.setTicketCode(newTicketCode());
        t.setUserId(user.getUserId());
        t.setCategory(cat);
        t.setDescription(text);
        if (screenshot != null && !screenshot.isEmpty()) {
            t.setScreenshotUrl(images.store(screenshot));
        }
        Ticket saved = DatabaseOperationException.guard("saving the ticket", () -> ticketRepo.save(t));

        notifications.createNotification(user.getUserId(), "Ticket " + saved.getTicketCode() + " raised",
                "We received your request. Your ticket id is " + saved.getTicketCode() + ".",
                NotificationType.TICKET_CREATED, "/my-tickets/" + saved.getId(), saved.getId());
        for (User admin : admins()) {
            notifications.createNotification(admin.getUserId(), "New ticket " + saved.getTicketCode(),
                    "A ticket has been raised by " + user.getUsername() + " (" + label(cat) + ").",
                    NotificationType.TICKET_RAISED, "/admin/tickets/" + saved.getId(), saved.getId());
        }
        log.info("Ticket {} raised by user {}", saved.getTicketCode(), user.getUserId());
        return TicketView.of(saved, user);
    }

    public List<TicketView> mine(User user) {
        return DatabaseOperationException.guard("loading your tickets", () -> ticketRepo.findByUserIdOrderByCreatedAtDesc(user.getUserId()))
                .stream().map(t -> TicketView.of(t, user)).toList();
    }

    public TicketView getMine(User user, Long id) {
        return TicketView.of(owned(user, id), user);
    }

    @Transactional
    public TicketView feedback(User user, Long id, FeedbackRequest req) {
        Ticket t = owned(user, id);
        if (t.getStatus() != TicketStatus.RESOLVED && t.getStatus() != TicketStatus.CLOSED) {
            throw new InvalidRequestException("You can give feedback once the ticket is resolved or closed");
        }
        if (t.getFeedbackAt() != null) {
            throw new InvalidRequestException("You have already sent feedback for this ticket");
        }
        t.setFeedbackRating(req.getRating());
        t.setFeedbackUsefulness(req.getUsefulness());
        String comment = req.getComment() == null ? null : req.getComment().trim();
        t.setFeedbackComment(comment == null || comment.isEmpty() ? null : comment);
        t.setFeedbackAt(LocalDateTime.now());
        return TicketView.of(DatabaseOperationException.guard("saving the feedback", () -> ticketRepo.save(t)), user);
    }

    // ------------------------------------------------------------------ admin side

    /** One summary per user who has raised tickets, the user with the newest ticket first. */
    public List<UserTicketSummary> summaries() {
        List<Ticket> all = DatabaseOperationException.guard("loading tickets", ticketRepo::findAllByOrderByCreatedAtDesc);
        Map<Long, List<Ticket>> byUser = all.stream().collect(Collectors.groupingBy(Ticket::getUserId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, User> users = usersById(byUser.keySet());
        List<UserTicketSummary> out = new ArrayList<>();
        byUser.forEach((userId, list) -> {
            User u = users.get(userId);
            out.add(new UserTicketSummary(userId, u == null ? "Deleted user" : u.getUsername(), u == null ? null : u.getEmail(),
                    u == null ? null : u.getRole(), list.size(), count(list, TicketStatus.OPEN), count(list, TicketStatus.PENDING),
                    count(list, TicketStatus.RESOLVED), count(list, TicketStatus.CLOSED), list.get(0).getCreatedAt()));
        });
        out.sort(Comparator.comparing(UserTicketSummary::lastTicketAt).reversed());
        return out;
    }

    public List<TicketView> forUser(Long userId) {
        User owner = userRepo.findById(userId).orElse(null);
        return DatabaseOperationException.guard("loading tickets", () -> ticketRepo.findByUserIdOrderByCreatedAtDesc(userId))
                .stream().map(t -> TicketView.of(t, owner)).toList();
    }

    /** Opening a ticket as admin moves an OPEN ticket to PENDING (somebody is looking at it). */
    @Transactional
    public TicketView adminOpen(Long id) {
        Ticket t = find(id);
        if (t.getStatus() == TicketStatus.OPEN) {
            t.setStatus(TicketStatus.PENDING);
            ticketRepo.save(t);
        }
        return TicketView.of(t, userRepo.findById(t.getUserId()).orElse(null));
    }

    /** Only one response per ticket: text resolves it, no text closes it. */
    @Transactional
    public TicketView respond(User admin, Long id, String response) {
        Ticket t = find(id);
        if (t.getStatus() == TicketStatus.RESOLVED || t.getStatus() == TicketStatus.CLOSED) {
            throw new InvalidRequestException("This ticket is already " + t.getStatus().name().toLowerCase());
        }
        String text = response == null ? "" : response.trim();
        boolean resolved = !text.isEmpty();
        t.setStatus(resolved ? TicketStatus.RESOLVED : TicketStatus.CLOSED);
        t.setAdminResponse(resolved ? text : null);
        t.setRespondedBy(admin.getUserId());
        t.setRespondedAt(LocalDateTime.now());
        Ticket saved = DatabaseOperationException.guard("saving the response", () -> ticketRepo.save(t));
        User owner = userRepo.findById(saved.getUserId()).orElse(null);

        if (owner != null) {
            if (resolved) {
                notifications.createNotification(owner.getUserId(), "Response to ticket " + saved.getTicketCode(),
                        "The admin has responded to your ticket. Open it to read the response.",
                        NotificationType.TICKET_RESPONSE, "/my-tickets/" + saved.getId(), saved.getId());
            } else {
                notifications.createNotification(owner.getUserId(), "Ticket " + saved.getTicketCode() + " closed",
                        "The admin closed your ticket without a written response.",
                        NotificationType.TICKET_RESPONSE, "/my-tickets/" + saved.getId(), saved.getId());
            }
        }
        for (User a : admins()) {
            notifications.createNotification(a.getUserId(), "Ticket " + saved.getTicketCode() + " resolved",
                    "Ticket " + saved.getTicketCode() + " has been " + (resolved ? "resolved" : "closed") + ".",
                    NotificationType.TICKET_RESOLVED, "/admin/tickets/" + saved.getId(), saved.getId());
        }
        log.info("Ticket {} {} by admin {}", saved.getTicketCode(), saved.getStatus(), admin.getUserId());
        return TicketView.of(saved, owner);
    }

    // ------------------------------------------------------------------ helpers

    private Ticket find(Long id) {
        return DatabaseOperationException.guard("finding the ticket", () -> ticketRepo.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id " + id));
    }

    /** A user may only see their own tickets; a foreign id looks exactly like a missing one. */
    private Ticket owned(User user, Long id) {
        Ticket t = find(id);
        if (!t.getUserId().equals(user.getUserId())) {
            throw new ResourceNotFoundException("Ticket not found with id " + id);
        }
        return t;
    }

    private List<User> admins() {
        return userRepo.findByRole(ADMIN_ROLE);
    }

    private Map<Long, User> usersById(Iterable<Long> ids) {
        Map<Long, User> map = new LinkedHashMap<>();
        userRepo.findAllById(ids).forEach(u -> map.put(u.getUserId(), u));
        return map;
    }

    private static long count(List<Ticket> list, TicketStatus status) {
        return list.stream().filter(t -> t.getStatus() == status).count();
    }

    private String newTicketCode() {
        String code;
        do {
            code = "TKT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        } while (ticketRepo.existsByTicketCode(code));
        return code;
    }

    private static String label(TicketCategory c) {
        String s = c.name().replace('_', ' ').toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}