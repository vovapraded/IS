import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:8080/server-0.1.0/api",
  headers: { "Content-Type": "application/json" }
});

export default api;
