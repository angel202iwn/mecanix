const API_BASE_URL = "/api/auth";

document.addEventListener("DOMContentLoaded", () => {
    const loginForm = document.getElementById("loginForm");
    const emailInput = document.getElementById("email");
    const passwordInput = document.getElementById("password");
    const togglePasswordBtn = document.getElementById("togglePassword");
    const alertBox = document.getElementById("alertBox");
    const alertMessage = document.getElementById("alertMessage");
    const submitBtn = document.getElementById("submitBtn");
    const btnText = document.getElementById("btnText");
    const btnSpinner = document.getElementById("btnSpinner");

    // Mostrar / Ocultar contraseña
    togglePasswordBtn.addEventListener("click", () => {
        const isPassword = passwordInput.type === "password";
        passwordInput.type = isPassword ? "text" : "password";
        togglePasswordBtn.textContent = isPassword ? "🙈" : "👁️";
    });

    // Autocompletar credenciales de prueba (creadas por DataInitializer en SQL Server)
    document.querySelectorAll(".role-btn").forEach(btn => {
        btn.addEventListener("click", () => {
            const emails = {
                ADMIN: "admin@mecanix.com",
                RECEPCIONISTA: "recepcion@mecanix.com",
                MECANICO: "mecanico@mecanix.com",
                CLIENTE: "cliente@mecanix.com"
            };
            emailInput.value = emails[btn.dataset.role];
            passwordInput.value = "123456";
            alertBox.classList.add("hidden");
        });
    });

    // Enviar Login al Backend Spring Boot
    loginForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        alertBox.classList.add("hidden");
        document.getElementById("emailError").textContent = "";
        document.getElementById("passwordError").textContent = "";

        const email = emailInput.value.trim();
        const password = passwordInput.value;

        let valid = true;
        if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
            document.getElementById("emailError").textContent = "Ingresa un correo válido.";
            valid = false;
        }
        if (!password || password.length < 6) {
            document.getElementById("passwordError").textContent = "Mínimo 6 caracteres.";
            valid = false;
        }
        if (!valid) return;

        submitBtn.disabled = true;
        btnText.textContent = "Verificando...";
        btnSpinner.classList.remove("hidden");

        try {
            const response = await fetch(`${API_BASE_URL}/login`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email, password })
            });

            const data = await response.json();
            if (!response.ok) {
                throw new Error(data.mensaje || "Correo o contraseña incorrectos.");
            }

            // Limpiar sesiones previas y guardar la nueva
            localStorage.removeItem("mecanix_token");
            localStorage.removeItem("mecanix_user");
            sessionStorage.removeItem("mecanix_token");
            sessionStorage.removeItem("mecanix_user");

            const storage = document.getElementById("rememberMe").checked ? localStorage : sessionStorage;
            storage.setItem("mecanix_token", data.token);
            storage.setItem("mecanix_user", JSON.stringify(data.user));

            window.location.href = "dashboard.html";
        } catch (error) {
            alertMessage.textContent = error.message;
            alertBox.classList.remove("hidden");
        } finally {
            submitBtn.disabled = false;
            btnText.textContent = "Iniciar Sesión";
            btnSpinner.classList.add("hidden");
        }
    });
});