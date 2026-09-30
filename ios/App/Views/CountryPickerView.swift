import SwiftUI

struct CountryPickerView: View {
    @Binding var selection: String
    let sex: Sex
    @Environment(\.dismiss) private var dismiss
    @State private var query = ""
    private let table = LifeExpectancyTable.shared

    private var results: [CountryLife] {
        let q = query.trimmingCharacters(in: .whitespaces)
        let all = table.sortedForDisplay
        return q.isEmpty ? all : all.filter { $0.matches(q) }
    }

    var body: some View {
        List {
            Section {
                ForEach(results) { c in
                    Button {
                        selection = c.code
                        dismiss()
                    } label: {
                        HStack {
                            Text(c.name).textStyle(Tokens.TypeScale.body).foregroundStyle(Color.cdForeground)
                            Spacer()
                            Text(L10n.expectancyValue(c.expectancy(for: sex).formatted(.number.precision(.fractionLength(1)))))
                                .textStyle(Tokens.TypeScale.subhead).foregroundStyle(Color.cdSecondary).monospacedDigit()
                            Image(systemName: "checkmark").foregroundStyle(Color.cdOlive).opacity(c.code == selection ? 1 : 0)
                        }
                        .frame(minHeight: Tokens.Layout.tapTarget)
                    }
                    .listRowBackground(Color.cdGlass)
                    .accessibilityAddTraits(c.code == selection ? .isSelected : [])
                }
            } footer: {
                Text(L10n.countrySource).textStyle(Tokens.TypeScale.footnote)
            }
        }
        .scrollContentBackground(.hidden)
        .background(SkyBackground())
        .searchable(text: $query, prompt: L10n.countrySearch)
        .navigationTitle(L10n.country)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(.visible, for: .navigationBar)
    }
}
