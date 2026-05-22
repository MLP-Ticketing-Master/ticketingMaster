import { Navigate, Route, Routes } from "react-router-dom";
import { Toaster } from "sonner";
import {
  AdminLayout,
  AuthLayout,
  MyPageLayout,
  PublicLayout,
} from "@/components/layout";
import { BookingDialog } from "@/components/main/seat/BookingDialog";
import HomePage from "@/pages/HomePage";
import EventDetailPage from "@/pages/EventDetailPage";
import LoginPage from "@/pages/LoginPage";
import SignupPage from "@/pages/SignupPage";
import SchedulePage from "@/pages/SchedulePage";
import ProfilePage from "@/pages/my/ProfilePage";
import BookingHistoryPage from "@/pages/my/BookingHistoryPage";
import EditProfilePage from "@/pages/my/EditProfilePage";
import ChangePasswordPage from "@/pages/my/ChangePasswordPage";
import DashboardPage from "@/pages/admin/DashboardPage";
import EventsAdminPage from "@/pages/admin/EventsAdminPage";
import MatchesAdminPage from "@/pages/admin/MatchesAdminPage";
import TeamsAdminPage from "@/pages/admin/TeamsAdminPage";
import SeatsAdminPage from "@/pages/admin/SeatsAdminPage";
import BookingsAdminPage from "@/pages/admin/BookingsAdminPage";
import StatsAdminPage from "@/pages/admin/StatsAdminPage";
import PasswordResetPage from "@/pages/PasswordResetPage";

export default function App() {
  return (
    <>
      <Routes>
        <Route element={<PublicLayout />}>
          <Route index element={<HomePage />} />
          <Route path="events" element={<SchedulePage />} />
          <Route path="events/:id" element={<EventDetailPage />} />
        </Route>

        <Route element={<AuthLayout />}>
          <Route path="login" element={<LoginPage />} />
          <Route path="signup" element={<SignupPage />} />
        </Route>

        {/* 이메일 링크로 접근하는 비밀번호 재설정 페이지 (/password-reset?token=...) */}
        <Route path="password-reset" element={<PasswordResetPage />} />

        <Route path="my" element={<MyPageLayout />}>
          <Route index element={<ProfilePage />} />
          <Route path="bookings" element={<BookingHistoryPage />} />
          <Route path="profile" element={<EditProfilePage />} />
          <Route path="password" element={<ChangePasswordPage />} />
        </Route>

        <Route path="admin" element={<AdminLayout />}>
          <Route index element={<DashboardPage />} />
          <Route path="events" element={<EventsAdminPage />} />
          <Route path="matches" element={<MatchesAdminPage />} />
          <Route path="teams" element={<TeamsAdminPage />} />
          <Route path="seats" element={<SeatsAdminPage />} />
          <Route path="bookings" element={<BookingsAdminPage />} />
          <Route path="stats" element={<StatsAdminPage />} />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>

      <BookingDialog />
      <Toaster richColors position="top-center" />
    </>
  );
}
