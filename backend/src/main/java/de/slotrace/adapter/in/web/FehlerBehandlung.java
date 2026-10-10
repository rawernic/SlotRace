package de.slotrace.adapter.in.web;

import de.slotrace.domain.NichtGefundenException;
import de.slotrace.domain.RegelVerletzungException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Übersetzt fachliche Fehler in Problem-Details (RFC 9457) mit deutscher Meldung im Feld {@code detail}. */
@RestControllerAdvice
class FehlerBehandlung {

    @ExceptionHandler(NichtGefundenException.class)
    ProblemDetail nichtGefunden(NichtGefundenException e) {
        return problem(HttpStatus.NOT_FOUND, "Nicht gefunden", e.getMessage());
    }

    @ExceptionHandler(RegelVerletzungException.class)
    ProblemDetail regelVerletzung(RegelVerletzungException e) {
        return problem(HttpStatusCode.valueOf(422), "Regelverletzung", e.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail ungueltig(Exception e) {
        return problem(HttpStatus.BAD_REQUEST, "Ungültige Anfrage", "Die Anfrage enthält ungültige Daten");
    }

    private static ProblemDetail problem(HttpStatusCode status, String titel, String meldung) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, meldung);
        p.setTitle(titel);
        return p;
    }
}
