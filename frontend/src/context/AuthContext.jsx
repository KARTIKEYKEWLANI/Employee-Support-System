import React, { createContext, useEffect, useMemo, useState } from "react";
import api from "../api/client.js";

export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const storedUser = localStorage.getItem("auth_user");
    if (storedUser) {
      setUser(JSON.parse(storedUser));
    }
    setLoading(false);
  }, []);

  const login = async (payload) => {
    const response = await api.post("/auth/login", payload);
    persistAuth(response.data);
    return response.data;
  };

  const register = async (payload) => {
    const response = await api.post("/auth/register", payload);
    persistAuth(response.data);
    return response.data;
  };

  const logout = () => {
    localStorage.removeItem("auth_token");
    localStorage.removeItem("auth_user");
    setUser(null);
  };

  const persistAuth = (data) => {
    localStorage.setItem("auth_token", data.token);
    const userPayload = {
      id: data.id,
      name: data.name,
      email: data.email,
      role: data.role
    };
    localStorage.setItem("auth_user", JSON.stringify(userPayload));
    setUser(userPayload);
  };

  const value = useMemo(
    () => ({ user, loading, login, register, logout }),
    [user, loading]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
