package edu.zju.approval.auth;

public record CsrfResponse(String headerName, String parameterName, String token) {
}
