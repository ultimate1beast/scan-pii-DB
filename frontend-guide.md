# PrivSense React Frontend Development Guide

## Table of Contents
1. [Introduction](#introduction)
2. [API Overview](#api-overview)
3. [Authentication & Authorization](#authentication--authorization)
4. [Project Structure](#recommended-project-structure)
5. [Core Dependencies](#core-dependencies)
6. [Authentication Implementation](#authentication-implementation)
7. [API Service Layer](#api-service-layer)
8. [State Management](#state-management)
9. [UI Component Structure](#ui-component-structure)
10. [Route Organization](#route-organization)
11. [Error Handling](#error-handling)
12. [Optimization Strategies](#optimization-strategies)
13. [WebSocket Integration](#websocket-integration)
14. [Security Best Practices](#security-best-practices)
15. [Deployment Considerations](#deployment-considerations)

## Introduction

This guide provides comprehensive information for building a React frontend application that consumes the PrivSense API. It covers architecture decisions, best practices, security considerations, and implementation strategies specifically tailored to the PrivSense system.

### Project Goals

The PrivSense frontend application should:
- Provide intuitive interfaces for database PII scanning and management
- Support real-time updates via WebSockets
- Implement proper authentication and authorization
- Follow React best practices for performance and maintainability
- Provide a responsive design for various device sizes

## API Overview

The PrivSense API is a RESTful service organized around the following main domains:

- **Authentication**: User registration, login, and token management
- **Database Connections**: Managing database connections and exploring metadata
- **PII Scans**: Configuring, running, and viewing results of PII scans
- **Database Sampling**: Testing sampling across database tables and columns
- **Dashboard**: Retrieving metrics and visualization data
- **User Management**: Administrative features for user management
- **System**: System information and health monitoring
- **Configuration**: System-wide configuration settings

The API follows REST conventions with standard HTTP methods and status codes. All endpoints are prefixed with `/privsense/api/v1/`.

### API Documentation

The API is documented using OpenAPI 3.0 (Swagger) and is accessible at `/privsense/swagger-ui.html` when the backend is running.

## Authentication & Authorization

### Authentication Flow

The PrivSense API uses JWT (JSON Web Token) authentication with the following flow:

1. **Login**: User submits credentials to `/api/v1/auth/login`
2. **Token Issuance**: Server validates credentials and returns a JWT token
3. **Token Usage**: Frontend includes the token in the `Authorization` header for subsequent requests
4. **Token Expiration**: Tokens expire after a configured time period (default: 1 hour)

### Role-Based Access Control

The API implements role-based access control with two primary roles:
- **ADMIN**: Full system access
- **API_USER**: Limited access to their own resources

Frontend components should adapt based on the user's role to show or hide functionality appropriately.

## Recommended Project Structure

```
src/
├── assets/             # Static assets like images, fonts, etc.
├── components/         # Reusable UI components
│   ├── common/         # Shared components (buttons, cards, etc.)
│   ├── connections/    # Database connection components
│   ├── dashboard/      # Dashboard and visualization components
│   ├── layout/         # Layout components (sidebar, header, etc.)
│   ├── scan/           # PII scan related components
│   └── sampling/       # Database sampling components
├── config/             # Configuration files
├── contexts/           # React contexts (auth, theme, etc.)
├── hooks/              # Custom React hooks
├── pages/              # Top-level page components
├── services/           # API service layer
│   ├── api.js          # Base API configuration
│   ├── auth.service.js # Authentication service
│   ├── connection.service.js # Connection service
│   └── ...             # Other domain-specific services
├── store/              # State management (if using Redux)
├── types/              # TypeScript type definitions
├── utils/              # Utility functions
└── App.jsx             # Root component
```

## Core Dependencies

Recommended libraries for the PrivSense frontend:

```json
{
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.18.0",
    "axios": "^1.6.0",
    "jwt-decode": "^4.0.0",
    "react-query": "^3.39.3",
    "react-hook-form": "^7.48.2",
    "yup": "^1.3.2",
    "date-fns": "^2.30.0",
    "recharts": "^2.9.3",
    "socket.io-client": "^4.7.2",
    "@tanstack/react-table": "^8.11.0",
    "@emotion/react": "^11.11.0",
    "@emotion/styled": "^11.11.0",
    "@mui/material": "^5.14.20",
    "@mui/icons-material": "^5.14.20"
  },
  "devDependencies": {
    "typescript": "^5.2.2",
    "vite": "^5.0.0",
    "@vitejs/plugin-react": "^4.2.0",
    "eslint": "^8.53.0",
    "prettier": "^3.1.0",
    "vitest": "^0.34.6",
    "cypress": "^13.5.0"
  }
}
```

## Authentication Implementation

### JWT Authentication Service

Create a dedicated service for handling authentication:

```javascript
// src/services/auth.service.js
import axios from 'axios';
import { jwtDecode } from 'jwt-decode';

const API_URL = '/privsense/api/v1/auth/';

class AuthService {
  async login(username, password) {
    const response = await axios.post(API_URL + 'login', { username, password });
    if (response.data.token) {
      this.setToken(response.data.token);
    }
    return response.data;
  }

  logout() {
    localStorage.removeItem('token');
  }

  register(username, email, password, firstName, lastName) {
    return axios.post(API_URL + 'register', { 
      username, email, password, firstName, lastName 
    });
  }

  getCurrentUser() {
    const token = this.getToken();
    if (!token) return null;
    
    try {
      const decoded = jwtDecode(token);
      // Check if token is expired
      if (decoded.exp * 1000 < Date.now()) {
        this.logout();
        return null;
      }
      return decoded;
    } catch (error) {
      this.logout();
      return null;
    }
  }

  getToken() {
    return localStorage.getItem('token');
  }

  setToken(token) {
    localStorage.setItem('token', token);
  }

  hasRole(role) {
    const user = this.getCurrentUser();
    return user && user.roles && user.roles.includes(role);
  }

  isAdmin() {
    return this.hasRole('ADMIN');
  }
}

export default new AuthService();
```

### Authentication Context

Set up a React context for application-wide authentication state:

```javascript
// src/contexts/AuthContext.jsx
import React, { createContext, useState, useEffect, useContext } from 'react';
import authService from '../services/auth.service';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [currentUser, setCurrentUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const user = authService.getCurrentUser();
    setCurrentUser(user);
    setLoading(false);
  }, []);

  const login = async (username, password) => {
    try {
      const data = await authService.login(username, password);
      setCurrentUser(authService.getCurrentUser());
      return data;
    } catch (error) {
      throw error;
    }
  };

  const logout = () => {
    authService.logout();
    setCurrentUser(null);
  };

  const value = {
    currentUser,
    login,
    logout,
    isAdmin: () => authService.isAdmin(),
    hasRole: (role) => authService.hasRole(role)
  };

  return (
    <AuthContext.Provider value={value}>
      {!loading && children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
```

## API Service Layer

### Base API Client

Create a centralized API client with interceptors for authentication and error handling:

```javascript
// src/services/api.js
import axios from 'axios';
import authService from './auth.service';

const apiClient = axios.create({
  baseURL: '/privsense/api/v1',
  headers: {
    'Content-Type': 'application/json'
  }
});

// Request interceptor for adding auth token
apiClient.interceptors.request.use(
  (config) => {
    const token = authService.getToken();
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor for handling errors
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const originalRequest = error.config;
    
    // Handle 401 Unauthorized errors (token expired)
    if (error.response && error.response.status === 401 && !originalRequest._retry) {
      // Automatically log out on auth errors
      authService.logout();
      window.location.href = '/login?expired=true';
      return Promise.reject(error);
    }
    
    // Handle other errors
    return Promise.reject(error);
  }
);

export default apiClient;
```

### Domain-Specific Services

Create services for each API domain:

```javascript
// src/services/connection.service.js
import apiClient from './api';

const ConnectionService = {
  getAllConnections() {
    return apiClient.get('/connections');
  },
  
  getConnection(connectionId) {
    return apiClient.get(`/connections/${connectionId}`);
  },
  
  createConnection(connectionData) {
    return apiClient.post('/connections', connectionData);
  },
  
  deleteConnection(connectionId) {
    return apiClient.delete(`/connections/${connectionId}`);
  },
  
  getDatabaseMetadata(connectionId) {
    return apiClient.get(`/connections/${connectionId}/metadata`);
  }
};

export default ConnectionService;
```

## State Management

### React Query for API State

Use React Query for managing server state:

```javascript
// src/hooks/useConnections.js
import { useQuery, useMutation, useQueryClient } from 'react-query';
import ConnectionService from '../services/connection.service';
import { useToast } from '../contexts/ToastContext';

export function useConnections() {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  
  const { data: connections, isLoading, error } = useQuery(
    'connections',
    ConnectionService.getAllConnections,
    {
      select: (data) => data.data,
      onError: (err) => {
        showToast('Error loading connections', 'error');
        console.error(err);
      }
    }
  );
  
  const createConnectionMutation = useMutation(
    (newConnection) => ConnectionService.createConnection(newConnection),
    {
      onSuccess: () => {
        queryClient.invalidateQueries('connections');
        showToast('Connection created successfully', 'success');
      },
      onError: (err) => {
        showToast(err.response?.data?.message || 'Error creating connection', 'error');
      }
    }
  );
  
  return {
    connections,
    isLoading,
    error,
    createConnection: createConnectionMutation.mutate
  };
}
```

### Context API for Application State

For global application state, use React Context API:

```javascript
// src/contexts/ScanContext.jsx
import React, { createContext, useContext, useReducer } from 'react';

// Initial state
const initialState = {
  selectedScan: null,
  scanFilters: {
    status: null,
    connectionId: null
  }
};

// Action types
const SET_SELECTED_SCAN = 'SET_SELECTED_SCAN';
const SET_SCAN_FILTERS = 'SET_SCAN_FILTERS';
const RESET_SCAN_FILTERS = 'RESET_SCAN_FILTERS';

// Reducer
function scanReducer(state, action) {
  switch (action.type) {
    case SET_SELECTED_SCAN:
      return { ...state, selectedScan: action.payload };
    case SET_SCAN_FILTERS:
      return { 
        ...state, 
        scanFilters: { ...state.scanFilters, ...action.payload } 
      };
    case RESET_SCAN_FILTERS:
      return { ...state, scanFilters: initialState.scanFilters };
    default:
      return state;
  }
}

// Context
const ScanContext = createContext();

// Provider
export function ScanProvider({ children }) {
  const [state, dispatch] = useReducer(scanReducer, initialState);
  
  const setSelectedScan = (scan) => {
    dispatch({ type: SET_SELECTED_SCAN, payload: scan });
  };
  
  const setScanFilters = (filters) => {
    dispatch({ type: SET_SCAN_FILTERS, payload: filters });
  };
  
  const resetScanFilters = () => {
    dispatch({ type: RESET_SCAN_FILTERS });
  };
  
  return (
    <ScanContext.Provider value={{
      ...state,
      setSelectedScan,
      setScanFilters,
      resetScanFilters
    }}>
      {children}
    </ScanContext.Provider>
  );
}

// Hook
export function useScanContext() {
  const context = useContext(ScanContext);
  if (!context) {
    throw new Error('useScanContext must be used within a ScanProvider');
  }
  return context;
}
```

## UI Component Structure

Follow a component hierarchy based on atomic design principles:

### Atoms (Base Components)

```javascript
// src/components/common/Button.jsx
import React from 'react';
import { Button as MuiButton } from '@mui/material';

function Button({ children, variant = 'contained', color = 'primary', ...props }) {
  return (
    <MuiButton
      variant={variant}
      color={color}
      {...props}
    >
      {children}
    </MuiButton>
  );
}

export default Button;
```

### Molecules (Compound Components)

```javascript
// src/components/connections/ConnectionCard.jsx
import React from 'react';
import { Card, CardContent, Typography, Chip, Box } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import Button from '../common/Button';

function ConnectionCard({ connection }) {
  const navigate = useNavigate();
  
  const getStatusColor = (status) => {
    switch (status) {
      case 'AVAILABLE': return 'success';
      case 'UNAVAILABLE': return 'warning';
      case 'ERROR': return 'error';
      default: return 'default';
    }
  };
  
  return (
    <Card sx={{ mb: 2 }}>
      <CardContent>
        <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
          <Typography variant="h6">{connection.databaseName}</Typography>
          <Chip 
            label={connection.status} 
            color={getStatusColor(connection.status)} 
            size="small" 
          />
        </Box>
        
        <Typography variant="body2" color="text.secondary" mb={1}>
          {connection.host}:{connection.port}
        </Typography>
        
        <Typography variant="body2" mb={2}>
          {connection.databaseProductName} {connection.databaseProductVersion}
        </Typography>
        
        <Box display="flex" gap={1}>
          <Button 
            size="small" 
            onClick={() => navigate(`/connections/${connection.connectionId}`)}
          >
            Details
          </Button>
          <Button 
            size="small" 
            color="secondary" 
            onClick={() => navigate(`/connections/${connection.connectionId}/metadata`)}
          >
            View Schema
          </Button>
        </Box>
      </CardContent>
    </Card>
  );
}

export default ConnectionCard;
```

### Organisms (Feature Components)

```javascript
// src/components/connections/ConnectionList.jsx
import React from 'react';
import { Grid, Typography, Box, CircularProgress } from '@mui/material';
import ConnectionCard from './ConnectionCard';
import Button from '../common/Button';
import { Add as AddIcon } from '@mui/icons-material';
import { useConnections } from '../../hooks/useConnections';
import { useAuth } from '../../contexts/AuthContext';

function ConnectionList() {
  const { connections, isLoading } = useConnections();
  const { isAdmin } = useAuth();
  
  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" my={4}>
        <CircularProgress />
      </Box>
    );
  }
  
  return (
    <div>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5">Database Connections</Typography>
        {isAdmin() && (
          <Button 
            startIcon={<AddIcon />}
            href="/connections/new"
          >
            New Connection
          </Button>
        )}
      </Box>
      
      {connections?.length === 0 ? (
        <Typography variant="body1">No connections found.</Typography>
      ) : (
        <Grid container spacing={2}>
          {connections?.map((connection) => (
            <Grid item xs={12} md={6} lg={4} key={connection.connectionId}>
              <ConnectionCard connection={connection} />
            </Grid>
          ))}
        </Grid>
      )}
    </div>
  );
}

export default ConnectionList;
```

## Route Organization

Use a centralized routing structure:

```javascript
// src/App.jsx
import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from 'react-query';
import { ReactQueryDevtools } from 'react-query/devtools';
import { ThemeProvider } from '@mui/material/styles';

// Contexts
import { AuthProvider } from './contexts/AuthContext';
import { ToastProvider } from './contexts/ToastContext';
import { ScanProvider } from './contexts/ScanContext';

// Layouts
import MainLayout from './components/layout/MainLayout';

// Pages
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import ConnectionsPage from './pages/ConnectionsPage';
import ConnectionDetailPage from './pages/ConnectionDetailPage';
import MetadataPage from './pages/MetadataPage';
import ScansPage from './pages/ScansPage';
import ScanDetailPage from './pages/ScanDetailPage';
import SamplingPage from './pages/SamplingPage';
import UserManagementPage from './pages/UserManagementPage';
import SettingsPage from './pages/SettingsPage';
import NotFoundPage from './pages/NotFoundPage';

// Guards
import PrivateRoute from './components/common/PrivateRoute';
import AdminRoute from './components/common/AdminRoute';

// Theme
import theme from './config/theme';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1
    }
  }
});

function App() {
  return (
    <BrowserRouter>
      <QueryClientProvider client={queryClient}>
        <ThemeProvider theme={theme}>
          <AuthProvider>
            <ToastProvider>
              <ScanProvider>
                <Routes>
                  <Route path="/login" element={<LoginPage />} />
                  <Route path="/register" element={<RegisterPage />} />
                  
                  {/* Protected routes within MainLayout */}
                  <Route path="/" element={<PrivateRoute><MainLayout /></PrivateRoute>}>
                    <Route index element={<Navigate to="/dashboard" replace />} />
                    <Route path="dashboard" element={<DashboardPage />} />
                    
                    <Route path="connections">
                      <Route index element={<ConnectionsPage />} />
                      <Route path=":connectionId" element={<ConnectionDetailPage />} />
                      <Route path=":connectionId/metadata" element={<MetadataPage />} />
                      <Route path="new" element={<AdminRoute><ConnectionFormPage /></AdminRoute>} />
                    </Route>
                    
                    <Route path="scans">
                      <Route index element={<ScansPage />} />
                      <Route path=":scanId" element={<ScanDetailPage />} />
                      <Route path="new" element={<ScanFormPage />} />
                    </Route>
                    
                    <Route path="sampling" element={<SamplingPage />} />
                    
                    {/* Admin-only routes */}
                    <Route path="users" element={<AdminRoute><UserManagementPage /></AdminRoute>} />
                    <Route path="settings" element={<AdminRoute><SettingsPage /></AdminRoute>} />
                  </Route>
                  
                  <Route path="*" element={<NotFoundPage />} />
                </Routes>
              </ScanProvider>
            </ToastProvider>
          </AuthProvider>
        </ThemeProvider>
        <ReactQueryDevtools initialIsOpen={false} position="bottom-right" />
      </QueryClientProvider>
    </BrowserRouter>
  );
}

export default App;
```

## Error Handling

### Centralized Error Handling

Implement a consistent error handling mechanism:

```javascript
// src/utils/errorHandler.js
import { toast } from 'react-toastify';

export function handleApiError(error, customMessage = null) {
  const defaultMessage = 'An unexpected error occurred';
  
  // Network errors
  if (!error.response) {
    console.error('Network error:', error);
    toast.error('Network error. Please check your connection.');
    return defaultMessage;
  }
  
  const { status, data } = error.response;
  
  // Log the error for debugging
  console.error(`API Error (${status}):`, data);
  
  // Handle specific status codes
  switch (status) {
    case 400:
      return formatValidationErrors(data) || customMessage || 'Invalid request';
    case 401:
      return customMessage || 'Authentication required';
    case 403:
      return customMessage || 'You do not have permission to perform this action';
    case 404:
      return customMessage || 'The requested resource was not found';
    case 409:
      return customMessage || data.message || 'Conflict with current state';
    case 500:
      return customMessage || 'Server error. Please try again later.';
    default:
      return customMessage || data.message || defaultMessage;
  }
}

function formatValidationErrors(data) {
  if (data.errors && Object.keys(data.errors).length > 0) {
    const errorMessages = Object.entries(data.errors)
      .map(([field, msg]) => `${field}: ${msg}`)
      .join('; ');
    
    return `Validation errors: ${errorMessages}`;
  }
  
  return data.message || null;
}
```

### Custom Error Boundary Component

```javascript
// src/components/common/ErrorBoundary.jsx
import React from 'react';
import { Typography, Button, Box, Paper } from '@mui/material';
import { ErrorOutline } from '@mui/icons-material';

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('Uncaught error:', error, errorInfo);
    // You can log to an error monitoring service here
  }

  render() {
    if (this.state.hasError) {
      return (
        <Box 
          display="flex" 
          justifyContent="center" 
          alignItems="center" 
          minHeight={400}
          p={3}
        >
          <Paper elevation={3} sx={{ p: 4, maxWidth: 600, textAlign: 'center' }}>
            <ErrorOutline color="error" sx={{ fontSize: 60, mb: 2 }} />
            <Typography variant="h5" gutterBottom>
              Something went wrong
            </Typography>
            <Typography variant="body1" color="text.secondary" mb={3}>
              {this.state.error?.message || 'An unexpected error occurred'}
            </Typography>
            <Box>
              <Button 
                variant="contained" 
                onClick={() => window.location.reload()}
                sx={{ mr: 2 }}
              >
                Reload page
              </Button>
              <Button 
                variant="outlined" 
                onClick={() => this.setState({ hasError: false, error: null })}
              >
                Try again
              </Button>
            </Box>
          </Paper>
        </Box>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
```

## WebSocket Integration

### WebSocket Service

Implement real-time updates for scan status:

```javascript
// src/services/websocket.service.js
import { io } from 'socket.io-client';
import authService from './auth.service';

class WebSocketService {
  constructor() {
    this.socket = null;
    this.listeners = new Map();
  }

  connect() {
    if (this.socket) return;

    const token = authService.getToken();
    if (!token) return;

    this.socket = io('/privsense', {
      path: '/websocket',
      auth: {
        token
      },
      transports: ['websocket', 'polling'],
      reconnectionAttempts: 5,
      reconnectionDelay: 1000
    });

    this.socket.on('connect', () => {
      console.log('WebSocket connected');
    });

    this.socket.on('connect_error', (err) => {
      console.error('WebSocket connection error:', err);
    });

    this.socket.on('disconnect', (reason) => {
      console.log('WebSocket disconnected:', reason);
      
      // If the disconnection is due to auth issues, logout the user
      if (reason === 'io server disconnect') {
        authService.logout();
        window.location.href = '/login?expired=true';
      }
    });

    // Set up event listeners for scan status updates
    this.socket.on('scan_update', (data) => {
      this.notifyListeners('scan_update', data);
    });

    this.socket.on('scan_completed', (data) => {
      this.notifyListeners('scan_completed', data);
    });

    this.socket.on('scan_failed', (data) => {
      this.notifyListeners('scan_failed', data);
    });
  }

  disconnect() {
    if (this.socket) {
      this.socket.disconnect();
      this.socket = null;
    }
  }

  addListener(event, callback) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set());
    }
    this.listeners.get(event).add(callback);
    return () => this.removeListener(event, callback);
  }

  removeListener(event, callback) {
    if (this.listeners.has(event)) {
      this.listeners.get(event).delete(callback);
    }
  }

  notifyListeners(event, data) {
    if (this.listeners.has(event)) {
      this.listeners.get(event).forEach(callback => callback(data));
    }
  }
}

export default new WebSocketService();
```

### WebSocket Context

Create a context for WebSocket:

```javascript
// src/contexts/WebSocketContext.jsx
import React, { createContext, useContext, useEffect, useState } from 'react';
import webSocketService from '../services/websocket.service';
import { useAuth } from './AuthContext';

const WebSocketContext = createContext(null);

export function WebSocketProvider({ children }) {
  const { currentUser } = useAuth();
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    if (currentUser) {
      webSocketService.connect();
      setConnected(true);

      // Add connection status listener
      const removeConnectListener = webSocketService.addListener('connect', () => {
        setConnected(true);
      });

      const removeDisconnectListener = webSocketService.addListener('disconnect', () => {
        setConnected(false);
      });

      return () => {
        removeConnectListener();
        removeDisconnectListener();
        webSocketService.disconnect();
      };
    } else {
      webSocketService.disconnect();
      setConnected(false);
    }
  }, [currentUser]);

  const subscribe = (event, callback) => {
    return webSocketService.addListener(event, callback);
  };

  return (
    <WebSocketContext.Provider value={{ connected, subscribe }}>
      {children}
    </WebSocketContext.Provider>
  );
}

export function useWebSocket() {
  const context = useContext(WebSocketContext);
  if (!context) {
    throw new Error('useWebSocket must be used within a WebSocketProvider');
  }
  return context;
}
```

## Security Best Practices

### Secure Storage

Never store sensitive information in local storage. Use JWT for authentication, but consider using secure HTTP-only cookies for token storage in production:

```javascript
// In production, modify your auth service to work with cookies instead
// and use a proper CSRF protection mechanism
```

### Input Validation

Validate all user input before sending to the API:

```javascript
// src/components/connections/ConnectionForm.jsx
import React from 'react';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import { TextField, Box, Grid, MenuItem } from '@mui/material';
import Button from '../common/Button';

// Validation schema
const connectionSchema = yup.object({
  host: yup.string().required('Host is required'),
  port: yup.number()
    .typeError('Port must be a number')
    .integer('Port must be an integer')
    .min(1, 'Port must be at least 1')
    .max(65535, 'Port must be at most 65535')
    .required('Port is required'),
  databaseName: yup.string().required('Database name is required'),
  username: yup.string().required('Username is required'),
  password: yup.string().required('Password is required'),
  driverClassName: yup.string().required('Driver is required')
}).required();

function ConnectionForm({ onSubmit, initialData = {} }) {
  const { register, handleSubmit, formState: { errors } } = useForm({
    resolver: yupResolver(connectionSchema),
    defaultValues: initialData
  });
  
  const driverOptions = [
    { value: 'org.postgresql.Driver', label: 'PostgreSQL' },
    { value: 'com.mysql.cj.jdbc.Driver', label: 'MySQL' },
    { value: 'oracle.jdbc.OracleDriver', label: 'Oracle' },
    { value: 'com.microsoft.sqlserver.jdbc.SQLServerDriver', label: 'SQL Server' }
  ];
  
  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <Grid container spacing={2}>
        <Grid item xs={12} sm={8}>
          <TextField
            fullWidth
            label="Host"
            {...register('host')}
            error={!!errors.host}
            helperText={errors.host?.message}
          />
        </Grid>
        <Grid item xs={12} sm={4}>
          <TextField
            fullWidth
            label="Port"
            type="number"
            {...register('port')}
            error={!!errors.port}
            helperText={errors.port?.message}
          />
        </Grid>
        <Grid item xs={12}>
          <TextField
            fullWidth
            label="Database Name"
            {...register('databaseName')}
            error={!!errors.databaseName}
            helperText={errors.databaseName?.message}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <TextField
            fullWidth
            label="Username"
            {...register('username')}
            error={!!errors.username}
            helperText={errors.username?.message}
          />
        </Grid>
        <Grid item xs={12} sm={6}>
          <TextField
            fullWidth
            label="Password"
            type="password"
            {...register('password')}
            error={!!errors.password}
            helperText={errors.password?.message}
          />
        </Grid>
        <Grid item xs={12}>
          <TextField
            fullWidth
            select
            label="Database Type"
            {...register('driverClassName')}
            error={!!errors.driverClassName}
            helperText={errors.driverClassName?.message}
          >
            {driverOptions.map(option => (
              <MenuItem key={option.value} value={option.value}>
                {option.label}
              </MenuItem>
            ))}
          </TextField>
        </Grid>
        <Grid item xs={12}>
          <Box display="flex" justifyContent="flex-end" mt={2}>
            <Button type="submit" variant="contained">
              Save Connection
            </Button>
          </Box>
        </Grid>
      </Grid>
    </form>
  );
}

export default ConnectionForm;
```

### Content Security Policy

```javascript
// Add to your index.html or configure in your server

// Example CSP header:
// Content-Security-Policy: default-src 'self'; connect-src 'self' wss://*.privsense.com; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:;
```



### Component Testing

Use React Testing Library with Vitest:

```javascript
// src/components/common/__tests__/Button.test.jsx
import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { expect, test, vi } from 'vitest';
import Button from '../Button';

test('renders button with provided text', () => {
  render(<Button>Click me</Button>);
  expect(screen.getByText('Click me')).toBeInTheDocument();
});

test('calls onClick handler when clicked', () => {
  const handleClick = vi.fn();
  render(<Button onClick={handleClick}>Click me</Button>);
  fireEvent.click(screen.getByText('Click me'));
  expect(handleClick).toHaveBeenCalled();
});

test('applies different variants correctly', () => {
  const { rerender } = render(<Button variant="contained">Contained</Button>);
  expect(screen.getByText('Contained')).toHaveClass('MuiButton-contained');
  
  rerender(<Button variant="outlined">Outlined</Button>);
  expect(screen.getByText('Outlined')).toHaveClass('MuiButton-outlined');
});
```

### End-to-End Testing

Use Cypress for end-to-end testing:

```javascript
// cypress/e2e/login.cy.js
describe('Login flow', () => {
  beforeEach(() => {
    cy.visit('/login');
  });
  
  it('shows validation errors for empty fields', () => {
    cy.get('button[type="submit"]').click();
    cy.contains('Username is required').should('be.visible');
    cy.contains('Password is required').should('be.visible');
  });
  
  it('shows error for invalid credentials', () => {
    cy.get('input[name="username"]').type('invaliduser');
    cy.get('input[name="password"]').type('wrongpassword');
    cy.get('button[type="submit"]').click();
    
    cy.contains('Invalid credentials').should('be.visible');
  });
  
  it('successfully logs in with valid credentials', () => {
    // Intercept the API call to mock successful login
    cy.intercept('POST', '/privsense/api/v1/auth/login', {
      statusCode: 200,
      body: {
        token: 'fake-jwt-token'
      }
    }).as('loginRequest');
    
    cy.get('input[name="username"]').type('testuser');
    cy.get('input[name="password"]').type('password123');
    cy.get('button[type="submit"]').click();
    
    cy.wait('@loginRequest');
    cy.url().should('include', '/dashboard');
  });
});
```

## Deployment Considerations

### Environment Configuration

Use environment variables for configuration:

```javascript
// src/config/environment.js
export const API_URL = import.meta.env.VITE_API_URL || '/privsense/api/v1';
export const WS_URL = import.meta.env.VITE_WS_URL || '/privsense';
export const ENV = import.meta.env.VITE_ENV || 'development';
export const DEBUG = ENV === 'development';
```

### Build Optimization

Optimize your Vite build configuration:

```javascript
// vite.config.js
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { visualizer } from 'rollup-plugin-visualizer';

export default defineConfig({
  plugins: [
    react(),
    visualizer({
      filename: 'stats.html',
      open: false,
    }),
  ],
  build: {
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom', 'react-router-dom'],
          mui: ['@mui/material', '@mui/icons-material'],
          charts: ['recharts'],
        },
      },
    },
    chunkSizeWarningLimit: 1000,
  },
  server: {
    proxy: {
      '/privsense': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
```

### Performance Monitoring

Add performance monitoring to track application performance:

```javascript
// src/utils/monitoring.js
export function trackPageLoad(pageName) {
  if (window.performance) {
    const perfData = window.performance.timing;
    const pageLoadTime = perfData.loadEventEnd - perfData.navigationStart;
    
    // Log to console in development
    if (process.env.NODE_ENV === 'development') {
      console.log(`Page ${pageName} loaded in ${pageLoadTime}ms`);
    }
    
    // In production, you would send this to a monitoring service
    // analyticsService.trackTiming('page_load', pageLoadTime, { page: pageName });
  }
}
```

## Conclusion

This guide provides a solid foundation for developing a React frontend for the PrivSense API. By following these best practices, you can build a maintainable, secure, and performant application that effectively leverages the capabilities of the PrivSense system.

Remember to:
- Use the appropriate patterns for authentication and state management
- Structure your components following atomic design principles
- Implement consistent error handling
- Write tests for critical functionality
- Optimize for performance

As you develop the application, continuously refer to the OpenAPI documentation to ensure you're correctly implementing the API contract.
