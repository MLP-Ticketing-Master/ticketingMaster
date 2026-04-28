package com.ticketmaster.backend.domain.team.repository;

import com.ticketmaster.backend.domain.event.entity.SportType;
import com.ticketmaster.backend.domain.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdminTeamRepository extends JpaRepository<Team, Long> {
    /** 팀명 중복 체크 - 팀 등록/수정 시 동일한 이름이 이미 존재하는지 확인할 때 사용 */
    boolean existsByName(String name);


    /** 전체 팀 목록 조회 (최신순) - BaseEntity의 {@code createdAt} 필드를 기준으로 내림차순 정렬한다 */
    List<Team> findAllBySportTypeOrderByCreatedAtDesc(SportType sportType);

    /** 종목별 팀 목록 조회 (최신순) - 예: {@code findAllBySportTypeOrderByCreatedAtDesc(SportType.LOL)} → LOL 종목 팀들만 최신순으로 반환 */
    List<Team> findAllByOrderByCreatedAtDesc();

    /** 팀명으로 팀 조회 - 삭제된 행까지 포함해서 조회 (재등록 시 복구용) */
    @Query(value = "SELECT * FROM teams WHERE name = :name", nativeQuery = true)
    Optional<Team> findByNameIncludingDeleted(@Param("name") String name);
}
