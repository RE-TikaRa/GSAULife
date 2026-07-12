/**
 * Cloudflare Worker for GitHub API and release downloads.
 * Custom domain: gh.re-tikara.fun
 */
export default {
  async fetch(request, env) {
    const url = new URL(request.url)
    const isApi = url.pathname.startsWith('/api/')
    const isRaw = url.pathname.startsWith('/raw/')
    let target
    if (isApi) {
      target = 'https://api.github.com' + url.pathname.slice(4) + url.search
    } else if (isRaw) {
      target = 'https://raw.githubusercontent.com' + url.pathname.slice(4) + url.search
    } else {
      target = 'https://github.com' + url.pathname + url.search
    }

    const headers = {
      'User-Agent': 'GSAULife-Updater',
      Accept: request.headers.get('Accept') || '*/*',
    }
    if (isApi && env.GITHUB_TOKEN) {
      headers.Authorization = 'Bearer ' + env.GITHUB_TOKEN
    }

    const response = await fetch(target, { headers, redirect: 'follow' })
    const responseHeaders = new Headers(response.headers)
    responseHeaders.set('Access-Control-Allow-Origin', '*')
    return new Response(response.body, {
      status: response.status,
      headers: responseHeaders,
    })
  },
}
