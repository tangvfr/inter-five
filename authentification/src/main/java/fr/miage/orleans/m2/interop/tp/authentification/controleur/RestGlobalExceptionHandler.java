package fr.miage.orleans.m2.interop.tp.authentification.controleur;

import org.apache.http.auth.InvalidCredentialsException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class RestGlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(value = {InvalidCredentialsException.class})
  protected ResponseEntity<Object> handleInvalideCredentials(
      RuntimeException ex, WebRequest request) {
    String bodyOfResponse = ex.getMessage();
    return handleExceptionInternal(
        ex, bodyOfResponse, new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
  }
}
