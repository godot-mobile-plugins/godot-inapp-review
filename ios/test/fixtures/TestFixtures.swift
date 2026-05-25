//
// © 2024-present https://github.com/cengiz-pz
//

import Foundation
import XCTest

// MARK: - TestFixtures

/// Static repository of all test data used across the In-App Review test suite.
///
/// Centralising fixtures here keeps individual test methods concise and makes it
/// easy to update sample data in one place.
enum TestFixtures {

	// MARK: - iTunes Lookup — App Store IDs

	static let validTrackId: Int = 123_456_789
	static let validTrackIdString = "123456789"

	// MARK: - iTunes Lookup — JSON payloads

	/// A well-formed iTunes lookup response containing a single result with a valid `trackId`.
	static let validLookupJSON: Data = encodedJSON([
		"resultCount": 1,
		"results": [
			[
				"trackId": validTrackId,
				"trackName": "Test Godot App",
				"bundleId": "org.godotengine.testapp",
				"version": "1.0.0",
				"kind": "software"
			]
		]
	])

	/// A lookup response where `results` is present but empty — app not found in the store.
	static let emptyResultsJSON: Data = encodedJSON([
		"resultCount": 0,
		"results": [] as [[String: Any]]
	])

	/// A lookup response where the first result is missing the `trackId` field.
	static let missingTrackIdJSON: Data = encodedJSON([
		"resultCount": 1,
		"results": [
			["trackName": "Test App", "bundleId": "org.godotengine.testapp"]
		]
	])

	/// A lookup response where `trackId` has the wrong type (String instead of Int).
	static let wrongTypeTrackIdJSON: Data = encodedJSON([
		"resultCount": 1,
		"results": [["trackId": "not-an-int", "trackName": "Test App"]]
	])

	/// A lookup response where the top-level `results` key is absent.
	static let missingResultsKeyJSON: Data = encodedJSON([
		"resultCount": 0
	])

	/// Completely malformed bytes — not valid JSON.
	static let malformedJSON = Data("not valid JSON !!!".utf8)

	/// An empty byte sequence — simulates a server returning a 200 with no body.
	static let emptyBodyData = Data()

	// MARK: - Expected App Store URLs

	/// The review URL the plugin should build for `validTrackId`.
	static let expectedReviewURL = URL(
		string: "https://apps.apple.com/app/id\(validTrackIdString)?action=write-review"
	)!

	static let itunesLookupHost = "itunes.apple.com"
	static let appStoreHost = "apps.apple.com"
	static let writeReviewAction = "action=write-review"

	// MARK: - Network error simulation

	/// Common transport-level errors to drive failure paths.
	static let networkErrors: [URLError] = [
		URLError(.notConnectedToInternet),
		URLError(.timedOut),
		URLError(.networkConnectionLost),
		URLError(.cannotFindHost),
		URLError(.badServerResponse)
	]

	// MARK: - Private helpers

	private static func encodedJSON(_ object: [String: Any]) -> Data {
		// A fixture encoding failure is a programming error; crashing loudly with
		// fatalError is correct and intentional — we avoid force_try to satisfy swiftlint.
		guard let data = try? JSONSerialization.data(withJSONObject: object, options: []) else {
			fatalError("TestFixtures.encodedJSON: JSONSerialization failed for object: \(object)")
		}
		return data
	}
}

// MARK: - XCTestCase helpers

extension XCTestCase {

	/// Waits for `expectation` with a standard 2-second timeout and fails the
	/// test with `message` if it times out.
	func wait(
		for expectation: XCTestExpectation,
		timeout: TimeInterval = 2,
		message: String = "Async expectation timed out"
	) {
		wait(for: [expectation], timeout: timeout)
	}

	/// Creates a named expectation and returns both the expectation and a
	/// one-shot fulfil closure, so tests don't have to capture `self`.
	func asyncExpectation(
		description: String
	) -> (expectation: XCTestExpectation, fulfill: () -> Void) {
		let exp = expectation(description: description)
		return (exp, { exp.fulfill() })
	}
}
