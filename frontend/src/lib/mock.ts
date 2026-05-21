import type {
  BookingItem,
  EventDetail,
  EventSummary,
  Match,
  Seat,
  SeatGrade,
  Section,
  Team,
  TicketPrice,
  User,
} from "@/types";

export const MOCK_USER: User = {
  id: 1,
  nickname: "홍길동",
  email: "hong@example.com",
  phone: "010-1234-5678",
  joinedAt: "2026-01-15",
  role: "USER",
};

export const MOCK_PRICES: TicketPrice[] = [
  { gradeCode: "VIP", gradeName: "VIP석", price: 150_000, color: "violet" },
  { gradeCode: "R", gradeName: "R석", price: 120_000, color: "red" },
  { gradeCode: "S", gradeName: "S석", price: 90_000, color: "blue" },
  { gradeCode: "A", gradeName: "A석", price: 70_000, color: "green" },
];

export const MOCK_EVENTS: EventSummary[] = [
  {
    id: 1,
    title: "LOL 챔피언스 코리아 2026 스프링 결승",
    game: "LOL",
    venue: "LoL Park",
    startDate: "2026-04-24",
    endDate: "2026-04-26",
    posterUrl:
      "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800",
    status: "ON_SALE",
  },
  {
    id: 2,
    title: "발로란트 챔피언스 투어 코리아",
    game: "VALORANT",
    venue: "코엑스 컨벤션홀",
    startDate: "2026-04-25",
    endDate: "2026-05-15",
    posterUrl:
      "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=800",
    status: "ON_SALE",
  },
  {
    id: 3,
    title: "오버워치 리그 서울 다이너스티",
    game: "OVERWATCH",
    venue: "e스타디움",
    startDate: "2026-04-28",
    endDate: "2026-05-05",
    posterUrl:
      "https://images.unsplash.com/photo-1593305841991-05c297ba4575?w=800",
    status: "ON_SALE",
  },
  {
    id: 4,
    title: "카운터 스트라이크 마스터즈",
    game: "VALORANT",
    venue: "서울 e스포츠 경기장",
    startDate: "2026-05-01",
    endDate: "2026-05-30",
    posterUrl:
      "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=800",
    status: "ON_SALE",
  },
  {
    id: 5,
    title: "배틀그라운드 프로리그 시즌3",
    game: "OVERWATCH",
    venue: "부산 벡스코",
    startDate: "2026-05-05",
    endDate: "2026-05-12",
    posterUrl:
      "https://images.unsplash.com/photo-1493711662062-fa541adb3fc8?w=800",
    status: "SCHEDULED",
  },
  {
    id: 6,
    title: "스타크래프트2 GSL 코드S",
    game: "LOL",
    venue: "프릭업 스튜디오",
    startDate: "2026-05-10",
    endDate: "2026-05-20",
    posterUrl:
      "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800",
    status: "SCHEDULED",
  },
];

export const MOCK_MATCHES: Match[] = [
  {
    id: 11002,
    eventId: 1,
    matchNo: 1,
    matchTitle: "1경기 - 예매 대기",
    matchUp: "T1 vs Gen.G",
    startAt: "2026-04-24T18:30:00",
    status: "SCHEDULED",
    totalSeats: 500,
    soldSeats: 234,
  },
  {
    id: 11003,
    eventId: 1,
    matchNo: 2,
    matchTitle: "2경기 - 예매 진행 중",
    matchUp: "T1 vs Gen.G",
    startAt: "2026-04-24T20:00:00",
    status: "SCHEDULED",
    totalSeats: 500,
    soldSeats: 189,
  },
  {
    id: 11004,
    eventId: 1,
    matchNo: 3,
    matchTitle: "3경기 - 예매 마감",
    matchUp: "T1 vs Gen.G",
    startAt: "2026-04-25T17:00:00",
    status: "SCHEDULED",
    totalSeats: 400,
    soldSeats: 120,
  },
];

export const MOCK_EVENT_DETAIL: EventDetail = {
  ...MOCK_EVENTS[0],
  description: "최고의 LOL 팀들이 격돌하는 2026 스프링 결승전",
  durationMinutes: 240,
  ageLimit: "전체관람가",
  prices: MOCK_PRICES,
  matches: MOCK_MATCHES.filter((r) => r.eventId === 1),
};

export const MOCK_SECTIONS: Section[] = [
  { id: 1, name: "A", description: "메인 스크린 좌측 영역", sortOrder: 1 },
  { id: 2, name: "B", description: "메인 스크린 중앙 영역 (최고 시야)", sortOrder: 2},
  { id: 3, name: "C", description: "메인 스크린 중앙 영역 (최고 시야)", sortOrder: 3},
  { id: 4, name: "D", description: "메인 스크린 우측 영역", sortOrder: 4 },
];

export const MOCK_SEAT_GRADES: SeatGrade[] = [
  {
    code: "VIP",
    name: "VIP석",
    price: 150_000,
    color: "violet",
    sortOrder: 1,
    remaining: 56,
  },
  {
    code: "R",
    name: "R석",
    price: 120_000,
    color: "red",
    sortOrder: 2,
    remaining: 49,
  },
  {
    code: "S",
    name: "S석",
    price: 90_000,
    color: "blue",
    sortOrder: 3,
    remaining: 47,
  },
  {
    code: "A",
    name: "A석",
    price: 70_000,
    color: "green",
    sortOrder: 4,
    remaining: 55,
  },
];

const buildRow = (
  row: string,
  count: number,
  gradeCode: Seat["gradeCode"],
  startId: number,
): Seat[] =>
  Array.from({ length: count }, (_, i) => ({
    id: startId + i,
    row,
    number: i + 1,
    sectionId: 2,
    gradeCode,
    status: Math.random() > 0.7 ? "SOLD" : "AVAILABLE",
  }));

export const MOCK_SEAT_LAYOUT = {
  rows: ["A", "B", "C", "D", "E", "F", "G", "H", "I", "J"],
  cols: 10,
  seats: [
    ...buildRow("A", 10, "VIP", 100),
    ...buildRow("B", 10, "VIP", 200),
    ...buildRow("C", 10, "R", 300),
    ...buildRow("D", 10, "R", 400),
    ...buildRow("E", 10, "S", 500),
    ...buildRow("F", 10, "S", 600),
    ...buildRow("G", 10, "A", 700),
    ...buildRow("H", 10, "A", 800),
    ...buildRow("I", 10, "A", 900),
    ...buildRow("J", 10, "A", 1000),
  ],
};

export const MOCK_BOOKINGS: BookingItem[] = [
  {
    id: 1,
    bookingNo: "B202604240001",
    eventTitle: "LOL 챔피언스 코리아 2026 스프링 결승",
    roundLabel: "결승 1경기",
    startAt: "2026-04-24T18:30:00",
    venue: "LoL Park",
    seatLabels: ["VIP석 A5-12"],
    amount: 150_000,
    status: "CONFIRMED",
    bookedAt: "2026-04-20T15:30:00",
    customerName: "홍길동",
    customerEmail: "hong@example.com",
    paymentMethod: "카드",
  },
  {
    id: 2,
    bookingNo: "B202604250002",
    eventTitle: "발로란트 챔피언스 투어 코리아",
    roundLabel: "8강 1경기",
    startAt: "2026-04-25T17:00:00",
    venue: "코엑스 컨벤션홀",
    seatLabels: ["R석 B3-8", "R석 B3-9"],
    amount: 240_000,
    status: "CONFIRMED",
    bookedAt: "2026-04-21T10:15:00",
    customerName: "김철수",
    customerEmail: "kim@example.com",
    paymentMethod: "간편결제",
  },
  {
    id: 3,
    bookingNo: "B202604190003",
    eventTitle: "오버워치 리그 서울 다이너스티",
    roundLabel: "정규 시즌",
    startAt: "2026-03-15T19:00:00",
    venue: "e스타디움",
    seatLabels: ["S석 C2-15"],
    amount: 90_000,
    status: "WATCHED",
    bookedAt: "2026-04-19T14:20:00",
    customerName: "이영희",
    customerEmail: "lee@example.com",
    paymentMethod: "카드",
  },
  {
    id: 4,
    bookingNo: "B202604220004",
    eventTitle: "TFT 월드 챔피언십 코리아 예선",
    roundLabel: "예선 1라운드",
    startAt: "2026-05-01T16:00:00",
    venue: "LoL Park",
    seatLabels: ["A석 D1-5", "D1-6"],
    amount: 140_000,
    status: "PENDING_PAYMENT",
    bookedAt: "2026-04-22T09:45:00",
    customerName: "박민수",
    customerEmail: "park@example.com",
    paymentMethod: "계좌이체",
  },
];

export const MOCK_TEAMS: Team[] = [
  {
    id: 1,
    name: "T1",
    game: "LOL",
    logoUrl:
      "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=200",
    totalMatches: 15,
    registeredAt: "2025-12-01",
  },
  {
    id: 2,
    name: "Gen.G",
    game: "LOL",
    logoUrl:
      "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=200",
    totalMatches: 12,
    registeredAt: "2025-12-01",
  },
  {
    id: 3,
    name: "DRX",
    game: "VALORANT",
    logoUrl:
      "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=200",
    totalMatches: 8,
    registeredAt: "2025-12-15",
  },
  {
    id: 4,
    name: "DAMWON",
    game: "VALORANT",
    logoUrl:
      "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=200",
    totalMatches: 6,
    registeredAt: "2025-12-15",
  },
  {
    id: 5,
    name: "서울 다이너스티",
    game: "OVERWATCH",
    logoUrl:
      "https://images.unsplash.com/photo-1593305841991-05c297ba4575?w=200",
    totalMatches: 10,
    registeredAt: "2025-11-20",
  },
];
