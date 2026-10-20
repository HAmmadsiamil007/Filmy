# Stream Proxy (/proxy/stream)

The single most important addition. Read before touching playback.

## Why it exists
The video CDN (bcdnxw.hakunaymatata.com) is Referer-locked to the source domain (e.g. https://123moviesfree.club/).
A browser <video> element CANNOT set a Referer header, so direct playback 403s. The proxy relays the bytes and
forwards User-Agent + Referer + Origin, so every client plays through one URL with zero custom headers.

## Verified facts
- CDN does NOT pin TLS/JA3 on video bytes. Proof: a plain .NET HttpWebRequest fetched them fine (Schannel stack, totally different JA3) as long as Referer/UA were present.
  => the proxy uses httpx, not curl_cffi. Do not add curl_cffi here.
- CDN sets Access-Control-Allow-Origin: * on video responses.
- Accept-Ranges: bytes is present, so seeking works.

## Bug that cost time (do not repeat)
curl_cffi .stream() returns a GENERATOR, not a context manager. Calling resp_cm.__enter__() throws, which surfaced as a confusing 502.
httpx.stream() IS a real async context manager, which is why it is the right tool here.

## Contract
- GET /proxy/stream?url=<encoded>&referer=<domain>
- url must pass is_proxyable() (PROXY_ALLOWED_HOST_SUFFIXES: hakunaymatata.com, aoneroom.com, moviebox.ph, inmoviebox.com). Else 403.
- Forwards the clients Range header upstream; passes back 206 + Content-Range + Accept-Ranges.
- Emits Access-Control-Allow-Origin: * and Expose-Headers for Content-Length/Content-Range/Accept-Ranges.
- Streams in 256 KiB chunks; closes the upstream CM in a finally block.

## Invariant
The allowlist is an SSRF control. Never widen it to arbitrary hosts and never accept a full URL from an unvalidated source.

## Verification bar
Testing this with a plain GET is insufficient - must assert 206 + Content-Range. See `mem:task_completion`.