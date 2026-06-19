<img width="300" height="180" alt="KakaoTalk_20260508_164930307" src="https://github.com/user-attachments/assets/6cada366-d0ca-40e8-bfe1-d4f11e06295c" />

#  티켓팅마스터 (TicketingMaster)

**트래픽 폭주는 대기열로 분산시키고, 좌석 동시성은 낙관적 락으로 처리하는 — 두 단계 방어 구조의 e스포츠 티켓 예매 시스템**

> 멀티캠퍼스 현대이지웰 Java 풀스택 개발자 아카데미 7회차 · 최종 프로젝트 1조



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
| <img width="400" height="250" alt="대기열" src="https://github.com/user-attachments/assets/235c8529-f9cd-4fb9-9f8e-d108a456197f" />| <img width="400" height="250" alt="좌석섵낵" src="https://github.com/user-attachments/assets/12a0cbcc-3045-41dd-98bb-47272e48ee4c" /> |

| **결제 (토스페이먼츠)** | **예매 내역 / 취소** |
|---|---|
| <img width="400" height="250" alt="결제토스" src="https://github.com/user-attachments/assets/7e470050-26a6-4449-8e5d-50934737b76e" /> | <img width="400" height="250" alt="예매취소" src="https://github.com/user-attachments/assets/6ebbcbdd-40d1-4d68-a7c8-90de1b8c4797" /> |

| **관리자 페이지** | **비밀번호 찾기** |
|---|---|
| <img width="400" height="250" alt="관리자 페이지" src="https://github.com/user-attachments/assets/48c84191-1e12-4a12-87f1-153c7248e6dc" /> | <img width="400" height="250" alt="비밀번호 찾기" src="https://github.com/user-attachments/assets/b7c750b0-8a9a-4a35-9fe4-ae467f5f7a5a" /> |

---

## 🏗️ 시스템 아키텍처

<img width="800" height="900" alt="system architecture" src="https://github.com/user-attachments/assets/867badb6-fba4-45c0-a27a-fd2ad0417be7" />

---

## ERD

<img width="1536" height="1024" alt="ChatGPT Image 2026년 6월 16일 오후 01_49_26" src="https://github.com/user-attachments/assets/cfab4159-b6a3-410c-8f89-cb938f67b5c1" />



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
> 스프린트 보드 이미지를 여기에 첨부해 주세요

### 데일리 스크럼 & 코드 리뷰
> PR 코드 리뷰 스크린샷 등을 여기에 첨부해 주세요

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




