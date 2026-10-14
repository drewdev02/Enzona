import SwiftUI
import WebKit

/// WebView de login embebido. Intercepta el redirect http://apk-callback?code=...
/// (ASWebAuthenticationSession no captura esquemas http; el redirect de EnZona es http).
struct AuthWebView: UIViewRepresentable {
    let url: URL
    let onCode: (String) -> Void
    let onError: (String) -> Void

    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        config.websiteDataStore = .nonPersistent()
        let web = WKWebView(frame: .zero, configuration: config)
        web.navigationDelegate = context.coordinator
        web.load(URLRequest(url: url))
        return web
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(onCode: onCode, onError: onError) }

    final class Coordinator: NSObject, WKNavigationDelegate {
        let onCode: (String) -> Void
        let onError: (String) -> Void
        private var finished = false

        init(onCode: @escaping (String) -> Void, onError: @escaping (String) -> Void) {
            self.onCode = onCode
            self.onError = onError
        }

        func webView(_ webView: WKWebView,
                     decidePolicyFor navigationAction: WKNavigationAction,
                     decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
            guard let url = navigationAction.request.url else {
                decisionHandler(.allow); return
            }
            if url.absoluteString.hasPrefix("http://apk-callback") {
                decisionHandler(.cancel)
                guard !finished else { return }
                finished = true
                let items = URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems
                if let code = items?.first(where: { $0.name == "code" })?.value {
                    onCode(code)
                } else {
                    onError(items?.first(where: { $0.name == "oauthErrorMsg" })?.value
                            ?? "Login cancelado")
                }
                return
            }
            decisionHandler(.allow)
        }
    }
}

/// Hoja de login con el WebView.
struct AuthSheet: View {
    let url: URL
    let onCode: (String) -> Void
    let onError: (String) -> Void
    let onCancel: () -> Void

    var body: some View {
        NavigationView {
            AuthWebView(url: url, onCode: onCode, onError: onError)
                .navigationTitle("Iniciar sesión")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancelar", action: onCancel)
                    }
                }
        }
    }
}
