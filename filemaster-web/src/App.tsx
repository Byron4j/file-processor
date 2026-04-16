import { useEffect } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { Spin } from 'antd'
import { useAuthStore, useThemeStore } from '@stores'
import { MainLayout } from '@components/Layout'
import { Login } from '@pages/Login'
import { Dashboard } from '@pages/Dashboard'
import { AllFiles, Recent, Favorites, Trash } from '@pages/Files'
import { Preview } from '@pages/Preview'
import { Convert } from '@pages/Convert'
import { PdfTools, OcrTool, WatermarkTool, SplitMergeTool } from '@pages/Tools'
import { AiSummary, AiQa, AiClassification, AiTags } from '@pages/AI'
import { Tasks } from '@pages/Tasks'
import { Share } from '@pages/Share'
import { Admin } from '@pages/Admin'
import './App.less'

// Protected route component
const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const { isAuthenticated, isLoading } = useAuthStore()

  if (isLoading) {
    return (
      <div className="app-loading">
        <Spin size="large" tip="Loading..." />
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  return <>{children}</>
}

// Public route component (redirects to dashboard if already logged in)
const PublicRoute = ({ children }: { children: React.ReactNode }) => {
  const { isAuthenticated, isLoading } = useAuthStore()

  if (isLoading) {
    return (
      <div className="app-loading">
        <Spin size="large" tip="Loading..." />
      </div>
    )
  }

  if (isAuthenticated) {
    return <Navigate to="/" replace />
  }

  return <>{children}</>
}

function App() {
  const { fetchCurrentUser } = useAuthStore()
  const { isDark } = useThemeStore()

  // Check authentication on app mount
  useEffect(() => {
    const token = localStorage.getItem('accessToken')
    if (token) {
      fetchCurrentUser()
    }
  }, [fetchCurrentUser])

  return (
    <div className={`app ${isDark ? 'dark' : 'light'}`}>
      <Routes>
        {/* Public routes */}
        <Route
          path="/login"
          element={
            <PublicRoute>
              <Login />
            </PublicRoute>
          }
        />
        <Route path="/share/:shareId" element={<Share />} />

        {/* Protected routes */}
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <MainLayout />
            </ProtectedRoute>
          }
        >
          <Route index element={<Dashboard />} />

          {/* File management */}
          <Route path="files" element={<AllFiles />} />
          <Route path="files/recent" element={<Recent />} />
          <Route path="files/favorites" element={<Favorites />} />
          <Route path="files/trash" element={<Trash />} />

          {/* Preview */}
          <Route path="preview/:fileId" element={<Preview />} />

          {/* Conversion */}
          <Route path="convert" element={<Convert />} />
          <Route path="convert/document" element={<Convert type="document" />} />
          <Route path="convert/image" element={<Convert type="image" />} />
          <Route path="convert/video" element={<Convert type="video" />} />
          <Route path="convert/audio" element={<Convert type="audio" />} />

          {/* Tools */}
          <Route path="tools/pdf" element={<PdfTools />} />
          <Route path="tools/ocr" element={<OcrTool />} />
          <Route path="tools/watermark" element={<WatermarkTool />} />
          <Route path="tools/split-merge" element={<SplitMergeTool />} />

          {/* AI features */}
          <Route path="ai/summary" element={<AiSummary />} />
          <Route path="ai/qa" element={<AiQa />} />
          <Route path="ai/classification" element={<AiClassification />} />
          <Route path="ai/tags" element={<AiTags />} />

          {/* Tasks */}
          <Route path="tasks" element={<Tasks />} />
          <Route path="tasks/history" element={<Tasks view="history" />} />

          {/* Admin */}
          <Route path="admin/*" element={<Admin />} />
        </Route>

        {/* 404 redirect */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  )
}

export default App
