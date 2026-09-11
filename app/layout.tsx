import type { Metadata } from "next";
import "./globals.css";
import FeedbackChatClient from "./feedback-chat-client";

export const metadata: Metadata = {
  title: "סקר",
  description: "שליחת שאלון ללקוח אחרי עבודה",
};

// The app is Hebrew end to end, so the document says so — the phone's own
// locale does not decide how this reads.
export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="he" dir="rtl">
      <body>
        {children}
        <FeedbackChatClient />
      </body>
    </html>
  );
}
