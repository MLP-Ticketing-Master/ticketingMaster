<img width="300" height="180" alt="KakaoTalk_20260508_164930307" src="https://github.com/user-attachments/assets/6cada366-d0ca-40e8-bfe1-d4f11e06295c" />

#  티켓팅마스터 (TicketingMaster)

> 멀티캠퍼스 현대이지웰 Java 풀스택 개발자 아카데미 7회차 · 최종 프로젝트 1조

**트래픽 폭주는 대기열로 분산시키고, 좌석 동시성은 낙관적 락으로 처리하는 — 두 단계 방어 구조의 e스포츠 티켓 예매 시스템**

- 📄 [프로젝트 문서 (Notion)]()
- 📌 [API 명세서]()

---

## 📌 목차

- [프로젝트 소개](#-프로젝트-소개)
- [주요 기능 데모](#-주요-기능-데모)
- [시스템 아키텍처](#-시스템-아키텍처)
- [ERD](#erd)
- [기술 스택](#-기술-스택)
- [폴더 구조](#-폴더-구조)
- [실행 방법](#-실행-방법)
- [팀 협업 문화](#-팀-협업-문화)
- [팀원 소개](#-팀원-소개)

---

## 🖥️ 프로젝트 소개

</p><img width="1536" height="815" alt="ChatGPT Image 2026년 6월 15일 오후 03_09_29" src="https://github.com/user-attachments/assets/d77d36d0-8849-47f0-b344-8df2d7ade90c" />

---

| 구분 | 내용 |
|------|------|
| 프로젝트 기간 | 2026.04.22 ~ 2026.06.24 |
| 팀 구성 | 풀스택 3명 |
| 기준 시나리오 | LCK 2026 스프링 개막전 T1 vs Gen.G, LoL Park 400석 |
| 아키텍처 | 모놀리식 (Spring Boot + React) |
| 배포 URL | http://13.208.100.144/ |

---

## 🎬 주요 기능 데모

| **로그인 / 회원가입** | **이벤트 필터** |
|---|---|
| <img width="400" height="250" alt="login" src="https://github.com/user-attachments/assets/329c99bc-6795-4c87-b123-35d72313e081" /> | <img width="400" height="250" alt="filter" src="https://github.com/user-attachments/assets/46438b3c-8608-458d-aebb-517c9f8bdcf1" /> |

| **대기열 입장** | **좌석 선택 & 점유** |
|---|---|
| <img width="400" height="250" alt="대기열" src="https://github.com/user-attachments/assets/235c8529-f9cd-4fb9-9f8e-d108a456197f" />| 스크린샷 또는 GIF |

| **결제 (토스페이먼츠)** | **예매 내역 / 취소** |
|---|---|
| 스크린샷 또는 GIF | 스크린샷 또는 GIF |

---

## 🏗️ 시스템 아키텍처

> 아키텍처 다이어그램 이미지를 여기에 첨부해 주세요

```
[Client (React + TypeScript)]
          │  Polling (3초 주기)
          ▼
[Spring Boot API Server]
  · JWT 인증 (Access: Authorization 헤더)
  · Queue-Token 헤더 검증 (Spring Filter)
  · @Scheduled 점유 만료 해제 (30초)
  · @Scheduled 대기열 승격 (10초)
          │
    ┌─────┴──────┐
    ▼            ▼
[Oracle 23ai]  [Redis 8]
  JPA @Version  Sorted Set 대기열
  낙관적 락     입장 토큰 / 매진 플래그

[Docker Compose] — 전체 환경 단일 구성
[GitHub Actions] — CI
[k6] — 부하 테스트
```

---

## ERD

<img width="3311" height="2002" alt="티켓팅마스터" src="https://github.com/user-attachments/assets/57731c55-6fc8-4728-9f73-21e3adc4fef3" />



---

## 🛠️ 기술 스택

### Backend
![Java](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Oracle](https://img.shields.io/badge/Oracle_Database-F80000?style=for-the-badge&logo=oracle&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=for-the-badge&logo=flyway&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)

### Frontend
![React](https://img.shields.io/badge/React_19-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white)
![Zustand](https://img.shields.io/badge/Zustand-443E38?style=for-the-badge&logoColor=white)
![TanStack Query](https://img.shields.io/badge/TanStack_Query-FF4154?style=for-the-badge&logo=reactquery&logoColor=white)
![TailwindCSS](https://img.shields.io/badge/Tailwind_CSS-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)
![shadcn/ui](https://img.shields.io/badge/shadcn/ui-000000?style=for-the-badge&logo=shadcnui&logoColor=white)

### Infrastructure
![Nginx](https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white)
![Docker Compose](https://img.shields.io/badge/Docker_Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![AWS EC2](https://img.shields.io/badge/AWS_EC2-FF9900?style=for-the-badge&logo=amazonaws&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)

### External Service
![Toss Payments](https://img.shields.io/badge/Toss_Payments-0064FF?style=for-the-badge&logoColor=white)


---


## 📁 폴더 구조

```
ticketingMaster/
├── backend/                   # Spring Boot 서버 (Java 21)
│   └── src/main/java/
│       ├── controller/        # REST API 컨트롤러
│       ├── service/           # 비즈니스 로직
│       ├── repository/        # DB 접근 (Spring Data JPA)
│       ├── domain/            # 엔티티 & DTO
│       └── scheduler/         # 점유 만료·대기열 승격 스케줄러
├── frontend/                  # React + TypeScript 클라이언트
│   └── src/
│       ├── components/        # 공통 UI 컴포넌트 (shadcn/ui)
│       ├── pages/             # 페이지 단위 컴포넌트
│       ├── store/             # 상태 관리 (Zustand)
│       ├── api/               # API 호출 (TanStack Query)
│       └── types/             # TypeScript 타입 정의
├── loadtest/                  # k6 부하 테스트 스크립트 (시나리오 A~F)
└── docker-compose.yml         # 전체 환경 단일 구성
```

---

## 🚀 실행 방법

### 사전 준비
- Java 21+
- Node.js 18+
- Docker & Docker Compose

### ✅ Docker로 실행 (권장)

```bash
# 1. 레포지토리 클론
git clone https://github.com/MLP-Ticketing-Master/ticketingMaster.git
cd ticketingMaster

# 2. 환경 변수 설정
cp .env.example .env
# .env 파일을 열어 필요한 값 입력 (Oracle, Redis, JWT 시크릿, 토스 API 키 등)

# 3. 전체 서비스 실행
docker-compose up -d
```

| 서비스 | 주소 |
|--------|------|
| 프론트엔드 | http://localhost:5173 |
| 백엔드 API | http://localhost:8080 |

---

### 개별 실행

**Backend**
```bash
cd backend
./gradlew bootRun
```

**Frontend**
```bash
cd frontend
npm install
npm run dev
```

**부하 테스트 (k6)**
```bash
cd loadtest
k6 run scenario-a-seat-concurrency.js
```

---

## 🤝 팀 협업 문화

### 칸반 보드 기반 스프린트 운영
> 스프린트 보드 이미지를 여기에 첨부해 주세요

### 데일리 스크럼 & 코드 리뷰
> PR 코드 리뷰 스크린샷 등을 여기에 첨부해 주세요

- 매일 스탠드업 미팅으로 진행 상황 공유
- PR 기반 코드 리뷰로 코드 품질 관리
- Notion으로 회의록 및 기술 문서 관리

---

## 👥 팀원 소개

| | | |
|---|---|---|
| **이름** | **이름** | **이름** |
| Fullstack | Fullstack | Fullstack |
| [GitHub]() | [GitHub]() | [GitHub]() |

> [자세한 팀원 소개 (Notion)]()

---

<p align="center">
  멀티캠퍼스 현대이지웰 Java 풀스택 개발자 아카데미 7회차 · 1조

<p align="center">




