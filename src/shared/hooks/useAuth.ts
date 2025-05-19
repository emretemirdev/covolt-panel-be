interface AuthState {
  isAuthenticated: boolean;
  user: {
    id: string;
    email: string;
    fullName: string;
  } | null;
  roles: string[];
  permissions: string[];
}

export function useAuth() {
  const [authState, setAuthState] = useState<AuthState>(() => {
    // LocalStorage'dan token ve kullanıcı bilgilerini al
    const accessToken = localStorage.getItem('accessToken');
    const userStr = localStorage.getItem('user');
    const rolesStr = localStorage.getItem('roles');
    const permissionsStr = localStorage.getItem('permissions');
    
    return {
      isAuthenticated: !!accessToken,
      user: userStr ? JSON.parse(userStr) : null,
      roles: rolesStr ? JSON.parse(rolesStr) : [],
      permissions: permissionsStr ? JSON.parse(permissionsStr) : []
    };
  });
  
  const login = async (credentials: LoginCredentials) => {
    try {
      const response = await axiosInstance.post('/api/auth/login', credentials);
      const { 
        accessToken, 
        refreshToken, 
        userId, 
        email, 
        fullName, 
        roles, 
        permissions 
      } = response.data;
      
      // Token'ları ve kullanıcı bilgilerini sakla
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);
      
      const user = { id: userId, email, fullName };
      localStorage.setItem('user', JSON.stringify(user));
      localStorage.setItem('roles', JSON.stringify(roles));
      localStorage.setItem('permissions', JSON.stringify(permissions));
      
      // Auth state'i güncelle
      setAuthState({
        isAuthenticated: true,
        user,
        roles,
        permissions
      });
      
      return true;
    } catch (error) {
      // Hata işleme...
      return false;
    }
  };
  
  // Diğer metodlar...
  
  return {
    ...authState,
    login,
    // Diğer metodlar...
  };
}