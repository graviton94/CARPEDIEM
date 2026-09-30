import SwiftUI

enum QuoteText {
    /// 설정한 언어에 따라 크게 보여줄 문장. ‘둘 다’면 한글이 크게, 영문이 작게.
    static func primary(_ q: Quote, language: QuoteLanguage) -> String {
        language == .english ? q.english : q.korean
    }

    static func secondary(_ q: Quote, language: QuoteLanguage) -> String? {
        language == .both ? q.english : nil
    }
}

extension TypeToken {
    /// 같은 크기·스타일에 명조 서체를 쓴 토큰 (문장용).
    var serifVariant: TypeToken { TypeToken(size: size, style: style, family: .serif, weight: .medium, tracking: tracking) }
}
