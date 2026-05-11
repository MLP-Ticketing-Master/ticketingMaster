export function Footer() {
  return (
    <footer className="mt-20 bg-[#2D2F3E] text-gray-300">
      <div className="mx-auto max-w-7xl px-6 py-12">
        <div className="grid gap-10 md:grid-cols-4">
          <div>
            <h3 className="text-lg font-bold text-white">티켓팅마스터</h3>
            <p className="mt-2 text-sm text-gray-400">
              최고의 E스포츠 경기 예매 플랫폼
            </p>
          </div>

          <FooterColumn
            title="고객센터"
            items={["공지사항", "자주 묻는 질문", "1:1 문의"]}
          />
          <FooterColumn
            title="이용안내"
            items={["이용약관", "개인정보처리방침", "환불정책"]}
          />
          <FooterColumn
            title="회사정보"
            items={["회사소개", "제휴문의", "채용정보"]}
          />
        </div>

        <div className="mt-10 border-t border-white/10 pt-6 text-sm text-gray-500">
          © 2026 티켓팅마스터. All rights reserved.
        </div>
      </div>
    </footer>
  );
}

function FooterColumn({ title, items }: { title: string; items: string[] }) {
  return (
    <div>
      <h4 className="font-semibold text-white">{title}</h4>
      <ul className="mt-3 space-y-2 text-sm">
        {items.map((it) => (
          <li
            key={it}
            className="cursor-pointer text-gray-400 hover:text-white"
          >
            {it}
          </li>
        ))}
      </ul>
    </div>
  );
}
