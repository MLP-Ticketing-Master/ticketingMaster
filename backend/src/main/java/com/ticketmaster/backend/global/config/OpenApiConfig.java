package com.ticketmaster.backend.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Comparator;
import java.util.List;

@Configuration
public class OpenApiConfig {

    // 인증 스킴 식별자 — SecurityScheme(정의)와 SecurityRequirement(적용)을 같은 이름으로 묶음
    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    // 태그 노출 순서 — 사용자 API 먼저, 관리자 API 를 아래로
    private static final List<String> TAG_ORDER = List.of(
            "인증 API",
            "사용자 API",
            "이벤트 API",
            "좌석 API",
            "대기열 API",
            "예매 API",
            "결제 API",
            "관리자 - 이벤트",
            "관리자 - 매치",
            "관리자 - 좌석",
            "관리자 - 좌석등급",
            "관리자 - 구역",
            "관리자 - 팀",
            "관리자 - 예매"
    );

    @Bean
    public OpenAPI ticketMasterOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                // 태그 표시 순서 고정 — 사용자 API 먼저, 관리자 API 를 아래로 배치
                // (application.yaml 의 tags-sorter 를 끄면 이 순서가 그대로 반영됨)
                .tags(apiTags())
                // 전역 보안 요구 — Authorize 에 넣은 토큰을 모든 요청 Authorization 헤더에 자동 첨부
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, bearerScheme()));
    }

    // 스펙 생성이 끝난 뒤 태그를 TAG_ORDER 순서로 재정렬
    // (springdoc 이 태그를 HashSet 으로 모아 빈의 선언 순서를 무시하므로 후처리로 정렬)
    @Bean
    public OpenApiCustomizer tagOrderCustomizer() {
        return openApi -> {
            if (openApi.getTags() != null) {
                openApi.getTags().sort(Comparator.comparingInt(t -> {
                    int idx = TAG_ORDER.indexOf(t.getName());
                    return idx < 0 ? Integer.MAX_VALUE : idx;   // 목록에 없는 태그는 맨 뒤로
                }));
            }
        };
    }

    // 태그 노출 순서 — 위에서부터 차례로 표시됨
    // name + description 을 컨트롤러 @Tag 와 동일하게 맞춰야 중복 없이 하나로 병합됨
    private List<Tag> apiTags() {
        return List.of(
                tag("인증 API", "회원가입 / 로그인 / 토큰 재발급 / 비밀번호 재설정"),
                tag("사용자 API", "내 정보 조회 / 수정 / 비밀번호 변경 / 회원 탈퇴"),
                tag("이벤트 API", "이벤트(대회) 목록 / 상세 조회"),
                tag("좌석 API", "구역 / 좌석 조회 + 좌석 점유 / 해제 (동시성 제어)"),
                tag("대기열 API", "회차 대기열 진입 / 상태 폴링"),
                tag("예매 API", "예매 생성 / 조회 / 취소"),
                tag("결제 API", "토스페이먼츠 결제 승인 / 결제 상세 조회"),
                tag("관리자 - 이벤트", "관리자 이벤트 등록 / 조회 / 수정 / 삭제"),
                tag("관리자 - 매치", "관리자 매치(회차) 등록 / 조회 / 수정 / 삭제"),
                tag("관리자 - 좌석", "관리자 좌석 단건 / 일괄 등록 + 조회 / 수정 / 삭제"),
                tag("관리자 - 좌석등급", "관리자 좌석 등급 조회 / 등록 / 수정 / 삭제"),
                tag("관리자 - 구역", "관리자 구역 조회 / 등록 / 수정 / 삭제"),
                tag("관리자 - 팀", "관리자 팀 조회 / 등록 / 수정 / 삭제"),
                tag("관리자 - 예매", "관리자 예매 전체 조회 / 상세 조회")
        );
    }

    private Tag tag(String name, String description) {
        return new Tag().name(name).description(description);
    }

    // 문서 상단 메타 정보 - 제목/설명/버전
    private Info apiInfo() {
        return new Info()
                .title("TicketMaster API")
                .description("e스포츠 티켓 예매 서비스 REST API 문서")
                .version("v1.0.0");
    }

    // Authorization: Bearer {accessToken} 헤더 인증 정의
    private SecurityScheme bearerScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
    }
}
