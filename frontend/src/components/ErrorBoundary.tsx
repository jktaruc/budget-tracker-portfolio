/// <reference types="vite/client" />

import React from "react";
import "../styles/ErrorBoundary.css";

interface ErrorBoundaryProps {
    children: React.ReactNode;
    fallback?: React.ReactNode;
}

interface ErrorBoundaryState {
    hasError: boolean;
    error: Error | null;
    errorInfo: React.ErrorInfo | null;
}

class ErrorBoundary extends React.Component<ErrorBoundaryProps, ErrorBoundaryState> {
    constructor(props: ErrorBoundaryProps) {
        super(props);
        this.state = { hasError: false, error: null, errorInfo: null };
    }

    static getDerivedStateFromError(error: Error): Partial<ErrorBoundaryState> {
        return { hasError: true, error };
    }

    componentDidCatch(error: Error, errorInfo: React.ErrorInfo): void {
        console.error("Error caught by boundary:", error, errorInfo);
        this.setState({ error, errorInfo });
    }

    handleReset = (): void => {
        this.setState({ hasError: false, error: null, errorInfo: null });
        window.location.reload();
    };

    render(): React.ReactNode {
        if (this.state.hasError) {
            if (this.props.fallback) {
                return this.props.fallback;
            }

            return (
                <div className="error-boundary">
                    <div className="error-content">
                        <h1>😕 Oops! Something went wrong</h1>
                        <p className="error-message">
                            {this.state.error?.message ?? "An unexpected error occurred"}
                        </p>

                        {import.meta.env.DEV && this.state.errorInfo && (
                            <details className="error-details">
                                <summary>Error Details (Dev Only)</summary>
                                <pre>{this.state.errorInfo.componentStack}</pre>
                            </details>
                        )}

                        <button className="error-reload-btn" onClick={this.handleReset}>
                            🔄 Reload Page
                        </button>
                    </div>
                </div>
            );
        }

        return this.props.children;
    }
}

export default ErrorBoundary;
