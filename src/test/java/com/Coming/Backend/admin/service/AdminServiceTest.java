package com.Coming.Backend.admin.service;

import com.Coming.Backend.admin.dto.AdminArtistCreateRequest;
import com.Coming.Backend.admin.dto.AdminArtistUpdateRequest;
import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.exception.ArtistNotFoundException;
import com.Coming.Backend.artist.repository.ArtistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @InjectMocks
    private AdminService adminService;

    @Mock
    private ArtistRepository artistRepository;

    @Test
    void should_save_artist_when_create_request_given() {
        // given
        AdminArtistCreateRequest request = new AdminArtistCreateRequest(
                "some-mbid-123",
                "IU",
                "IU",
                LocalDate.of(2008, 9, 18)
        );
        given(artistRepository.save(any(Artist.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        adminService.createArtist(request);

        // then
        ArgumentCaptor<Artist> captor = ArgumentCaptor.forClass(Artist.class);
        verify(artistRepository).save(captor.capture());

        Artist saved = captor.getValue();
        assertThat(saved.getMbid()).isEqualTo("some-mbid-123");
        assertThat(saved.getName()).isEqualTo("IU");
        assertThat(saved.getSortName()).isEqualTo("IU");
        assertThat(saved.getDebutDate()).isEqualTo(LocalDate.of(2008, 9, 18));
        assertThat(saved.isComing()).isFalse();
    }

    @Test
    void should_update_all_fields_when_full_update_request_given() {
        // given
        Artist artist = Artist.builder()
                .mbid("some-mbid-123")
                .name("IU")
                .sortName("IU")
                .debutDate(LocalDate.of(2008, 9, 18))
                .isComing(false)
                .build();
        AdminArtistUpdateRequest request = new AdminArtistUpdateRequest(
                "아이유",
                "Iu, Lee Ji Eun",
                LocalDate.of(2008, 9, 18)
        );
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist));

        // when
        adminService.updateArtist(1L, request);

        // then
        assertThat(artist.getName()).isEqualTo("아이유");
        assertThat(artist.getSortName()).isEqualTo("Iu, Lee Ji Eun");
        assertThat(artist.getDebutDate()).isEqualTo(LocalDate.of(2008, 9, 18));
    }

    @Test
    void should_not_overwrite_null_fields_when_partial_update_request_given() {
        // given
        Artist artist = Artist.builder()
                .mbid("some-mbid-123")
                .name("IU")
                .sortName("IU")
                .debutDate(LocalDate.of(2008, 9, 18))
                .isComing(false)
                .build();
        AdminArtistUpdateRequest request = new AdminArtistUpdateRequest("아이유", null, null);
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist));

        // when
        adminService.updateArtist(1L, request);

        // then
        assertThat(artist.getName()).isEqualTo("아이유");
        assertThat(artist.getSortName()).isEqualTo("IU");
        assertThat(artist.getDebutDate()).isEqualTo(LocalDate.of(2008, 9, 18));
    }

    @Test
    void should_throw_artist_not_found_exception_when_invalid_id_given() {
        // given
        given(artistRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminService.updateArtist(999L, new AdminArtistUpdateRequest("IU", null, null)))
                .isInstanceOf(ArtistNotFoundException.class);
    }
}
