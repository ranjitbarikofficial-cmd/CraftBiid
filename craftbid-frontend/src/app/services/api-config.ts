export function getApiBaseUrl(): string {
  if (typeof window !== 'undefined') {
    const host = window.location.hostname;
    if (host === 'localhost' || host === '127.0.0.1') {
      return 'http://localhost:8081';
    }
    // If accessed via local Wi-Fi from a friend's mobile/laptop
    if (
      host.startsWith('192.168.') ||
      host.startsWith('10.') ||
      host.startsWith('172.')
    ) {
      return `http://${host}:8081`;
    }
    // If accessed via tunnel / proxy
    return '';
  }
  return 'http://localhost:8081';
}
