# PrivSense React Frontend Development Plan

## Project Planning Phase

### 1. API Documentation Analysis

After reviewing the OpenAPI specification for the PrivSense API, here's a comprehensive breakdown of the API structure:

#### API Structure Overview
- **Authentication & Authorization**: JWT-based authentication with role-based access (ADMIN and API_USER roles)
- **Core Domain Areas**: 11 primary functional domains covering the entire system
- **Data Models**: 35+ data transfer objects handling various aspects of the system
- **API Endpoints**: ~50 endpoints organized by functional domain

#### Key Endpoints by Domain

##### Authentication
- Login endpoint (POST /api/v1/auth/login)
- Registration endpoint (POST /api/v1/auth/register)
- Current user endpoint (GET /api/v1/auth/me)

##### Database Connections
- List/create/view/delete database connections
- Fetch database metadata (schema information)

##### PII Scans
- Start new scans on databases
- View scan status and progress
- Access scan results, statistics, and reports
- Cancel ongoing scans

##### Scan Templates
- Create/update/delete reusable scan configurations
- Execute scans from templates

##### Data Sampling
- Test sampling configurations on database columns
- Batch sampling across multiple tables

##### System Information
- Health checks and system status
- System configuration information

##### Dashboard
- Overview statistics
- Recent scans
- Trend analysis
- Top PII findings

##### Configuration Management
- View/update application configuration
- Manage detection rules

#### Potential Challenges
- **Real-time Scan Updates**: Scan status updates are provided via both polling endpoints and WebSockets, requiring a hybrid approach
- **Complex Data Visualization**: Dashboard and scan results involve complex data visualization requirements
- **Hierarchical Data**: Database metadata includes nested structures (tables, columns, relationships)
- **Authentication Management**: Token handling, expiration, and refresh mechanisms
- **Form Complexity**: Several endpoints require complex nested form data structures

### 2. User Experience Planning

#### User Personas

##### Database Administrator
- Primary goals: Connect databases, manage database connections
- Technical expertise: High
- Usage frequency: Occasional to regular

##### Data Privacy Officer
- Primary goals: Configure and run PII scans, review reports
- Technical expertise: Medium
- Usage frequency: Regular

##### Compliance Manager
- Primary goals: View reports, understand compliance status
- Technical expertise: Low to medium
- Usage frequency: Regular to frequent

##### System Administrator
- Primary goals: Configure system, manage users
- Technical expertise: High
- Usage frequency: Occasional

#### Core User Flows

##### Database Connection Management Flow
- Landing on connections page
- Viewing existing connections
- Creating new connection (admin only)
- Viewing connection details
- Exploring database schema
- Closing/deleting connections

##### PII Scanning Flow
- Configuring a new scan
- Starting a scan
- Monitoring scan progress in real-time
- Viewing scan results
- Exploring PII findings by table/column
- Generating and downloading reports

##### Dashboard & Reporting Flow
- Landing on dashboard
- Viewing summary statistics
- Exploring trends
- Drilling down into specific findings
- Accessing detailed reports

##### Administration Flow
- Managing users
- Configuring system settings
- Monitoring system health and performance

#### Information Architecture

##### Main Navigation Areas
- Dashboard
- Database Connections
- PII Scans
- Reports
- Templates
- Administration (for admin users)
- Settings

##### Secondary Navigation
- User profile
- Notifications
- Help/documentation
- System health status

#### UX Enhancement Opportunities

##### Progressive Loading
- Implement skeleton screens for data-heavy pages
- Use progressive loading for large result sets
- Implement virtualized lists for long data tables

##### Real-time Updates
- WebSocket integration for scan progress
- Live updates for dashboard metrics

##### Contextual Help
- In-app guidance for complex workflows
- Tooltips for technical terms and metrics

##### Error & Success Feedback
- Consistent toast notifications
- Inline validation for forms
- Contextual error messages with recovery options
- Success confirmation with next steps

### 3. Architecture Design

#### Folder Structure
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
│   ├── AuthContext.tsx    # Authentication context
│   ├── UIContext.tsx      # UI state context
│   └── WebSocketContext.tsx # WebSocket connection management
├── features/              # Feature-specific code
│   ├── auth/              # Authentication feature
│   ├── connections/       # Database connections feature
│   │   ├── components/    # Feature-specific components
│   │   ├── hooks/         # Feature-specific hooks
│   │   ├── types/         # Feature-specific types
│   │   ├── utils/         # Feature-specific utilities
│   │   └── index.ts       # Feature exports
│   ├── scans/             # PII scans feature
│   ├── dashboard/         # Dashboard feature
│   ├── templates/         # Scan templates feature
│   ├── administration/    # Administration feature
│   └── settings/          # Settings feature
├── hooks/                 # Custom React hooks
│   ├── api/               # API-related hooks
│   ├── auth/              # Authentication-related hooks
│   ├── ui/                # UI-related hooks
│   └── utils/             # Utility hooks
├── layouts/               # Page layouts
├── lib/                   # External library wrappers and configurations
│   ├── api.ts             # API client configuration
│   ├── axios-config.ts    # Axios configuration
│   └── websocket.ts       # WebSocket client
├── pages/                 # Page components
│   ├── auth/              # Authentication pages
│   ├── connections/       # Connection pages
│   ├── scans/             # Scan pages
│   ├── dashboard/         # Dashboard page
│   ├── templates/         # Template pages
│   ├── administration/    # Administration pages
│   └── settings/          # Settings pages
├── router/                # Routing configuration
│   ├── routes.tsx         # Route definitions
│   ├── PrivateRoute.tsx   # Protected route component
│   └── index.ts           # Router exports
├── services/              # API service layer
│   ├── auth.service.ts    # Authentication API calls
│   ├── connections.service.ts # Connections API calls
│   └── ... (other service files)
├── store/                 # Global state management
│   ├── slices/            # State slices (if using Redux Toolkit)
│   ├── hooks.ts           # Custom store hooks
│   └── index.ts           # Store configuration
├── styles/                # Global styles and theming
│   ├── theme.ts           # Theme configuration
│   ├── global.css         # Global styles
│   └── mixins.ts          # Style mixins and utilities
├── types/                 # TypeScript type definitions
│   ├── api/               # API response and request types
│   ├── common.ts          # Common type definitions
│   └── models.ts          # Data model types
├── utils/                 # Utility functions
│   ├── api.ts             # API utilities
│   ├── date.ts            # Date formatting utilities
│   ├── format.ts          # Data formatting utilities
│   ├── validation.ts      # Validation utilities
│   └── storage.ts         # Local storage utilities
├── App.tsx                # Root component
├── index.tsx              # Application entry point
└── vite-env.d.ts          # Vite environment types
```

#### State Management Approach

##### Global State Management
- Authentication State: React Context + local storage for persistence
- UI State: React Context for theme, notifications, and global UI elements
- Feature Flags: React Context for feature availability based on user roles

##### Server State Management
- React Query: For all API data fetching, caching, and synchronization
- Custom Hooks: Wrapping React Query to provide domain-specific APIs

##### Local State Management
- React useState/useReducer: For component-specific state
- Form State: React Hook Form for form state management

##### Real-time State Management
- WebSocket Context: For managing WebSocket connections and events
- Subscription Patterns: For components to subscribe to relevant events

#### API Integration Layer

##### Base API Client
```typescript
// lib/api.ts
import axios, { AxiosInstance, AxiosRequestConfig, AxiosError } from 'axios';
import { refreshToken, logout } from '../services/auth.service';

const baseURL = import.meta.env.VITE_API_BASE_URL || '/privsense';

const api: AxiosInstance = axios.create({
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
  async (error: AxiosError) => {
    const originalRequest = error.config as AxiosRequestConfig & { _retry?: boolean };
    
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

### 4. Authentication Implementation

#### Login Page
```tsx
// src/pages/auth/LoginPage.tsx
import { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { 
  Container, 
  Alert
} from '@mui/material';
import { useAuth } from '../../contexts/AuthContext';

// Form validation schema
const loginSchema = z.object({
  username: z.string().min(1, 'Username is required'),
  password: z.string().min(1, 'Password is required'),
});

type LoginFormData = z.infer<typeof loginSchema>;

const LoginPage = () => {
  const navigate = useNavigate();
  // Implementation details
};

export default LoginPage;
```

### 5. Core Layout Implementation

```tsx
// src/layouts/AppLayout.tsx
import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { Box, CssBaseline, Toolbar } from '@mui/material';
import { Header } from '../components/layout/Header';
import { Sidebar } from '../components/layout/Sidebar';
import { ErrorBoundary } from '../components/feedback/ErrorBoundary';

const drawerWidth = 240;

export const AppLayout = () => {
  const [sidebarOpen, setSidebarOpen] = useState(true);
  // Implementation details
};

// src/components/layout/Header.tsx
import { useNavigate } from 'react-router-dom';
import {
  AppBar,
  Tooltip,
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import AccountCircleIcon from '@mui/icons-material/AccountCircle';
import NotificationsIcon from '@mui/icons-material/Notifications';
import { useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';

interface HeaderProps {
  drawerWidth: number;
  toggleSidebar: () => void;
}

export const Header = ({ drawerWidth, sidebarOpen, toggleSidebar }: HeaderProps) => {
  const navigate = useNavigate();
  // Implementation details
};

// src/components/layout/Sidebar.tsx
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

interface SidebarProps {
  drawerWidth: number;
  open: boolean;
}

export const Sidebar = ({ drawerWidth, open }: SidebarProps) => {
  const navigate = useNavigate();
  const location = useLocation();
  const { isAdmin } = useAuth();
  
  const isActive = (path: string) => {
    return location.pathname.startsWith(path);
  };
  
  const navItems = [
    { label: 'Dashboard', icon: <DashboardIcon />, path: '/dashboard' },
    { label: 'Templates', icon: <DescriptionIcon />, path: '/templates' },
  ];
  
  const adminItems = [
    { label: 'Users', icon: <PeopleIcon />, path: '/administration' },
    { label: 'Settings', icon: <SettingsIcon />, path: '/settings' },
  ];
  
  // Implementation details
};
```

### 6. Reusability and Maintainability Strategy

#### Component Library Structure
```
src/components/
├── common/              # Basic UI building blocks
│   ├── Button/          # Enhanced button components
│   │   ├── Button.tsx
│   │   ├── IconButton.tsx
│   │   └── index.ts
│   ├── Card/            # Card components
│   ├── Form/            # Form components
│   └── ... other common components
├── feedback/            # Feedback components
│   ├── ErrorBoundary.tsx
│   ├── ErrorMessage.tsx
│   ├── LoadingIndicator.tsx
│   └── ... other feedback components
├── layout/              # Layout components
│   ├── Header.tsx
│   ├── Sidebar.tsx
│   ├── Footer.tsx
│   └── ... other layout components
├── data/                # Data display components
│   ├── DataTable.tsx
│   ├── Chart.tsx
│   └── ... other data components
└── domain/              # Domain-specific components
    ├── connections/
    ├── scans/
    └── ... other domain components
```

#### TypeScript Interface Structure
```typescript
// src/types/models.ts - Core data models
export interface User {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
  enabled: boolean;
  locked: boolean;
  createdAt: string;
  updatedAt: string;
  lastLogin: string;
}

export interface DatabaseConnection {
  connectionId: string;
  host: string;
  port: number;
  databaseName: string;
  username: string;
  databaseProductName: string;
  databaseProductVersion: string;
  sslEnabled: boolean;
  status: ConnectionStatus;
}

export type ConnectionStatus = 'AVAILABLE' | 'UNAVAILABLE' | 'ERROR';

// More model interfaces...

// src/types/api/requests.ts - API request types
export interface LoginRequest {
  username: string;
  password: string;
}

export interface DatabaseConnectionRequest {
  host: string;
  port: number;
  databaseName: string;
  username: string;
  password: string;
  driverClassName: string;
  sslEnabled?: boolean;
  sslTrustStorePath?: string;
  sslTrustStorePassword?: string;
}

// More request types...

// src/types/api/responses.ts - API response types
export interface ApiResponse<T> {
  data?: T;
  success: boolean;
  message?: string;
  meta?: Record<string, any>;
  links?: Link[];
}

export interface Link {
  href: string;
  rel?: string;
  type?: string;
}

export interface AuthResponse {
  token: string;
  message?: string;
}

// More response types...

// src/types/props.ts - Component prop types
export interface DataTableProps<T> {
  data: T[];
  columns: TableColumn<T>[];
  isLoading?: boolean;
  onRowClick?: (row: T) => void;
  pagination?: {
    page: number;
    onPageChange: (page: number) => void;
  };
}

export interface TableColumn<T> {
  id: string;
  header: string;
  accessor: (row: T) => any;
  Cell?: (value: any, row: T) => React.ReactNode;
  sortable?: boolean;
  width?: string;
}

// More prop types...
```

### 7. Coding Standards and Patterns

#### Naming Conventions
- Use PascalCase for components, interfaces, and types
- Use camelCase for variables, functions, and properties
- Use ALL_CAPS for constants
- Prefix interface names with "I" (optional but helpful)
- Suffix type names with "Type" for clarity

#### File Organization
- One component per file (except for very small related components)
- Export named components as default from their files
- Group related files in feature folders
- Use index.ts files to re-export components for cleaner imports

#### Component Patterns
- Use functional components with hooks
- Extract complex logic to custom hooks
- Keep components focused on a single responsibility
- Use composition over inheritance
- Avoid prop drilling by using context where appropriate

#### State Management Patterns
- Use local state for UI-specific state
- Use context for shared state that doesn't change often
- Use React Query for server state
- Keep state as close as possible to where it's used

### 8. Documentation Practices

#### Component Documentation
- Document component purpose
- Document component props with TypeScript interfaces
- Include usage examples
- Document state and side effects
- Document accessibility considerations

Example:
```tsx
/**
 * DataTable component displays data in a tabular format with sorting,
 * filtering, and pagination capabilities.
 *
 * @example
 * ```tsx
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
 * ```
 */
export const DataTable = <T extends Record<string, any>>({
  data,
  columns,
  isLoading,
  onRowClick,
  pagination
}: DataTableProps<T>) => {
  // Implementation...
}
```

#### Hook Documentation
- Document hook purpose
- Document parameters and return values
- Include usage examples
- Document side effects and dependencies

Example:
```tsx
/**
 * Hook for managing connections data and operations.
 *
 * @param page - Current page number (zero-based)
 * @param pageSize - Number of items per page
 * @returns Object containing connections data and operations
 *
 * @example
 * ```tsx
 * const { connections, isLoading, error, createConnection } = useConnections(0, 10);
 * ```
 */
export const useConnections = (page = 0, pageSize = 10) => {
  // Implementation...
}
```

### 9. Code Review Checklist

#### Functionality
- Does the code meet the requirements?
- Are all edge cases handled?
- Is the UI responsive and accessible?

#### Code Quality
- Is the code clean and readable?
- Are there any unnecessary complexities?
- Is the code DRY (Don't Repeat Yourself)?
- Are functions and components focused on single responsibilities?

#### Performance
- Are there any unnecessary re-renders?
- Is memoization used appropriately?
- Are expensive operations optimized?

#### Type Safety
- Are types defined properly?
- Are there any any types that could be more specific?
- Are nullability and undefined handled correctly?

#### State Management
- Is state kept at the appropriate level?
- Is context used appropriately?
- Are API calls and data fetching optimized?

#### Security
- Is user input sanitized?
- Are authentication and authorization checks in place?
- Are API keys and secrets properly handled?

