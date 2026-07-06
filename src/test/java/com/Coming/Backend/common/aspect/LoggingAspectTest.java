package com.Coming.Backend.common.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.Coming.Backend.artist.exception.ArtistNotFoundException;
import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoggingAspectTest {

    @InjectMocks
    private LoggingAspect loggingAspect;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Signature signature;

    @Test
    void should_logInfo_when_methodCompletesNormally() throws Throwable {
        // given
        String expectedResult = "result";
        given(joinPoint.getTarget()).willReturn(new Object());
        given(joinPoint.getSignature()).willReturn(signature);
        given(signature.getName()).willReturn("getArtist");
        given(joinPoint.proceed()).willReturn(expectedResult);

        // when
        Object result = loggingAspect.log(joinPoint);

        // then
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void should_warnAndRethrow_when_businessExceptionThrown() throws Throwable {
        // given
        BusinessException exception = new ArtistNotFoundException();
        given(joinPoint.getTarget()).willReturn(new Object());
        given(joinPoint.getSignature()).willReturn(signature);
        given(signature.getName()).willReturn("getArtist");
        given(joinPoint.proceed()).willThrow(exception);

        // when & then
        assertThatThrownBy(() -> loggingAspect.log(joinPoint))
                .isInstanceOf(ArtistNotFoundException.class)
                .hasMessage(ErrorCode.ARTIST_NOT_FOUND.getMessage());
    }

    @Test
    void should_errorAndRethrow_when_unexpectedExceptionThrown() throws Throwable {
        // given
        RuntimeException exception = new RuntimeException("unexpected error");
        given(joinPoint.getTarget()).willReturn(new Object());
        given(joinPoint.getSignature()).willReturn(signature);
        given(signature.getName()).willReturn("getArtist");
        given(joinPoint.proceed()).willThrow(exception);

        // when & then
        assertThatThrownBy(() -> loggingAspect.log(joinPoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("unexpected error");
    }
}
