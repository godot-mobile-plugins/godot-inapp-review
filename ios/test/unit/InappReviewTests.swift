//
// © 2024-present https://github.com/cengiz-pz
//

import XCTest
@testable import inapp_review_plugin

// MARK: - InappReviewTests

/// Unit-test suite for `InappReview`.
///
/// **What is covered**
/// | Area                        | Tests                                                          |
/// |-----------------------------|----------------------------------------------------------------|
/// | `fetchAppStoreID` — success | valid JSON → correct ID returned                               |
/// | `fetchAppStoreID` — failure | network error, empty results, missing/wrong-type trackId,      |
/// |                             | malformed JSON, empty body, missing results key                |
/// | `fetchAppStoreID` — request | correct host, correct HTTP method, bundle ID in query string   |
/// | `getAppReviewUrl` — success | URL scheme, host, path, query string, full URL equality        |
/// | `getAppReviewUrl` — failure | propagates fetchAppStoreID failures as nil                     |
/// | `requestReview`             | no-crash on headless simulator, idempotent multi-call          |
///
/// **How mocking works**
/// `InappReview.init(session:)` accepts any `URLSessionProtocol`.  Each test
/// creates a `URLSession` backed by `MockURLProtocol` and injects it.
/// `MockURLProtocol.requestHandler` is the per-test seam: set it to return
/// canned data or throw an error, then call into `sut`.
final class InappReviewTests: XCTestCase {

	// MARK: - System under test

	private var sut: InappReview!
	private var mockSession: URLSession!

	// MARK: - Test lifecycle

	override func setUp() {
		super.setUp()
		MockURLProtocol.reset()
		mockSession = MockURLProtocol.makeSession()
		sut = InappReview(session: mockSession)
	}

	override func tearDown() {
		sut = nil
		mockSession = nil
		MockURLProtocol.reset()
		super.tearDown()
	}

	// =========================================================================
	// MARK: - fetchAppStoreID — success path
	// =========================================================================

	func test_fetchAppStoreID_validResponse_returnsCorrectStoreId() {
		givenNetworkReturns(data: TestFixtures.validLookupJSON)

		let exp = expectation(description: "completion called")
		var result: String?

		sut.fetchAppStoreID { id in
			result = id
			exp.fulfill()
		}

		wait(for: exp)
		XCTAssertEqual(result, TestFixtures.validTrackIdString,
						"Should parse the trackId field as a String")
	}

	// =========================================================================
	// MARK: - fetchAppStoreID — failure paths
	// =========================================================================

	func test_fetchAppStoreID_networkError_returnsNil() {
		// Test each common transport error individually.
		for urlError in TestFixtures.networkErrors {
			MockURLProtocol.reset()
			mockSession = MockURLProtocol.makeSession()
			sut = InappReview(session: mockSession)

			MockURLProtocol.requestHandler = { _ in throw urlError }

			let exp = expectation(description: "completion called for \(urlError.code)")
			var result: String? = "sentinel"

			sut.fetchAppStoreID { id in
				result = id
				exp.fulfill()
			}

			wait(for: exp, message: "Timed out for URLError \(urlError.code)")
			XCTAssertNil(result,
							"Network error \(urlError.code) should produce nil, got \(result ?? "nil")")
		}
	}

	func test_fetchAppStoreID_emptyResults_returnsNil() {
		givenNetworkReturns(data: TestFixtures.emptyResultsJSON)

		let (exp, fulfill) = asyncExpectation(description: "completion called")
		var result: String? = "sentinel"

		sut.fetchAppStoreID { id in
			result = id
			fulfill()
		}

		wait(for: exp)
		XCTAssertNil(result, "Empty results array should yield nil")
	}

	func test_fetchAppStoreID_missingTrackId_returnsNil() {
		givenNetworkReturns(data: TestFixtures.missingTrackIdJSON)

		let (exp, fulfill) = asyncExpectation(description: "completion called")
		var result: String? = "sentinel"

		sut.fetchAppStoreID { id in
			result = id
			fulfill()
		}

		wait(for: exp)
		XCTAssertNil(result, "Result missing trackId should yield nil")
	}

	func test_fetchAppStoreID_wrongTypeTrackId_returnsNil() {
		givenNetworkReturns(data: TestFixtures.wrongTypeTrackIdJSON)

		let (exp, fulfill) = asyncExpectation(description: "completion called")
		var result: String? = "sentinel"

		sut.fetchAppStoreID { id in
			result = id
			fulfill()
		}

		wait(for: exp)
		XCTAssertNil(result, "trackId with wrong type (String) should yield nil")
	}

	func test_fetchAppStoreID_missingResultsKey_returnsNil() {
		givenNetworkReturns(data: TestFixtures.missingResultsKeyJSON)

		let (exp, fulfill) = asyncExpectation(description: "completion called")
		var result: String? = "sentinel"

		sut.fetchAppStoreID { id in
			result = id
			fulfill()
		}

		wait(for: exp)
		XCTAssertNil(result, "Response without a 'results' key should yield nil")
	}

	func test_fetchAppStoreID_malformedJSON_returnsNil() {
		givenNetworkReturns(data: TestFixtures.malformedJSON)

		let (exp, fulfill) = asyncExpectation(description: "completion called")
		var result: String? = "sentinel"

		sut.fetchAppStoreID { id in
			result = id
			fulfill()
		}

		wait(for: exp)
		XCTAssertNil(result, "Malformed JSON should yield nil")
	}

	func test_fetchAppStoreID_emptyBody_returnsNil() {
		givenNetworkReturns(data: TestFixtures.emptyBodyData)

		let (exp, fulfill) = asyncExpectation(description: "completion called")
		var result: String? = "sentinel"

		sut.fetchAppStoreID { id in
			result = id
			fulfill()
		}

		wait(for: exp)
		XCTAssertNil(result, "Empty body should yield nil")
	}

	// =========================================================================
	// MARK: - fetchAppStoreID — request validation
	// =========================================================================

	func test_fetchAppStoreID_sendsRequestToItunesHost() {
		givenNetworkReturns(data: TestFixtures.validLookupJSON)

		let (exp, fulfill) = asyncExpectation(description: "request captured")
		MockURLProtocol.requestHandler = { request in
			fulfill()
			return (.ok(for: request.url!), TestFixtures.validLookupJSON)
		}

		sut.fetchAppStoreID { _ in }

		wait(for: exp)

		let captured = MockURLProtocol.capturedRequests.first
		XCTAssertEqual(captured?.url?.host, TestFixtures.itunesLookupHost,
						"Lookup request should target itunes.apple.com")
	}

	func test_fetchAppStoreID_usesGetMethod() {
		let (exp, fulfill) = asyncExpectation(description: "request captured")
		MockURLProtocol.requestHandler = { request in
			fulfill()
			return (.ok(for: request.url!), TestFixtures.validLookupJSON)
		}

		sut.fetchAppStoreID { _ in }

		wait(for: exp)

		let method = MockURLProtocol.capturedRequests.first?.httpMethod ?? ""
		// URLSession defaults to GET when no body/method is specified.
		XCTAssertTrue(method == "GET" || method.isEmpty,
						"iTunes lookup should use HTTP GET, got '\(method)'")
	}

	func test_fetchAppStoreID_queryStringContainsBundleId() {
		guard let bundleId = Bundle.main.bundleIdentifier else {
			// In a bare test host the main bundle may have no identifier;
			// skip rather than produce a false failure.
			return
		}

		let (exp, fulfill) = asyncExpectation(description: "request captured")
		MockURLProtocol.requestHandler = { request in
			fulfill()
			return (.ok(for: request.url!), TestFixtures.validLookupJSON)
		}

		sut.fetchAppStoreID { _ in }

		wait(for: exp)

		let urlString = MockURLProtocol.capturedRequests.first?.url?.absoluteString ?? ""
		XCTAssertTrue(urlString.contains(bundleId),
						"Lookup URL '\(urlString)' should contain the bundle ID '\(bundleId)'")
	}

	func test_fetchAppStoreID_makesExactlyOneRequest() {
		givenNetworkReturns(data: TestFixtures.validLookupJSON)

		let (exp, fulfill) = asyncExpectation(description: "completion")

		sut.fetchAppStoreID { _ in fulfill() }

		wait(for: exp)
		XCTAssertEqual(MockURLProtocol.capturedRequests.count, 1,
						"fetchAppStoreID should issue exactly one network request")
	}

	// =========================================================================
	// MARK: - getAppReviewUrl — success path
	// =========================================================================

	func test_getAppReviewUrl_validResponse_returnsNonNilURL() {
		givenNetworkReturns(data: TestFixtures.validLookupJSON)

		let (exp, fulfill) = asyncExpectation(description: "URL received")
		var result: URL?

		sut.getAppReviewUrl { url in
			result = url
			fulfill()
		}

		wait(for: exp)
		XCTAssertNotNil(result)
	}

	func test_getAppReviewUrl_returnsExpectedFullURL() {
		givenNetworkReturns(data: TestFixtures.validLookupJSON)

		let (exp, fulfill) = asyncExpectation(description: "URL received")
		var result: URL?

		sut.getAppReviewUrl { url in
			result = url
			fulfill()
		}

		wait(for: exp)
		XCTAssertEqual(result, TestFixtures.expectedReviewURL,
						"Full review URL should match the expected App Store URL")
	}

	func test_getAppReviewUrl_urlHasCorrectScheme() {
		givenNetworkReturns(data: TestFixtures.validLookupJSON)

		let (exp, fulfill) = asyncExpectation(description: "URL received")
		var result: URL?

		sut.getAppReviewUrl { url in result = url; fulfill() }

		wait(for: exp)
		XCTAssertEqual(result?.scheme, "https")
	}

	func test_getAppReviewUrl_urlTargetsAppStoreHost() {
		givenNetworkReturns(data: TestFixtures.validLookupJSON)

		let (exp, fulfill) = asyncExpectation(description: "URL received")
		var result: URL?

		sut.getAppReviewUrl { url in result = url; fulfill() }

		wait(for: exp)
		XCTAssertEqual(result?.host, TestFixtures.appStoreHost)
	}

	func test_getAppReviewUrl_pathContainsStoreId() {
		givenNetworkReturns(data: TestFixtures.validLookupJSON)

		let (exp, fulfill) = asyncExpectation(description: "URL received")
		var result: URL?

		sut.getAppReviewUrl { url in result = url; fulfill() }

		wait(for: exp)
		XCTAssertTrue(result?.path.contains(TestFixtures.validTrackIdString) == true,
						"URL path should contain the App Store ID")
	}

	func test_getAppReviewUrl_queryContainsWriteReviewAction() {
		givenNetworkReturns(data: TestFixtures.validLookupJSON)

		let (exp, fulfill) = asyncExpectation(description: "URL received")
		var result: URL?

		sut.getAppReviewUrl { url in result = url; fulfill() }

		wait(for: exp)
		XCTAssertEqual(result?.query, TestFixtures.writeReviewAction,
						"Query string should be 'action=write-review'")
	}

	// =========================================================================
	// MARK: - getAppReviewUrl — failure paths
	// =========================================================================

	func test_getAppReviewUrl_networkError_returnsNil() {
		MockURLProtocol.requestHandler = { _ in throw URLError(.notConnectedToInternet) }

		let (exp, fulfill) = asyncExpectation(description: "URL callback")
		var result: URL? = URL(string: "https://sentinel.example.com")

		sut.getAppReviewUrl { url in
			result = url
			fulfill()
		}

		wait(for: exp)
		XCTAssertNil(result, "Network failure should propagate as nil URL")
	}

	func test_getAppReviewUrl_emptyResults_returnsNil() {
		givenNetworkReturns(data: TestFixtures.emptyResultsJSON)

		let (exp, fulfill) = asyncExpectation(description: "URL callback")
		var result: URL? = URL(string: "https://sentinel.example.com")

		sut.getAppReviewUrl { url in
			result = url
			fulfill()
		}

		wait(for: exp)
		XCTAssertNil(result, "Empty results should propagate as nil URL")
	}

	func test_getAppReviewUrl_missingTrackId_returnsNil() {
		givenNetworkReturns(data: TestFixtures.missingTrackIdJSON)

		let (exp, fulfill) = asyncExpectation(description: "URL callback")
		var result: URL? = URL(string: "https://sentinel.example.com")

		sut.getAppReviewUrl { url in
			result = url
			fulfill()
		}

		wait(for: exp)
		XCTAssertNil(result, "Missing trackId should propagate as nil URL")
	}

	func test_getAppReviewUrl_malformedJSON_returnsNil() {
		givenNetworkReturns(data: TestFixtures.malformedJSON)

		let (exp, fulfill) = asyncExpectation(description: "URL callback")
		var result: URL? = URL(string: "https://sentinel.example.com")

		sut.getAppReviewUrl { url in
			result = url
			fulfill()
		}

		wait(for: exp)
		XCTAssertNil(result, "Malformed JSON should propagate as nil URL")
	}

	// =========================================================================
	// MARK: - requestReview (static — UI-less simulator)
	// =========================================================================

	/// `requestReview` dispatches to the main queue and guards on an active
	/// `UIWindowScene`.  In a headless unit-test host there is no such scene,
	/// so the method should silently bail out without crashing.
	func test_requestReview_noActiveScene_doesNotCrash() {
		// Enqueue a sentinel work item AFTER the call; if requestReview didn't
		// crash by the time the sentinel fires, the test passes.
		let exp = expectation(description: "Main queue drained after requestReview")

		InappReview.requestReview()

		DispatchQueue.main.async {
			exp.fulfill()
		}

		wait(for: [exp], timeout: 2)
	}

	func test_requestReview_multipleCallsInSuccession_doesNotCrash() {
		let exp = expectation(description: "Main queue drained")

		for _ in 0..<5 { InappReview.requestReview() }

		DispatchQueue.main.async { exp.fulfill() }
		wait(for: [exp], timeout: 2)
	}

	func test_requestReview_canBeCalledFromBackgroundThread_doesNotCrash() {
		let exp = expectation(description: "Background → main queue drained")

		DispatchQueue.global(qos: .background).async {
			InappReview.requestReview()
		}

		// Allow the main-queue async inside requestReview to execute.
		DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
			exp.fulfill()
		}

		wait(for: [exp], timeout: 2)
	}

	// =========================================================================
	// MARK: - Private helpers
	// =========================================================================

	/// Convenience: configure MockURLProtocol to return a 200 OK with `data`.
	private func givenNetworkReturns(data: Data) {
		MockURLProtocol.requestHandler = { request in
			(.ok(for: request.url!), data)
		}
	}
}
