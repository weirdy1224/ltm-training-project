package com.examly.springapp.service;

/**
 * Sends the application's emails over SMTP (host, port and login come from spring.mail.* in application.properties).
 *
 * @author Lokesh
 */
public interface EmailService {
    /** Emails the 6-digit code that confirms a new account. Throws ApiCommunicationException when the mail cannot be sent. */
    void sendOtpEmail(String to, String name, String code, int validMinutes);

    /** Tells an instructor that an admin approved the account. Never throws: a failed notice must not undo the approval. */
    void sendInstructorApprovedEmail(String to, String name);
}
