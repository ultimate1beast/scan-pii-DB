PrivSense React Frontend Development Plan
Project Planning Phase
1. API Documentation Analysis
After reviewing the OpenAPI specification for the PrivSense API, here's a comprehensive breakdown of the API structure:

API Structure Overview
Authentication & Authorization: JWT-based authentication with role-based access (ADMIN and API_USER roles)
Core Domain Areas: 11 primary functional domains covering the entire system
Data Models: 35+ data transfer objects handling various aspects of the system
API Endpoints: ~50 endpoints organized by functional domain
Key Endpoints by Domain
Authentication

Login endpoint (POST /api/v1/auth/login)
Registration endpoint (POST /api/v1/auth/register)
Current user endpoint (GET /api/v1/auth/me)
Database Connections

List/create/view/delete database connections
Fetch database metadata (schema information)
PII Scans

Start new scans on databases
View scan status and progress
Access scan results, statistics, and reports
Cancel ongoing scans
Scan Templates

Create/update/delete reusable scan configurations
Execute scans from templates
Data Sampling

Test sampling configurations on database columns
Batch sampling across multiple tables
System Information

Health checks and system status
System configuration information
Dashboard

Overview statistics
Recent scans
Trend analysis
Top PII findings
Configuration Management

View/update application configuration
Manage detection rules
Potential Challenges
Real-time Scan Updates: Scan status updates are provided via both polling endpoints and WebSockets, requiring a hybrid approach
Complex Data Visualization: Dashboard and scan results involve complex data visualization requirements
Hierarchical Data: Database metadata includes nested structures (tables, columns, relationships)
Authentication Management: Token handling, expiration, and refresh mechanisms
Form Complexity: Several endpoints require complex nested form data structures
2. User Experience Planning
User Personas
Database Administrator

Primary goals: Connect databases, manage database connections
Technical expertise: High
Usage frequency: Occasional to regular
Data Privacy Officer

Primary goals: Configure and run PII scans, review reports
Technical expertise: Medium
Usage frequency: Regular
Compliance Manager

Primary goals: View reports, understand compliance status
Technical expertise: Low to medium
Usage frequency: Regular to frequent
System Administrator

Primary goals: Configure system, manage users
Technical expertise: High
Usage frequency: Occasional
Core User Flows
Database Connection Management Flow

Landing on connections page
Viewing existing connections
Creating new connection (admin only)
Viewing connection details
Exploring database schema
Closing/deleting connections
PII Scanning Flow

Configuring a new scan
Starting a scan
Monitoring scan progress in real-time
Viewing scan results
Exploring PII findings by table/column
Generating and downloading reports
Dashboard & Reporting Flow

Landing on dashboard
Viewing summary statistics
Exploring trends
Drilling down into specific findings
Accessing detailed reports
Administration Flow

Managing users
Configuring system settings
Monitoring system health and performance
Information Architecture
Main Navigation Areas

Dashboard
Database Connections
PII Scans
Reports
Templates
Administration (for admin users)
Settings
Secondary Navigation

User profile
Notifications
Help/documentation
System health status
UX Enhancement Opportunities
Progressive Loading

Implement skeleton screens for data-heavy pages
Use progressive loading for large result sets
Implement virtualized lists for long data tables
Real-time Updates

WebSocket integration for scan progress
Live updates for dashboard metrics
Contextual Help

In-app guidance for complex workflows
Tooltips for technical terms and metrics
Error & Success Feedback

Consistent toast notifications
Inline validation for forms
Contextual error messages with recovery options
Success confirmation with next steps
3. Architecture Design
Folder Structure
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



State Management Approach
Global State Management

Authentication State: React Context + local storage for persistence
UI State: React Context for theme, notifications, and global UI elements
Feature Flags: React Context for feature availability based on user roles
Server State Management

React Query: For all API data fetching, caching, and synchronization
Custom Hooks: Wrapping React Query to provide domain-specific APIs
Local State Management

React useState/useReducer: For component-specific state
Form State: React Hook Form for form state management
Real-time State Management

WebSocket Context: For managing WebSocket connections and events
Subscription Patterns: For components to subscribe to relevant events
API Integration Layer
Base API Client

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


GitHub Copilot
PrivSense React Frontend Development Plan
Project Planning Phase
1. API Documentation Analysis
After reviewing the OpenAPI specification for the PrivSense API, here's a comprehensive breakdown of the API structure:

API Structure Overview
Authentication & Authorization: JWT-based authentication with role-based access (ADMIN and API_USER roles)
Core Domain Areas: 11 primary functional domains covering the entire system
Data Models: 35+ data transfer objects handling various aspects of the system
API Endpoints: ~50 endpoints organized by functional domain
Key Endpoints by Domain
Authentication

Login endpoint (POST /api/v1/auth/login)
Registration endpoint (POST /api/v1/auth/register)
Current user endpoint (GET /api/v1/auth/me)
Database Connections

List/create/view/delete database connections
Fetch database metadata (schema information)
PII Scans

Start new scans on databases
View scan status and progress
Access scan results, statistics, and reports
Cancel ongoing scans
Scan Templates

Create/update/delete reusable scan configurations
Execute scans from templates
Data Sampling

Test sampling configurations on database columns
Batch sampling across multiple tables
System Information

Health checks and system status
System configuration information
Dashboard

Overview statistics
Recent scans
Trend analysis
Top PII findings
Configuration Management

View/update application configuration
Manage detection rules
Potential Challenges
Real-time Scan Updates: Scan status updates are provided via both polling endpoints and WebSockets, requiring a hybrid approach
Complex Data Visualization: Dashboard and scan results involve complex data visualization requirements
Hierarchical Data: Database metadata includes nested structures (tables, columns, relationships)
Authentication Management: Token handling, expiration, and refresh mechanisms
Form Complexity: Several endpoints require complex nested form data structures
2. User Experience Planning
User Personas
Database Administrator

Primary goals: Connect databases, manage database connections
Technical expertise: High
Usage frequency: Occasional to regular
Data Privacy Officer

Primary goals: Configure and run PII scans, review reports
Technical expertise: Medium
Usage frequency: Regular
Compliance Manager

Primary goals: View reports, understand compliance status
Technical expertise: Low to medium
Usage frequency: Regular to frequent
System Administrator

Primary goals: Configure system, manage users
Technical expertise: High
Usage frequency: Occasional
Core User Flows
Database Connection Management Flow

Landing on connections page
Viewing existing connections
Creating new connection (admin only)
Viewing connection details
Exploring database schema
Closing/deleting connections
PII Scanning Flow

Configuring a new scan
Starting a scan
Monitoring scan progress in real-time
Viewing scan results
Exploring PII findings by table/column
Generating and downloading reports
Dashboard & Reporting Flow

Landing on dashboard
Viewing summary statistics
Exploring trends
Drilling down into specific findings
Accessing detailed reports
Administration Flow

Managing users
Configuring system settings
Monitoring system health and performance
Information Architecture
Main Navigation Areas

Dashboard
Database Connections
PII Scans
Reports
Templates
Administration (for admin users)
Settings
Secondary Navigation

User profile
Notifications
Help/documentation
System health status
UX Enhancement Opportunities
Progressive Loading

Implement skeleton screens for data-heavy pages
Use progressive loading for large result sets
Implement virtualized lists for long data tables
Real-time Updates

WebSocket integration for scan progress
Live updates for dashboard metrics
Contextual Help

In-app guidance for complex workflows
Tooltips for technical terms and metrics
Error & Success Feedback

Consistent toast notifications
Inline validation for forms
Contextual error messages with recovery options
Success confirmation with next steps
3. Architecture Design
Folder Structure
State Management Approach
Global State Management

Authentication State: React Context + local storage for persistence
UI State: React Context for theme, notifications, and global UI elements
Feature Flags: React Context for feature availability based on user roles
Server State Management

React Query: For all API data fetching, caching, and synchronization
Custom Hooks: Wrapping React Query to provide domain-specific APIs
Local State Management

React useState/useReducer: For component-specific state
Form State: React Hook Form for form state management
Real-time State Management

WebSocket Context: For managing WebSocket connections and events
Subscription Patterns: For components to subscribe to relevant events
API Integration Layer
Base API Client


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


Service Layer

// services/scans.service.ts
import api from '../lib/api';
import { ScanRequest, ScanJobResponse, ScanStatsDTO } from '../types/api/scans';

export const scanService = {
  listScans: async (page = 0, size = 20, status?: string, connectionId?: string) => {
    const response = await api.get('/api/v1/scans', {
      params: { page, size, status, connectionId },
    });
    return response.data;
  },
  
  startScan: async (scanRequest: ScanRequest) => {
    const response = await api.post('/api/v1/scans', scanRequest);
    return response.data as ScanJobResponse;
  },
  
  getScanStatus: async (jobId: string) => {
    const response = await api.get(`/api/v1/scans/${jobId}`);
    return response.data as ScanJobResponse;
  },
  
  getScanStats: async (jobId: string) => {
    const response = await api.get(`/api/v1/scans/${jobId}/stats`);
    return response.data as ScanStatsDTO;
  },
  
  // Other scan-related API methods...
};



GitHub Copilot
PrivSense React Frontend Development Plan
Project Planning Phase
1. API Documentation Analysis
After reviewing the OpenAPI specification for the PrivSense API, here's a comprehensive breakdown of the API structure:

API Structure Overview
Authentication & Authorization: JWT-based authentication with role-based access (ADMIN and API_USER roles)
Core Domain Areas: 11 primary functional domains covering the entire system
Data Models: 35+ data transfer objects handling various aspects of the system
API Endpoints: ~50 endpoints organized by functional domain
Key Endpoints by Domain
Authentication

Login endpoint (POST /api/v1/auth/login)
Registration endpoint (POST /api/v1/auth/register)
Current user endpoint (GET /api/v1/auth/me)
Database Connections

List/create/view/delete database connections
Fetch database metadata (schema information)
PII Scans

Start new scans on databases
View scan status and progress
Access scan results, statistics, and reports
Cancel ongoing scans
Scan Templates

Create/update/delete reusable scan configurations
Execute scans from templates
Data Sampling

Test sampling configurations on database columns
Batch sampling across multiple tables
System Information

Health checks and system status
System configuration information
Dashboard

Overview statistics
Recent scans
Trend analysis
Top PII findings
Configuration Management

View/update application configuration
Manage detection rules
Potential Challenges
Real-time Scan Updates: Scan status updates are provided via both polling endpoints and WebSockets, requiring a hybrid approach
Complex Data Visualization: Dashboard and scan results involve complex data visualization requirements
Hierarchical Data: Database metadata includes nested structures (tables, columns, relationships)
Authentication Management: Token handling, expiration, and refresh mechanisms
Form Complexity: Several endpoints require complex nested form data structures
2. User Experience Planning
User Personas
Database Administrator

Primary goals: Connect databases, manage database connections
Technical expertise: High
Usage frequency: Occasional to regular
Data Privacy Officer

Primary goals: Configure and run PII scans, review reports
Technical expertise: Medium
Usage frequency: Regular
Compliance Manager

Primary goals: View reports, understand compliance status
Technical expertise: Low to medium
Usage frequency: Regular to frequent
System Administrator

Primary goals: Configure system, manage users
Technical expertise: High
Usage frequency: Occasional
Core User Flows
Database Connection Management Flow

Landing on connections page
Viewing existing connections
Creating new connection (admin only)
Viewing connection details
Exploring database schema
Closing/deleting connections
PII Scanning Flow

Configuring a new scan
Starting a scan
Monitoring scan progress in real-time
Viewing scan results
Exploring PII findings by table/column
Generating and downloading reports
Dashboard & Reporting Flow

Landing on dashboard
Viewing summary statistics
Exploring trends
Drilling down into specific findings
Accessing detailed reports
Administration Flow

Managing users
Configuring system settings
Monitoring system health and performance
Information Architecture
Main Navigation Areas

Dashboard
Database Connections
PII Scans
Reports
Templates
Administration (for admin users)
Settings
Secondary Navigation

User profile
Notifications
Help/documentation
System health status
UX Enhancement Opportunities
Progressive Loading

Implement skeleton screens for data-heavy pages
Use progressive loading for large result sets
Implement virtualized lists for long data tables
Real-time Updates

WebSocket integration for scan progress
Live updates for dashboard metrics
Contextual Help

In-app guidance for complex workflows
Tooltips for technical terms and metrics
Error & Success Feedback

Consistent toast notifications
Inline validation for forms
Contextual error messages with recovery options
Success confirmation with next steps
3. Architecture Design
Folder Structure
State Management Approach
Global State Management

Authentication State: React Context + local storage for persistence
UI State: React Context for theme, notifications, and global UI elements
Feature Flags: React Context for feature availability based on user roles
Server State Management

React Query: For all API data fetching, caching, and synchronization
Custom Hooks: Wrapping React Query to provide domain-specific APIs
Local State Management

React useState/useReducer: For component-specific state
Form State: React Hook Form for form state management
Real-time State Management

WebSocket Context: For managing WebSocket connections and events
Subscription Patterns: For components to subscribe to relevant events
API Integration Layer
Base API Client

Service Layer

React Query Integration

// hooks/api/useScans.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { scanService } from '../../services/scans.service';
import { ScanRequest } from '../../types/api/scans';

export const useScans = (page = 0, size = 20, status?: string, connectionId?: string) => {
  return useQuery({
    queryKey: ['scans', page, size, status, connectionId],
    queryFn: () => scanService.listScans(page, size, status, connectionId),
  });
};

export const useScanStatus = (jobId: string, enabled = true) => {
  return useQuery({
    queryKey: ['scan', jobId, 'status'],
    queryFn: () => scanService.getScanStatus(jobId),
    enabled,
    refetchInterval: (data) => {
      // Only keep polling if scan is not completed
      return data?.completed ? false : 5000;
    },
  });
};

export const useStartScan = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (scanRequest: ScanRequest) => scanService.startScan(scanRequest),
    onSuccess: () => {
      // Invalidate scans list query to refresh the data
      queryClient.invalidateQueries({ queryKey: ['scans'] });
    },
  });
};

// Other custom hooks for scan-related operations...



GitHub Copilot
PrivSense React Frontend Development Plan
Project Planning Phase
1. API Documentation Analysis
After reviewing the OpenAPI specification for the PrivSense API, here's a comprehensive breakdown of the API structure:

API Structure Overview
Authentication & Authorization: JWT-based authentication with role-based access (ADMIN and API_USER roles)
Core Domain Areas: 11 primary functional domains covering the entire system
Data Models: 35+ data transfer objects handling various aspects of the system
API Endpoints: ~50 endpoints organized by functional domain
Key Endpoints by Domain
Authentication

Login endpoint (POST /api/v1/auth/login)
Registration endpoint (POST /api/v1/auth/register)
Current user endpoint (GET /api/v1/auth/me)
Database Connections

List/create/view/delete database connections
Fetch database metadata (schema information)
PII Scans

Start new scans on databases
View scan status and progress
Access scan results, statistics, and reports
Cancel ongoing scans
Scan Templates

Create/update/delete reusable scan configurations
Execute scans from templates
Data Sampling

Test sampling configurations on database columns
Batch sampling across multiple tables
System Information

Health checks and system status
System configuration information
Dashboard

Overview statistics
Recent scans
Trend analysis
Top PII findings
Configuration Management

View/update application configuration
Manage detection rules
Potential Challenges
Real-time Scan Updates: Scan status updates are provided via both polling endpoints and WebSockets, requiring a hybrid approach
Complex Data Visualization: Dashboard and scan results involve complex data visualization requirements
Hierarchical Data: Database metadata includes nested structures (tables, columns, relationships)
Authentication Management: Token handling, expiration, and refresh mechanisms
Form Complexity: Several endpoints require complex nested form data structures
2. User Experience Planning
User Personas
Database Administrator

Primary goals: Connect databases, manage database connections
Technical expertise: High
Usage frequency: Occasional to regular
Data Privacy Officer

Primary goals: Configure and run PII scans, review reports
Technical expertise: Medium
Usage frequency: Regular
Compliance Manager

Primary goals: View reports, understand compliance status
Technical expertise: Low to medium
Usage frequency: Regular to frequent
System Administrator

Primary goals: Configure system, manage users
Technical expertise: High
Usage frequency: Occasional
Core User Flows
Database Connection Management Flow

Landing on connections page
Viewing existing connections
Creating new connection (admin only)
Viewing connection details
Exploring database schema
Closing/deleting connections
PII Scanning Flow

Configuring a new scan
Starting a scan
Monitoring scan progress in real-time
Viewing scan results
Exploring PII findings by table/column
Generating and downloading reports
Dashboard & Reporting Flow

Landing on dashboard
Viewing summary statistics
Exploring trends
Drilling down into specific findings
Accessing detailed reports
Administration Flow

Managing users
Configuring system settings
Monitoring system health and performance
Information Architecture
Main Navigation Areas

Dashboard
Database Connections
PII Scans
Reports
Templates
Administration (for admin users)
Settings
Secondary Navigation

User profile
Notifications
Help/documentation
System health status
UX Enhancement Opportunities
Progressive Loading

Implement skeleton screens for data-heavy pages
Use progressive loading for large result sets
Implement virtualized lists for long data tables
Real-time Updates

WebSocket integration for scan progress
Live updates for dashboard metrics
Contextual Help

In-app guidance for complex workflows
Tooltips for technical terms and metrics
Error & Success Feedback

Consistent toast notifications
Inline validation for forms
Contextual error messages with recovery options
Success confirmation with next steps
3. Architecture Design
Folder Structure
State Management Approach
Global State Management

Authentication State: React Context + local storage for persistence
UI State: React Context for theme, notifications, and global UI elements
Feature Flags: React Context for feature availability based on user roles
Server State Management

React Query: For all API data fetching, caching, and synchronization
Custom Hooks: Wrapping React Query to provide domain-specific APIs
Local State Management

React useState/useReducer: For component-specific state
Form State: React Hook Form for form state management
Real-time State Management

WebSocket Context: For managing WebSocket connections and events
Subscription Patterns: For components to subscribe to relevant events
API Integration Layer
Base API Client

Service Layer

React Query Integration

Data Flow Diagrams
Authentication Flow

User Input → Authentication Form → Auth Service → API
    ↓                                   ↑
AuthContext ← ← ← ← ← ← ← ← ← ← ← ← ← ← ┘
    ↓
Protected Routes

Data Fetching Flow

Component → Custom Hook → React Query → API Service → API
    ↑           ↑             ↑
    │           │             └── Query Cache ──┐
    │           │                               │
    └── ← ← ← ← └── ← ← ← ← ← ← ← ← ← ← ← ← ← ─┘


    Data Fetching Flow

    Component → Custom Hook → React Query → API Service → API
    ↑           ↑             ↑
    │           │             └── Query Cache ──┐
    │           │                               │
    └── ← ← ← ← └── ← ← ← ← ← ← ← ← ← ← ← ← ← ─┘

    Real-time Updates Flow

    WebSocket Connection → WebSocket Context → Event Handlers
      ↑                       ↓
      │                    Components
      │                       ↓
WebSocket Server ← ← ← ← API Server

Custom Hooks Strategy
API Hooks

useApiResource: Generic hook for CRUD operations
use{Resource}: Resource-specific hooks (e.g., useConnections, useScans)
use{Operation}: Operation-specific hooks (e.g., useStartScan, useCreateConnection)
Authentication Hooks

useAuth: Access authentication context and methods
useCurrentUser: Get current user information
UI Hooks

useNotification: Display toast notifications
useModal: Control modal dialogs
useSelection: Handle selection in lists/tables
Feature-specific Hooks

useScanProgress: Track scan progress with WebSocket updates
useDatabaseMetadata: Fetch and transform database schema
usePiiVisualization: Prepare PII data for visualizations
4. Development Phases
Phase 1: Project Setup and Core Infrastructure (2 weeks)
Tasks:

Initialize React project with TypeScript and Vite
Set up routing with React Router
Configure API client with Axios
Implement authentication context and services
Set up React Query for data fetching
Configure global state management
Set up project structure and folder organization
Create basic layouts and navigation components
Implement protected routes and role-based access control
Set up theming and global styles
Expected Challenges:

Configuring the optimal development environment
Setting up proper type definitions for API responses
Configuring authentication flow with token management
Deliverables:

Working project structure with navigation
Authentication flow (login, logout, token management)
Protected routes based on user roles
API client with interceptors for authentication
Phase 2: Core Components and Layout Development (2 weeks)
Tasks:

Implement common UI components (buttons, inputs, cards, etc.)
Develop layout components (header, sidebar, page container)
Create feedback components (loading states, error messages, notifications)
Implement form components and validation patterns
Create data visualization components (charts, graphs)
Develop table components with sorting, filtering, and pagination
Implement modal and dialog components
Create wizard/stepper components for multi-step processes
Expected Challenges:

Ensuring consistent design across components
Building flexible yet consistent form handling
Creating accessible components
Deliverables:

Component library with documentation
Layout system with responsive design
Form handling utilities and validation
Data visualization components
Phase 3: Dashboard and Connections Features (2 weeks)
Tasks:

Implement dashboard page with summary statistics
Create charts and visualizations for PII distribution
Develop recent scans list component
Implement database connections list view
Create connection details view with metadata explorer
Develop new connection form with validation
Implement database schema visualization
Expected Challenges:

Creating effective visualizations for complex data
Handling large schema metadata efficiently
Form validation for connection parameters
Deliverables:

Working dashboard with real data
Connections management feature
Database schema explorer
Phase 4: Scan Management and Results Visualization (3 weeks)
Tasks:

Implement scans list view with filtering and sorting
Create scan configuration form with validation
Develop scan templates management
Implement scan execution and monitoring with real-time updates
Create scan results visualization with drill-down capabilities
Develop scan reports generation and download
Implement PII findings exploration interface
Expected Challenges:

Real-time progress updates via WebSockets
Complex form for scan configuration
Visualizing hierarchical PII findings effectively
Deliverables:

Complete scan management workflow
Real-time scan monitoring
Interactive results visualization
Report generation and export
Phase 5: Administration and Settings (2 weeks)
Tasks:

Implement user management interface (admin only)
Create system settings configuration
Develop detection rules management
Implement system monitoring interface
Create application health dashboard
Expected Challenges:

Complex form validation for configuration settings
Permissions management for admin features
Displaying technical information in user-friendly way
Deliverables:

User management interface
System configuration interface
Detection rules management
System health monitoring
Phase 6: Polish, Testing and Optimization (2 weeks)
Tasks:

Add animations and transitions
Implement advanced filtering and search
Optimize performance with code splitting and lazy loading
Perform comprehensive testing
Address accessibility issues
Implement error tracking and monitoring
Create user documentation
Expected Challenges:

Balancing visual polish with performance
Edge case handling
Cross-browser compatibility
Deliverables:

Fully polished user interface
Comprehensive test coverage
Optimized application performance
User documentation
5. Component Strategy
Authentication Components
LoginForm Component

State: Form values, validation errors, submission state
Props: onLoginSuccess callback
Behavior: Form validation, API error handling, redirect on success
Accessibility: Form labels, error announcements, keyboard navigation
UserMenu Component

State: Menu open/closed
Props: User information, logout handler
Behavior: Display user info, provide logout option
Reusability: Can be used in various layouts
Database Connection Components
ConnectionsList Component

State: Loading, error, filter criteria
Props: Connections array, onSelect handler
Behavior: Display paginated list, support filtering and sorting
States: Loading, empty, error, populated
ConnectionForm Component

State: Form values, validation state, submission state
Props: Initial values (for editing), onSubmit handler
Behavior: Field validation, test connection option
Accessibility: Form groups, validation feedback
DatabaseSchemaExplorer Component

State: Expanded nodes, selected table/column
Props: Schema metadata, onSelect handlers
Behavior: Tree view with expanding/collapsing nodes
Performance: Virtualized rendering for large schemas
PII Scan Components
ScanConfiguration Component

State: Form values, validation state
Props: Available templates, database connections
Behavior: Multi-step configuration wizard
Reusability: Configuration sections as sub-components
ScanProgressMonitor Component

State: Scan status, progress percentage
Props: Scan ID, onComplete handler
Behavior: Real-time updates via WebSocket or polling
States: Running, completed, failed, with appropriate visuals
PiiResultsExplorer Component

State: Selected view (table/chart), filter criteria
Props: Scan results data
Behavior: Interactive exploration of PII findings
Accessibility: Data tables with proper ARIA attributes
Dashboard Components
SummaryStatistics Component

State: Time range selection
Props: Statistical data
Behavior: Display key metrics with comparison to previous periods
Reusability: Individual stat cards as reusable components
PiiDistributionChart Component

State: Chart type selection
Props: Distribution data
Behavior: Visual representation of PII types found
Accessibility: Color contrast, alternative text representations
RecentScansTable Component

State: Pagination state
Props: Recent scans data
Behavior: Quick access to recent scan information
Performance: Load data incrementally
6. Technology Selection
Core Framework and Libraries
React 18+: For component-based UI development with concurrent features
TypeScript: For type safety and improved developer experience
Vite: For fast development and optimized builds
UI Component Library
MUI (Material-UI): Comprehensive component library with accessibility support
Tailwind CSS: For utility-first styling approach and customization
State Management
React Context API: For global state like authentication and UI preferences
React Query: For server state management, caching, and synchronization
Form Handling
React Hook Form: For efficient form state management with minimal re-renders
Zod: For schema validation with TypeScript integration
Routing
React Router 6: For declarative routing with nested routes support
API Communication
Axios: For HTTP requests with interceptors for authentication
SWR: Alternative to React Query for simple data fetching cases
Data Visualization
Recharts: For charts and graphs with React integration
react-table: For advanced table functionality
react-flow: For relationship visualizations (schema diagrams)
Animation and Transitions
Framer Motion: For animations and transitions
react-spring: Alternative for physics-based animations
Testing
Vitest: For unit and integration testing
React Testing Library: For component testing
Cypress: For end-to-end testing
DevOps and Tooling
ESLint: For code quality and consistency
Prettier: For code formatting
Husky & lint-staged: For pre-commit hooks
7. Performance and Optimization Plan
Component-Level Optimization
Memoization Strategy

Use React.memo() for expensive components that render often
Implement useMemo() for expensive calculations
Use useCallback() for handler functions passed to child components
Render Optimization

Avoid unnecessary re-renders by isolating state
Use the React DevTools Profiler to identify performance issues
Implement virtualization for long lists using react-window or react-virtualized
Data Fetching Optimization
Caching Strategy

Leverage React Query's caching capabilities
Implement stale-while-revalidate pattern
Configure appropriate cache TTLs based on data volatility
Request Batching

Implement request batching for related data
Use GraphQL (if applicable) to reduce over-fetching
Prefetching

Prefetch data for likely user paths
Implement hover prefetching for navigation items
Code Splitting Strategy
Route-Based Splitting

Split code by routes using React's lazy loading
Implement suspense boundaries with fallbacks
Component-Based Splitting

Split large components or features that aren't immediately needed
Use dynamic imports for rarely used functionality
Vendor Chunk Optimization

Configure build to separate vendor chunks
Optimize chunk sizes for better caching
Asset Optimization
Image Optimization

Use modern image formats (WebP, AVIF)
Implement responsive images with srcset
Lazy load images that are below the fold
Font Optimization

Use font-display: swap for text visibility during font loading
Subset fonts to include only necessary characters
Preload critical fonts
Caching Strategies
Response Caching

Configure appropriate cache control headers
Implement service worker for offline support
Use localStorage/IndexedDB for persistence when appropriate
Computed Value Caching

Cache expensive computations with memoization
Implement debouncing for frequent updates
Implementation Guide
8. Step-by-Step Implementation Plan
Initial Project Setup

# Initialize project with Vite
npm create vite@latest privsense-frontend -- --template react-ts

# Navigate to project directory
cd privsense-frontend

# Install core dependencies
npm install react-router-dom @tanstack/react-query axios jwt-decode
npm install @mui/material @mui/icons-material @emotion/react @emotion/styled
npm install react-hook-form zod @hookform/resolvers/zod
npm install recharts

# Install dev dependencies
npm install -D vitest @testing-library/react @testing-library/jest-dom @testing-library/user-event
npm install -D eslint prettier eslint-config-prettier husky lint-staged


Core Infrastructure Implementation
Set up API Client

// src/lib/api.ts
import axios from 'axios';
import { toast } from 'react-toastify';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/privsense',
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
  (error) => {
    // Handle different error types
    if (!error.response) {
      toast.error('Network error. Please check your connection.');
    } else {
      const { status, data } = error.response;
      
      switch (status) {
        case 401:
          // Handle authentication error
          localStorage.removeItem('auth_token');
          window.location.href = '/login';
          toast.error('Your session has expired. Please login again.');
          break;
          
        case 403:
          toast.error('You do not have permission to perform this action.');
          break;
          
        case 404:
          toast.error('The requested resource was not found.');
          break;
          
        case 500:
          toast.error('An unexpected server error occurred. Please try again later.');
          break;
          
        default:
          toast.error(data?.message || 'An error occurred.');
      }
    }
    
    return Promise.reject(error);
  }
);

export default api;

Set up Authentication Context

// src/contexts/AuthContext.tsx
import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { jwtDecode } from 'jwt-decode';
import api from '../lib/api';

interface User {
  id: string;
  username: string;
  roles: string[];
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  isLoading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(localStorage.getItem('auth_token'));
  const [isLoading, setIsLoading] = useState(true);
  
  // Check if user has admin role
  const isAdmin = user?.roles.includes('ADMIN') || false;
  
  // Check if user is authenticated
  const isAuthenticated = !!user && !!token;
  
  // Load user from token on mount
  useEffect(() => {
    const initAuth = async () => {
      const storedToken = localStorage.getItem('auth_token');
      if (storedToken) {
        try {
          // Verify token is valid
          const decoded = jwtDecode<any>(storedToken);
          
          // Check if token is expired
          if (decoded.exp * 1000 < Date.now()) {
            throw new Error('Token expired');
          }
          
          // Get user details
          const response = await api.get('/api/v1/auth/me');
          setUser(response.data);
          setToken(storedToken);
        } catch (error) {
          // Invalid token - clear it
          localStorage.removeItem('auth_token');
          setUser(null);
          setToken(null);
        }
      }
      setIsLoading(false);
    };
    
    initAuth();
  }, []);
  
  // Login function
  const login = async (username: string, password: string) => {
    setIsLoading(true);
    try {
      const response = await api.post('/api/v1/auth/login', { username, password });
      const { token } = response.data;
      
      // Store token
      localStorage.setItem('auth_token', token);
      setToken(token);
      
      // Get user details
      const userResponse = await api.get('/api/v1/auth/me');
      setUser(userResponse.data);
    } finally {
      setIsLoading(false);
    }
  };
  
  // Logout function
  const logout = () => {
    localStorage.removeItem('auth_token');
    setUser(null);
    setToken(null);
  };
  
  return (
    <AuthContext.Provider value={{ 
      user, 
      token, 
      isAuthenticated, 
      isAdmin, 
      login, 
      logout, 
      isLoading 
    }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};


Set up React Query

// src/lib/react-query.ts
import { QueryClient } from '@tanstack/react-query';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 5 * 60 * 1000, // 5 minutes
    },
  },
});


Set up Routing

// src/router/routes.tsx
import { lazy, Suspense } from 'react';
import { Navigate } from 'react-router-dom';
import { AppLayout } from '../layouts/AppLayout';
import { PrivateRoute } from './PrivateRoute';
import { AdminRoute } from './AdminRoute';
import { LoadingPage } from '../components/feedback/LoadingPage';

// Lazy-loaded page components
const LoginPage = lazy(() => import('../pages/auth/LoginPage'));
const DashboardPage = lazy(() => import('../pages/dashboard/DashboardPage'));
const ConnectionsPage = lazy(() => import('../pages/connections/ConnectionsPage'));
const ConnectionDetailsPage = lazy(() => import('../pages/connections/ConnectionDetailsPage'));
const NewConnectionPage = lazy(() => import('../pages/connections/NewConnectionPage'));
const ScansPage = lazy(() => import('../pages/scans/ScansPage'));
const ScanDetailsPage = lazy(() => import('../pages/scans/ScanDetailsPage'));
const NewScanPage = lazy(() => import('../pages/scans/NewScanPage'));
const TemplatesPage = lazy(() => import('../pages/templates/TemplatesPage'));
const UsersPage = lazy(() => import('../pages/administration/UsersPage'));
const SettingsPage = lazy(() => import('../pages/settings/SettingsPage'));
const NotFoundPage = lazy(() => import('../pages/NotFoundPage'));

const routes = [
  {
    path: '/login',
    element: (
      <Suspense fallback={<LoadingPage />}>
        <LoginPage />
      </Suspense>
    ),
  },
  {
    path: '/',
    element: (
      <PrivateRoute>
        <AppLayout />
      </PrivateRoute>
    ),
    children: [
      { path: '', element: <Navigate to="/dashboard" replace /> },
      { 
        path: 'dashboard', 
        element: (
          <Suspense fallback={<LoadingPage />}>
            <DashboardPage />
          </Suspense>
        ) 
      },
      {
        path: 'connections',
        children: [
          { 
            path: '', 
            element: (
              <Suspense fallback={<LoadingPage />}>
                <ConnectionsPage />
              </Suspense>
            )
          },
          { 
            path: ':id', 
            element: (
              <Suspense fallback={<LoadingPage />}>
                <ConnectionDetailsPage />
              </Suspense>
            )
          },
          { 
            path: 'new', 
            element: (
              <AdminRoute>
                <Suspense fallback={<LoadingPage />}>
                  <NewConnectionPage />
                </Suspense>
              </AdminRoute>
            )
          },
        ],
      },
      {
        path: 'scans',
        children: [
          { 
            path: '', 
            element: (
              <Suspense fallback={<LoadingPage />}>
                <ScansPage />
              </Suspense>
            )
          },
          { 
            path: ':id', 
            element: (
              <Suspense fallback={<LoadingPage />}>
                <ScanDetailsPage />
              </Suspense>
            )
          },
          { 
            path: 'new', 
            element: (
              <Suspense fallback={<LoadingPage />}>
                <NewScanPage />
              </Suspense>
            )
          },
        ],
      },
      {
        path: 'templates',
        element: (
          <Suspense fallback={<LoadingPage />}>
            <TemplatesPage />
          </Suspense>
        )
      },
      {
        path: 'administration',
        element: (
          <AdminRoute>
            <Suspense fallback={<LoadingPage />}>
              <UsersPage />
            </Suspense>
          </AdminRoute>
        )
      },
      {
        path: 'settings',
        element: (
          <AdminRoute>
            <Suspense fallback={<LoadingPage />}>
            <SettingsPage />
            </Suspense>
          </AdminRoute>
        )
      },
    ],
  },
  {
    path: '*',
    element: (
      <Suspense fallback={<LoadingPage />}>
        <NotFoundPage />
      </Suspense>
    ),
  },
];

export default routes;

Set up Protected Routes

// src/router/PrivateRoute.tsx
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { LoadingPage } from '../components/feedback/LoadingPage';

interface PrivateRouteProps {
  children: React.ReactNode;
}

export const PrivateRoute = ({ children }: PrivateRouteProps) => {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return <LoadingPage />;
  }

  if (!isAuthenticated) {
    // Redirect to login page but save the current location to redirect back after login
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <>{children}</>;
};

// src/router/AdminRoute.tsx
import { Navigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { LoadingPage } from '../components/feedback/LoadingPage';

interface AdminRouteProps {
  children: React.ReactNode;
}

export const AdminRoute = ({ children }: AdminRouteProps) => {
  const { isAdmin, isLoading, isAuthenticated } = useAuth();

  if (isLoading) {
    return <LoadingPage />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (!isAdmin) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <>{children}</>;
};


Authentication Implementation
Login Page

// src/pages/auth/LoginPage.tsx
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
  Paper, 
  CircularProgress,
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
  const location = useLocation();
  const { login } = useAuth();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  // Get redirect path if user was redirected to login
  const from = location.state?.from?.pathname || '/dashboard';
  
  const { register, handleSubmit, formState: { errors } } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  });
  
  const onSubmit = async (data: LoginFormData) => {
    setIsSubmitting(true);
    setError(null);
    
    try {
      await login(data.username, data.password);
      navigate(from, { replace: true });
    } catch (err: any) {
      setError(err.response?.data?.message || 'Login failed. Please check your credentials.');
    } finally {
      setIsSubmitting(false);
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
              autoComplete="username"
              autoFocus
              {...register('username')}
              error={!!errors.username}
              helperText={errors.username?.message}
              disabled={isSubmitting}
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
              disabled={isSubmitting}
            />
            <Button
              type="submit"
              fullWidth
              variant="contained"
              color="primary"
              sx={{ mt: 3, mb: 2, py: 1.5 }}
              disabled={isSubmitting}
            >
              {isSubmitting ? <CircularProgress size={24} /> : 'Sign In'}
            </Button>
          </Box>
        </Paper>
      </Box>
    </Container>
  );
};

export default LoginPage;

Core Layout Implementation

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
  
  const toggleSidebar = () => {
    setSidebarOpen(!sidebarOpen);
  };
  
  return (
    <Box sx={{ display: 'flex', height: '100vh' }}>
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
          p: 3,
          width: { sm: `calc(100% - ${drawerWidth}px)` },
          overflowY: 'auto',
        }}
      >
        <Toolbar /> {/* Adds spacing below app bar */}
        <ErrorBoundary>
          <Outlet />
        </ErrorBoundary>
      </Box>
    </Box>
  );
};


// src/components/layout/Header.tsx
import { useNavigate } from 'react-router-dom';
import {
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  Box,
  Button,
  Menu,
  MenuItem,
  Avatar,
  Tooltip,
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import AccountCircleIcon from '@mui/icons-material/AccountCircle';
import NotificationsIcon from '@mui/icons-material/Notifications';
import { useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';

interface HeaderProps {
  drawerWidth: number;
  sidebarOpen: boolean;
  toggleSidebar: () => void;
}

export const Header = ({ drawerWidth, sidebarOpen, toggleSidebar }: HeaderProps) => {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  
  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };
  
  const handleMenuClose = () => {
    setAnchorEl(null);
  };
  
  const handleLogout = () => {
    handleMenuClose();
    logout();
    navigate('/login');
  };
  
  return (
    <AppBar
      position="fixed"
      sx={{
        width: { sm: sidebarOpen ? `calc(100% - ${drawerWidth}px)` : '100%' },
        ml: { sm: sidebarOpen ? `${drawerWidth}px` : 0 },
        zIndex: (theme) => theme.zIndex.drawer + 1,
        transition: (theme) =>
          theme.transitions.create(['width', 'margin'], {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen,
          }),
      }}
    >
      <Toolbar>
        <IconButton
          color="inherit"
          aria-label="toggle sidebar"
          edge="start"
          onClick={toggleSidebar}
          sx={{ mr: 2, display: { sm: 'none' } }}
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
              onClick={handleMenuOpen}
              color="inherit"
              sx={{ ml: 1 }}
              aria-controls="user-menu"
              aria-haspopup="true"
            >
              <Avatar sx={{ width: 32, height: 32, bgcolor: 'primary.dark' }}>
                <AccountCircleIcon />
              </Avatar>
            </IconButton>
          </Tooltip>
          
          <Menu
            id="user-menu"
            anchorEl={anchorEl}
            open={Boolean(anchorEl)}
            onClose={handleMenuClose}
            anchorOrigin={{
              vertical: 'bottom',
              horizontal: 'right',
            }}
            transformOrigin={{
              vertical: 'top',
              horizontal: 'right',
            }}
          >
            <MenuItem onClick={handleMenuClose}>
              <Typography variant="body2">
                Signed in as <strong>{user?.username}</strong>
              </Typography>
            </MenuItem>
            <MenuItem onClick={() => navigate('/settings')}>Settings</MenuItem>
            <MenuItem onClick={handleLogout}>Logout</MenuItem>
          </Menu>
        </Box>
      </Toolbar>
    </AppBar>
  );
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
    { label: 'Connections', icon: <StorageIcon />, path: '/connections' },
    { label: 'PII Scans', icon: <SearchIcon />, path: '/scans' },
    { label: 'Templates', icon: <DescriptionIcon />, path: '/templates' },
  ];
  
  const adminItems = [
    { label: 'Users', icon: <PeopleIcon />, path: '/administration' },
    { label: 'Settings', icon: <SettingsIcon />, path: '/settings' },
  ];
  
  return (
    <Drawer
      variant="permanent"
      sx={{
        width: drawerWidth,
        flexShrink: 0,
        [`& .MuiDrawer-paper`]: {
          width: drawerWidth,
          boxSizing: 'border-box',
          display: open ? 'block' : 'none',
        },
      }}
      open={open}
    >
      <Toolbar /> {/* Adds spacing below app bar */}
      <Box sx={{ overflow: 'auto' }}>
        <List>
          {navItems.map((item) => (
            <ListItem key={item.label} disablePadding>
              <ListItemButton
                onClick={() => navigate(item.path)}
                selected={isActive(item.path)}
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
                    onClick={() => navigate(item.path)}
                    selected={isActive(item.path)}
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
};

9. Reusability and Maintainability Strategy
Component Library Structure

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



TypeScript Interface Structure
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
    pageSize: number;
    totalItems: number;
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


Coding Standards and Patterns
Naming Conventions

Use PascalCase for components, interfaces, and types
Use camelCase for variables, functions, and properties
Use ALL_CAPS for constants
Prefix interface names with "I" (optional but helpful)
Suffix type names with "Type" for clarity
File Organization

One component per file (except for very small related components)
Export named components as default from their files
Group related files in feature folders
Use index.ts files to re-export components for cleaner imports
Component Patterns

Use functional components with hooks
Extract complex logic to custom hooks
Keep components focused on a single responsibility
Use composition over inheritance
Avoid prop drilling by using context where appropriate
State Management Patterns

Use local state for UI-specific state
Use context for shared state that doesn't change often
Use React Query for server state
Keep state as close as possible to where it's used
Documentation Practices
Component Documentation

Document component purpose
Document component props with TypeScript interfaces
Include usage examples
Document state and side effects
Document accessibility considerations
Example:


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


Hook Documentation

Document hook purpose
Document parameters and return values
Include usage examples
Document side effects and dependencies
Example:


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


Code Review Checklist
Functionality

Does the code meet the requirements?
Are all edge cases handled?
Is the UI responsive and accessible?
Code Quality

Is the code clean and readable?
Are there any unnecessary complexities?
Is the code DRY (Don't Repeat Yourself)?
Are functions and components focused on single responsibilities?
Performance

Are there any unnecessary re-renders?
Is memoization used appropriately?
Are expensive operations optimized?
Type Safety

Are types defined properly?
Are there any any types that could be more specific?
Are nullability and undefined handled correctly?
State Management

Is state kept at the appropriate level?
Is context used appropriately?
Are API calls and data fetching optimized?
Security

Is user input sanitized?
Are authentication and authorization checks in place?
Are API keys and secrets properly handled?

Environment Variable Management
Development Environment Variables

Use .env.development for development-specific variables
Use .env.local for local overrides (not committed to source control)
Production Environment Variables

Use .env.production for production defaults
Configure deployment platform to provide environment-specific values
Typing Environment Variables

Create a type definition file for environment variables

// src/types/env.d.ts
/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string;
  readonly VITE_WEBSOCKET_URL: string;
  readonly VITE_ENABLE_MOCK_API: string;
  // Add other environment variables here
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

Build Optimization
Code Splitting

Configure route-based code splitting
Use dynamic imports for large components


// vite.config.ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  build: {
    outDir: 'dist',
    sourcemap: false,
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
      },
    },
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor': ['react', 'react-dom', 'react-router-dom'],
          'material-ui': ['@mui/material', '@mui/icons-material'],
          'data-libs': ['recharts', '@tanstack/react-query'],
          'form-libs': ['react-hook-form', 'zod'],
        },
      },
    },
  },
});

Asset Optimization

Configure image optimization with appropriate plugins
Implement font preloading
Optimize bundle size with tree shaking

CI/CD Integration
GitHub Actions Workflow


# .github/workflows/ci.yml
name: CI/CD Pipeline

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v2
    
    - name: Set up Node.js
      uses: actions/setup-node@v2
      with:
        node-version: '18'
        cache: 'npm'
        
    - name: Install dependencies
      run: npm ci
      
    - name: Lint
      run: npm run lint
      
    - name: Type check
      run: npm run type-check
      
    - name: Run tests
      run: npm test
      
    - name: Build
      run: npm run build
      
    - name: Upload build artifacts
      uses: actions/upload-artifact@v2
      with:
        name: build
        path: dist



 Deployment Strategies
Static Hosting

Deploy to CDN-backed static hosting (Netlify, Vercel, AWS S3 + CloudFront)
Configure caching strategies
Set up proper HTTP headers
Container-based Deployment

Create podman container for the frontend


Nginx Configuration

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




