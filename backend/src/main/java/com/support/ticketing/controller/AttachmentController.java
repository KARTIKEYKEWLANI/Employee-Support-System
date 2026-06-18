package com.support.ticketing.controller;

import com.support.ticketing.entity.TicketAttachment;
import com.support.ticketing.exception.ResourceNotFoundException;
import com.support.ticketing.repository.TicketAttachmentRepository;
import com.support.ticketing.security.SecurityUser;
import com.support.ticketing.service.StorageService;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final StorageService storageService;

    @GetMapping("/{id}/download")
    public ResponseEntity<ByteArrayResource> download(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUser currentUser
    ) throws Exception {
        TicketAttachment attachment = ticketAttachmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found."));
        boolean admin = currentUser.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (!admin && !attachment.getTicket().getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Attachment not found.");
        }
        Path file = storageService.load(attachment.getStoragePath());
        byte[] content = Files.readAllBytes(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + attachment.getFilename())
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .body(new ByteArrayResource(content));
    }
}
