package com.ticketmaster.backend.global.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 모든 엔티티가 공통으로 가지는 시간 필드 + soft delete 필드를 담는 부모 클래스
 *
 * - @MappedSuperclass : 테이블로 생성되지 않고, 상속받은 엔티티에 필드만 포함됨
 * - @EntityListeners(AuditingEntityListener.class) : @CreatedDate, @LastModifiedDate를 자동 채움.
 *   (활성화하려면 @EnableJpaAuditing 설정 클래스가 있어야 함 → JpaAuditingConfig 참고)
 *
 * 사용 예:
 *   @Entity
 *   public class User extends BaseEntity { ... }
 */

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /** 생성 일시 - insert 시점에 자동 세팅, 이후 변경 X */
    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    /** 수정 일시 - update 마다 자동 갱신 */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 소프트 삭제 일시
     * - null : 활성 데이터
     * - 값 존재 : 삭제된 데이터
     *
     * 실제 삭제 대신 deletedAt 만 채우는 정책
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** 소프트 삭제 처리 */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /** 복구 (관리자 기능에서 사용) */
    public void restore() {
        this.deletedAt = null;
    }

    /** 삭제 여부 확인 */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
