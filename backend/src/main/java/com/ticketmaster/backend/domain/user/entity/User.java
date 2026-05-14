package com.ticketmaster.backend.domain.user.entity;

import com.ticketmaster.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    @SequenceGenerator(name = "user_seq", sequenceName = "USER_SEQ", allocationSize = 50)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /** 암호화된 비밀번호. 평문 저장 금지 */
    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING) // DB에 enum 값을 문자열로 저장하겠다는 뜻
    @Column(nullable = false, length = 20)
    private Role role;

    /**
     * 정적 팩토리 메서드 = "객체를 만들어 돌려주는 static 메서드"
     * 비밀번호 인코딩은 Service 레이어에서 끝낸 뒤 여기로 넘기기
     */
    /** 회원가입 - 신규 USER 생성 */
    public static User create(String email, String encodedPassword, String nickname, String phone) {
        User user = new User();
        user.email = email;
        user.password = encodedPassword;
        user.nickname = nickname;
        user.phone = phone;
        user.role = Role.USER;
        return user;
    }

    public void withdraw() {
        super.softDelete();
    }

    /** 프로필 수정 - null이 아닌 값만 변경 */
    public void updateProfile(String nickname, String phone) {
        if (nickname != null) this.nickname = nickname;
        if (phone != null) this.phone = phone;
    }

    /** 비밀번호 변경 */
    public void changePassword(String newEncodedPassword) {
        this.password = newEncodedPassword;
    }
}
