package com.Coming.Backend.common.exception;

import com.Coming.Backend.auth.exception.InvalidTokenException;
import com.Coming.Backend.common.discord.DiscordNotifier;
import com.Coming.Backend.concert.exception.ConcertNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Mock
    private DiscordNotifier discordNotifier;

    @Mock
    private HttpServletRequest request;

    @Mock
    private MethodArgumentNotValidException validationException;

    @Mock
    private BindingResult bindingResult;

    @Test
    void should_return_error_response_when_business_exception_thrown() {
        // given
        BusinessException exception = new ConcertNotFoundException();

        // when
        ResponseEntity<ErrorResponse> response = handler.handleBusinessException(exception, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("CONCERT_NOT_FOUND");
        assertThat(response.getBody().message()).isEqualTo(ErrorCode.CONCERT_NOT_FOUND.getMessage());
        then(discordNotifier).should(never()).notifyFourXx(request, ErrorCode.CONCERT_NOT_FOUND);
    }

    @Test
    void should_notify_discord_when_alertable_business_exception_thrown() {
        // given
        BusinessException exception = new BusinessException(ErrorCode.USER_SUSPENDED) {};

        // when
        handler.handleBusinessException(exception, request);

        // then
        then(discordNotifier).should().notifyFourXx(request, ErrorCode.USER_SUSPENDED);
    }

    @Test
    void should_not_notify_discord_when_non_alertable_business_exception_thrown() {
        // given
        BusinessException exception = new InvalidTokenException();

        // when
        handler.handleBusinessException(exception, request);

        // then
        then(discordNotifier).should(never()).notifyFourXx(request, ErrorCode.INVALID_TOKEN);
    }

    @Test
    void should_return_400_with_field_message_when_validation_exception_thrown() {
        // given
        FieldError fieldError = new FieldError("request", "title", "공백일 수 없습니다");
        given(validationException.getBindingResult()).willReturn(bindingResult);
        given(bindingResult.getFieldErrors()).willReturn(List.of(fieldError));

        // when
        ResponseEntity<ErrorResponse> response = handler.handleValidationException(validationException);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_INPUT");
        assertThat(response.getBody().message()).isEqualTo("title: 공백일 수 없습니다");
    }

    @Test
    void should_return_400_with_default_message_when_no_field_errors() {
        // given
        given(validationException.getBindingResult()).willReturn(bindingResult);
        given(bindingResult.getFieldErrors()).willReturn(List.of());

        // when
        ResponseEntity<ErrorResponse> response = handler.handleValidationException(validationException);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_INPUT");
        assertThat(response.getBody().message()).isEqualTo(ErrorCode.INVALID_INPUT.getMessage());
    }

    @Test
    void should_return_400_when_property_reference_exception_thrown() {
        // given
        PropertyReferenceException exception = mock(PropertyReferenceException.class);

        // when
        ResponseEntity<ErrorResponse> response = handler.handlePropertyReference(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_INPUT");
        assertThat(response.getBody().message()).isEqualTo(ErrorCode.INVALID_INPUT.getMessage());
    }

    @Test
    void should_return_400_when_type_mismatch_exception_thrown() {
        // given
        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);

        // when
        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_INPUT");
        assertThat(response.getBody().message()).isEqualTo(ErrorCode.INVALID_INPUT.getMessage());
    }

    @Test
    void should_return_500_and_notify_discord_when_unhandled_exception_thrown() {
        // given
        Exception exception = new RuntimeException("unexpected error");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleException(exception, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        then(discordNotifier).should().notifyFiveXx(request, exception);
    }
}
