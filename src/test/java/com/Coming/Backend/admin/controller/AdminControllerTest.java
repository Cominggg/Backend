package com.Coming.Backend.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Coming.Backend.admin.dto.AdminArtistCollectRequest;
import com.Coming.Backend.admin.dto.AdminArtistDetailResponse;
import com.Coming.Backend.admin.dto.AdminArtistSearchResult;
import com.Coming.Backend.admin.dto.AdminConcertCollectRequest;
import com.Coming.Backend.admin.dto.AdminConcertCreateRequest;
import com.Coming.Backend.admin.dto.AdminConcertCreateResponse;
import com.Coming.Backend.admin.dto.AdminConcertStateUpdateRequest;
import com.Coming.Backend.admin.dto.AdminConcertUpdateRequest;
import com.Coming.Backend.admin.dto.AdminExcludedArtistResponse;
import com.Coming.Backend.admin.dto.AdminExcludedConcertResponse;
import com.Coming.Backend.admin.dto.AdminInquiryDetailResponse;
import com.Coming.Backend.admin.dto.AdminInquiryListItemResponse;
import com.Coming.Backend.admin.dto.AdminInquiryStatusUpdateRequest;
import com.Coming.Backend.admin.dto.DataArtistSearchResult;
import com.Coming.Backend.admin.dto.DataConcertSearchResult;
import com.Coming.Backend.admin.dto.PipelineArtistCollectResult;
import com.Coming.Backend.admin.dto.PipelineConcertCollectResult;
import com.Coming.Backend.admin.service.AdminService;
import com.Coming.Backend.artist.exception.ArtistNotFoundException;
import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.common.exception.GlobalExceptionHandler;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.concert.entity.ConcertStatus;
import com.Coming.Backend.concert.exception.ConcertArtistNotFoundException;
import com.Coming.Backend.concert.exception.ConcertIsPendingException;
import com.Coming.Backend.concert.exception.ConcertNotFoundException;
import com.Coming.Backend.concert.exception.ConcertNotPendingException;
import com.Coming.Backend.inquiry.entity.InquiryStatus;
import com.Coming.Backend.inquiry.entity.InquiryType;
import com.Coming.Backend.inquiry.exception.InquiryNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Long INQUIRY_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final Long TARGET_ID = 99L;
    private static final Long CONCERT_ID = 1L;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(adminController)
                .setControllerAdvice(new GlobalExceptionHandler(new com.Coming.Backend.common.discord.NoOpDiscordNotifier()))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    // -------------------------------------------------------------------------
    // GET /api/admin/inquiries
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_inquiry_list_when_no_filter_given() throws Exception {
        // given
        AdminInquiryListItemResponse item = new AdminInquiryListItemResponse(
                INQUIRY_ID, InquiryType.CONCERT, "공연 정보 오류", InquiryStatus.PENDING,
                "2024-06-01", null, null, USER_ID, "테스트유저"
        );
        PageResponse<AdminInquiryListItemResponse> pageResponse = new PageResponse<>(List.of(item), 0, 10, 1, 1);
        given(adminService.getInquiries(isNull(), isNull(), any(Pageable.class)))
                .willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/admin/inquiries").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(INQUIRY_ID))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    void should_pass_type_and_status_filter_to_service_when_filter_params_given() throws Exception {
        // given
        AdminInquiryListItemResponse item = new AdminInquiryListItemResponse(
                INQUIRY_ID, InquiryType.CONCERT, "공연 정보 오류", InquiryStatus.RESOLVED,
                "2024-06-01", "처리 완료되었습니다.", null, USER_ID, "테스트유저"
        );
        PageResponse<AdminInquiryListItemResponse> pageResponse = new PageResponse<>(List.of(item), 0, 10, 1, 1);
        given(adminService.getInquiries(eq(InquiryType.CONCERT), eq(InquiryStatus.RESOLVED), any(Pageable.class)))
                .willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/admin/inquiries")
                        .param("type", "CONCERT")
                        .param("status", "RESOLVED")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].type").value("CONCERT"))
                .andExpect(jsonPath("$.content[0].status").value("RESOLVED"));
    }

    // -------------------------------------------------------------------------
    // GET /api/admin/inquiries/{id}
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_inquiry_detail_when_valid_id_given() throws Exception {
        // given
        AdminInquiryDetailResponse detail = new AdminInquiryDetailResponse(
                INQUIRY_ID, InquiryType.CONCERT, "공연 정보 오류", InquiryStatus.PENDING,
                "2024-06-01", null, null, USER_ID, "테스트유저", "공연 날짜가 잘못되었습니다.", TARGET_ID
        );
        given(adminService.getInquiryDetail(eq(INQUIRY_ID))).willReturn(detail);

        // when & then
        mockMvc.perform(get("/api/admin/inquiries/{id}", INQUIRY_ID).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(INQUIRY_ID))
                .andExpect(jsonPath("$.content").value("공연 날짜가 잘못되었습니다."))
                .andExpect(jsonPath("$.targetId").value(TARGET_ID))
                .andExpect(jsonPath("$.userNickname").value("테스트유저"));
    }

    @Test
    void should_return_404_when_inquiry_not_found_on_detail() throws Exception {
        // given
        given(adminService.getInquiryDetail(eq(999L)))
                .willThrow(new InquiryNotFoundException());

        // when & then
        mockMvc.perform(get("/api/admin/inquiries/{id}", 999L).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.INQUIRY_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    // -------------------------------------------------------------------------
    // PATCH /api/admin/inquiries/{id}/status
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_when_inquiry_status_updated_successfully() throws Exception {
        // given
        AdminInquiryStatusUpdateRequest request = new AdminInquiryStatusUpdateRequest(
                InquiryStatus.RESOLVED, "처리 완료되었습니다."
        );
        willDoNothing().given(adminService).updateInquiryStatus(eq(INQUIRY_ID), any(AdminInquiryStatusUpdateRequest.class));

        // when & then
        mockMvc.perform(patch("/api/admin/inquiries/{id}/status", INQUIRY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void should_return_400_when_status_field_is_null() throws Exception {
        // given — status 필드 누락
        String requestBody = """
                {
                    "adminNote": "처리 완료되었습니다."
                }
                """;

        // when & then
        mockMvc.perform(patch("/api/admin/inquiries/{id}/status", INQUIRY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_404_when_inquiry_not_found_on_status_update() throws Exception {
        // given
        AdminInquiryStatusUpdateRequest request = new AdminInquiryStatusUpdateRequest(
                InquiryStatus.RESOLVED, null
        );
        willThrow(new InquiryNotFoundException())
                .given(adminService).updateInquiryStatus(eq(999L), any(AdminInquiryStatusUpdateRequest.class));

        // when & then
        mockMvc.perform(patch("/api/admin/inquiries/{id}/status", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.INQUIRY_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    // -------------------------------------------------------------------------
    // POST /api/admin/concerts
    // -------------------------------------------------------------------------

    @Test
    void should_return_201_when_valid_concert_create_request_given() throws Exception {
        // given
        String requestBody = """
                {
                    "title": "아이유 콘서트",
                    "startDate": "2026-09-01",
                    "endDate": "2026-09-30",
                    "venueName": "올림픽공원 체조경기장"
                }
                """;
        given(adminService.createConcert(any(AdminConcertCreateRequest.class)))
                .willReturn(new AdminConcertCreateResponse(CONCERT_ID));

        // when & then
        mockMvc.perform(post("/api/admin/concerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.concertId").value(CONCERT_ID));
    }

    @Test
    void should_return_400_when_title_is_blank_on_create() throws Exception {
        // given
        String requestBody = """
                {
                    "title": "",
                    "startDate": "2026-09-01",
                    "endDate": "2026-09-30",
                    "venueName": "올림픽공원 체조경기장"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/admin/concerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_start_date_is_null_on_create() throws Exception {
        // given
        String requestBody = """
                {
                    "title": "아이유 콘서트",
                    "endDate": "2026-09-30",
                    "venueName": "올림픽공원 체조경기장"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/admin/concerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_venue_name_is_blank_on_create() throws Exception {
        // given
        String requestBody = """
                {
                    "title": "아이유 콘서트",
                    "startDate": "2026-09-01",
                    "endDate": "2026-09-30",
                    "venueName": ""
                }
                """;

        // when & then
        mockMvc.perform(post("/api/admin/concerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // PUT /api/admin/concerts/{id}
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_when_valid_concert_update_request_given() throws Exception {
        // given
        String requestBody = """
                {
                    "title": "아이유 앙코르 콘서트",
                    "startDate": "2025-10-01",
                    "endDate": "2025-10-31"
                }
                """;
        willDoNothing().given(adminService).updateConcert(eq(CONCERT_ID), any(AdminConcertUpdateRequest.class));

        // when & then
        mockMvc.perform(put("/api/admin/concerts/{id}", CONCERT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    void should_return_404_when_concert_not_found_on_update() throws Exception {
        // given
        String requestBody = """
                {
                    "title": "아이유 앙코르 콘서트"
                }
                """;
        willThrow(new ConcertNotFoundException())
                .given(adminService).updateConcert(eq(999L), any(AdminConcertUpdateRequest.class));

        // when & then
        mockMvc.perform(put("/api/admin/concerts/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.CONCERT_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    // -------------------------------------------------------------------------
    // PUT /api/admin/concerts/{id}/state
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_when_valid_state_update_request_given() throws Exception {
        // given
        String requestBody = """
                {
                    "status": "CANCELLED",
                    "reason": "공연 취소"
                }
                """;
        willDoNothing().given(adminService).forceChangeConcertState(eq(CONCERT_ID), any(AdminConcertStateUpdateRequest.class));

        // when & then
        mockMvc.perform(put("/api/admin/concerts/{id}/state", CONCERT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    void should_return_400_when_status_is_null() throws Exception {
        // given — status 필드 누락
        String requestBody = """
                {
                    "reason": "공연 취소"
                }
                """;

        // when & then
        mockMvc.perform(put("/api/admin/concerts/{id}/state", CONCERT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_404_when_concert_not_found_on_state_update() throws Exception {
        // given
        String requestBody = """
                {
                    "status": "CANCELLED",
                    "reason": "공연 취소"
                }
                """;
        willThrow(new ConcertNotFoundException())
                .given(adminService).forceChangeConcertState(eq(999L), any(AdminConcertStateUpdateRequest.class));

        // when & then
        mockMvc.perform(put("/api/admin/concerts/{id}/state", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.CONCERT_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    // -------------------------------------------------------------------------
    // GET /api/admin/data/search/artists
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_artist_candidates_when_name_given() throws Exception {
        // given
        List<DataArtistSearchResult> results = List.of(
                new DataArtistSearchResult("mbid-1", "IU", "KR", "Person", "https://musicbrainz.org/artist/mbid-1"),
                new DataArtistSearchResult("mbid-2", "IU (instrumental)", "JP", null, null)
        );
        given(adminService.searchArtists("IU")).willReturn(results);

        // when & then
        mockMvc.perform(get("/api/admin/data/search/artists")
                        .param("name", "IU")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].mbid").value("mbid-1"))
                .andExpect(jsonPath("$[0].name").value("IU"))
                .andExpect(jsonPath("$[0].country").value("KR"))
                .andExpect(jsonPath("$[0].type").value("Person"));
    }

    @Test
    void should_return_200_with_empty_list_when_no_artists_match() throws Exception {
        // given
        given(adminService.searchArtists("존재하지않는아티스트")).willReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/admin/data/search/artists")
                        .param("name", "존재하지않는아티스트")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // -------------------------------------------------------------------------
    // GET /api/admin/data/search/concerts
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_concert_candidates_when_title_given() throws Exception {
        // given
        List<DataConcertSearchResult> results = List.of(
                new DataConcertSearchResult("PF001", "아이유 콘서트", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 2), "올림픽공원 체조경기장", "https://kopis.or.kr/PF001")
        );
        given(adminService.searchConcerts("아이유")).willReturn(results);

        // when & then
        mockMvc.perform(get("/api/admin/data/search/concerts")
                        .param("title", "아이유")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].kopisId").value("PF001"))
                .andExpect(jsonPath("$[0].title").value("아이유 콘서트"))
                .andExpect(jsonPath("$[0].startDate").value("2026-03-01"))
                .andExpect(jsonPath("$[0].venue").value("올림픽공원 체조경기장"));
    }

    @Test
    void should_return_200_with_empty_list_when_no_concerts_match() throws Exception {
        // given
        given(adminService.searchConcerts("존재하지않는공연")).willReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/admin/data/search/concerts")
                        .param("title", "존재하지않는공연")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // -------------------------------------------------------------------------
    // POST /api/admin/data/collect/artists
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_when_artist_collect_triggered_with_mbid() throws Exception {
        // given
        String requestBody = """
                {
                    "mbid": "some-mbid-123"
                }
                """;
        given(adminService.collectArtist(any(AdminArtistCollectRequest.class)))
                .willReturn(new PipelineArtistCollectResult(true, 1L, "some-mbid-123", "IU", null, List.of(), null));

        // when & then
        mockMvc.perform(post("/api/admin/data/collect/artists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    void should_return_400_when_mbid_is_blank() throws Exception {
        // given
        String requestBody = """
                {
                    "mbid": ""
                }
                """;

        // when & then
        mockMvc.perform(post("/api/admin/data/collect/artists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // POST /api/admin/data/collect/concerts
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_when_concert_collect_triggered_with_kopis_id() throws Exception {
        // given
        String requestBody = """
                {
                    "kopisId": "PF123456"
                }
                """;
        given(adminService.collectConcert(any(AdminConcertCollectRequest.class)))
                .willReturn(new PipelineConcertCollectResult(true, 10L, "YOASOBI Live", List.of(), null));

        // when & then
        mockMvc.perform(post("/api/admin/data/collect/concerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    void should_return_400_when_kopis_id_is_blank() throws Exception {
        // given
        String requestBody = """
                {
                    "kopisId": ""
                }
                """;

        // when & then
        mockMvc.perform(post("/api/admin/data/collect/concerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // GET /api/admin/concerts/excluded
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_excluded_concert_list_when_excluded_concerts_exist() throws Exception {
        // given
        AdminExcludedArtistResponse artist = new AdminExcludedArtistResponse(1L, "IU", null);
        AdminExcludedConcertResponse concert = new AdminExcludedConcertResponse(
                1L, "아이유 콘서트", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 2),
                "올림픽공원 체조경기장", "https://example.com/poster.jpg", List.of(artist)
        );
        PageResponse<AdminExcludedConcertResponse> pageResponse = new PageResponse<>(List.of(concert), 0, 20, 1L, 1);
        given(adminService.getExcludedConcerts(any(Pageable.class))).willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/admin/concerts/excluded").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].title").value("아이유 콘서트"))
                .andExpect(jsonPath("$.content[0].startDate").value("2026-03-01"))
                .andExpect(jsonPath("$.content[0].endDate").value("2026-03-02"))
                .andExpect(jsonPath("$.content[0].venueName").value("올림픽공원 체조경기장"))
                .andExpect(jsonPath("$.content[0].posterUrl").value("https://example.com/poster.jpg"))
                .andExpect(jsonPath("$.content[0].artists[0].artistId").value(1L))
                .andExpect(jsonPath("$.content[0].artists[0].name").value("IU"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // -------------------------------------------------------------------------
    // POST /api/admin/concerts/{id}/candidates
    // -------------------------------------------------------------------------

    @Test
    void should_return_201_when_candidate_assigned_to_pending_concert() throws Exception {
        // given
        String requestBody = """
                { "artistId": 20 }
                """;
        willDoNothing().given(adminService).assignCandidateToConcert(CONCERT_ID, 20L);

        // when & then
        mockMvc.perform(post("/api/admin/concerts/{id}/candidates", CONCERT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());
    }

    @Test
    void should_return_400_when_assign_candidate_to_non_pending_concert() throws Exception {
        // given
        String requestBody = """
                { "artistId": 20 }
                """;
        willThrow(new ConcertNotPendingException())
                .given(adminService).assignCandidateToConcert(CONCERT_ID, 20L);

        // when & then
        mockMvc.perform(post("/api/admin/concerts/{id}/candidates", CONCERT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.CONCERT_NOT_PENDING.name()));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/admin/concerts/{id}/candidates/{artistId}
    // -------------------------------------------------------------------------

    @Test
    void should_return_204_when_candidate_removed_from_pending_concert() throws Exception {
        // given
        willDoNothing().given(adminService).removeCandidateFromConcert(CONCERT_ID, 20L);

        // when & then
        mockMvc.perform(delete("/api/admin/concerts/{id}/candidates/{artistId}", CONCERT_ID, 20L))
                .andExpect(status().isNoContent());
    }

    @Test
    void should_return_404_when_candidate_not_found_on_remove() throws Exception {
        // given
        willThrow(new ConcertArtistNotFoundException())
                .given(adminService).removeCandidateFromConcert(CONCERT_ID, 20L);

        // when & then
        mockMvc.perform(delete("/api/admin/concerts/{id}/candidates/{artistId}", CONCERT_ID, 20L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.CONCERT_ARTIST_NOT_FOUND.name()));
    }

    // -------------------------------------------------------------------------
    // POST /api/admin/concerts/{id}/artists
    // -------------------------------------------------------------------------

    @Test
    void should_return_400_when_assign_artist_to_pending_concert() throws Exception {
        // given
        String requestBody = """
                { "artistId": 20 }
                """;
        willThrow(new ConcertIsPendingException())
                .given(adminService).assignArtistToConcert(CONCERT_ID, 20L);

        // when & then
        mockMvc.perform(post("/api/admin/concerts/{id}/artists", CONCERT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.CONCERT_IS_PENDING.name()));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/admin/concerts/{id}/artists/{artistId}
    // -------------------------------------------------------------------------

    @Test
    void should_return_400_when_remove_artist_from_pending_concert() throws Exception {
        // given
        willThrow(new ConcertIsPendingException())
                .given(adminService).removeArtistFromConcert(CONCERT_ID, 20L);

        // when & then
        mockMvc.perform(delete("/api/admin/concerts/{id}/artists/{artistId}", CONCERT_ID, 20L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.CONCERT_IS_PENDING.name()));
    }

    // -------------------------------------------------------------------------
    // GET /api/admin/artists?name={name}
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_artist_list_when_name_given() throws Exception {
        // given
        AdminArtistSearchResult result = new AdminArtistSearchResult(1L, "IU");
        PageResponse<AdminArtistSearchResult> pageResponse = new PageResponse<>(List.of(result), 0, 20, 1L, 1);
        given(adminService.searchLocalArtists(eq("IU"), any(Pageable.class))).willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/admin/artists")
                        .param("name", "IU")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].name").value("IU"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // -------------------------------------------------------------------------
    // GET /api/admin/artists/{id}
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_with_image_url_and_links_when_valid_id_given() throws Exception {
        // given
        AdminArtistDetailResponse response = new AdminArtistDetailResponse(
                1L, "IU", "https://example.com/iu.jpg",
                new AdminArtistDetailResponse.AliasesDto(List.of(), List.of(), List.of()),
                List.of(new AdminArtistDetailResponse.LinkDto("twitter", "https://twitter.com/iu"))
        );
        given(adminService.getAdminArtist(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/admin/artists/{id}", 1L).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value("https://example.com/iu.jpg"))
                .andExpect(jsonPath("$.links[0].type").value("twitter"))
                .andExpect(jsonPath("$.links[0].url").value("https://twitter.com/iu"));
    }

    @Test
    void should_return_404_when_artist_not_found_on_get() throws Exception {
        // given
        given(adminService.getAdminArtist(999L)).willThrow(new ArtistNotFoundException());

        // when & then
        mockMvc.perform(get("/api/admin/artists/{id}", 999L).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.ARTIST_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    // -------------------------------------------------------------------------
    // PUT /api/admin/artists/{id}
    // -------------------------------------------------------------------------

    @Test
    void should_return_200_when_valid_artist_update_request_with_image_url_and_links_given() throws Exception {
        // given
        String requestBody = """
                {
                    "imageUrl": "https://example.com/iu.jpg",
                    "links": [
                        { "type": "twitter", "url": "https://twitter.com/iu" }
                    ]
                }
                """;
        willDoNothing().given(adminService).updateArtist(eq(1L), any());

        // when & then
        mockMvc.perform(put("/api/admin/artists/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    void should_return_400_when_link_type_is_blank_on_update() throws Exception {
        // given
        String requestBody = """
                {
                    "links": [
                        { "type": "", "url": "https://twitter.com/iu" }
                    ]
                }
                """;

        // when & then
        mockMvc.perform(put("/api/admin/artists/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}
