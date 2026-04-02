import axios from 'axios';

const apiBaseURL = import.meta.env.VITE_API_BASE_URL ?? '/api/api';

export const http = axios.create({
  baseURL: apiBaseURL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
});

