import SwiftUI

struct OnboardingView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.deviceClass) private var deviceClass
    @State private var draft = LifeProfile.makeDefault()

    var body: some View {
        NavigationStack {
            ZStack {
                SkyBackground()
                ScrollView {
                    VStack(alignment: .leading, spacing: Tokens.Space.sp5) {
                        VStack(alignment: .leading, spacing: Tokens.Space.sp3) {
                            Text(verbatim: "Carpe Diem").textStyle(Tokens.TypeScale.display(deviceClass))
                            Text(L10n.tagline).textStyle(Tokens.TypeScale.title3).foregroundStyle(Color.cdSecondary)
                        }
                        .padding(.top, Tokens.Space.sp10)
                        .padding(.horizontal, Tokens.Space.sp2)

                        ProfileFields(draft: $draft)

                        Button { model.save(draft) } label: {
                            Text(L10n.begin).textStyle(Tokens.TypeScale.headline)
                                .frame(maxWidth: .infinity, minHeight: Tokens.Layout.tapTarget + Tokens.Space.sp3)
                                .foregroundStyle(Color.cdOnOlive)
                                .background(Capsule().fill(Color.cdOlive))
                                .shadow(color: Color.cdLight.opacity(Tokens.Effect.glowStrength), radius: Tokens.Space.sp5)
                        }
                        .buttonStyle(.plain)

                        Text(L10n.privacy).textStyle(Tokens.TypeScale.footnote).foregroundStyle(Color.cdSecondary)
                            .frame(maxWidth: .infinity)
                    }
                    .padding(.horizontal, deviceClass.pageMargin)
                    .padding(.bottom, Tokens.Space.sp10)
                }
            }
            .navigationDestination(for: Route.self) { _ in CountryPickerView(selection: $draft.countryCode, sex: draft.sex) }
            .toolbar(.hidden, for: .navigationBar)
        }
    }
}
