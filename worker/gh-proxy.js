const RELEASE_API_PATHS = new Set([
  '/api/repos/RE-TikaRa/GSAULife/releases/latest',
  '/api/repos/RE-TikaRa/GSAU-Card/releases/latest',
])

export default {
  async fetch(request, env, context) {
    const url = new URL(request.url)
    const isApi = url.pathname.startsWith('/api/')
    const isRaw = url.pathname.startsWith('/raw/')
    if (isApi && (request.method !== 'GET' || !RELEASE_API_PATHS.has(url.pathname))) {
      return new Response('Not Found', { status: 404 })
    }
    if (!isApi && request.method !== 'GET' && request.method !== 'HEAD') {
      return new Response('Method Not Allowed', { status: 405 })
    }

    let target
    if (isApi) {
      target = 'https://api.github.com' + url.pathname.slice(4)
    } else if (isRaw) {
      target = 'https://raw.githubusercontent.com' + url.pathname.slice(4) + url.search
    } else {
      target = 'https://github.com' + url.pathname + url.search
    }

    const headers = {
      'User-Agent': 'GSAULife-Updater',
      Accept: request.headers.get('Accept') || '*/*',
    }
    const range = request.headers.get('Range')
    if (range) headers.Range = range
    if (isApi && env.GITHUB_TOKEN) {
      headers.Authorization = 'Bearer ' + env.GITHUB_TOKEN
    }

    const cache = isApi ? caches.default : null
    const cacheKey = isApi ? new Request(url.origin + url.pathname + '?s-maxage=300') : null
    const cached = cacheKey && await cache.match(cacheKey)
    if (cached) return cached

    const response = await fetch(target, {
      method: request.method,
      headers,
      redirect: 'follow',
    })
    const responseHeaders = new Headers(response.headers)
    responseHeaders.set('Access-Control-Allow-Origin', '*')
    if (isApi) responseHeaders.set('Cache-Control', 'public, max-age=0, s-maxage=300')
    const result = new Response(response.body, {
      status: response.status,
      headers: responseHeaders,
    })
    if (cacheKey && response.ok) {
      context.waitUntil(cache.put(cacheKey, result.clone()))
    }
    return result
  },
}
