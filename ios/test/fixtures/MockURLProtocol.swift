//
// © 2024-present https://github.com/cengiz-pz
//

import Foundation

// MARK: - MockURLProtocol

/// A `URLProtocol` subclass that short-circuits the network stack so tests
/// never hit the real Internet.
///
/// **Typical usage in a test case:**
///
/// ```swift
/// override func setUp() {
///     super.setUp()
///     // Build a URLSession backed entirely by this protocol …
///     let session = MockURLProtocol.makeSession()
///     // … then inject it into the system under test.
///     sut = InappReview(session: session)
/// }
///
/// override func tearDown() {
///     MockURLProtocol.reset()
///     sut = nil
///     super.tearDown()
/// }
///
/// func test_something() {
///     MockURLProtocol.requestHandler = { request in
///         return (.ok(for: request.url!), MyFixtures.validJSON)
///     }
///     // … exercise sut …
/// }
/// ```
final class MockURLProtocol: URLProtocol {

	// MARK: - Handler

	/// Set this before each test that exercises networking.
	/// - Throw any `Error` to simulate a transport-level failure.
	/// - Return `(HTTPURLResponse, Data)` to simulate a successful response.
	static var requestHandler: ((URLRequest) throws -> (HTTPURLResponse, Data))?

	// MARK: - Captured requests (inspection)

	/// Every request that passes through this protocol in a test run.
	private(set) static var capturedRequests: [URLRequest] = []

	// MARK: - URLProtocol overrides

	override class func canInit(with request: URLRequest) -> Bool { true }
	override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

	override func startLoading() {
		MockURLProtocol.capturedRequests.append(request)

		guard let handler = MockURLProtocol.requestHandler else {
			// No handler registered — fail with a clear error so tests surface the problem quickly.
			client?.urlProtocol(
				self,
				didFailWithError: MockURLProtocolError.noHandlerRegistered
			)
			return
		}

		do {
			let (response, data) = try handler(request)
			client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
			client?.urlProtocol(self, didLoad: data)
			client?.urlProtocolDidFinishLoading(self)
		} catch {
			client?.urlProtocol(self, didFailWithError: error)
		}
	}

	override func stopLoading() {}

	// MARK: - Convenience factory

	/// Returns a `URLSession` whose requests are handled entirely by `MockURLProtocol`.
	/// Use this session when constructing the system under test.
	static func makeSession() -> URLSession {
		let config = URLSessionConfiguration.ephemeral
		config.protocolClasses = [MockURLProtocol.self]
		return URLSession(configuration: config)
	}

	/// Resets shared state between tests.
	static func reset() {
		requestHandler = nil
		capturedRequests = []
	}
}

// MARK: - Errors

enum MockURLProtocolError: LocalizedError {
	case noHandlerRegistered

	var errorDescription: String? {
		switch self {
		case .noHandlerRegistered:
			return "MockURLProtocol: no requestHandler was set before the network call was made."
		}
	}
}

// MARK: - HTTPURLResponse convenience

extension HTTPURLResponse {

	/// Returns a 200 OK response for the given URL.
	static func ok(for url: URL) -> HTTPURLResponse {
		HTTPURLResponse(url: url, statusCode: 200, httpVersion: "HTTP/1.1", headerFields: nil)!
	}

	/// Returns a response with any arbitrary status code.
	static func with(statusCode: Int, url: URL) -> HTTPURLResponse {
		HTTPURLResponse(url: url, statusCode: statusCode, httpVersion: "HTTP/1.1", headerFields: nil)!
	}
}
