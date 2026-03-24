import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import LanguageSwitcher from './components/LanguageSwitcher';
import LoginPage from './pages/LoginPage';

function App() {
  return (
    <BrowserRouter>
      <LanguageSwitcher />
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        {/* Redirect root to login for now; replace with dashboard route later */}
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
