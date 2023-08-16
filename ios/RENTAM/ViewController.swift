//
//  ViewController.swift
//  BudiFit
//
//  Created by IDK Studio on 2/19/22.
//

import UIKit
import WebKit

class ViewController: UIViewController {

    let webView: WKWebView = {

        let prefs = WKWebpagePreferences()
        prefs.allowsContentJavaScript = true
        let configuration = WKWebViewConfiguration()
        configuration.defaultWebpagePreferences = prefs

        let webView = WKWebView(frame: .zero,
                                configuration: configuration)

        return webView
    }()


    override func viewDidLoad() {
        super.viewDidLoad()
        view.addSubview(webView)
        let file = "token.txt" //this is the file. we will write to and read from it
        var deviceToken = ""
        
        if let dir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first {

            let fileURL = dir.appendingPathComponent(file)

            //reading
            do {
                deviceToken = try String(contentsOf: fileURL, encoding: .utf8)
            }
            catch {/* error handling here */}
        }
        
        //let token = AppDelegate().deviceTokenString
        guard let url = URL(string: "https://staging.rentam.ba/login?token_app_login="+deviceToken) else{
            return
        }

        webView.load(URLRequest(url: url))
        webView.customUserAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.5 Mobile/15E148 Safari/604.1"

        DispatchQueue.main.asyncAfter(deadline: .now()+5) {
            self.webView.evaluateJavaScript("document.body.innerHTML"){ result, error in
                guard let html = result as? String, error == nil else {
                    return
                }
                
            }
        }

        
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        webView.frame = view.bounds
    }

}

