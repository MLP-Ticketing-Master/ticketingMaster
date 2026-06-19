<img width="300" height="180" alt="KakaoTalk_20260508_164930307" src="https://github.com/user-attachments/assets/6cada366-d0ca-40e8-bfe1-d4f11e06295c" />

## **티켓팅마스터 (TicketingMaster)**

**트래픽 폭주는 대기열로 분산시키고, 좌석 동시성은 낙관적 락으로 처리하는 — 두 단계 방어 구조의 e스포츠 티켓 예매 시스템**

멀티캠퍼스 현대이지웰 Java 풀스택 개발자 아카데미 7회차 · 최종 프로젝트 1조


---

## 📌 목차

- [프로젝트 소개](#-프로젝트-소개)
- [주요 기능 데모](#-주요-기능-데모)
- [시스템 아키텍처](#-시스템-아키텍처)
- [ERD](#erd)
- [기술 스택](#-기술-스택)
- [폴더 구조](#-폴더-구조)
- [팀원 소개](#-팀원-소개)

---

## 🖥️ 프로젝트 소개

</p><img width="1536" height="815" alt="ChatGPT Image 2026년 6월 15일 오후 03_09_29" src="https://github.com/user-attachments/assets/d77d36d0-8849-47f0-b344-8df2d7ade90c" />


---


### 팀 정보
 
- **프로젝트명**: 티켓팅마스터 — 대기열 트래픽 분산 및 낙관적 락 기반 좌석 동시성 제어 e스포츠 티켓 예매 플랫폼
- **프로젝트 기간**: 2026.04.22 ~ 2026.06.24
- **배포 URL**: http://13.208.100.144/


### 기획 의도
 
1. 오픈 직후 동시 수천 명이 몰리는 트래픽을 대기열로 분산하여 서버 다운 없이 안정적으로 흡수.
2. DB 낙관적 락(`@Version`)으로 동일 좌석에 대한 동시 결제 요청을 제어하여 좌석 중복 예약을 0건으로 보장.
3. 결제와 좌석 확정 단계를 분리하고 멱등성·보상 트랜잭션을 적용하여 결제는 됐는데 좌석이 비는 정합성 문제를 방지.
4. k6 부하 테스트로 대기열 적용 전후, 낙관적 락의 한계 지점을 수치로 검증하여 데이터 기반으로 다음 설계를 결정.

---

## 🎬 주요 기능 데모

| **로그인 / 회원가입** | **이벤트 필터** |
|---|---|
| <img width="400" height="250" alt="login" src="https://github.com/user-attachments/assets/329c99bc-6795-4c87-b123-35d72313e081" /> | <img width="400" height="250" alt="filter" src="https://github.com/user-attachments/assets/46438b3c-8608-458d-aebb-517c9f8bdcf1" /> |

| **대기열 입장** | **좌석 선택 & 점유** |
|---|---|
| <img width="400" height="250" alt="대기열" src="https://github.com/user-attachments/assets/235c8529-f9cd-4fb9-9f8e-d108a456197f" />| <img width="400" height="250" alt="좌석섵낵" src="https://github.com/user-attachments/assets/12a0cbcc-3045-41dd-98bb-47272e48ee4c" /> |

| **결제 (토스페이먼츠)** | **예매 내역 / 취소** |
|---|---|
| <img width="400" height="250" alt="결제토스" src="https://github.com/user-attachments/assets/7e470050-26a6-4449-8e5d-50934737b76e" /> | <img width="400" height="250" alt="예매취소" src="https://github.com/user-attachments/assets/6ebbcbdd-40d1-4d68-a7c8-90de1b8c4797" /> |

| **관리자 페이지** | **비밀번호 찾기** |
|---|---|
| <img width="400" height="250" alt="관리자 페이지" src="https://github.com/user-attachments/assets/48c84191-1e12-4a12-87f1-153c7248e6dc" /> | <img width="400" height="250" alt="비밀번호 찾기" src="https://github.com/user-attachments/assets/b7c750b0-8a9a-4a35-9fe4-ae467f5f7a5a" /> |

---

## <img width="30" height="30" alt="image" src="https://github.com/user-attachments/assets/1aef54b0-80a4-4151-b16a-c05e51d23d9f" />  시스템 아키텍처

<img width="1536" height="1024" alt="Image20260616143304" src="https://github.com/user-attachments/assets/72583b66-d914-45c8-9c5c-fd41f05058bc" />

- React 프론트엔드 — Nginx가 정적 파일로 서빙 (별도 서버 아님)
- Nginx — 정적 서빙 + /api 리버스 프록시 (+ 좌석조회 응답캐시)
- Spring Boot — API 서버 (모놀리식)
- Redis — 대기열(Sorted Set) + JWT refresh 토큰
- Oracle — 메인 데이터 저장 (JPA, 좌석 낙관적 락)
- Toss Payments — 외부 결제 연동
- 전체가 단일 EC2에 도커 컴포즈 올인원

---

## <img width="20" height="20" alt="image" src="https://github.com/user-attachments/assets/0c97151c-263f-4b57-a310-31ce8ac7e2b6" />   CI/CD


<img width="1693" height="929" alt="Image20260616143315" src="https://github.com/user-attachments/assets/7eb30c1f-fb66-40ac-9924-0392679f603c" />

- CI (검증) — develop/main의 모든 push/PR에서 실행
     · Backend 빌드 + 테스트 (Testcontainers)
     · Frontend 빌드
- CD (배포) — main 머지(push) 시에만 자동 배포 (SSH → EC2 재배포)

---

## <img width="30" height="30" alt="image" src="https://github.com/user-attachments/assets/db838ae4-a0e6-486d-b35e-029bff21e12e" /> ERD

<img width="6676" height="8192" alt="Event Booking Management-2026-06-19-074316" src="https://github.com/user-attachments/assets/d26207c7-27ca-4ae5-8d25-1d15dc591370" />



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
├── backend/                             
│   └── src/
│       ├── main/
│       │   ├── java/com/ticketmaster/backend/
│       │   │   ├── admin/               
│       │   │   │   ├── booking/
│       │   │   │   ├── event/
│       │   │   │   ├── match/
│       │   │   │   ├── seat/
│       │   │   │   ├── seatgrade/
│       │   │   │   ├── section/
│       │   │   │   └── team/
│       │   │   ├── domain/           
│       │   │   │   ├── auth/
│       │   │   │   ├── booking/
│       │   │   │   ├── event/
│       │   │   │   ├── match/
│       │   │   │   ├── payment/
│       │   │   │   ├── queue/        
│       │   │   │   ├── seat/          
│       │   │   │   ├── team/
│       │   │   │   └── user/
│       │   │   └── global/              
│       │   │       ├── common/
│       │   │       ├── config/
│       │   │       ├── exception/
│       │   │       ├── security/         
│       │   │       └── util/
│       │   └── resources/
│       │       ├── db/
│       │       │   ├── migration/       
│       │       │   └── seed-loadtest.sql
│       │       ├── application.yaml
│       │       ├── application-prod.yaml
│       │       └── application-loadtest.yaml
│       └── test/
├── frontend/                            
│   └── src/
│       ├── components/                  
│       ├── pages/                  
│       ├── store/                       
│       ├── api/                         
│       └── types/                       
├── loadtest/                             
└── docker-compose.yml                    
```
---

## 🤝 팀 협업 문화
 
### 칸반 보드 기반 스프린트 운영

 
### 데일리 스크럼 & 코드 리뷰

<img width="1206" height="1130" alt="스크린샷 2026-06-19 174703" src="https://github.com/user-attachments/assets/c4e839d1-a95d-41c1-ab83-fc7924bc9e80" />
 
- 매일 스탠드업 미팅으로 진행 상황 공유
- PR 기반 코드 리뷰로 코드 품질 관리
- Notion으로 회의록 및 기술 문서 관리
---

## 👥 팀원 소개

| <img width="60" height="70" alt="Open Peeps - Avatar (4)" src="https://github.com/user-attachments/assets/dcd7cf4b-3907-4f8b-a2ef-3bc65ccd210c" /> | <img width="60" height="70" alt="Open Peeps - Avatar (1)" src="https://github.com/user-attachments/assets/90b47aa5-f40d-4ee4-8937-90f43cb4e645" /> | <img width="60" height="70" alt="Open Peeps - Avatar (3)" src="https://github.com/user-attachments/assets/f3ab6fe6-1249-4d90-b32a-dd61a87f41d6" /> |
|---|---|---|
| **이승헌👑** | **김소강** | **이지원** |
| Fullstack | Fullstack | Fullstack |
| [GitHub](https://github.com/2shoneycom) | [GitHub](https://github.com/cherry2766) | [GitHub](https://github.com/Koalaman0) |




<p align="center">




