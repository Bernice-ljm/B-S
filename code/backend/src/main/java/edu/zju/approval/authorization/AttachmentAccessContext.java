package edu.zju.approval.authorization;

import java.util.Objects;

public record AttachmentAccessContext(ApplicationAccessContext owningApplication) {
    public AttachmentAccessContext {
        Objects.requireNonNull(owningApplication, "owningApplication");
    }
}
