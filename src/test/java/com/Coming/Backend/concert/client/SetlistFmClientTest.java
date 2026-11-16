package com.Coming.Backend.concert.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.concert.dto.SetlistResponse;
import com.Coming.Backend.concert.dto.SetlistTrackDto;
import com.Coming.Backend.concert.exception.SetlistUnavailableException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

class SetlistFmClientTest {

    private static final String SETLIST_FM_ID = "63de4613";
    private static final String SOURCE_URL =
            "https://www.setlist.fm/setlist/the-beatles/1964/hollywood-bowl-los-angeles-ca-63de4613.html";
    private static final String BEATLES_SETLIST = """
            {"id":"63de4613","versionId":"7be1aaa0","eventDate":"23-08-1964",
             "url":"%s",
             "artist":{"mbid":"b10bbbfc","name":"The Beatles"},
             "venue":{"id":"6bd6ca6e","name":"Hollywood Bowl"},
             "sets":{"set":[
               {"song":[{"name":"Twist and Shout","info":"x"},{"name":"She Loves You","tape":false}]},
               {"encore":1,"song":[{"name":"Long Tall Sally"}]}
             ]}}
            """.formatted(SOURCE_URL);

    private final List<ClientRequest> requests = new ArrayList<>();

    private SetlistFmClient client(Function<ClientRequest, ClientResponse> responder) {
        return exchangingClient(request -> Mono.just(responder.apply(request)));
    }

    private SetlistFmClient exchangingClient(Function<ClientRequest, Mono<ClientResponse>> exchange) {
        WebClient webClient = WebClient.builder()
                .baseUrl(SetlistFmClient.BASE_URL)
                .exchangeFunction(request -> {
                    requests.add(request);
                    return exchange.apply(request);
                })
                .build();
        return new SetlistFmClient(webClient);
    }

    private static ClientResponse json(HttpStatus status, String body) {
        return ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build();
    }

    @Test
    void should_request_setlist_path_with_setlist_fm_id_when_get_setlist() {
        // given
        SetlistFmClient client = client(request -> json(HttpStatus.OK, BEATLES_SETLIST));

        // when
        client.getSetlist(SETLIST_FM_ID);

        // then
        assertThat(requests)
                .extracting(request -> request.url().getPath())
                .containsExactly("/rest/1.0/setlist/" + SETLIST_FM_ID);
    }

    @Test
    void should_number_songs_continuously_across_sets_when_setlist_has_encore() {
        // given
        SetlistFmClient client = client(request -> json(HttpStatus.OK, BEATLES_SETLIST));

        // when
        Optional<SetlistResponse> result = client.getSetlist(SETLIST_FM_ID);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().tracks()).containsExactly(
                new SetlistTrackDto(1, "Twist and Shout"),
                new SetlistTrackDto(2, "She Loves You"),
                new SetlistTrackDto(3, "Long Tall Sally"));
    }

    @Test
    void should_pass_response_url_as_source_url_when_url_exists() {
        // given
        SetlistFmClient client = client(request -> json(HttpStatus.OK, BEATLES_SETLIST));

        // when
        Optional<SetlistResponse> result = client.getSetlist(SETLIST_FM_ID);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().sourceUrl()).isEqualTo(SOURCE_URL);
    }

    @Test
    void should_return_null_source_url_when_response_has_no_url() {
        // given
        SetlistFmClient client = client(request -> json(HttpStatus.OK,
                "{\"id\":\"63de4613\",\"sets\":{\"set\":[{\"song\":[{\"name\":\"Twist and Shout\"}]}]}}"));

        // when
        Optional<SetlistResponse> result = client.getSetlist(SETLIST_FM_ID);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().sourceUrl()).isNull();
    }

    @Test
    void should_return_empty_tracks_when_sets_is_empty() {
        // given
        SetlistFmClient client = client(request -> json(HttpStatus.OK,
                "{\"id\":\"63de4613\",\"url\":\"" + SOURCE_URL + "\",\"sets\":{\"set\":[]}}"));

        // when
        Optional<SetlistResponse> result = client.getSetlist(SETLIST_FM_ID);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().tracks()).isEmpty();
        assertThat(result.get().sourceUrl()).isEqualTo(SOURCE_URL);
    }

    @Test
    void should_skip_set_without_song_when_some_set_has_no_song() {
        // given
        SetlistFmClient client = client(request -> json(HttpStatus.OK, """
                {"id":"63de4613","sets":{"set":[
                  {"name":"Intro"},
                  {"song":[{"name":"Twist and Shout"}]},
                  {"encore":1,"song":[{"name":"Long Tall Sally"}]}
                ]}}
                """));

        // when
        Optional<SetlistResponse> result = client.getSetlist(SETLIST_FM_ID);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().tracks()).containsExactly(
                new SetlistTrackDto(1, "Twist and Shout"),
                new SetlistTrackDto(2, "Long Tall Sally"));
    }

    @Test
    void should_parse_setlist_when_response_contains_undefined_fields() {
        // given
        SetlistFmClient client = client(request -> json(HttpStatus.OK, BEATLES_SETLIST));

        // when
        Optional<SetlistResponse> result = client.getSetlist(SETLIST_FM_ID);

        // then
        assertThat(result).contains(new SetlistResponse(List.of(
                new SetlistTrackDto(1, "Twist and Shout"),
                new SetlistTrackDto(2, "She Loves You"),
                new SetlistTrackDto(3, "Long Tall Sally")), SOURCE_URL));
    }

    @Test
    void should_return_empty_when_setlist_fm_returns_404() {
        // given
        SetlistFmClient client = client(request -> json(HttpStatus.NOT_FOUND,
                "{\"code\":404,\"status\":\"Not Found\",\"message\":\"not found\"}"));

        // when
        Optional<SetlistResponse> result = client.getSetlist(SETLIST_FM_ID);

        // then
        assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = HttpStatus.class, names = {"TOO_MANY_REQUESTS", "INTERNAL_SERVER_ERROR", "UNAUTHORIZED"})
    void should_throw_setlist_unavailable_when_setlist_fm_returns_error_status(HttpStatus status) {
        // given
        SetlistFmClient client = client(request -> json(status, "{}"));

        // when & then
        assertThatThrownBy(() -> client.getSetlist(SETLIST_FM_ID))
                .isInstanceOf(SetlistUnavailableException.class)
                .hasMessage(ErrorCode.SETLIST_TEMPORARILY_UNAVAILABLE.getMessage());
    }

    @Test
    void should_throw_setlist_unavailable_when_connection_to_setlist_fm_fails() {
        // given
        SetlistFmClient client = exchangingClient(request -> Mono.error(
                new WebClientRequestException(new ConnectException("Connection refused"),
                        HttpMethod.GET, request.url(), new HttpHeaders())));

        // when & then
        assertThatThrownBy(() -> client.getSetlist(SETLIST_FM_ID))
                .isInstanceOf(SetlistUnavailableException.class)
                .hasMessage(ErrorCode.SETLIST_TEMPORARILY_UNAVAILABLE.getMessage());
    }
}
