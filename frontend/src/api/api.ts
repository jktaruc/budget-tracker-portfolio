import axios from "axios";

const api = axios.create({
    // On Render: VITE_API_BASE_URL = "https://budget-tracker-backend-n5jp.onrender.com/api"
    // Locally:   VITE_API_BASE_URL is unset → falls back to "/api" → Nginx proxies to backend
    baseURL: import.meta.env.VITE_API_BASE_URL || "/api",
});

// Attach stored token to every request
api.interceptors.request.use((config) => {
    const token = localStorage.getItem("accessToken");
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// Auth and demo endpoints pass errors straight through to the component
const PUBLIC_ENDPOINTS = ["/auth/", "/demo/"];

api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;
        const isPublicEndpoint = PUBLIC_ENDPOINTS.some(p => originalRequest.url?.includes(p));

        if (isPublicEndpoint) {
            return Promise.reject(error);
        }

        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;
            const refreshToken = localStorage.getItem("refreshToken");

            if (refreshToken) {
                try {
                    const { data } = await api.post("/auth/refresh", { refreshToken });
                    localStorage.setItem("accessToken", data.accessToken);
                    localStorage.setItem("refreshToken", data.refreshToken);
                    api.defaults.headers.common["Authorization"] = `Bearer ${data.accessToken}`;
                    originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
                    return api(originalRequest);
                } catch {
                    // Refresh failed — clear session and send to login, not demo
                    localStorage.clear();
                    window.location.href = "/login";
                }
            } else {
                window.location.href = "/login";
            }
        }

        return Promise.reject(error);
    }
);

export default api;
