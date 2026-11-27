package edu.zju.approval.authorization;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceAuthorizationServiceTests {

    @Test
    void databaseFailureIsNotConvertedIntoAuthorization() {
        var loader = mock(ApplicationAccessContextLoader.class);
        var service = new ResourceAuthorizationService(loader, new AuthorizationService());
        var failure = new DataAccessResourceFailureException("database unavailable");
        when(loader.loadApplication(42L)).thenThrow(failure);
        when(loader.loadAttachment(84L)).thenThrow(failure);

        assertThatThrownBy(() -> service.canViewApplication(42L)).isSameAs(failure);
        assertThatThrownBy(() -> service.canEditDraft(42L)).isSameAs(failure);
        assertThatThrownBy(() -> service.canApproveTask(42L)).isSameAs(failure);
        assertThatThrownBy(() -> service.canDownloadAttachment(84L)).isSameAs(failure);
    }
}
