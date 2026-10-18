import React, { useState, useEffect } from 'react';
import { User } from './types';
import { Navbar } from './components/Navbar';
import { Footer } from './components/Footer';
import { CataloguePage } from './pages/CataloguePage';
import { MyLoansPage } from './pages/MyLoansPage';
import { FinesPage } from './pages/FinesPage';
import { SystemDesignDashboard } from './pages/SystemDesignDashboard';
import { AdminPage } from './pages/AdminPage';
import { BookDetailModal } from './components/BookDetailModal';
import { AuthModal } from './components/AuthModal';

export const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<string>('catalogue');
  const [user, setUser] = useState<User | null>(null);
  const [selectedBookId, setSelectedBookId] = useState<string | null>(null);
  const [authModalOpen, setAuthModalOpen] = useState<boolean>(false);
  const [searchQuery, setSearchQuery] = useState<string>('');

  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    const token = localStorage.getItem('token');
    if (storedUser && token) {
      try {
        setUser(JSON.parse(storedUser));
      } catch (e) {
        localStorage.removeItem('user');
        localStorage.removeItem('token');
      }
    }
  }, []);

  const handleLoginSuccess = (loggedInUser: User) => {
    setUser(loggedInUser);
    setAuthModalOpen(false);
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
    if (activeTab === 'loans' || activeTab === 'fines' || activeTab === 'admin') {
      setActiveTab('catalogue');
    }
  };

  const handleSelectBook = (id: string) => {
    setSelectedBookId(id);
  };

  const handleSearchSubmit = (q: string) => {
    setSearchQuery(q);
    setActiveTab('catalogue');
  };

  return (
    <div className="min-h-screen bg-vidya-bg text-vidya-text-main flex flex-col font-sans selection:bg-vidya-blue-light selection:text-vidya-blue">
      {/* 1. Global Navigation Bar */}
      <Navbar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        user={user}
        onOpenAuth={() => setAuthModalOpen(true)}
        onLogout={handleLogout}
        onSelectBook={handleSelectBook}
        onSearchSubmit={handleSearchSubmit}
      />

      {/* 2. Main Content Area */}
      <main className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full">
        {activeTab === 'catalogue' && (
          <CataloguePage
            onSelectBook={handleSelectBook}
            externalSearchQuery={searchQuery}
          />
        )}

        {activeTab === 'loans' && (
          <MyLoansPage
            onNavigateToCatalogue={() => setActiveTab('catalogue')}
          />
        )}

        {activeTab === 'fines' && <FinesPage />}

        {activeTab === 'system-design' && <SystemDesignDashboard />}

        {activeTab === 'admin' && <AdminPage />}
      </main>

      {/* 3. Global Book Detail Modal */}
      <BookDetailModal
        bookId={selectedBookId}
        onClose={() => setSelectedBookId(null)}
        onRefresh={() => {
          // Triggered on borrow or reservation updates
        }}
        isAuthenticated={!!user}
        onOpenAuth={() => setAuthModalOpen(true)}
      />

      {/* 4. Global Authentication & Registration Modal */}
      <AuthModal
        isOpen={authModalOpen}
        onClose={() => setAuthModalOpen(false)}
        onSuccess={handleLoginSuccess}
      />

      {/* 5. Global Academic Footer */}
      <Footer />
    </div>
  );
};

export default App;
