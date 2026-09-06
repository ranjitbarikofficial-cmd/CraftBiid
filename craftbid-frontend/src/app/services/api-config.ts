export function getApiBaseUrl(): string {
  if (typeof window !== 'undefined') {
    const host = window.location.hostname;

    // Local development
    if (host === 'localhost' || host === '127.0.0.1') {
      return 'http://localhost:8081';
    }

    // Local network development
    if (host.startsWith('192.168.') || host.startsWith('10.') || host.startsWith('172.')) {
      return `http://${host}:8081`;
    }

    // Production
    return 'https://craftbid.onrender.com';
  }

  // Production fallback
  return 'https://craftbid.onrender.com';
}

export function resolveMediaUrl(url: string | null | undefined): string {
  if (!url) return '';
  if (
    url.startsWith('http://') ||
    url.startsWith('https://') ||
    url.startsWith('data:') ||
    url.startsWith('blob:')
  ) {
    return url;
  }
  const base = getApiBaseUrl();
  return `${base}${url.startsWith('/') ? '' : '/'}${url}`;
}

