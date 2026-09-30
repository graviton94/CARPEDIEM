import SwiftUI

struct SettingsView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    @Environment(\.deviceClass) private var deviceClass
    @State private var draft: LifeProfile
    @State private var confirmErase = false

    init(profile: LifeProfile) { _draft = State(initialValue: profile) }

    var body: some View {
        NavigationStack {
            ZStack {
                SkyBackground()
                ScrollView {
                    VStack(alignment: .leading, spacing: Tokens.Space.sp5) {
                        ProfileFields(draft: $draft)

                        FormSection(header: L10n.words) {
                            FormRow(title: L10n.wordsLanguage) {
                                ChipPicker(options: QuoteLanguage.allCases, selection: $model.quoteLanguage) { $0.label }
                            }
                        }

                        FormSection(header: L10n.widgets) {
                            help("square.grid.2x2", L10n.widgetsHelp1)
                            RowDivider()
                            help("magnifyingglass", L10n.widgetsHelp2)
                            RowDivider()
                            help("slider.horizontal.3", L10n.widgetsHelp3)
                            RowDivider()
                            help("lock", L10n.widgetsHelp4)
                        }

                        FormSection(footer: L10n.privacy + "\nCarpe Diem " + (Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "")) {
                            Button(role: .destructive) { confirmErase = true } label: {
                                Text(L10n.erase).textStyle(Tokens.TypeScale.body).foregroundStyle(Color.cdDanger)
                                    .frame(maxWidth: .infinity, minHeight: Tokens.Layout.tapTarget, alignment: .leading)
                            }
                        }
                    }
                    .padding(.horizontal, deviceClass.pageMargin)
                    .padding(.vertical, Tokens.Space.sp4)
                }
            }
            .navigationTitle(L10n.settings)
            .navigationBarTitleDisplayMode(.inline)
            .navigationDestination(for: Route.self) { _ in CountryPickerView(selection: $draft.countryCode, sex: draft.sex) }
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(L10n.cancel) { dismiss() } }
                ToolbarItem(placement: .confirmationAction) { Button(L10n.done) { model.save(draft); dismiss() }.bold() }
            }
            .confirmationDialog(L10n.eraseConfirm, isPresented: $confirmErase, titleVisibility: .visible) {
                Button(L10n.eraseAction, role: .destructive) { model.eraseAll(); dismiss() }
            }
        }
    }

    private func help(_ symbol: String, _ text: String) -> some View {
        HStack(alignment: .top, spacing: Tokens.Space.sp3) {
            Image(systemName: symbol).foregroundStyle(Color.cdOlive).frame(width: Tokens.Stroke.icon)
            Text(text).textStyle(Tokens.TypeScale.subhead).fixedSize(horizontal: false, vertical: true)
            Spacer(minLength: 0)
        }
        .padding(.vertical, Tokens.Space.sp3)
    }
}
