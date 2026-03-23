import axios from 'axios';

const api = axios.create({
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
});

export async function login(username: string, password: string): Promise<void> {
  const params = new URLSearchParams();
  params.append('username', username);
  params.append('password', password);
  await api.post('/api/auth/login', params, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  });
}
