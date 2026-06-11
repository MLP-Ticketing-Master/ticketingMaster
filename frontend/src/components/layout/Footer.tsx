export function Footer() {
  return (
    <footer className="mt-20 bg-[#2D2F3E] text-gray-300">
      <div className="mx-auto max-w-7xl px-6 py-6 md:py-8">
        {/* 모바일: 브랜드(상단 full) + 3컬럼(하단) / PC: 4컬럼 한 줄 */}
        <div className="grid grid-cols-3 gap-4 md:grid-cols-4 md:gap-8">
          <div className="col-span-3 md:col-span-1">
            <h3 className="text-sm font-bold text-white md:text-lg">
              티켓팅마스터
            </h3>
            <p className="mt-1 text-xs text-gray-400 md:mt-2 md:text-sm">
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

        <div className="mt-6 border-t border-white/10 pt-3 text-xs text-gray-500 md:mt-8 md:pt-4 md:text-sm">
          © 2026 티켓팅마스터. All rights reserved.
        </div>
      </div>
    </footer>
  );
}

function FooterColumn({ title, items }: { title: string; items: string[] }) {
  return (
    <div>
      <h4 className="text-xs font-semibold text-white md:text-base">
        {title}
      </h4>
      <ul className="mt-2 space-y-1.5 text-[11px] md:mt-3 md:space-y-2 md:text-sm">
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
