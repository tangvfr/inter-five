package fr.miage.orleans.m2.interop.tp.authentification.model.exception;

public class PasswordIncorrectException extends RuntimeException {
    public PasswordIncorrectException(String message) {
        super(message);
    }
}
