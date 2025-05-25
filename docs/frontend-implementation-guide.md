# PrivSense React Frontend Implementation Guide

This guide provides best practices for implementing a React frontend application that consumes the PrivSense API to create a smooth user experience.

## Table of Contents

1. [Project Setup](#project-setup)
2. [Project Structure](#project-structure)
3. [State Management](#state-management)
4. [API Integration](#api-integration)
5. [Authentication](#authentication)
6. [Component Design](#component-design)
7. [Routing](#routing)
8. [Error Handling](#error-handling)
9. [Loading States](#loading-states)
10. [Real-time Updates](#real-time-updates)
11. [Testing](#testing)
12. [Deployment](#deployment)
13. [Performance Optimization](#performance-optimization)

## Project Setup

### Recommended Tools

- **Create React App** or **Vite**: For fast bootstrapping (Vite recommended for better performance)
- **TypeScript**: For type safety and better developer experience
- **React 18+**: For concurrent rendering features and Suspense
- **Node.js 16+**: For development environment

### Installation

```bash
# Using Vite (recommended)
npm create vite@latest privsense-frontend -- --template react-ts

# OR using Create React App
npx create-react-app privsense-frontend --template typescript

cd privsense-frontend
```

### Essential Dependencies

```bash
# Core dependencies
npm install react-router-dom @tanstack/react-query axios jwt-decode

# UI libraries
npm install @mui/material @mui/icons-material @emotion/react @emotion/styled

# Form handling
npm install react-hook-form zod @hookform/resolvers

# State management
npm install zustand

# Date handling
npm install date-fns

# Charts
npm install recharts

# Development dependencies
npm install -D eslint prettier eslint-config-prettier
```

## Project Structure

Organize your project with a feature-based approach for better maintainability:

```
src/
├── assets/                # Static assets like images, fonts
├── components/            # Shared components used across features
│   ├── common/            # Very common components like Button, Card, etc.
│   ├── layout/            # Layout components like Sidebar, Header, etc.
│   └── ui/                # UI components like modals, tooltips, etc.
├── config/                # Configuration files
├── features/              # Feature-based modules
│   ├── auth/              # Authentication feature
│   ├── connections/       # Database connections feature
│   ├── scans/             # PII scans feature
│   └── sampling/          # Database sampling feature
├── hooks/                 # Custom hooks
├── lib/                   # Third-party library wrappers
├── services/              # API services
│   ├── api.ts             # API client setup
│   ├── auth.service.ts    # Authentication related API calls
│   ├── connections.service.ts
│   └── scans.service.ts
├── store/                 # State management
│   ├── auth.store.ts
│   └── ...
├── types/                 # TypeScript type definitions
├── utils/                 # Utility functions
├── App.tsx                # Main App component
├── main.tsx               # Entry point
└── index.css              # Global styles
```

## State Management

Use a combination of React Query for server state and Zustand for client state:

### API State Management with React Query

```typescript
// src/lib/react-query.ts
import { QueryClient } from '@tanstack/react-query';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});
```

```typescript
// src/main.tsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClientProvider } from '@tanstack/react-query';
import { queryClient } from './lib/react-query';
import App from './App';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  </React.StrictMode>
);
```

### Client State Management with Zustand

```typescript
// src/store/auth.store.ts
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
  token: string | null;
  user: User | null;
  isAuthenticated: boolean;
  login: (token: string, user: User) => void;
  logout: () => void;
}

interface User {
  id: string;
  username: string;
  role: string;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      isAuthenticated: false,
      login: (token, user) => set({ token, user, isAuthenticated: true }),
      logout: () => set({ token: null, user: null, isAuthenticated: false }),
    }),
    {
      name: 'auth-storage',
    }
  )
);
```

## API Integration

Create a structured API layer using Axios:

```typescript
// src/services/api.ts
import axios from 'axios';
import { useAuthStore } from '../store/auth.store';

const API_URL = import.meta.env.VITE_API_URL || 'https://your-server/privsense/api/v1';

export const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add request interceptor to attach token
api.interceptors.request.use(
  (config) => {
    const token = useAuthStore.getState().token;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Add response interceptor for error handling
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    // Handle token expiration
    if (error.response?.status === 401 && useAuthStore.getState().isAuthenticated) {
      useAuthStore.getState().logout();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

### Service Layer for API Calls

```typescript
// src/services/connections.service.ts
import { api } from './api';
import { ConnectionResponse, ConnectionRequest, SchemaInfoDTO } from '../types';

export const connectionsService = {
  listConnections: async (): Promise<ConnectionResponse[]> => {
    const response = await api.get('/connections');
    return response.data;
  },

  getConnection: async (id: string): Promise<ConnectionResponse> => {
    const response = await api.get(`/connections/${id}`);
    return response.data;
  },

  createConnection: async (data: ConnectionRequest): Promise<ConnectionResponse> => {
    const response = await api.post('/connections', data);
    return response.data;
  },

  getDatabaseMetadata: async (id: string): Promise<SchemaInfoDTO> => {
    const response = await api.get(`/connections/${id}/metadata`);
    return response.data;
  },

  closeConnection: async (id: string): Promise<void> => {
    await api.delete(`/connections/${id}`);
  },
};
```

### Hooks for API Calls with React Query

```typescript
// src/features/connections/hooks/useConnections.ts
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { connectionsService } from '../../../services/connections.service';
import { ConnectionRequest } from '../../../types';

export const useConnections = () => {
  const queryClient = useQueryClient();

  const listConnections = useQuery({
    queryKey: ['connections'],
    queryFn: () => connectionsService.listConnections(),
  });

  const getConnection = (id: string) => useQuery({
    queryKey: ['connections', id],
    queryFn: () => connectionsService.getConnection(id),
    enabled: !!id,
  });

  const createConnection = useMutation({
    mutationFn: (data: ConnectionRequest) => connectionsService.createConnection(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['connections'] });
    },
  });

  const getDatabaseMetadata = (id: string) => useQuery({
    queryKey: ['connections', id, 'metadata'],
    queryFn: () => connectionsService.getDatabaseMetadata(id),
    enabled: !!id,
  });

  const closeConnection = useMutation({
    mutationFn: (id: string) => connectionsService.closeConnection(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['connections'] });
    },
  });

  return {
    listConnections,
    getConnection,
    createConnection,
    getDatabaseMetadata,
    closeConnection,
  };
};
```

## Authentication

Create a secure authentication system:

```typescript
// src/services/auth.service.ts
import { api } from './api';

interface LoginRequest {
  username: string;
  password: string;
}

interface LoginResponse {
  meta: {
    timestamp: string;
    status: string;
  };
  token: string;
  refreshToken: string;
  expiresIn: number;
  tokenType: string;
}

export const authService = {
  login: async (data: LoginRequest): Promise<LoginResponse> => {
    const response = await api.post('/auth/login', data);
    return response.data;
  },

  register: async (data: any): Promise<any> => {
    const response = await api.post('/auth/register', data);
    return response.data;
  },
};
```

```typescript
// src/features/auth/hooks/useAuth.ts
import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { authService } from '../../../services/auth.service';
import { useAuthStore } from '../../../store/auth.store';
import { jwtDecode } from 'jwt-decode';

export const useAuth = () => {
  const navigate = useNavigate();
  const { login, logout } = useAuthStore();

  const loginMutation = useMutation({
    mutationFn: authService.login,
    onSuccess: (data) => {
      try {
        const decodedToken = jwtDecode(data.token);
        const user = {
          id: decodedToken.sub as string,
          username: decodedToken.preferred_username as string,
          role: (decodedToken.roles as string[])[0],
        };
        login(data.token, user);
        navigate('/dashboard');
      } catch (error) {
        console.error('Error decoding token', error);
      }
    },
  });

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return {
    loginMutation,
    logout: handleLogout,
    isLoading: loginMutation.isPending,
  };
};
```

### Authentication Component

```tsx
// src/features/auth/components/LoginForm.tsx
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { TextField, Button, Alert, Paper, Typography, Box } from '@mui/material';
import { useAuth } from '../hooks/useAuth';

const loginSchema = z.object({
  username: z.string().email('Invalid email address'),
  password: z.string().min(6, 'Password must be at least 6 characters'),
});

type LoginFormData = z.infer<typeof loginSchema>;

export const LoginForm = () => {
  const { loginMutation, isLoading } = useAuth();
  const [error, setError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data: LoginFormData) => {
    try {
      await loginMutation.mutateAsync(data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to login');
    }
  };

  return (
    <Paper elevation={3} sx={{ p: 4, maxWidth: 400, mx: 'auto', my: 8 }}>
      <Typography variant="h5" component="h1" gutterBottom align="center">
        Login to PrivSense
      </Typography>
      
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      
      <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
        <TextField
          margin="normal"
          required
          fullWidth
          id="username"
          label="Email Address"
          autoComplete="email"
          autoFocus
          {...register('username')}
          error={!!errors.username}
          helperText={errors.username?.message}
        />
        <TextField
          margin="normal"
          required
          fullWidth
          label="Password"
          type="password"
          id="password"
          autoComplete="current-password"
          {...register('password')}
          error={!!errors.password}
          helperText={errors.password?.message}
        />
        <Button
          type="submit"
          fullWidth
          variant="contained"
          sx={{ mt: 3, mb: 2 }}
          disabled={isLoading}
        >
          {isLoading ? 'Logging in...' : 'Login'}
        </Button>
      </Box>
    </Paper>
  );
};
```

## Component Design

Create reusable components for consistent UI:

### Layout Components

```tsx
// src/components/layout/AppLayout.tsx
import { Outlet } from 'react-router-dom';
import { Box } from '@mui/material';
import { Header } from './Header';
import { Sidebar } from './Sidebar';

export const AppLayout = () => {
  return (
    <Box sx={{ display: 'flex', height: '100vh' }}>
      <Header />
      <Sidebar />
      <Box component="main" sx={{ flexGrow: 1, p: 3, mt: 8, overflow: 'auto' }}>
        <Outlet />
      </Box>
    </Box>
  );
};
```

### Feature Components

```tsx
// src/features/connections/components/ConnectionList.tsx
import { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Button,
  Typography,
  Chip,
  Box,
  LinearProgress,
} from '@mui/material';
import { Add as AddIcon } from '@mui/icons-material';
import { useConnections } from '../hooks/useConnections';
import { ConnectionResponse } from '../../../types';

export const ConnectionList = () => {
  const { listConnections } = useConnections();
  const { data: connections, isLoading, error } = listConnections;

  if (isLoading) {
    return (
      <Box sx={{ width: '100%' }}>
        <LinearProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Typography color="error" variant="body1">
        Error loading connections: {error.message}
      </Typography>
    );
  }

  return (
    <>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h5" component="h1">
          Database Connections
        </Typography>
        <Button 
          variant="contained" 
          color="primary" 
          startIcon={<AddIcon />}
          component={Link}
          to="/connections/new"
        >
          Add Connection
        </Button>
      </Box>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Name</TableCell>
              <TableCell>Database Type</TableCell>
              <TableCell>Host</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {connections?.map((connection: ConnectionResponse) => (
              <TableRow key={connection.connectionId}>
                <TableCell>{connection.name}</TableCell>
                <TableCell>{connection.databaseType}</TableCell>
                <TableCell>{connection.host}</TableCell>
                <TableCell>
                  <Chip 
                    label={connection.status} 
                    color={connection.status === 'AVAILABLE' ? 'success' : 'error'} 
                    size="small" 
                  />
                </TableCell>
                <TableCell>
                  <Button 
                    size="small" 
                    variant="outlined"
                    component={Link}
                    to={`/connections/${connection.connectionId}`}
                  >
                    View Details
                  </Button>
                </TableCell>
              </TableRow>
            ))}
            {connections?.length === 0 && (
              <TableRow>
                <TableCell colSpan={5} align="center">
                  No connections found. Create your first connection.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </>
  );
};
```

## Routing

Set up routing with protected routes:

```tsx
// src/components/auth/ProtectedRoute.tsx
import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '../../store/auth.store';

interface ProtectedRouteProps {
  allowedRoles?: string[];
}

export const ProtectedRoute = ({ allowedRoles }: ProtectedRouteProps) => {
  const { isAuthenticated, user } = useAuthStore();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && user && !allowedRoles.includes(user.role)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <Outlet />;
};
```

```tsx
// src/App.tsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AppLayout } from './components/layout/AppLayout';
import { ProtectedRoute } from './components/auth/ProtectedRoute';
import { LoginForm } from './features/auth/components/LoginForm';
import { ConnectionList } from './features/connections/components/ConnectionList';
import { ConnectionDetails } from './features/connections/components/ConnectionDetails';
import { NewConnection } from './features/connections/components/NewConnection';
import { ScanList } from './features/scans/components/ScanList';
import { ScanDetails } from './features/scans/components/ScanDetails';
import { NewScan } from './features/scans/components/NewScan';
import { Dashboard } from './features/dashboard/components/Dashboard';
import { NotFound } from './components/common/NotFound';
import { Unauthorized } from './components/common/Unauthorized';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginForm />} />
        
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<Dashboard />} />
            
            <Route path="/connections">
              <Route index element={<ConnectionList />} />
              <Route path=":id" element={<ConnectionDetails />} />
              <Route path="new" element={<NewConnection />} />
            </Route>

            <Route path="/scans">
              <Route index element={<ScanList />} />
              <Route path=":id" element={<ScanDetails />} />
              <Route path="new" element={<NewScan />} />
            </Route>
            
            {/* Admin only routes */}
            <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
              <Route path="/admin" element={<div>Admin Panel</div>} />
            </Route>
          </Route>
        </Route>
        
        <Route path="/unauthorized" element={<Unauthorized />} />
        <Route path="*" element={<NotFound />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
```

## Error Handling

Implement consistent error handling:

```tsx
// src/components/common/ErrorBoundary.tsx
import { Component, ErrorInfo, ReactNode } from 'react';
import { Typography, Button, Box, Paper } from '@mui/material';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Error caught by error boundary:', error, errorInfo);
    // You could log to an error monitoring service here
  }

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <Paper sx={{ p: 4, maxWidth: 500, mx: 'auto', my: 4 }}>
          <Typography variant="h5" color="error" gutterBottom>
            Something went wrong
          </Typography>
          <Typography variant="body1" paragraph>
            {this.state.error?.message || 'An unexpected error occurred'}
          </Typography>
          <Box sx={{ mt: 2 }}>
            <Button
              variant="contained"
              onClick={() => {
                this.setState({ hasError: false, error: null });
                window.location.href = '/';
              }}
            >
              Go to Homepage
            </Button>
          </Box>
        </Paper>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
```

Use the ErrorBoundary in key components:

```tsx
// src/main.tsx (updated)
import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClientProvider } from '@tanstack/react-query';
import { queryClient } from './lib/react-query';
import App from './App';
import ErrorBoundary from './components/common/ErrorBoundary';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <App />
      </QueryClientProvider>
    </ErrorBoundary>
  </React.StrictMode>
);
```

### API Error Handling Hook

```typescript
// src/hooks/useApiError.ts
import { useState, useCallback } from 'react';
import { AxiosError } from 'axios';

interface ApiError {
  message: string;
  status?: number;
  details?: Record<string, string>;
}

export const useApiError = () => {
  const [error, setError] = useState<ApiError | null>(null);

  const handleError = useCallback((err: unknown) => {
    if (err instanceof AxiosError) {
      const status = err.response?.status;
      const errorData = err.response?.data;
      
      setError({
        message: errorData?.message || err.message || 'An unexpected error occurred',
        status,
        details: errorData?.errors,
      });
    } else if (err instanceof Error) {
      setError({
        message: err.message,
      });
    } else {
      setError({
        message: 'An unexpected error occurred',
      });
    }
  }, []);

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  return {
    error,
    handleError,
    clearError,
  };
};
```

## Loading States

Implement consistent loading states:

```tsx
// src/components/common/LoadingSpinner.tsx
import { CircularProgress, Box, Typography } from '@mui/material';

interface LoadingSpinnerProps {
  message?: string;
}

export const LoadingSpinner = ({ message = 'Loading...' }: LoadingSpinnerProps) => {
  return (
    <Box 
      sx={{ 
        display: 'flex', 
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '200px'
      }}
    >
      <CircularProgress size={40} />
      <Typography variant="body1" sx={{ mt: 2 }}>
        {message}
      </Typography>
    </Box>
  );
};
```

### Skeleton Loaders

```tsx
// src/components/common/TableSkeleton.tsx
import { Skeleton, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper } from '@mui/material';

interface TableSkeletonProps {
  rowCount?: number;
  columnCount?: number;
}

export const TableSkeleton = ({ rowCount = 5, columnCount = 5 }: TableSkeletonProps) => {
  return (
    <TableContainer component={Paper}>
      <Table>
        <TableHead>
          <TableRow>
            {Array(columnCount).fill(0).map((_, index) => (
              <TableCell key={`header-${index}`}>
                <Skeleton variant="text" width="80%" />
              </TableCell>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {Array(rowCount).fill(0).map((_, rowIndex) => (
            <TableRow key={`row-${rowIndex}`}>
              {Array(columnCount).fill(0).map((_, colIndex) => (
                <TableCell key={`cell-${rowIndex}-${colIndex}`}>
                  <Skeleton variant="text" width={colIndex === 0 ? "60%" : "40%"} />
                </TableCell>
              ))}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
};
```

## Real-time Updates

Implement polling for scan status updates:

```tsx
// src/features/scans/hooks/useScanPolling.ts
import { useQuery } from '@tanstack/react-query';
import { useState, useEffect } from 'react';
import { scansService } from '../../../services/scans.service';

export const useScanPolling = (scanId: string) => {
  const [shouldPoll, setShouldPoll] = useState(false);
  
  // Initial fetch
  const scanQuery = useQuery({
    queryKey: ['scans', scanId],
    queryFn: () => scansService.getScanStatus(scanId),
    refetchInterval: shouldPoll ? 5000 : false, // Poll every 5 seconds if shouldPoll is true
  });
  
  // Start/stop polling based on scan status
  useEffect(() => {
    const scan = scanQuery.data;
    if (scan) {
      // Only poll if the scan is pending or running
      setShouldPoll(['PENDING', 'RUNNING'].includes(scan.status));
    } else {
      // Start polling initially
      setShouldPoll(true);
    }
  }, [scanQuery.data]);
  
  return {
    scan: scanQuery.data,
    isLoading: scanQuery.isLoading,
    isError: scanQuery.isError,
    error: scanQuery.error,
  };
};
```

### Progress Tracking Component

```tsx
// src/features/scans/components/ScanProgress.tsx
import { useEffect } from 'react';
import { Box, Typography, LinearProgress, Paper, Chip } from '@mui/material';
import { useScanPolling } from '../hooks/useScanPolling';
import { format } from 'date-fns';

interface ScanProgressProps {
  scanId: string;
  onComplete?: () => void;
}

export const ScanProgress = ({ scanId, onComplete }: ScanProgressProps) => {
  const { scan, isLoading, isError } = useScanPolling(scanId);
  
  useEffect(() => {
    if (scan?.completed && onComplete) {
      onComplete();
    }
  }, [scan?.completed, onComplete]);
  
  if (isLoading) {
    return <LinearProgress />;
  }
  
  if (isError || !scan) {
    return (
      <Typography color="error">
        Failed to load scan status
      </Typography>
    );
  }
  
  return (
    <Paper sx={{ p: 3, mb: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h6">Scan Progress</Typography>
        <Chip 
          label={scan.status} 
          color={
            scan.status === 'COMPLETED' 
              ? 'success' 
              : scan.status === 'FAILED' 
                ? 'error' 
                : 'primary'
          }
        />
      </Box>
      
      <LinearProgress 
        variant="determinate" 
        value={scan.progress} 
        sx={{ height: 10, borderRadius: 5, mb: 2 }}
      />
      
      <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
        <Typography variant="body2">
          Started: {format(new Date(scan.startTime), 'MMM d, yyyy HH:mm:ss')}
        </Typography>
        {scan.estimatedEndTime && (
          <Typography variant="body2">
            Estimated completion: {format(new Date(scan.estimatedEndTime), 'MMM d, yyyy HH:mm:ss')}
          </Typography>
        )}
      </Box>
      
      {scan.currentAction && (
        <Typography variant="body2" sx={{ mt: 1, fontStyle: 'italic' }}>
          Current activity: {scan.currentAction}
        </Typography>
      )}
    </Paper>
  );
};
```

## Testing

Implement testing strategy with React Testing Library and Vitest (or Jest):

```typescript
// src/features/auth/components/__tests__/LoginForm.test.tsx
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { LoginForm } from '../LoginForm';
import { useAuth } from '../../hooks/useAuth';

// Mock the useAuth hook
vi.mock('../../hooks/useAuth', () => ({
  useAuth: vi.fn(),
}));

describe('LoginForm', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    (useAuth as any).mockReturnValue({
      loginMutation: {
        mutateAsync: vi.fn(),
        isPending: false,
      },
      isLoading: false,
    });
  });

  it('renders login form with email and password fields', () => {
    render(<LoginForm />);
    
    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /login/i })).toBeInTheDocument();
  });

  it('validates email input', async () => {
    render(<LoginForm />);
    
    const emailInput = screen.getByLabelText(/email address/i);
    const submitButton = screen.getByRole('button', { name: /login/i });
    
    fireEvent.change(emailInput, { target: { value: 'invalid-email' } });
    fireEvent.click(submitButton);
    
    await waitFor(() => {
      expect(screen.getByText(/invalid email address/i)).toBeInTheDocument();
    });
  });

  it('validates password input', async () => {
    render(<LoginForm />);
    
    const passwordInput = screen.getByLabelText(/password/i);
    const submitButton = screen.getByRole('button', { name: /login/i });
    
    fireEvent.change(passwordInput, { target: { value: '12345' } });
    fireEvent.click(submitButton);
    
    await waitFor(() => {
      expect(screen.getByText(/password must be at least 6 characters/i)).toBeInTheDocument();
    });
  });

  it('submits the form with valid data', async () => {
    const mockMutateAsync = vi.fn().mockResolvedValue({});
    (useAuth as any).mockReturnValue({
      loginMutation: {
        mutateAsync: mockMutateAsync,
        isPending: false,
      },
      isLoading: false,
    });
    
    render(<LoginForm />);
    
    const emailInput = screen.getByLabelText(/email address/i);
    const passwordInput = screen.getByLabelText(/password/i);
    const submitButton = screen.getByRole('button', { name: /login/i });
    
    fireEvent.change(emailInput, { target: { value: 'test@example.com' } });
    fireEvent.change(passwordInput, { target: { value: 'password123' } });
    fireEvent.click(submitButton);
    
    await waitFor(() => {
      expect(mockMutateAsync).toHaveBeenCalledWith({
        username: 'test@example.com',
        password: 'password123',
      });
    });
  });
});
```

## Deployment

Configure for production:

```typescript
// vite.config.ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    outDir: 'build',
    sourcemap: false,
    minify: true,
    // Chunk size optimization
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: [
            'react', 
            'react-dom', 
            'react-router-dom', 
            '@mui/material', 
            '@mui/icons-material'
          ],
          charts: ['recharts'],
          forms: ['react-hook-form', 'zod'],
          queries: ['@tanstack/react-query'],
        },
      },
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'https://your-server/privsense',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '/api/v1'),
      },
    },
  },
});
```

### Docker Configuration

```dockerfile
# Dockerfile
FROM node:18-alpine as build

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# Production stage
FROM nginx:alpine

COPY --from=build /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

```
# nginx.conf
server {
    listen 80;
    root /usr/share/nginx/html;
    index index.html;

    # gzip configuration
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
    gzip_min_length 1000;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/v1/ {
        proxy_pass https://your-server/privsense/api/v1/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Cache static assets
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
        expires 30d;
        add_header Cache-Control "public, no-transform";
    }
}
```

## Performance Optimization

### Code Splitting

Implement code splitting with React.lazy and Suspense:

```tsx
// src/App.tsx (updated with code splitting)
import { Suspense, lazy } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { LoadingSpinner } from './components/common/LoadingSpinner';
import { AppLayout } from './components/layout/AppLayout';
import { ProtectedRoute } from './components/auth/ProtectedRoute';
import { NotFound } from './components/common/NotFound';
import { Unauthorized } from './components/common/Unauthorized';

// Lazy-loaded components
const LoginForm = lazy(() => import('./features/auth/components/LoginForm').then(module => ({ default: module.LoginForm })));
const Dashboard = lazy(() => import('./features/dashboard/components/Dashboard').then(module => ({ default: module.Dashboard })));
const ConnectionList = lazy(() => import('./features/connections/components/ConnectionList').then(module => ({ default: module.ConnectionList })));
const ConnectionDetails = lazy(() => import('./features/connections/components/ConnectionDetails').then(module => ({ default: module.ConnectionDetails })));
const NewConnection = lazy(() => import('./features/connections/components/NewConnection').then(module => ({ default: module.NewConnection })));
const ScanList = lazy(() => import('./features/scans/components/ScanList').then(module => ({ default: module.ScanList })));
const ScanDetails = lazy(() => import('./features/scans/components/ScanDetails').then(module => ({ default: module.ScanDetails })));
const NewScan = lazy(() => import('./features/scans/components/NewScan').then(module => ({ default: module.NewScan })));

function App() {
  return (
    <BrowserRouter>
      <Suspense fallback={<LoadingSpinner message="Loading application..." />}>
        <Routes>
          <Route path="/login" element={<LoginForm />} />
          
          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route path="/dashboard" element={<Dashboard />} />
              
              <Route path="/connections">
                <Route index element={<ConnectionList />} />
                <Route path=":id" element={<ConnectionDetails />} />
                <Route path="new" element={<NewConnection />} />
              </Route>

              <Route path="/scans">
                <Route index element={<ScanList />} />
                <Route path=":id" element={<ScanDetails />} />
                <Route path="new" element={<NewScan />} />
              </Route>
              
              {/* Admin only routes */}
              <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
                <Route path="/admin" element={<div>Admin Panel</div>} />
              </Route>
            </Route>
          </Route>
          
          <Route path="/unauthorized" element={<Unauthorized />} />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
}

export default App;
```

### Memoization

Use React.memo for expensive components:

```tsx
// src/features/scans/components/PiiDistributionChart.tsx
import { memo } from 'react';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend } from 'recharts';

interface PiiDistributionChartProps {
  distribution: Record<string, number>;
}

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884D8', '#82CA9D'];

// Using memo to prevent unnecessary re-renders
export const PiiDistributionChart = memo(({ distribution }: PiiDistributionChartProps) => {
  const data = Object.entries(distribution).map(([name, value]) => ({
    name,
    value,
  }));

  return (
    <ResponsiveContainer width="100%" height={300}>
      <PieChart>
        <Pie
          data={data}
          cx="50%"
          cy="50%"
          labelLine={true}
          label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
          outerRadius={100}
          fill="#8884d8"
          dataKey="value"
        >
          {data.map((entry, index) => (
            <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
          ))}
        </Pie>
        <Tooltip formatter={(value) => [value, 'Count']} />
        <Legend />
      </PieChart>
    </ResponsiveContainer>
  );
});

// Set display name for dev tools
PiiDistributionChart.displayName = 'PiiDistributionChart';
```

By following these best practices and implementation guidelines, you'll be able to build a robust, performant React frontend application that provides a smooth user experience while consuming the PrivSense API.