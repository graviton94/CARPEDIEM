import Foundation

/// 따옴표로 감싼 칸과 칸 안의 쉼표를 지원하는 작은 CSV 읽기. `#`으로 시작하는 줄은 주석.
enum CSV {
    static func rows(_ text: String) -> [[String]] {
        var rows: [[String]] = []
        var row: [String] = []
        var field = ""
        var quoted = false
        var chars = Array(text.replacingOccurrences(of: "\r\n", with: "\n"))
        chars.append("\n")
        var i = 0
        while i < chars.count {
            let c = chars[i]
            if quoted {
                if c == "\"" {
                    if i + 1 < chars.count, chars[i + 1] == "\"" { field.append("\""); i += 1 } else { quoted = false }
                } else {
                    field.append(c)
                }
            } else {
                switch c {
                case "\"": quoted = true
                case ",": row.append(field); field = ""
                case "\n":
                    row.append(field); field = ""
                    let isComment = row.first?.hasPrefix("#") ?? false
                    let isEmpty = row.count == 1 && row[0].isEmpty
                    if !isComment && !isEmpty { rows.append(row) }
                    row = []
                default: field.append(c)
                }
            }
            i += 1
        }
        return rows
    }

    /// 첫 줄을 머리글로 보고 [머리글: 값] 목록을 돌려준다.
    static func records(_ text: String) -> [[String: String]] {
        let all = rows(text)
        guard let header = all.first else { return [] }
        return all.dropFirst().map { values in
            Dictionary(uniqueKeysWithValues: header.enumerated().map { ($1, $0 < values.count ? values[$0] : "") })
        }
    }

    static func bundled(_ name: String, bundle: Bundle = .main) -> String {
        guard let url = bundle.url(forResource: name, withExtension: "csv"),
              let text = try? String(contentsOf: url, encoding: .utf8) else { return "" }
        return text
    }
}
