# PrivSense React JS Implementation Plan

This document outlines a step-by-step approach to implementing the PrivSense React JS frontend application based on the workflow document and API specifications.

## Phase 1: Project Setup and Foundation (Week 1)

### Step 1: Environment Setup (Day 1)
- [ ] Create React application using Vite
  ```bash
  npm create vite@latest privsense-frontend -- --template react
  cd privsense-frontend
  ```
- [ ] Install core dependencies
  ```bash
  npm install react-router-dom @tanstack/react-query axios @mui/material @mui/icons-material @emotion/react @emotion/styled zod @hookform/resolvers react-hook-form
  ```
- [ ] Install development dependencies
  ```bash
  npm install -D eslint eslint-plugin-react eslint-plugin-react-hooks prettier eslint-plugin-prettier
  ```

### Step 2: Configuration Files (Day 1-2)
- [ ] Configure ESLint and Prettier
  ```js
  // .eslintrc.js
  module.exports = {
    "env": {
      "browser": true,
      "es2021": true,
      "node": true
    },
    "extends": [
      "eslint:recommended",
      "plugin:react/recommended",
      "plugin:react-hooks/recommended"
    ],
    "parserOptions": {
      "ecmaFeatures": {
        "jsx": true
      },
      "ecmaVersion": "latest",
      "sourceType": "module"
    },
    "plugins": [
      "react",
      "react-hooks"
    ],
    "rules": {
      "react/react-in-jsx-scope": "off",
      "react-hooks/rules-of-hooks": "error",
      "react-hooks/exhaustive-deps": "warn"
    },
    "settings": {
      "react": {
        "version": "detect"
      }
    }
  }
  ```
- [ ] Create .env files (.env.development, .env.production)
- [ ] Set up Vite configuration (vite.config.js)
  ```js
  // vite.config.js
  import { defineConfig } from 'vite';
  import react from '@vitejs/plugin-react';
  
  export default defineConfig({
    plugins: [react()],
    server: {
      port: 3000,
      proxy: {
        '/privsense': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        }
      }
    }
  });
  ```

### Step 3: Project Structure (Day 2)
- [ ] Create folder structure according to the architecture design
```
src/
├── assets/                # Static assets (images, fonts, icons)
├── components/            # Shared UI components
│   ├── common/            # Basic UI elements (buttons, inputs, etc.)
│   ├── layout/            # Layout components (header, sidebar, etc.)
│   ├── feedback/          # Loading states, error messages, etc.
│   ├── tables/            # Table components and related utilities
│   ├── charts/            # Data visualization components
│   └── forms/             # Form components and form-related utilities
├── config/                # Configuration files
├── contexts/              # React context providers
├── features/              # Feature-specific code
│   ├── auth/              # Authentication feature
│   ├── connections/       # Database connections feature
│   ├── scans/             # PII scans feature
│   ├── dashboard/         # Dashboard feature
│   ├── templates/         # Scan templates feature
│   ├── administration/    # Administration feature
│   └── settings/          # Settings feature
├── hooks/                 # Custom React hooks
├── layouts/               # Page layouts
├── lib/                   # External library wrappers and configurations
├── pages/                 # Page components
├── router/                # Routing configuration
├── services/              # API service layer
├── store/                 # Global state management (if needed)
├── styles/                # Global styles and theming
├── utils/                 # Utility functions
├── App.jsx                # Root component
└── index.jsx              # Application entry point
```
- [ ] Set up routing with React Router
  ```jsx
  // src/router/index.jsx
  import { createBrowserRouter } from 'react-router-dom';
  import AppLayout from '../layouts/AppLayout';
  import LoginPage from '../pages/auth/LoginPage';
  import DashboardPage from '../pages/dashboard/DashboardPage';
  import Error404Page from '../pages/Error404Page';
  
  const router = createBrowserRouter([
    {
      path: '/login',
      element: <LoginPage />
    },
    {
      path: '/',
      element: <AppLayout />,
      children: [
        {
          path: 'dashboard',
          element: <DashboardPage />
        },
        // Add more routes as needed
      ]
    },
    {
      path: '*',
      element: <Error404Page />
    }
  ]);
  
  export default router;
  ```

- [ ] Configure base API client
  ```jsx
  // src/lib/api.js
  import axios from 'axios';
  import { refreshToken, logout } from '../services/auth.service';
  
  const baseURL = import.meta.env.VITE_API_BASE_URL || '/privsense';
  
  const api = axios.create({
    baseURL,
    headers: {
      'Content-Type': 'application/json',
    },
  });
  
  // Request interceptor
  api.interceptors.request.use(
    (config) => {
      const token = localStorage.getItem('auth_token');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    },
    (error) => Promise.reject(error)
  );
  
  // Response interceptor
  api.interceptors.response.use(
    (response) => response,
    async (error) => {
      const originalRequest = error.config;
      
      // Handle 401 Unauthorized errors
      if (error.response?.status === 401 && !originalRequest._retry) {
        originalRequest._retry = true;
        try {
          // Try to refresh the token
          await refreshToken();
          return api(originalRequest);
        } catch (refreshError) {
          // If refresh fails, logout
          logout();
          return Promise.reject(refreshError);
        }
      }
      
      return Promise.reject(error);
    }
  );
  
  export default api;
  ```

- [ ] Set up the global theme and styles
  ```jsx
  // src/styles/theme.js
  import { createTheme } from '@mui/material/styles';
  
  const theme = createTheme({
    palette: {
      primary: {
        main: '#1976d2',
      },
      secondary: {
        main: '#dc004e',
      },
    },
    typography: {
      fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
    },
  });
  
  export default theme;
  ```

### Step 4: Authentication Context (Day 3-4)
- [ ] Implement AuthContext with login/logout functionality
  ```jsx
  // src/contexts/AuthContext.jsx
  import { createContext, useState, useContext, useEffect } from 'react';
  import { authService } from '../services/auth.service';
  
  const AuthContext = createContext(null);
  
  export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    
    useEffect(() => {
      const initAuth = async () => {
        try {
          const token = localStorage.getItem('auth_token');
          if (token) {
            const currentUser = await authService.getCurrentUser();
            setUser(currentUser);
          }
        } catch (err) {
          console.error('Failed to initialize auth:', err);
          logout();
        } finally {
          setLoading(false);
        }
      };
      
      initAuth();
    }, []);
    
    const login = async (username, password) => {
      setLoading(true);
      setError(null);
      try {
        const { token, user } = await authService.login(username, password);
        localStorage.setItem('auth_token', token);
        setUser(user);
        return user;
      } catch (err) {
        setError(err.message || 'Failed to login');
        throw err;
      } finally {
        setLoading(false);
      }
    };
    
    const logout = () => {
      localStorage.removeItem('auth_token');
      setUser(null);
    };
    
    const isAuthenticated = !!user;
    const isAdmin = user?.roles?.includes('ADMIN') || false;
    
    return (
      <AuthContext.Provider value={{
        user,
        login,
        logout,
        loading,
        error,
        isAuthenticated,
        isAdmin
      }}>
        {children}
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

- [ ] Create authentication services
  ```jsx
  // src/services/auth.service.js
  import api from '../lib/api';
  
  export const authService = {
    login: async (username, password) => {
      const response = await api.post('/api/v1/auth/login', { username, password });
      return response.data;
    },
    
    register: async (userData) => {
      const response = await api.post('/api/v1/auth/register', userData);
      return response.data;
    },
    
    refreshToken: async () => {
      // Implement token refresh logic here
      // This is a placeholder and depends on your backend implementation
      const token = localStorage.getItem('auth_token');
      if (!token) {
        throw new Error('No token to refresh');
      }
      
      // Example implementation
      const response = await api.post('/api/v1/auth/refresh', { token });
      localStorage.setItem('auth_token', response.data.token);
      return response.data;
    },
    
    getCurrentUser: async () => {
      const response = await api.get('/api/v1/auth/me');
      return response.data;
    },
    
    logout: () => {
      localStorage.removeItem('auth_token');
    }
  };
  
  export const { refreshToken, logout } = authService;
  ```

- [ ] Create protected route components
  ```jsx
  // src/router/PrivateRoute.jsx
  import { Navigate, useLocation } from 'react-router-dom';
  import { useAuth } from '../contexts/AuthContext';
  import LoadingScreen from '../components/feedback/LoadingScreen';
  
  export function PrivateRoute({ children }) {
    const { isAuthenticated, loading } = useAuth();
    const location = useLocation();
    
    if (loading) {
      return <LoadingScreen />;
    }
    
    if (!isAuthenticated) {
      return <Navigate to="/login" state={{ from: location }} replace />;
    }
    
    return children;
  }
  
  export function AdminRoute({ children }) {
    const { isAdmin, loading, isAuthenticated } = useAuth();
    const location = useLocation();
    
    if (loading) {
      return <LoadingScreen />;
    }
    
    if (!isAuthenticated) {
      return <Navigate to="/login" state={{ from: location }} replace />;
    }
    
    if (!isAdmin) {
      return <Navigate to="/dashboard" replace />;
    }
    
    return children;
  }
  ```

### Step 5: Core Layout Components (Day 5)
- [ ] Create AppLayout with responsive design
  ```jsx
  // src/layouts/AppLayout.jsx
  import { useState } from 'react';
  import { Outlet } from 'react-router-dom';
  import { Box, CssBaseline, Toolbar } from '@mui/material';
  import Header from '../components/layout/Header';
  import Sidebar from '../components/layout/Sidebar';
  import ErrorBoundary from '../components/feedback/ErrorBoundary';
  
  const drawerWidth = 240;
  
  export default function AppLayout() {
    const [sidebarOpen, setSidebarOpen] = useState(true);
    
    const toggleSidebar = () => {
      setSidebarOpen(!sidebarOpen);
    };
    
    return (
      <Box sx={{ display: 'flex' }}>
        <CssBaseline />
        <Header 
          drawerWidth={drawerWidth} 
          sidebarOpen={sidebarOpen} 
          toggleSidebar={toggleSidebar} 
        />
        <Sidebar 
          drawerWidth={drawerWidth} 
          open={sidebarOpen} 
        />
        <Box
          component="main"
          sx={{
            flexGrow: 1,
            padding: 3,
            width: { sm: `calc(100% - ${drawerWidth}px)` },
            marginLeft: { sm: sidebarOpen ? `${drawerWidth}px` : 0 },
            transition: 'margin 0.2s ease',
          }}
        >
          <Toolbar />
          <ErrorBoundary>
            <Outlet />
          </ErrorBoundary>
        </Box>
      </Box>
    );
  }
  ```

- [ ] Implement Header component
  ```jsx
  // src/components/layout/Header.jsx
  import { useState } from 'react';
  import { useNavigate } from 'react-router-dom';
  import {
    AppBar,
    Toolbar,
    Typography,
    IconButton,
    Box,
    Menu,
    MenuItem,
    Tooltip,
    Avatar,
  } from '@mui/material';
  import MenuIcon from '@mui/icons-material/Menu';
  import AccountCircleIcon from '@mui/icons-material/AccountCircle';
  import NotificationsIcon from '@mui/icons-material/Notifications';
  import { useAuth } from '../../contexts/AuthContext';
  
  export default function Header({ drawerWidth, sidebarOpen, toggleSidebar }) {
    const navigate = useNavigate();
    const { user, logout } = useAuth();
    const [accountMenu, setAccountMenu] = useState(null);
    
    const handleAccountMenuOpen = (event) => {
      setAccountMenu(event.currentTarget);
    };
    
    const handleAccountMenuClose = () => {
      setAccountMenu(null);
    };
    
    const handleLogout = () => {
      logout();
      navigate('/login');
      handleAccountMenuClose();
    };
    
    const handleProfileClick = () => {
      navigate('/profile');
      handleAccountMenuClose();
    };
    
    return (
      <AppBar
        position="fixed"
        sx={{
          width: { sm: sidebarOpen ? `calc(100% - ${drawerWidth}px)` : '100%' },
          ml: { sm: sidebarOpen ? `${drawerWidth}px` : 0 },
          transition: 'width 0.2s ease, margin-left 0.2s ease',
          zIndex: (theme) => theme.zIndex.drawer + 1,
        }}
      >
        <Toolbar>
          <IconButton
            color="inherit"
            aria-label="toggle sidebar"
            edge="start"
            onClick={toggleSidebar}
            sx={{ mr: 2 }}
          >
            <MenuIcon />
          </IconButton>
          <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1 }}>
            PrivSense
          </Typography>
          
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <Tooltip title="Notifications">
              <IconButton color="inherit">
                <NotificationsIcon />
              </IconButton>
            </Tooltip>
            
            <Tooltip title="Account">
              <IconButton
                color="inherit"
                onClick={handleAccountMenuOpen}
                aria-controls="account-menu"
                aria-haspopup="true"
              >
                {user?.profileImage ? (
                  <Avatar alt={user.name} src={user.profileImage} sx={{ width: 32, height: 32 }} />
                ) : (
                  <AccountCircleIcon />
                )}
              </IconButton>
            </Tooltip>
          </Box>
          
          <Menu
            id="account-menu"
            anchorEl={accountMenu}
            open={Boolean(accountMenu)}
            onClose={handleAccountMenuClose}
            anchorOrigin={{
              vertical: 'bottom',
              horizontal: 'right',
            }}
            transformOrigin={{
              vertical: 'top',
              horizontal: 'right',
            }}
          >
            <MenuItem onClick={handleProfileClick}>Profile</MenuItem>
            <MenuItem onClick={handleLogout}>Logout</MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>
    );
  }
  ```

- [ ] Implement Sidebar component with navigation
  ```jsx
  // src/components/layout/Sidebar.jsx
  import { useLocation, useNavigate } from 'react-router-dom';
  import {
    Drawer,
    Toolbar,
    List,
    Divider,
    ListItem,
    ListItemButton,
    ListItemIcon,
    ListItemText,
    Box,
  } from '@mui/material';
  import DashboardIcon from '@mui/icons-material/Dashboard';
  import StorageIcon from '@mui/icons-material/Storage';
  import SearchIcon from '@mui/icons-material/Search';
  import DescriptionIcon from '@mui/icons-material/Description';
  import SettingsIcon from '@mui/icons-material/Settings';
  import PeopleIcon from '@mui/icons-material/People';
  import { useAuth } from '../../contexts/AuthContext';
  
  export default function Sidebar({ drawerWidth, open }) {
    const navigate = useNavigate();
    const location = useLocation();
    const { isAdmin } = useAuth();
    
    const isActive = (path) => {
      return location.pathname.startsWith(path);
    };
    
    const navItems = [
      { label: 'Dashboard', icon: <DashboardIcon />, path: '/dashboard' },
      { label: 'Database Connections', icon: <StorageIcon />, path: '/connections' },
      { label: 'PII Scans', icon: <SearchIcon />, path: '/scans' },
      { label: 'Templates', icon: <DescriptionIcon />, path: '/templates' },
    ];
    
    const adminItems = [
      { label: 'Users', icon: <PeopleIcon />, path: '/administration' },
      { label: 'Settings', icon: <SettingsIcon />, path: '/settings' },
    ];
    
    return (
      <Drawer
        variant="persistent"
        open={open}
        sx={{
          width: drawerWidth,
          flexShrink: 0,
          '& .MuiDrawer-paper': {
            width: drawerWidth,
            boxSizing: 'border-box',
          },
        }}
      >
        <Toolbar />
        <Box sx={{ overflow: 'auto' }}>
          <List>
            {navItems.map((item) => (
              <ListItem key={item.label} disablePadding>
                <ListItemButton
                  selected={isActive(item.path)}
                  onClick={() => navigate(item.path)}
                >
                  <ListItemIcon>{item.icon}</ListItemIcon>
                  <ListItemText primary={item.label} />
                </ListItemButton>
              </ListItem>
            ))}
          </List>
          
          {isAdmin && (
            <>
              <Divider />
              <List>
                {adminItems.map((item) => (
                  <ListItem key={item.label} disablePadding>
                    <ListItemButton
                      selected={isActive(item.path)}
                      onClick={() => navigate(item.path)}
                    >
                      <ListItemIcon>{item.icon}</ListItemIcon>
                      <ListItemText primary={item.label} />
                    </ListItemButton>
                  </ListItem>
                ))}
              </List>
            </>
          )}
        </Box>
      </Drawer>
    );
  }
  ```

- [ ] Add basic error boundary and loading states
  ```jsx
  // src/components/feedback/ErrorBoundary.jsx
  import { Component } from 'react';
  import { Box, Typography, Button } from '@mui/material';
  
  export default class ErrorBoundary extends Component {
    constructor(props) {
      super(props);
      this.state = { hasError: false, error: null, errorInfo: null };
    }
    
    static getDerivedStateFromError(error) {
      return { hasError: true };
    }
    
    componentDidCatch(error, errorInfo) {
      this.setState({
        error: error,
        errorInfo: errorInfo
      });
      console.error('Error caught by ErrorBoundary:', error, errorInfo);
    }
    
    render() {
      if (this.state.hasError) {
        return (
          <Box
            sx={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              height: '100%',
              p: 3,
            }}
          >
            <Typography variant="h5" gutterBottom>
              Something went wrong
            </Typography>
            <Typography variant="body1" color="text.secondary" sx={{ mb: 2 }}>
              We've encountered an unexpected error. Please try again.
            </Typography>
            <Button
              variant="contained"
              onClick={() => {
                this.setState({ hasError: false });
                window.location.href = '/dashboard';
              }}
            >
              Go to Dashboard
            </Button>
          </Box>
        );
      }
      
      return this.props.children;
    }
  }
  
  // src/components/feedback/LoadingScreen.jsx
  import { Box, CircularProgress, Typography } from '@mui/material';
  
  export default function LoadingScreen({ message = 'Loading...' }) {
    return (
      <Box
        sx={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          height: '100vh',
        }}
      >
        <CircularProgress size={60} />
        <Typography variant="h6" sx={{ mt: 2 }}>
          {message}
        </Typography>
      </Box>
    );
  }
  ```

## Phase 2: Authentication and Dashboard (Week 2)

### Step 6: Authentication Pages (Day 1-2)
- [ ] Create Login page with form validation
  ```jsx
  // src/pages/auth/LoginPage.jsx
  import { useState } from 'react';
  import { useNavigate, useLocation } from 'react-router-dom';
  import { useForm } from 'react-hook-form';
  import { zodResolver } from '@hookform/resolvers/zod';
  import { z } from 'zod';
  import { 
    Container, 
    Box, 
    Typography, 
    TextField, 
    Button, 
    Alert, 
    Paper
  } from '@mui/material';
  import { useAuth } from '../../contexts/AuthContext';
  
  // Form validation schema
  const loginSchema = z.object({
    username: z.string().min(1, 'Username is required'),
    password: z.string().min(1, 'Password is required'),
  });
  
  export default function LoginPage() {
    const navigate = useNavigate();
    const location = useLocation();
    const { login } = useAuth();
    const [error, setError] = useState('');
    
    const { register, handleSubmit, formState: { errors } } = useForm({
      resolver: zodResolver(loginSchema)
    });
    
    const from = location.state?.from?.pathname || '/dashboard';
    
    const onSubmit = async (data) => {
      try {
        await login(data.username, data.password);
        navigate(from, { replace: true });
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to login');
      }
    };
    
    return (
      <Container component="main" maxWidth="xs">
        <Box
          sx={{
            marginTop: 8,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
          }}
        >
          <Paper elevation={3} sx={{ p: 4, width: '100%' }}>
            <Typography component="h1" variant="h5" align="center" gutterBottom>
              PrivSense Login
            </Typography>
            
            {error && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {error}
              </Alert>
            )}
            
            <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate sx={{ mt: 1 }}>
              <TextField
                margin="normal"
                required
                fullWidth
                id="username"
                label="Username"
                name="username"
                autoComplete="username"
                autoFocus
                {...register('username')}
                error={!!errors.username}
                helperText={errors.username?.message}
              />
              <TextField
                margin="normal"
                required
                fullWidth
                name="password"
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
              >
                Sign In
              </Button>
            </Box>
          </Paper>
        </Box>
      </Container>
    );
  }
  ```

- [ ] Create user profile page
  ```jsx
  // src/pages/auth/ProfilePage.jsx
  import { useState } from 'react';
  import { 
    Container, 
    Paper, 
    Typography, 
    Box, 
    Avatar, 
    Grid, 
    TextField, 
    Button, 
    Divider, 
    Alert 
  } from '@mui/material';
  import { useAuth } from '../../contexts/AuthContext';
  
  export default function ProfilePage() {
    const { user } = useAuth();
    const [message, setMessage] = useState(null);
    
    const handleSubmit = (event) => {
      event.preventDefault();
      setMessage({ type: 'success', text: 'Profile updated successfully!' });
      // Here you would implement the profile update logic
    };
    
    if (!user) {
      return null;
    }
    
    return (
      <Container maxWidth="md">
        <Paper elevation={3} sx={{ p: 4, mt: 4 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 4 }}>
            <Avatar 
              sx={{ width: 80, height: 80, mr: 3 }}
              alt={user.firstName}
              src={user.profileImage || ''}
            />
            <Box>
              <Typography variant="h5">
                {user.firstName} {user.lastName}
              </Typography>
              <Typography color="textSecondary">
                {user.roles.join(', ')}
              </Typography>
            </Box>
          </Box>
          
          {message && (
            <Alert 
              severity={message.type} 
              sx={{ mb: 3 }}
              onClose={() => setMessage(null)}
            >
              {message.text}
            </Alert>
          )}
          
          <Box component="form" onSubmit={handleSubmit}>
            <Grid container spacing={3}>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="First Name"
                  name="firstName"
                  defaultValue={user.firstName}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Last Name"
                  name="lastName"
                  defaultValue={user.lastName}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Email"
                  name="email"
                  defaultValue={user.email}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Username"
                  name="username"
                  defaultValue={user.username}
                  disabled
                />
              </Grid>
              <Grid item xs={12}>
                <Divider sx={{ my: 2 }} />
                <Typography variant="h6" sx={{ mb: 2 }}>
                  Change Password
                </Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="New Password"
                  name="newPassword"
                  type="password"
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Confirm New Password"
                  name="confirmPassword"
                  type="password"
                />
              </Grid>
              <Grid item xs={12}>
                <Button type="submit" variant="contained" sx={{ mt: 2 }}>
                  Save Changes
                </Button>
              </Grid>
            </Grid>
          </Box>
        </Paper>
      </Container>
    );
  }
  ```

### Step 7: Dashboard Framework (Day 3-5)
- [ ] Create dashboard layout
- [ ] Implement summary widgets
- [ ] Create placeholder charts and data visualizations
- [ ] Implement dashboard API services
- [ ] Add React Query hooks for dashboard data

## Phase 3: Database Connections Module (Week 3)

### Step 8: Connection List View (Day 1-2)
- [ ] Create connection list table component
- [ ] Implement filtering and pagination
- [ ] Create connection service for API integration
- [ ] Add React Query hooks for connections data

### Step 9: Connection Detail View (Day 3-4)
- [ ] Create connection detail page
- [ ] Implement metadata exploration UI
- [ ] Display schema, tables, and columns
- [ ] Add connection status indicators

### Step 10: Connection Management (Day 5)
- [ ] Create new connection form with validation
- [ ] Implement connection testing functionality
- [ ] Add connection deletion with confirmation
- [ ] Implement connection editing capabilities

## Phase 4: PII Scanning Module (Week 4)

### Step 11: Scan Configuration (Day 1-2)
- [ ] Create scan configuration form
- [ ] Implement table/column selection interface
- [ ] Add sampling configuration options
- [ ] Create scan service for API integration

### Step 12: Scan Execution and Monitoring (Day 3)
- [ ] Implement scan execution workflow
- [ ] Create real-time progress indicators
- [ ] Set up WebSocket integration for live updates
- [ ] Add scan cancellation functionality

### Step 13: Scan Results View (Day 4-5)
- [ ] Create scan results summary view
- [ ] Implement detailed findings exploration
- [ ] Add data visualization for scan results
- [ ] Implement report generation and download

## Phase 5: Templates and Configuration (Week 5)

### Step 14: Scan Templates (Day 1-2)
- [ ] Create template management interface
- [ ] Implement template creation form
- [ ] Add template execution functionality
- [ ] Create template service for API integration

### Step 15: System Configuration (Day 3-4)
- [ ] Implement configuration management interface
- [ ] Create detection rule editor
- [ ] Add system settings controls
- [ ] Implement configuration service for API integration

### Step 16: Admin Functions (Day 5)
- [ ] Create user management interface
- [ ] Implement role management
- [ ] Add system health monitoring
- [ ] Create admin services for API integration

## Phase 6: Data Sampling and Advanced Features (Week 6)

### Step 17: Sampling Interface (Day 1-2)
- [ ] Create sampling test interface
- [ ] Implement column selection for sampling
- [ ] Add sampling results visualization
- [ ] Create sampling service for API integration

### Step 18: Batch Operations (Day 3)
- [ ] Implement batch sampling functionality
- [ ] Create batch scan capabilities
- [ ] Add multi-selection interfaces
- [ ] Implement batch operation services

### Step 19: Reporting Features (Day 4-5)
- [ ] Create comprehensive reporting interface
- [ ] Implement report customization options
- [ ] Add export functionality (PDF, CSV, etc.)
- [ ] Create reporting services for API integration

## Phase 7: Integration and Optimization (Week 7)

### Step 20: Integration Testing (Day 1-2)
- [ ] Test all API integrations
- [ ] Verify WebSocket functionality
- [ ] Test authentication flows
- [ ] Validate form submissions and error handling

### Step 21: Performance Optimization (Day 3-4)
- [ ] Implement code splitting and lazy loading using React.lazy and Suspense
  ```jsx
  // src/router/index.jsx - Example of lazy loading
  import { lazy, Suspense } from 'react';
  import { createBrowserRouter } from 'react-router-dom';
  import LoadingScreen from '../components/feedback/LoadingScreen';
  
  // Lazy-loaded components
  const DashboardPage = lazy(() => import('../pages/dashboard/DashboardPage'));
  const ConnectionsPage = lazy(() => import('../pages/connections/ConnectionsPage'));
  
  // Component to handle lazy loading with fallback
  const LazyLoad = ({ children }) => (
    <Suspense fallback={<LoadingScreen />}>
      {children}
    </Suspense>
  );
  
  const router = createBrowserRouter([
    {
      path: '/dashboard',
      element: <LazyLoad><DashboardPage /></LazyLoad>
    },
    {
      path: '/connections',
      element: <LazyLoad><ConnectionsPage /></LazyLoad>
    },
    // More routes...
  ]);
  ```
- [ ] Optimize bundle size
- [ ] Add caching strategies
- [ ] Optimize React renders using React.memo, useMemo, and useCallback
  ```jsx
  // Example of optimizing a component with React.memo
  import { memo, useMemo, useCallback } from 'react';
  
  const ExpensiveComponent = memo(function ExpensiveComponent({ data, onItemClick }) {
    // This calculation will only run when data changes
    const processedData = useMemo(() => {
      return data.map(item => ({
        ...item,
        processed: expensiveOperation(item)
      }));
    }, [data]);
    
    // This callback will only be recreated when itemId changes
    const handleClick = useCallback((item) => {
      onItemClick(item.id);
    }, [onItemClick]);
    
    return (
      <div>
        {processedData.map(item => (
          <div key={item.id} onClick={() => handleClick(item)}>
            {item.processed}
          </div>
        ))}
      </div>
    );
  });
  ```

### Step 22: Progressive Loading (Day 5)
- [ ] Implement skeleton screens
- [ ] Add virtualized lists for large datasets using react-window or react-virtualized
  ```jsx
  // Example of virtualized list with react-window
  import { FixedSizeList } from 'react-window';
  
  function VirtualizedList({ data }) {
    const Row = ({ index, style }) => (
      <div style={style}>
        {data[index].name}
      </div>
    );
    
    return (
      <FixedSizeList
        height={400}
        width="100%"
        itemCount={data.length}
        itemSize={35}
      >
        {Row}
      </FixedSizeList>
    );
  }
  ```
- [ ] Implement infinite scrolling where appropriate
- [ ] Optimize initial load time

## Phase 8: Final Touches and Deployment (Week 8)

### Step 23: UI Polish (Day 1-2)
- [ ] Implement consistent styling
- [ ] Add transitions and animations
- [ ] Ensure responsive design on all views
- [ ] Implement dark mode (if required)

### Step 24: Accessibility (Day 3)
- [ ] Perform accessibility audit
- [ ] Add ARIA attributes
- [ ] Ensure keyboard navigation
- [ ] Fix any accessibility issues

### Step 25: Documentation (Day 4)
- [ ] Create component documentation using JSDoc comments
  ```jsx
  /**
   * DataTable component displays data in a tabular format with sorting,
   * filtering, and pagination capabilities.
   *
   * @param {object[]} data - Array of data objects to display in the table
   * @param {object[]} columns - Array of column configuration objects
   * @param {boolean} [isLoading=false] - Whether the data is currently loading
   * @param {function} [onRowClick] - Callback function when a row is clicked
   * @param {object} [pagination] - Pagination configuration object
   * @returns {JSX.Element} The rendered DataTable component
   *
   * @example
   * <DataTable
   *   data={users}
   *   columns={userColumns}
   *   isLoading={isLoading}
   *   pagination={{
   *     page: 0,
   *     pageSize: 10,
   *     totalItems: 100,
   *     onPageChange: handlePageChange
   *   }}
   * />
   */
  function DataTable({ data, columns, isLoading, onRowClick, pagination }) {
    // Implementation...
  }
  ```
- [ ] Document API integrations
- [ ] Add inline code comments
- [ ] Create user guide (if required)

### Step 26: Deployment (Day 5)
- [ ] Set up build pipeline
  ```bash
  # Build command
  npm run build
  ```
- [ ] Configure Nginx for SPA routing
  ```
  # nginx.conf
  server {
      listen 80;
      server_name _;
      root /usr/share/nginx/html;
      index index.html;
      
      # Enable gzip compression
      gzip on;
      gzip_types text/plain text/css application/json application/javascript text/xml application/xml text/javascript;
      
      # Serve static assets with cache headers
      location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
          expires 30d;
          add_header Cache-Control "public, max-age=2592000, immutable";
      }
      
      # For SPA routing - redirect all requests to index.html
      location / {
          try_files $uri $uri/ /index.html;
          add_header Cache-Control "no-cache, no-store, must-revalidate";
      }
  }
  ```
- [ ] Set up container for deployment
  ```Dockerfile
  # Dockerfile
  FROM node:18-alpine as build
  WORKDIR /app
  COPY package*.json ./
  RUN npm ci
  COPY . .
  RUN npm run build
  
  FROM nginx:alpine
  COPY --from=build /app/dist /usr/share/nginx/html
  COPY nginx.conf /etc/nginx/conf.d/default.conf
  EXPOSE 80
  CMD ["nginx", "-g", "daemon off;"]
  ```
- [ ] Deploy to production environment

## Testing Strategy Throughout Development

### Unit Testing with Jest and React Testing Library
```jsx
// Example test for Login component
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from '../../contexts/AuthContext';
import LoginPage from './LoginPage';

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
  useLocation: () => ({ state: { from: { pathname: '/dashboard' } } }),
}));

describe('LoginPage', () => {
  test('renders login form', () => {
    render(
      <BrowserRouter>
        <AuthProvider>
          <LoginPage />
        </AuthProvider>
      </BrowserRouter>
    );
    
    expect(screen.getByLabelText(/username/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
  });
  
  test('shows error messages for invalid inputs', async () => {
    render(
      <BrowserRouter>
        <AuthProvider>
          <LoginPage />
        </AuthProvider>
      </BrowserRouter>
    );
    
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));
    
    expect(await screen.findByText(/username is required/i)).toBeInTheDocument();
    expect(await screen.findByText(/password is required/i)).toBeInTheDocument();
  });
});
```

### Integration Testing
- Test component interactions
- Validate API integrations with mocks
- Test authentication flows

### End-to-End Testing with Cypress
```javascript
// Example Cypress test for login flow
describe('Login Flow', () => {
  it('should login successfully with valid credentials', () => {
    cy.visit('/login');
    cy.get('input[name="username"]').type('testuser');
    cy.get('input[name="password"]').type('Password123!');
    cy.get('button[type="submit"]').click();
    cy.url().should('include', '/dashboard');
    cy.get('h1').should('contain', 'Dashboard');
  });
  
  it('should show error with invalid credentials', () => {
    cy.visit('/login');
    cy.get('input[name="username"]').type('testuser');
    cy.get('input[name="password"]').type('wrongpassword');
    cy.get('button[type="submit"]').click();
    cy.get('.MuiAlert-root').should('contain', 'Invalid credentials');
    cy.url().should('include', '/login');
  });
});
```

## Development Best Practices

### Code Quality
- Follow the established naming conventions
- Use propTypes for component props validation
```jsx
import PropTypes from 'prop-types';

function DataTable({ data, columns, isLoading, onRowClick, pagination }) {
  // Implementation...
}

DataTable.propTypes = {
  data: PropTypes.array.isRequired,
  columns: PropTypes.array.isRequired,
  isLoading: PropTypes.bool,
  onRowClick: PropTypes.func,
  pagination: PropTypes.shape({
    page: PropTypes.number.isRequired,
    pageSize: PropTypes.number,
    totalItems: PropTypes.number,
    onPageChange: PropTypes.func.isRequired,
  }),
};

DataTable.defaultProps = {
  isLoading: false,
  pagination: null,
};
```
- Document components and functions
- Follow ESLint and Prettier rules

### State Management
- Use React Query for server state
- Use React Context for global UI state
- Keep local state close to components
- Implement optimistic updates for better UX

### Performance
- Memoize expensive calculations with useMemo
- Optimize callbacks with useCallback
- Prevent unnecessary re-renders with React.memo
- Implement proper loading states and error handling
- Monitor bundle size and runtime performance

