//
// © 2024-present https://github.com/cengiz-pz
//

import Foundation
import StoreKit
import OSLog

@objcMembers public class InappReview : NSObject
{

	private static let logger = Logger(subsystem: "org.godotengine.plugin", category: "InappReview")

	static func requestReview() {
		logger.debug("Attempting to request in-app review.")

		// Dispatch to main thread (Godot may call us from background)
		DispatchQueue.main.async {
			// Find the active foreground scene
			guard let scene = UIApplication.shared.connectedScenes
					.compactMap({ $0 as? UIWindowScene })
					.first(where: { $0.activationState == .foregroundActive }) else {

				logger.error("Failed to find an active foreground UIWindowScene. Cannot request review.")
				return
			}

			if #available(iOS 18.0, *) {
				logger.debug("Using AppStore.requestReview(in:).")
				AppStore.requestReview(in: scene)
			} else if #available(iOS 14.0, *) {
				logger.debug("Using SKStoreReviewController.requestReview(in:).")
				SKStoreReviewController.requestReview(in: scene)
			} else {
				logger.debug("Using SKStoreReviewController.requestReview().")
				SKStoreReviewController.requestReview()
			}

			logger.info("requestReview method called successfully.")
		}
	}

	func fetchAppStoreID(completion: @escaping (String?) -> Void) {
		Self.logger.debug("Starting fetchAppStoreID process.")
		
		// Get the local Bundle ID
		guard let bundleId = Bundle.main.bundleIdentifier,
			let encodedBundleId = bundleId.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) else {
			Self.logger.error("Failed to get or encode bundle identifier.")
			completion(nil)
			return
		}

		Self.logger.debug("Bundle ID: \(bundleId, privacy: .public)")

		// Construct the iTunes Lookup URL
		let lookupURLString = "https://itunes.apple.com/lookup?bundleId=\(encodedBundleId)"
		guard let lookupURL = URL(string: lookupURLString) else {
			Self.logger.error("Failed to construct valid lookup URL from string: \(lookupURLString, privacy: .public)")
			completion(nil)
			return
		}

		Self.logger.debug("Lookup URL: \(lookupURL.absoluteString, privacy: .public)")

		// Perform the network request
		URLSession.shared.dataTask(with: lookupURL) { data, response, error in
			
			// Check for network errors
			if let error = error {
				Self.logger.error("Network request failed: \(error.localizedDescription, privacy: .public)")
				completion(nil)
				return
			}
			
			// Check for data presence
			guard let data = data else {
				Self.logger.error("Network request returned no data and no error.")
				completion(nil)
				return
			}

			do {
				// Parse the JSON response
				if let json = try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any],
				let results = json["results"] as? [[String: Any]],
				let firstResult = results.first,
				let trackId = firstResult["trackId"] as? Int {

					let storeId = String(trackId) // The trackId is the App Store ID

					Self.logger.info("Successfully fetched App Store ID: \(storeId, privacy: .public)")
					completion(storeId)
				} else {
					Self.logger.error("JSON parsing failed to extract 'trackId' (App Store ID).")
					completion(nil)
				}
			} catch {
				Self.logger.error("JSON serialization error: \(error.localizedDescription, privacy: .public)")
				completion(nil)
			}
		}.resume()
	}

	/**
	* Asynchronously fetches the application's Store ID and constructs the URL
	* that directs the user directly to the 'Write a Review' page in the App Store.
	*
	* @param completion A closure that receives the constructed review URL or nil on failure.
	*/
	func getAppReviewUrl(completion: @escaping (URL?) -> Void) {
		Self.logger.debug("Starting getAppReviewUrl process.")

		fetchAppStoreID { storeId in
			guard let storeId = storeId,
				let url = URL(string: "https://apps.apple.com/app/id\(storeId)?action=write-review") else {
				
				Self.logger.error("Failed to get App Store ID or construct review URL.")
				completion(nil)
				return
			}

			Self.logger.info("Successfully constructed review URL: \(url.absoluteString, privacy: .public)")
			completion(url)
		}
	}
}
