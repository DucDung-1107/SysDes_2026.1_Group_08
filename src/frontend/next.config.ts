import type { NextConfig } from 'next';
const backend = (process.env.MEDORA_API_ORIGIN || 'http://127.0.0.1:8080').replace(/\/$/, '');
if (!/^https?:\/\/[^/]+$/.test(backend)) throw new Error('MEDORA_API_ORIGIN must be an HTTP(S) origin');
const config: NextConfig = {
  reactStrictMode: true,
  poweredByHeader: false,
  output: process.env.MEDORA_STANDALONE === 'true' ? 'standalone' : undefined,
  async rewrites() {
    return [{ source: '/api/:path*', destination: `${backend}/api/:path*` },
      { source: '/backend-health', destination: `${backend}/health` }];
  },
  async headers() {
    return [{ source: '/:path*', headers: [
      {key:'X-Content-Type-Options',value:'nosniff'}, {key:'X-Frame-Options',value:'DENY'},
      {key:'Referrer-Policy',value:'same-origin'}, {key:'Permissions-Policy',value:'camera=(), microphone=(), geolocation=()'}
    ]}];
  }
};
export default config;
