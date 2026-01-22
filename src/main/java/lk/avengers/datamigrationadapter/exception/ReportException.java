package lk.avengers.datamigrationadapter.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class ReportException extends RuntimeException {

    private final int status;

    public ReportException(int status, String message) {
        super(message);
        this.status = status;
    }

    public ReportException(HttpStatus status, String message) {
        super(message);
        this.status = status.value();
    }

}
