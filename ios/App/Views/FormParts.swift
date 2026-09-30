import SwiftUI

/// 유리 카드 안의 한 줄.
struct FormRow<Trailing: View>: View {
    let title: String
    @ViewBuilder var trailing: Trailing

    var body: some View {
        HStack(spacing: Tokens.Space.sp3) {
            Text(title).textStyle(Tokens.TypeScale.body)
            Spacer(minLength: Tokens.Space.sp2)
            trailing
        }
        .frame(minHeight: Tokens.Layout.tapTarget)
        .contentShape(Rectangle()) // 가운데 빈 곳을 눌러도 반응하도록
    }
}

struct FormSection<Content: View>: View {
    var header: String?
    var footer: String?
    @ViewBuilder var content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: Tokens.Space.sp2) {
            if let header {
                Text(header).textStyle(Tokens.TypeScale.footnote).foregroundStyle(Color.cdSecondary).padding(.horizontal, Tokens.Space.sp4)
            }
            GlassCard(padding: Tokens.Space.sp4) {
                VStack(spacing: 0) { content }
            }
            if let footer {
                Text(footer).textStyle(Tokens.TypeScale.footnote).foregroundStyle(Color.cdSecondary).padding(.horizontal, Tokens.Space.sp4)
            }
        }
    }
}

struct RowDivider: View {
    var body: some View { Rectangle().fill(Color.cdDim).frame(height: Tokens.Stroke.hair) }
}

/// 선택지를 알약 모양으로 나란히 두는 선택기.
struct ChipPicker<Value: Hashable>: View {
    let options: [Value]
    @Binding var selection: Value
    let label: (Value) -> String

    var body: some View {
        HStack(spacing: Tokens.Space.sp1) {
            ForEach(options, id: \.self) { option in
                let on = option == selection
                Button { selection = option } label: {
                    Text(label(option))
                        .textStyle(Tokens.TypeScale.subhead)
                        .fontWeight(on ? .bold : .medium)
                        .foregroundStyle(on ? Color.cdOnOlive : Color.cdForeground)
                        .padding(.horizontal, Tokens.Space.sp3)
                        .padding(.vertical, Tokens.Space.sp1)
                        .background(Capsule().fill(on ? Color.cdOlive : Color.cdDim))
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(on ? .isSelected : [])
            }
        }
    }
}

/// 생년월일 · 나라 · 성별 · 기대수명 입력 (온보딩과 설정에서 함께 씀).
struct ProfileFields: View {
    @Binding var draft: LifeProfile
    private let table = LifeExpectancyTable.shared

    private enum Mode: Hashable { case auto, custom }

    private var mode: Binding<Mode> {
        Binding(get: { draft.customExpectancy == nil ? .auto : .custom },
                set: { draft.customExpectancy = $0 == .auto ? nil : draft.expectancy(in: table) })
    }

    var body: some View {
        FormSection(header: L10n.you) {
            FormRow(title: L10n.birthday) {
                DatePicker("", selection: $draft.birthDate, in: ...Date.now, displayedComponents: .date).labelsHidden()
            }
            RowDivider()
            NavigationLink(value: Route.country) {
                FormRow(title: L10n.country) {
                    Text(table.country(draft.countryCode)?.name ?? draft.countryCode).foregroundStyle(Color.cdSecondary)
                    Image(systemName: "chevron.right").foregroundStyle(Color.cdSecondary).imageScale(.small)
                }
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier("country")
            RowDivider()
            FormRow(title: L10n.sex) {
                Picker(L10n.sex, selection: $draft.sex) {
                    ForEach(Sex.allCases) { Text($0.label).tag($0) }
                }
                .pickerStyle(.menu)
            }
        }
        FormSection(footer: L10n.lifeExpectancyFooter) {
            FormRow(title: L10n.lifeExpectancy) {
                ChipPicker(options: [Mode.auto, .custom], selection: mode) { $0 == .auto ? L10n.auto : L10n.custom }
            }
            RowDivider()
            if let custom = draft.customExpectancy {
                Stepper(value: Binding(get: { custom }, set: { draft.customExpectancy = $0 }), in: 30...120, step: 0.5) {
                    Text(L10n.expectancyValue(custom.formatted(.number.precision(.fractionLength(1))))).textStyle(Tokens.TypeScale.headline).monospacedDigit()
                }
                .frame(minHeight: Tokens.Layout.tapTarget)
            } else {
                FormRow(title: "\(table.country(draft.countryCode)?.name ?? "") · \(draft.sex.label)") {
                    Text(L10n.expectancyValue(draft.expectancy(in: table).formatted(.number.precision(.fractionLength(1)))))
                        .textStyle(Tokens.TypeScale.headline).monospacedDigit()
                }
                .foregroundStyle(Color.cdSecondary)
            }
        }
    }
}

enum Route: Hashable { case country }
