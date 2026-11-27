package edu.zju.approval.authorization;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class ResourceAuthorizationService {

    private final ApplicationAccessContextLoader contextLoader;
    private final AuthorizationService authorizationService;

    public ResourceAuthorizationService(
            ApplicationAccessContextLoader contextLoader,
            AuthorizationService authorizationService) {
        this.contextLoader = contextLoader;
        this.authorizationService = authorizationService;
    }

    public boolean canViewApplication(long applicationId) {
        return contextLoader.loadApplication(applicationId)
                .map(authorizationService::canViewApplication)
                .orElse(false);
    }

    public boolean canEditDraft(long applicationId) {
        return contextLoader.loadApplication(applicationId)
                .map(authorizationService::canEditDraft)
                .orElse(false);
    }

    public boolean canApproveTask(long applicationId) {
        return contextLoader.loadApplication(applicationId)
                .map(authorizationService::canApproveTask)
                .orElse(false);
    }

    public boolean canDownloadAttachment(long attachmentId) {
        return contextLoader.loadAttachment(attachmentId)
                .map(authorizationService::canDownloadAttachment)
                .orElse(false);
    }
}
