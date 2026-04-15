import axios from 'axios';

const rawBaseURL = import.meta.env.VITE_API_BASE_URL ?? '';
const apiBaseURL = rawBaseURL.replace(/\/api\/?$/, '');

export const http = axios.create({
  baseURL: apiBaseURL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
});
