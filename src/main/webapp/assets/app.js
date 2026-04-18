(() => {
    const SESSION_KEY = "web_adso_ia.session";

    const state = {
        page: "",
        apiBase: "",
        session: null
    };

    document.addEventListener("DOMContentLoaded", init);

    function init() {
        state.page = document.body.dataset.page || "home";
        state.apiBase = inferApiBase();
        state.session = getSession();

        wireCommonLayout();
        renderApiBaseHint();

        switch (state.page) {
            case "home":
                initHomePage();
                break;
            case "login":
                initLoginPage();
                break;
            case "register":
                initRegisterPage();
                break;
            case "products":
                initProductsPage();
                break;
            case "sales":
                initSalesPage();
                break;
            default:
                break;
        }
    }

    function inferApiBase() {
        const parts = window.location.pathname.split("/").filter(Boolean);
        const contextPath = parts.length > 0 ? `/${parts[0]}` : "";
        return `${contextPath}/resources`;
    }

    function getCurrentFileName() {
        const parts = window.location.pathname.split("/").filter(Boolean);
        if (!parts.length) {
            return "index.html";
        }

        return parts[parts.length - 1];
    }

    function getSession() {
        const raw = window.localStorage.getItem(SESSION_KEY);
        if (!raw) {
            return null;
        }

        try {
            const parsed = JSON.parse(raw);
            if (parsed && parsed.user && parsed.user.id) {
                return parsed;
            }

            if (parsed && parsed.id) {
                return { user: parsed };
            }
        } catch (error) {
            console.error("No se pudo leer la sesion local", error);
        }

        return null;
    }

    function setSession(user) {
        state.session = { user };
        window.localStorage.setItem(SESSION_KEY, JSON.stringify(state.session));
        renderSessionInfo();
    }

    function clearSession() {
        state.session = null;
        window.localStorage.removeItem(SESSION_KEY);
        renderSessionInfo();
    }

    function getUser() {
        return state.session ? state.session.user : null;
    }

    function isAdmin() {
        const user = getUser();
        return Boolean(user) && String(user.rol || "").toUpperCase() === "ADMIN";
    }

    function wireCommonLayout() {
        renderSessionInfo();

        const logoutButton = byId("logout-button");
        if (!logoutButton) {
            return;
        }

        logoutButton.addEventListener("click", () => {
            clearSession();
            showToast("Sesion cerrada correctamente.", "info");
            window.setTimeout(() => {
                window.location.href = "login.html";
            }, 250);
        });
    }

    function renderSessionInfo() {
        const user = getUser();
        const pill = byId("user-pill");
        const logoutButton = byId("logout-button");

        if (pill) {
            if (user) {
                const role = escapeHtml(user.rol || "CLIENTE");
                const name = escapeHtml(user.nombre || user.correo || "Usuario");
                pill.textContent = `${name} (${role})`;
                pill.classList.add("pill-auth");
            } else {
                pill.textContent = "Invitado";
                pill.classList.remove("pill-auth");
            }
        }

        if (logoutButton) {
            logoutButton.classList.toggle("hidden", !user);
        }
    }

    function renderApiBaseHint() {
        const apiNode = byId("api-base");
        if (apiNode) {
            apiNode.textContent = state.apiBase;
        }
    }

    function byId(id) {
        return document.getElementById(id);
    }

    function resolveRedirect(defaultPath) {
        const params = new URLSearchParams(window.location.search);
        const redirect = params.get("redirect");

        if (redirect && /^[a-zA-Z0-9_.-]+\.html$/.test(redirect)) {
            return redirect;
        }

        return defaultPath;
    }

    function requireAuth() {
        if (getUser()) {
            return true;
        }

        const target = encodeURIComponent(getCurrentFileName());
        window.location.href = `login.html?redirect=${target}`;
        return false;
    }

    async function apiFetch(endpoint, options = {}) {
        const config = {
            method: options.method || "GET",
            headers: {
                ...(options.headers || {})
            }
        };

        if (options.body !== undefined) {
            config.headers["Content-Type"] = "application/json";
            config.body = JSON.stringify(options.body);
        }

        const response = await fetch(`${state.apiBase}${endpoint}`, config);
        const contentType = response.headers.get("content-type") || "";

        let payload;
        if (contentType.includes("application/json")) {
            payload = await response.json();
        } else {
            payload = await response.text();
        }

        if (!response.ok) {
            throw new Error(readErrorMessage(payload, response.status));
        }

        return payload;
    }

    function readErrorMessage(payload, status) {
        if (payload && typeof payload === "object") {
            if (payload.mensaje) {
                return payload.mensaje;
            }
            return JSON.stringify(payload);
        }

        if (typeof payload === "string" && payload.trim().length > 0) {
            return payload;
        }

        return `Error ${status}`;
    }

    function showToast(message, type = "info") {
        const region = byId("toast-region");
        if (!region) {
            return;
        }

        const toast = document.createElement("div");
        toast.className = `toast ${type}`;
        toast.textContent = message;

        region.appendChild(toast);
        window.setTimeout(() => toast.remove(), 3600);
    }

    function setLoading(button, label) {
        if (!button) {
            return () => {};
        }

        const originalLabel = button.textContent;
        button.disabled = true;
        button.textContent = label;

        return () => {
            button.disabled = false;
            button.textContent = originalLabel;
        };
    }

    function escapeHtml(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#39;");
    }

    function toNumber(value, fallback = 0) {
        const number = Number(value);
        return Number.isFinite(number) ? number : fallback;
    }

    function formatMoney(value) {
        const number = Number(value);
        if (!Number.isFinite(number)) {
            return String(value ?? "0");
        }

        return number.toLocaleString("es-CO", {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        });
    }

    function formatDate(value) {
        if (!value) {
            return "Sin fecha";
        }

        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return String(value);
        }

        return date.toLocaleString("es-CO", {
            dateStyle: "medium",
            timeStyle: "short"
        });
    }

    function initHomePage() {
        const primaryAction = byId("home-primary-action");
        if (!primaryAction) {
            return;
        }

        if (getUser()) {
            primaryAction.textContent = "Ir a productos y carrito";
            primaryAction.href = "productos.html";
        }
    }

    function initLoginPage() {
        const form = byId("login-form");
        if (!form) {
            return;
        }

        form.addEventListener("submit", async (event) => {
            event.preventDefault();

            const correo = (form.correo.value || "").trim();
            const password = form.password.value || "";

            if (!correo || !password) {
                showToast("Ingresa correo y contrasena.", "error");
                return;
            }

            const submitButton = byId("login-submit");
            const done = setLoading(submitButton, "Ingresando...");

            try {
                const response = await apiFetch("/auth/login", {
                    method: "POST",
                    body: {
                        correo,
                        password
                    }
                });

                if (!response || !response.usuario) {
                    throw new Error("No se recibio informacion de usuario.");
                }

                setSession(response.usuario);
                showToast("Sesion iniciada correctamente.", "success");

                const destination = resolveRedirect("productos.html");
                window.setTimeout(() => {
                    window.location.href = destination;
                }, 380);
            } catch (error) {
                showToast(error.message, "error");
            } finally {
                done();
            }
        });
    }

    function initRegisterPage() {
        const form = byId("register-form");
        if (!form) {
            return;
        }

        form.addEventListener("submit", async (event) => {
            event.preventDefault();

            const payload = {
                nombre: (form.nombre.value || "").trim(),
                correo: (form.correo.value || "").trim(),
                password: form.password.value || "",
                rol: form.rol.value || "CLIENTE"
            };

            if (!payload.nombre || !payload.correo || !payload.password) {
                showToast("Completa todos los campos obligatorios.", "error");
                return;
            }

            if (payload.password.length < 6) {
                showToast("La contrasena debe tener al menos 6 caracteres.", "error");
                return;
            }

            const submitButton = byId("register-submit");
            const done = setLoading(submitButton, "Creando cuenta...");

            try {
                const response = await apiFetch("/auth/register", {
                    method: "POST",
                    body: payload
                });

                if (!response || !response.usuario) {
                    throw new Error("No se pudo iniciar sesion luego del registro.");
                }

                setSession(response.usuario);
                showToast("Cuenta creada correctamente.", "success");

                const destination = resolveRedirect("productos.html");
                window.setTimeout(() => {
                    window.location.href = destination;
                }, 420);
            } catch (error) {
                showToast(error.message, "error");
            } finally {
                done();
            }
        });
    }

    function initProductsPage() {
        if (!requireAuth()) {
            return;
        }

        const refs = {
            grid: byId("products-grid"),
            onlyActive: byId("only-active"),
            refreshProducts: byId("refresh-products"),
            refreshCart: byId("refresh-cart"),
            clearCart: byId("clear-cart"),
            checkout: byId("checkout"),
            cartList: byId("cart-list"),
            cartEmpty: byId("cart-empty"),
            cartTotal: byId("cart-total"),
            adminPanel: byId("admin-panel"),
            productForm: byId("product-form"),
            productFormTitle: byId("product-form-title"),
            saveProduct: byId("save-product"),
            cancelEdit: byId("cancel-edit-product"),
            prodNombre: byId("prod-nombre"),
            prodDescripcion: byId("prod-descripcion"),
            prodPrecio: byId("prod-precio"),
            prodStock: byId("prod-stock"),
            prodActivo: byId("prod-activo")
        };

        const pageState = {
            products: [],
            cart: null,
            editingProductId: null
        };

        if (isAdmin() && refs.adminPanel) {
            refs.adminPanel.classList.remove("hidden");
        }

        if (refs.refreshProducts) {
            refs.refreshProducts.addEventListener("click", loadProducts);
        }

        if (refs.onlyActive) {
            refs.onlyActive.addEventListener("change", loadProducts);
        }

        if (refs.grid) {
            refs.grid.addEventListener("click", onProductGridAction);
        }

        if (refs.refreshCart) {
            refs.refreshCart.addEventListener("click", loadCart);
        }

        if (refs.clearCart) {
            refs.clearCart.addEventListener("click", clearCart);
        }

        if (refs.checkout) {
            refs.checkout.addEventListener("click", checkout);
        }

        if (refs.cartList) {
            refs.cartList.addEventListener("click", onCartAction);
        }

        if (refs.productForm && isAdmin()) {
            refs.productForm.addEventListener("submit", onProductFormSubmit);
        }

        if (refs.cancelEdit && isAdmin()) {
            refs.cancelEdit.addEventListener("click", resetProductForm);
        }

        loadProducts();
        loadCart();

        async function loadProducts() {
            if (!refs.grid) {
                return;
            }

            const onlyActive = refs.onlyActive ? Boolean(refs.onlyActive.checked) : true;
            const done = setLoading(refs.refreshProducts, "Actualizando...");

            try {
                const response = await apiFetch(`/productos?soloActivos=${onlyActive}`);
                pageState.products = Array.isArray(response) ? response : [];
                renderProducts();
            } catch (error) {
                refs.grid.innerHTML = `<div class="empty-box">${escapeHtml(error.message)}</div>`;
                showToast(error.message, "error");
            } finally {
                done();
            }
        }

        function renderProducts() {
            if (!refs.grid) {
                return;
            }

            if (!pageState.products.length) {
                refs.grid.innerHTML = "<div class='empty-box'>No hay productos disponibles.</div>";
                return;
            }

            refs.grid.innerHTML = pageState.products.map((product) => buildProductCard(product)).join("");
        }

        function buildProductCard(product) {
            const active = Boolean(product.activo);
            const stock = toNumber(product.stock, 0);
            const canAdd = active && stock > 0;

            const badges = [
                `<span class="badge ${active ? "badge-ok" : "badge-off"}">${active ? "Activo" : "Inactivo"}</span>`
            ];

            if (stock <= 3) {
                badges.push("<span class=\"badge badge-stock-low\">Stock bajo</span>");
            }

            const adminTools = isAdmin()
                ? `
                    <div class="admin-tools">
                        <button class="btn btn-light" data-action="edit-product" data-id="${product.id}" type="button">Editar</button>
                        <button class="btn btn-danger" data-action="disable-product" data-id="${product.id}" type="button">Inactivar</button>
                    </div>
                `
                : "";

            return `
                <article class="product-card" data-product-id="${product.id}">
                    <div class="product-head">
                        <h3>${escapeHtml(product.nombre)}</h3>
                        <div class="product-badges">${badges.join("")}</div>
                    </div>
                    <p class="product-desc">${escapeHtml(product.descripcion || "Sin descripcion")}</p>
                    <div class="product-meta">
                        <span><strong>ID:</strong> ${escapeHtml(product.id)}</span>
                        <span><strong>Precio:</strong> ${formatMoney(product.precio)}</span>
                        <span><strong>Stock:</strong> ${escapeHtml(stock)}</span>
                    </div>
                    <div class="product-footer">
                        <div class="qty-group">
                            <label for="qty-${product.id}">Cantidad</label>
                            <input id="qty-${product.id}" data-role="qty" type="number" min="1" value="1">
                        </div>
                        <button class="btn btn-primary" data-action="add-cart" data-id="${product.id}" type="button" ${canAdd ? "" : "disabled"}>Agregar al carrito</button>
                    </div>
                    ${adminTools}
                </article>
            `;
        }

        async function onProductGridAction(event) {
            const button = event.target.closest("button[data-action]");
            if (!button) {
                return;
            }

            const action = button.dataset.action;
            const productId = Number(button.dataset.id);

            if (!Number.isFinite(productId)) {
                return;
            }

            if (action === "add-cart") {
                await addToCart(productId, button);
                return;
            }

            if (!isAdmin()) {
                showToast("Solo administradores pueden ejecutar esta accion.", "error");
                return;
            }

            if (action === "edit-product") {
                startProductEdition(productId);
                return;
            }

            if (action === "disable-product") {
                await disableProduct(productId, button);
            }
        }

        async function addToCart(productId, sourceButton) {
            const user = getUser();
            const card = sourceButton.closest(".product-card");
            const qtyInput = card ? card.querySelector("input[data-role='qty']") : null;
            const quantity = qtyInput ? Number(qtyInput.value) : 1;

            if (!Number.isInteger(quantity) || quantity <= 0) {
                showToast("Ingresa una cantidad valida.", "error");
                return;
            }

            const done = setLoading(sourceButton, "Agregando...");

            try {
                await apiFetch(`/carritos/usuario/${user.id}/items`, {
                    method: "POST",
                    body: {
                        productoId: productId,
                        cantidad: quantity
                    }
                });

                showToast("Producto agregado al carrito.", "success");
                await loadCart();
            } catch (error) {
                showToast(error.message, "error");
            } finally {
                done();
            }
        }

        function startProductEdition(productId) {
            const product = pageState.products.find((item) => Number(item.id) === Number(productId));
            if (!product) {
                showToast("No se encontro el producto.", "error");
                return;
            }

            pageState.editingProductId = Number(product.id);

            refs.prodNombre.value = product.nombre || "";
            refs.prodDescripcion.value = product.descripcion || "";
            refs.prodPrecio.value = toNumber(product.precio, 0);
            refs.prodStock.value = toNumber(product.stock, 0);
            refs.prodActivo.checked = Boolean(product.activo);

            refs.productFormTitle.textContent = `Editando producto #${product.id}`;
            refs.saveProduct.textContent = "Actualizar producto";
            refs.cancelEdit.classList.remove("hidden");

            refs.productForm.scrollIntoView({ behavior: "smooth", block: "start" });
        }

        function resetProductForm() {
            if (!refs.productForm) {
                return;
            }

            pageState.editingProductId = null;
            refs.productForm.reset();
            refs.prodActivo.checked = true;
            refs.productFormTitle.textContent = "Crear producto";
            refs.saveProduct.textContent = "Guardar producto";
            refs.cancelEdit.classList.add("hidden");
        }

        async function onProductFormSubmit(event) {
            event.preventDefault();

            const payload = {
                nombre: (refs.prodNombre.value || "").trim(),
                descripcion: (refs.prodDescripcion.value || "").trim(),
                precio: Number(refs.prodPrecio.value),
                stock: Number(refs.prodStock.value),
                activo: Boolean(refs.prodActivo.checked)
            };

            if (!payload.nombre) {
                showToast("El nombre del producto es obligatorio.", "error");
                return;
            }

            if (!Number.isFinite(payload.precio) || payload.precio <= 0) {
                showToast("El precio debe ser mayor a cero.", "error");
                return;
            }

            if (!Number.isInteger(payload.stock) || payload.stock < 0) {
                showToast("El stock debe ser entero y no negativo.", "error");
                return;
            }

            const done = setLoading(refs.saveProduct, pageState.editingProductId ? "Actualizando..." : "Guardando...");

            try {
                if (pageState.editingProductId) {
                    await apiFetch(`/productos/${pageState.editingProductId}`, {
                        method: "PUT",
                        body: payload
                    });
                    showToast("Producto actualizado.", "success");
                } else {
                    await apiFetch("/productos", {
                        method: "POST",
                        body: payload
                    });
                    showToast("Producto creado.", "success");
                }

                resetProductForm();
                await loadProducts();
            } catch (error) {
                showToast(error.message, "error");
            } finally {
                done();
            }
        }

        async function disableProduct(productId, sourceButton) {
            const confirmed = window.confirm("El producto se marcara como inactivo. Deseas continuar?");
            if (!confirmed) {
                return;
            }

            const done = setLoading(sourceButton, "Inactivando...");

            try {
                await apiFetch(`/productos/${productId}`, {
                    method: "DELETE"
                });

                showToast("Producto inactivado correctamente.", "success");
                await loadProducts();
            } catch (error) {
                showToast(error.message, "error");
            } finally {
                done();
            }
        }

        async function loadCart() {
            const user = getUser();
            const done = setLoading(refs.refreshCart, "Cargando...");

            try {
                const response = await apiFetch(`/carritos/usuario/${user.id}`);
                pageState.cart = response;
                renderCart();
            } catch (error) {
                refs.cartList.innerHTML = "";
                refs.cartEmpty.textContent = "No se pudo cargar el carrito.";
                refs.cartEmpty.classList.remove("hidden");
                refs.clearCart.disabled = true;
                refs.checkout.disabled = true;
                showToast(error.message, "error");
            } finally {
                done();
            }
        }

        function renderCart() {
            const cart = pageState.cart;
            if (!cart) {
                refs.cartList.innerHTML = "";
                refs.cartEmpty.textContent = "No hay carrito disponible.";
                refs.cartEmpty.classList.remove("hidden");
                refs.cartTotal.textContent = "0.00";
                refs.clearCart.disabled = true;
                refs.checkout.disabled = true;
                return;
            }

            const items = Array.isArray(cart.items) ? cart.items : [];
            refs.cartTotal.textContent = formatMoney(cart.total);

            if (!items.length) {
                refs.cartList.innerHTML = "";
                refs.cartEmpty.textContent = "Tu carrito esta vacio.";
                refs.cartEmpty.classList.remove("hidden");
                refs.clearCart.disabled = true;
                refs.checkout.disabled = true;
                return;
            }

            refs.cartEmpty.classList.add("hidden");
            refs.clearCart.disabled = false;
            refs.checkout.disabled = false;

            refs.cartList.innerHTML = items.map((item) => {
                return `
                    <li class="cart-item" data-item-id="${item.itemId}">
                        <div class="cart-item-top">
                            <div>
                                <div class="cart-name">${escapeHtml(item.producto)}</div>
                                <div class="cart-meta">Item #${escapeHtml(item.itemId)} | Precio: ${formatMoney(item.precioUnitario)} | Subtotal: ${formatMoney(item.subtotal)}</div>
                            </div>
                        </div>
                        <div class="cart-controls">
                            <input data-role="qty" type="number" min="1" value="${escapeHtml(item.cantidad)}">
                            <button class="btn btn-light" data-action="update-item" type="button">Actualizar</button>
                            <button class="btn btn-danger" data-action="remove-item" type="button">Quitar</button>
                        </div>
                    </li>
                `;
            }).join("");
        }

        async function onCartAction(event) {
            const button = event.target.closest("button[data-action]");
            if (!button) {
                return;
            }

            const row = button.closest(".cart-item");
            const itemId = row ? Number(row.dataset.itemId) : NaN;
            if (!Number.isFinite(itemId)) {
                return;
            }

            if (button.dataset.action === "update-item") {
                const qtyInput = row.querySelector("input[data-role='qty']");
                const quantity = qtyInput ? Number(qtyInput.value) : NaN;
                await updateCartItem(itemId, quantity, button);
            }

            if (button.dataset.action === "remove-item") {
                await removeCartItem(itemId, button);
            }
        }

        async function updateCartItem(itemId, quantity, sourceButton) {
            const user = getUser();
            if (!Number.isInteger(quantity) || quantity <= 0) {
                showToast("La cantidad debe ser mayor que cero.", "error");
                return;
            }

            const done = setLoading(sourceButton, "Guardando...");

            try {
                await apiFetch(`/carritos/usuario/${user.id}/items/${itemId}`, {
                    method: "PUT",
                    body: { cantidad: quantity }
                });

                showToast("Cantidad actualizada.", "success");
                await loadCart();
            } catch (error) {
                showToast(error.message, "error");
            } finally {
                done();
            }
        }

        async function removeCartItem(itemId, sourceButton) {
            const user = getUser();
            const done = setLoading(sourceButton, "Quitando...");

            try {
                await apiFetch(`/carritos/usuario/${user.id}/items/${itemId}`, {
                    method: "DELETE"
                });

                showToast("Item eliminado del carrito.", "success");
                await loadCart();
            } catch (error) {
                showToast(error.message, "error");
            } finally {
                done();
            }
        }

        async function clearCart() {
            const user = getUser();
            const confirmed = window.confirm("Se eliminaran todos los items del carrito. Deseas continuar?");
            if (!confirmed) {
                return;
            }

            const done = setLoading(refs.clearCart, "Vaciando...");

            try {
                await apiFetch(`/carritos/usuario/${user.id}/items`, {
                    method: "DELETE"
                });

                showToast("Carrito vaciado.", "success");
                await loadCart();
            } catch (error) {
                showToast(error.message, "error");
            } finally {
                done();
            }
        }

        async function checkout() {
            const user = getUser();
            const confirmed = window.confirm("Se generara una venta con los items actuales del carrito. Continuar?");
            if (!confirmed) {
                return;
            }

            const done = setLoading(refs.checkout, "Procesando...");

            try {
                const response = await apiFetch(`/ventas/usuario/${user.id}/checkout`, {
                    method: "POST"
                });

                const saleId = response && response.venta ? response.venta.ventaId : null;
                if (saleId) {
                    showToast(`Compra realizada. Venta #${saleId}.`, "success");
                } else {
                    showToast("Compra realizada correctamente.", "success");
                }

                await Promise.all([loadCart(), loadProducts()]);
            } catch (error) {
                showToast(error.message, "error");
            } finally {
                done();
            }
        }
    }

    function initSalesPage() {
        if (!requireAuth()) {
            return;
        }

        const refs = {
            loadMy: byId("load-my-sales"),
            loadAll: byId("load-all-sales"),
            list: byId("sales-list")
        };

        const salesState = {
            mode: "user",
            sales: []
        };

        if (isAdmin() && refs.loadAll) {
            refs.loadAll.classList.remove("hidden");
        }

        if (refs.loadMy) {
            refs.loadMy.addEventListener("click", () => loadSales("user"));
        }

        if (refs.loadAll && isAdmin()) {
            refs.loadAll.addEventListener("click", () => loadSales("all"));
        }

        if (refs.list) {
            refs.list.addEventListener("click", onSalesAction);
        }

        loadSales("user");

        async function loadSales(mode) {
            salesState.mode = mode;
            toggleModeButtons();

            const user = getUser();
            const endpoint = mode === "all" && isAdmin()
                ? "/ventas"
                : `/ventas/usuario/${user.id}`;

            try {
                const response = await apiFetch(endpoint);
                salesState.sales = Array.isArray(response) ? response : [];
                renderSales();
            } catch (error) {
                refs.list.innerHTML = `<div class="empty-box">${escapeHtml(error.message)}</div>`;
                showToast(error.message, "error");
            }
        }

        function toggleModeButtons() {
            if (!refs.loadMy) {
                return;
            }

            refs.loadMy.classList.toggle("btn-primary", salesState.mode === "user");
            refs.loadMy.classList.toggle("btn-light", salesState.mode !== "user");

            if (refs.loadAll && isAdmin()) {
                refs.loadAll.classList.toggle("btn-primary", salesState.mode === "all");
                refs.loadAll.classList.toggle("btn-light", salesState.mode !== "all");
            }
        }

        function renderSales() {
            if (!refs.list) {
                return;
            }

            if (!salesState.sales.length) {
                refs.list.innerHTML = "<div class='empty-box'>No hay ventas para mostrar.</div>";
                return;
            }

            refs.list.innerHTML = salesState.sales.map((sale) => buildSaleCard(sale)).join("");
        }

        function buildSaleCard(sale) {
            const details = Array.isArray(sale.detalles) ? sale.detalles : [];
            const status = String(sale.estado || "CREADA").toUpperCase();
            const statusClass = status === "CANCELADA" ? "badge-off" : "badge-ok";

            const detailRows = details.length
                ? details.map((detail) => {
                    return `
                        <div class="sale-row">
                            <span>${escapeHtml(detail.producto)}</span>
                            <span>x${escapeHtml(detail.cantidad)}</span>
                            <span>${formatMoney(detail.subtotal)}</span>
                        </div>
                    `;
                }).join("")
                : "<p class='muted'>Sin detalles en esta venta.</p>";

            const adminTools = isAdmin()
                ? `
                    <div class="sale-toolbar">
                        <select data-role="status">
                            ${statusOption("CREADA", status)}
                            ${statusOption("PAGADA", status)}
                            ${statusOption("CANCELADA", status)}
                        </select>
                        <button class="btn btn-light" data-action="update-sale" data-id="${sale.ventaId}" type="button">Actualizar</button>
                        <button class="btn btn-danger" data-action="delete-sale" data-id="${sale.ventaId}" type="button" ${status !== "CANCELADA" ? "disabled" : ""}>Eliminar</button>
                    </div>
                `
                : "";

            return `
                <article class="sale-card" data-sale-id="${sale.ventaId}">
                    <div class="sale-head">
                        <h3>Venta #${escapeHtml(sale.ventaId)}</h3>
                        <span class="badge ${statusClass}">${escapeHtml(status)}</span>
                    </div>
                    <p class="muted"><strong>Usuario:</strong> ${escapeHtml(sale.usuarioId)} | <strong>Total:</strong> ${formatMoney(sale.total)}</p>
                    <p class="muted"><strong>Fecha:</strong> ${escapeHtml(formatDate(sale.fechaVenta))}</p>
                    <div class="sale-details">${detailRows}</div>
                    ${adminTools}
                </article>
            `;
        }

        function statusOption(status, current) {
            return `<option value="${status}" ${status === current ? "selected" : ""}>${status}</option>`;
        }

        async function onSalesAction(event) {
            const button = event.target.closest("button[data-action]");
            if (!button || !isAdmin()) {
                return;
            }

            const saleId = Number(button.dataset.id);
            if (!Number.isFinite(saleId)) {
                return;
            }

            if (button.dataset.action === "update-sale") {
                await updateSaleStatus(saleId, button);
            }

            if (button.dataset.action === "delete-sale") {
                await deleteSale(saleId, button);
            }
        }

        async function updateSaleStatus(saleId, sourceButton) {
            const card = sourceButton.closest(".sale-card");
            const selector = card ? card.querySelector("select[data-role='status']") : null;
            const status = selector ? selector.value : "";

            if (!status) {
                showToast("Selecciona un estado valido.", "error");
                return;
            }

            const done = setLoading(sourceButton, "Guardando...");

            try {
                await apiFetch(`/ventas/${saleId}/estado`, {
                    method: "PUT",
                    body: { estado: status }
                });

                showToast("Estado de venta actualizado.", "success");
                await loadSales(salesState.mode);
            } catch (error) {
                showToast(error.message, "error");
            } finally {
                done();
            }
        }

        async function deleteSale(saleId, sourceButton) {
            const confirmed = window.confirm("Solo se eliminan ventas canceladas. Deseas continuar?");
            if (!confirmed) {
                return;
            }

            const done = setLoading(sourceButton, "Eliminando...");

            try {
                await apiFetch(`/ventas/${saleId}`, {
                    method: "DELETE"
                });

                showToast("Venta eliminada correctamente.", "success");
                await loadSales(salesState.mode);
            } catch (error) {
                showToast(error.message, "error");
            } finally {
                done();
            }
        }
    }
})();
