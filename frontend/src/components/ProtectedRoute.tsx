import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function ProtectedRoute({ children }: { children: React.ReactNode }) {
    const { user, isLoading } = useAuth();

    if (isLoading) {
        return (
            <div className="page-loader">
                <div className="loader-spinner"></div>
                <p>Loading...</p>
            </div>
        );
    }

    return user ? <>{children}</> : <Navigate to="/demo" replace />;
}
