package com.ticketmaster.backend.domain.event.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SportType {
    LOL("리그 오브 레전드"),         // LOL 챔피언스 코리아 2026 스프링 결승
    VALORANT("발로란트"),           // 발로란트 챔피언스 투어 코리아
    OVERWATCH("오버워치"),          // 오버워치 리그 서울 다이너스티
    TFT("전략적 팀 전투"),           // TFT 챔피언스 코리아
    PUBG("배틀그라운드"),            // 배틀그라운드 프로리그 시즌3
    STARCRAFT("스타크래프트 2");     // 스타크래프트2 GSL 코드S
    // TODO: 종목 추가 시 여기 한 줄 추가

    private final String displayName;
}
