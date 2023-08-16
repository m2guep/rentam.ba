//
//  AppDelegate.swift
//  BudiFit
//
//  Created by IDK Studio on 2/19/22.
//

import UIKit
import FirebaseCore
import FirebaseMessaging

@main
class AppDelegate: UIResponder, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    
    let gcmMessageIDKey = "testKey"
    public var deviceTokenString = ""
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Override point for customization after application launch.

        if #available(iOS 10.0, *) {
          // For iOS 10 display notification (sent via APNS)
          UNUserNotificationCenter.current().delegate = self

          let authOptions: UNAuthorizationOptions = [.alert, .badge, .sound]
          UNUserNotificationCenter.current().requestAuthorization(
            options: authOptions,
            completionHandler: { _, _ in }
          )
        } else {
          let settings: UIUserNotificationSettings =
            UIUserNotificationSettings(types: [.alert, .badge, .sound], categories: nil)
          application.registerUserNotificationSettings(settings)
        }

        application.registerForRemoteNotifications()
        
        Messaging.messaging().delegate = self
        
        FirebaseApp.configure()
        return true
        
    }

    // MARK: UISceneSession Lifecycle

    func application(_ application: UIApplication, configurationForConnecting connectingSceneSession: UISceneSession, options: UIScene.ConnectionOptions) -> UISceneConfiguration {
        // Called when a new scene session is being created.
        // Use this method to select a configuration to create the new scene with.
        return UISceneConfiguration(name: "Default Configuration", sessionRole: connectingSceneSession.role)
    }

    func application(_ application: UIApplication, didDiscardSceneSessions sceneSessions: Set<UISceneSession>) {
        // Called when the user discards a scene session.
        // If any sessions were discarded while the application was not running, this will be called shortly after application:didFinishLaunchingWithOptions.
        // Use this method to release any resources that were specific to the discarded scenes, as they will not return.
    }
    
func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
  //print("Firebase registration token: \(String(describing: fcmToken))")

  let dataDict:[String: String] = ["token": fcmToken ?? ""]
  NotificationCenter.default.post(name: Notification.Name("FCMToken"), object: nil, userInfo: dataDict)
  
}

func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
    //print("Firebase registration token2: \(String(describing: deviceToken))")
    Messaging.messaging().apnsToken = deviceToken
    
    Messaging.messaging().token { token, error in
      if let error = error {
        print("Error fetching FCM registration token: \(error)")
      } else if let token = token {
        print("FCM registration token: \(token)")
        //self.fcmRegTokenMessage.text  = "Remote FCM registration token: \(token)"
      
    //let deviceTokenStr = token.hexString
    let deviceTokenString = token
    let file = "token.txt" //this is the file. we will write to and read from it

    let text = deviceTokenString //just a text
    
    if let dir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first {

        let fileURL = dir.appendingPathComponent(file)
        
        //writing
        do {
            try text.write(to: fileURL, atomically: false, encoding: .utf8)
        }
        catch {/* error handling here */}

    }
          
      }
    }
    
    
}

  // Receive displayed notifications for iOS 10 devices.
  func userNotificationCenter(_ center: UNUserNotificationCenter,
                              willPresent notification: UNNotification,
                              withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions)
                                -> Void) {
    let userInfo = notification.request.content.userInfo

    // With swizzling disabled you must let Messaging know about the message, for Analytics
     Messaging.messaging().appDidReceiveMessage(userInfo)

    // ...

    // Print full message.
    print(userInfo)

    // Change this to your preferred presentation option
      completionHandler([[.banner,.list, .sound]])
  }

  func userNotificationCenter(_ center: UNUserNotificationCenter,
                              didReceive response: UNNotificationResponse,
                              withCompletionHandler completionHandler: @escaping () -> Void) {
    let userInfo = response.notification.request.content.userInfo

    // ...

    // With swizzling disabled you must let Messaging know about the message, for Analytics
    Messaging.messaging().appDidReceiveMessage(userInfo)

    // Print full message.
    print(userInfo)

    completionHandler()
  }

func application(_ application: UIApplication,
                 didReceiveRemoteNotification userInfo: [AnyHashable: Any],
                 fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult)
                   -> Void) {
  // If you are receiving a notification message while your app is in the background,
  // this callback will not be fired till the user taps on the notification launching the application.
  // TODO: Handle data of notification

  // With swizzling disabled you must let Messaging know about the message, for Analytics
  Messaging.messaging().appDidReceiveMessage(userInfo)

  // Print message ID.
  if let messageID = userInfo[gcmMessageIDKey] {
    print("Message ID: \(messageID)")
  }

  // Print full message.
  print(userInfo)

  completionHandler(UIBackgroundFetchResult.newData)
}
    
    
}
    
extension Data {
    var hexString : String {
        let hexString = map { String(format: "%02.2hhx", $0)}.joined()
        return hexString
    }
}
