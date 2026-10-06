package com.sliit.helpdesk.ticket.controller;

import com.sliit.helpdesk.ticket.service.TicketService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One-off admin check for attachment rows whose file is gone, and files left behind after a delete.
 */
@RestController
@RequestMapping("/api/admin/attachments")
@PreAuthorize("hasRole('ADMIN')")
public class AttachmentCleanupController {

    private final TicketService ticketService;

    public AttachmentCleanupController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/missing-files")
    public Map<String, Object> missingFiles() {
        List<Map<String, Object>> missing = ticketService.attachmentsMissingOnDisk();
        List<String> orphanFiles = ticketService.orphanUploadFiles();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("missingFiles", missing);
        body.put("orphanFiles", orphanFiles);
        return body;
    }

    @PostMapping("/orphan-files")
    public Map<String, Object> deleteOrphanFiles() {
        int removed = ticketService.deleteOrphanUploadFiles();
        return Map.of("removed", removed);
    }
}
