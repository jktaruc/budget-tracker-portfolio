import axios from "axios";

const api = axios.create({
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

        // 402 Payment Required — user hit a pro gate
        // Fire a custom DOM event so any component can show the PaywallModal
        if (error.response?.status === 402) {
            const feature = error.response.data?.message
                ?.replace("' requires a Pro subscription.", "")
                ?.replace("'", "") ?? "This feature";
            window.dispatchEvent(new CustomEvent("paywall", { detail: { feature } }));
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
