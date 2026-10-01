window.AUTOFLOW_CONFIG = {
  JAVA_API_URL: location.hostname.includes("onrender.com")
    ? "https://autoflow-java-api.onrender.com"
    : "http://localhost:8080",
  PYTHON_API_URL: location.hostname.includes("onrender.com")
    ? "https://autoflow-python-service.onrender.com"
    : "http://localhost:8001"
};
