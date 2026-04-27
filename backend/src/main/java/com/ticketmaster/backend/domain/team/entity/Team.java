package com.ticketmaster.backend.domain.team.entity;

import com.ticketmaster.backend.domain.event.entity.SportType;
import com.ticketmaster.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@Entity
@Table(name = "teams")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Team extends BaseEntity {

    /** 팀 고유 ID (PK) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 팀명 (예: T1, Gen.G) — 필수, 최대 100자 */
    @Column(nullable = false, length = 100)
    private String name;

    /** 팀 로고 이미지 URL — 선택, 최대 500자 */
    @Column(name = "logo_image_url", length = 500)
    private String logoImageUrl;

    /** 종목 구분 (LOL / VALORANT / OVERWATCH 등) — 필수 */
    @Enumerated(EnumType.STRING)
    @Column(name = "sport_type", nullable = false, length = 30)
    private SportType sportType;

    /**
     * 빌더 전용 생성자
     *
     * <p>private으로 막아 두어 외부에서는 반드시 {@code Team.builder()...build()}
     * 형태로만 객체를 생성하도록 강제한다.
     * 이렇게 하면 필드 순서가 바뀌어도 안전하고, 생성 시 의도가 명확해진다
     */

    @Builder
    private Team(String name, String logoImageUrl, SportType sportType) {
        this.name = name;
        this.logoImageUrl = logoImageUrl;
        this.sportType = sportType;
    }
    /**팀 정보 수정 (PATCH 시맨틱)
     *null이 아닌 필드만 변경하므로,
     *클라이언트가 수정하고 싶은 값만 보내면 된다.
     *예시: {@code team.update("T1 Esports", null, null)} → 이름만 수정되고 로고/종목은 유지된다.*/
    public void update(String name, String logoImageUrl, SportType sportType) {
        if (name != null) this.name = name;
        if (logoImageUrl != null) this.logoImageUrl = logoImageUrl;
        if (sportType != null) this.sportType = sportType;
    }
}
