// FIMACOR · Tablero de gerencia.
// Recibe del servidor una fila por maquina activa (window.GERENCIA_DATOS) y calcula en el navegador los indicadores,
// los graficos y la lista. Todo el aspecto sale de Bootstrap 5 y Material Symbols: aqui solo se arman elementos con
// sus clases. Sede y area filtran todo el tablero; tipo, estado, garantia y "pendiente" filtran el detalle.
(function () {
  const DATOS = window.GERENCIA_DATOS || { maquinas: [] };
  const TODAS = Array.isArray(DATOS.maquinas) ? DATOS.maquinas : [];
  const PAGINA = 25;

  const nf = new Intl.NumberFormat("es-CO");
  const pf = function (v) { return new Intl.NumberFormat("es-CO", { maximumFractionDigits: 1 }).format(v) + " %"; };
  const redondear1 = function (v) { return Math.round(v * 10) / 10; };
  const $ = function (id) { return document.getElementById(id); };

  function h(tag, clases, texto) {
    const e = document.createElement(tag);
    if (clases) e.className = clases;
    if (texto != null) e.textContent = texto;
    return e;
  }
  function icono(nombre, clases) {
    const s = h("span", "material-symbols-rounded" + (clases ? " " + clases : ""), nombre);
    s.setAttribute("aria-hidden", "true");
    return s;
  }

  const ESTADOS = {
    "FUNCIONAL": { label: "Funcional", color: "#2e7d32", badge: "text-bg-success" },
    "MANTENIMIENTO": { label: "En mantenimiento", color: "#ffc107", badge: "text-bg-warning" },
    "FUERA DE SERVICIO": { label: "Fuera de servicio", color: "#dc3545", badge: "text-bg-danger" }
  };
  const ORDEN_ESTADOS = ["FUNCIONAL", "MANTENIMIENTO", "FUERA DE SERVICIO"];
  function infoEstado(e) {
    if (ESTADOS[e]) return ESTADOS[e];
    const t = (e || "").toLowerCase();
    return { label: t ? t.charAt(0).toUpperCase() + t.slice(1) : "Sin estado", color: "#6c757d", badge: "text-bg-secondary" };
  }
  const GARANTIAS = {
    "Garantía vigente": "text-bg-success", "Garantía vencida": "text-bg-danger",
    "Sin garantía": "text-bg-secondary", "Garantía por revisar": "text-bg-warning"
  };
  const esFuncional = function (m) { return m.estado === "FUNCIONAL"; };

  const ANIO_ACTUAL = new Date().getFullYear();
  const fechaValida = function (m) { return !!m.fechaCompra && m.anioCompra >= 1950 && m.anioCompra <= ANIO_ACTUAL; };
  const conValor = function (m) { return m.valor != null && m.valor > 0; };

  // Datos que debe tener cada ficha (campos diligenciados y fichas completas).
  const CAMPOS = [
    { nombre: "Código SENA", tiene: function (m) { return !!m.codigo; } },
    { nombre: "Descripción", tiene: function (m) { return !!m.descripcion; } },
    { nombre: "Tipo de máquina", tiene: function (m) { return !!m.tipo; } },
    { nombre: "Marca", tiene: function (m) { return !!m.marca; } },
    { nombre: "Modelo", tiene: function (m) { return !!m.modelo; } },
    { nombre: "Estado", tiene: function (m) { return !!m.estado; } },
    { nombre: "Sede", tiene: function (m) { return !!m.sede; } },
    { nombre: "Área", tiene: function (m) { return !!m.area; } },
    { nombre: "Ambiente", tiene: function (m) { return !!m.ambiente; } },
    { nombre: "Fecha de compra", tiene: fechaValida },
    { nombre: "Valor de la máquina", tiene: conValor },
    { nombre: "Cuentadante asignado", tiene: function (m) { return !!m.cuentadante; } }
  ];
  const fichaCompleta = function (m) { return CAMPOS.every(function (c) { return c.tiene(m); }); };

  // Pendientes por corregir: cada regla dice que maquinas tienen el problema.
  const PENDIENTES = [
    { tema: "Garantía con dato por revisar", aplica: function (m) { return m.estadoGarantia === "Garantía por revisar"; } },
    { tema: "Sin fecha de compra válida", aplica: function (m) { return !fechaValida(m); } },
    { tema: "Sin valor de la máquina", aplica: function (m) { return !conValor(m); } },
    { tema: "Sin cuentadante", aplica: function (m) { return !m.cuentadante; } },
    { tema: "Sin marca o sin modelo", aplica: function (m) { return !m.marca || !m.modelo; } },
    { tema: "Sin área", aplica: function (m) { return !m.area; } }
  ];

  const COLS = [
    ["id", "#", false, "num"], ["codigo", "Código SENA", true], ["descripcion", "Descripción", false], ["tipo", "Tipo de máquina", true],
    ["marca", "Marca", true], ["modelo", "Modelo", false], ["estado", "Estado", true], ["sede", "Sede", false], ["area", "Área", false],
    ["ambiente", "Ambiente", true], ["fechaCompra", "Fecha de compra", false], ["estadoGarantia", "Garantía", true],
    ["venceGarantia", "Vence la garantía", true], ["valor", "Valor (COP)", false], ["cuentadante", "Cuentadante", false],
    ["cuentadanteDocumento", "Documento del cuentadante", false]
  ];

  const p = { sede: null, area: null, tipo: null, estado: null, garantia: null, pendiente: null };
  const st = { q: "", sort: "codigo", dir: 1, limite: PAGINA, vis: new Set(COLS.filter(function (c) { return c[2]; }).map(function (c) { return c[0]; })) };

  const unicos = function (rows, k) {
    const s = new Set();
    rows.forEach(function (m) { if (m[k]) s.add(m[k]); });
    return Array.from(s).sort(function (a, b) { return String(a).localeCompare(String(b), "es"); });
  };
  const contar = function (rows, k) {
    const c = new Map();
    rows.forEach(function (m) { const v = m[k]; if (v) c.set(v, (c.get(v) || 0) + 1); });
    return Array.from(c, function (e) { return { nombre: e[0], total: e[1] }; }).sort(function (a, b) { return b.total - a.total || String(a.nombre).localeCompare(String(b.nombre), "es"); });
  };
  const baseFiltrada = function () {
    return TODAS.filter(function (m) { return (!p.sede || m.sede === p.sede) && (!p.area || m.area === p.area); });
  };

  // Ir al detalle con un filtro nuevo; siempre vuelve a la primera pagina.
  function irAlDetalle(fn) {
    p.tipo = p.estado = p.garantia = p.pendiente = null;
    fn();
    st.limite = PAGINA;
    render();
    const b = $("btn-tab-detalle");
    if (window.bootstrap && b) window.bootstrap.Tab.getOrCreateInstance(b).show();
    $("pestanas").scrollIntoView({ behavior: "smooth", block: "start" });
  }
  function cambiar(fn) { fn(); st.limite = PAGINA; render(); }

  // ---------- filtros sede y area ----------
  function llenarSelect(sel, textoTodas, valores, actual) {
    const todas = h("option", null, textoTodas); todas.value = "";
    sel.replaceChildren(todas, ...valores.map(function (v) { const o = h("option", null, v); o.value = v; return o; }));
    sel.value = valores.indexOf(actual) >= 0 ? actual : "";
  }
  function dibujarFiltros() {
    const sedes = unicos(TODAS, "sede");
    llenarSelect($("fSede"), "Todas", sedes, p.sede);
    p.sede = $("fSede").value || null;
    llenarSelect($("fArea"), "Todas", unicos(TODAS.filter(function (m) { return !p.sede || m.sede === p.sede; }), "area"), p.area);
    p.area = $("fArea").value || null;
    $("chip-sede").textContent = p.sede ? p.sede : (sedes.length === 1 ? sedes[0] : "Todas las sedes");
    $("chip-area").textContent = p.area ? p.area : "Todas las áreas";
  }

  // ---------- indicadores ----------
  function dibujarIndicadores(base) {
    const total = base.length;
    const func = base.filter(esFuncional).length;
    const fuera = total - func;
    const completas = base.filter(fichaCompleta).length;
    const pctFunc = total ? redondear1(func / total * 100) : 0;
    const pctCompletas = total ? redondear1(completas / total * 100) : 0;
    $("k-total").textContent = nf.format(total);
    $("k-func").textContent = pf(pctFunc);
    $("k-func-s").textContent = nf.format(func) + " máquinas funcionales";
    $("k-nofunc").textContent = nf.format(fuera);
    $("k-listas").textContent = pf(pctCompletas);
    $("k-listas-s").textContent = nf.format(completas) + " con todos los datos";
    const sinCta = base.filter(function (m) { return !m.cuentadante; }).length;
    $("takeaway").textContent = total
      ? nf.format(total) + " máquinas inventariadas, " + pf(pctFunc) + " funcionales. "
        + (fuera ? nf.format(fuera) + (fuera === 1 ? " está fuera de servicio" : " están fuera de servicio") + ". " : "")
        + (sinCta ? "Faltan por asignar cuentadante a " + nf.format(sinCta) + "." : "Todas tienen cuentadante asignado.")
      : "No hay máquinas registradas con estos filtros.";
  }

  // ---------- estado operativo (dona) ----------
  function dibujarEstado(base) {
    const c = contar(base, "estado").sort(function (a, b) { return ORDEN_ESTADOS.indexOf(a.nombre) - ORDEN_ESTADOS.indexOf(b.nombre); });
    const total = base.length;
    const donut = $("donut"), centro = $("donut-centro");
    donut.style.width = "10rem"; donut.style.height = "10rem";
    centro.style.width = "6.6rem"; centro.style.height = "6.6rem";
    if (!total) { donut.style.background = "#e9ecef"; $("donut-pct").textContent = "—"; $("estado-lista").replaceChildren(); return; }
    let acum = 0;
    donut.style.background = "conic-gradient(" + c.map(function (r) {
      const ini = acum; acum += r.total / total * 100;
      return infoEstado(r.nombre).color + " " + ini + "% " + acum + "%";
    }).join(", ") + ")";
    const f = c.find(function (r) { return r.nombre === "FUNCIONAL"; });
    $("donut-pct").textContent = pf(redondear1((f ? f.total : 0) / total * 100));
    donut.setAttribute("aria-label", c.map(function (r) { return infoEstado(r.nombre).label + ": " + r.total; }).join(", "));
    $("estado-lista").replaceChildren(...c.map(function (r) {
      const i = infoEstado(r.nombre);
      const b = h("button", "list-group-item list-group-item-action d-flex align-items-center gap-3 border-0 rounded-3");
      b.type = "button";
      const punto = h("span", "rounded-circle d-inline-block flex-shrink-0"); punto.style.width = "0.9rem"; punto.style.height = "0.9rem"; punto.style.background = i.color;
      b.append(punto, h("span", "flex-grow-1 fw-semibold", i.label), h("span", "badge rounded-pill text-bg-light border", nf.format(r.total)), h("span", "small text-secondary", pf(redondear1(r.total / total * 100))));
      b.addEventListener("click", function () { irAlDetalle(function () { p.estado = r.nombre; }); });
      return b;
    }));
  }

  // ---------- año de compra: columnas con Bootstrap ----------
  function dibujarAnios(base) {
    const el = $("anio-grafico"), nota = $("anio-nota");
    const c = new Map();
    base.forEach(function (m) { if (fechaValida(m)) c.set(m.anioCompra, (c.get(m.anioCompra) || 0) + 1); });
    const filas = Array.from(c, function (e) { return { anio: e[0], total: e[1] }; }).sort(function (a, b) { return a.anio - b.anio; });
    if (!filas.length) {
      el.replaceChildren(h("p", "text-secondary small mb-0", "Todavía no hay fechas de compra registradas."));
      nota.textContent = "Máquinas por año de compra. " + nf.format(base.length) + " sin fecha de compra válida.";
      return;
    }
    const max = Math.max(1, ...filas.map(function (r) { return r.total; }));
    const cols = h("div", "d-flex align-items-end gap-1"); cols.style.height = "11rem";
    const rotulos = h("div", "d-flex gap-1 mt-1");
    const paso = Math.max(1, Math.ceil(filas.length / 8));
    filas.forEach(function (r, i) {
      const col = h("div", "flex-fill d-flex align-items-end h-100");
      col.title = "Compradas en " + r.anio + ": " + r.total + (r.total === 1 ? " máquina" : " máquinas");
      const barra = h("div", "w-100 rounded-top " + (r.anio <= 2012 ? "bg-dark" : "bg-success")); barra.style.height = Math.max(3, r.total / max * 100) + "%";
      col.append(barra); cols.append(col);
      rotulos.append(h("div", "flex-fill text-center small text-secondary overflow-hidden", i % paso === 0 ? String(r.anio) : ""));
    });
    el.replaceChildren(cols, rotulos);
    const con = filas.reduce(function (s, r) { return s + r.total; }, 0);
    const viejas = filas.filter(function (r) { return r.anio <= 2012; }).reduce(function (s, r) { return s + r.total; }, 0);
    nota.textContent = "En negro, compradas en 2012 o antes: " + nf.format(viejas) + " (" + pf(redondear1(viejas / con * 100)) + "). "
      + nf.format(base.length - con) + " sin fecha de compra válida.";
  }

  // ---------- calidad de datos ----------
  function dibujarCalidad(base) {
    const filas = CAMPOS.map(function (c) {
      const ok = base.filter(c.tiene).length;
      return { c: c, ok: ok, pct: base.length ? ok / base.length * 100 : 0 };
    }).sort(function (a, b) { return b.pct - a.pct; });
    $("calidad-lista").replaceChildren(...filas.map(function (r) {
      const tono = r.pct >= 95 ? "success" : r.pct >= 80 ? "warning" : "danger";
      const fila = h("div", "list-group-item border-0 px-2");
      const sup = h("div", "d-flex align-items-center justify-content-between gap-2 mb-1 small");
      sup.append(h("span", "fw-semibold", r.c.nombre), h("span", "fw-bold", Math.round(r.pct) + " %"));
      const barra = h("div", "progress"); barra.setAttribute("role", "progressbar"); barra.setAttribute("aria-valuenow", Math.round(r.pct)); barra.setAttribute("aria-valuemin", 0); barra.setAttribute("aria-valuemax", 100);
      barra.setAttribute("aria-label", r.c.nombre + ": " + Math.round(r.pct) + " %"); barra.title = nf.format(r.ok) + " de " + nf.format(base.length);
      const relleno = h("div", "progress-bar bg-" + tono); relleno.style.width = r.pct + "%"; barra.append(relleno);
      fila.append(sup, barra);
      return fila;
    }));

    $("pend-lista").replaceChildren(...PENDIENTES.map(function (r) {
      const n = base.filter(r.aplica).length;
      const fila = h(n ? "button" : "div", "list-group-item d-flex justify-content-between align-items-center gap-2 border-0 rounded-3" + (n ? " list-group-item-action" : ""));
      if (n) { fila.type = "button"; fila.addEventListener("click", function () { irAlDetalle(function () { p.pendiente = r.tema; }); }); }
      fila.append(h("span", "fw-semibold", r.tema),
        n ? h("span", "badge rounded-pill text-bg-warning", nf.format(n) + (n === 1 ? " registro" : " registros")) : h("span", "badge rounded-pill text-bg-success", "Al día"));
      return fila;
    }));
  }

  // ---------- listas con barras ----------
  function barras(el, filas, opciones) {
    if (!filas.length) { el.replaceChildren(h("p", "text-secondary small mb-0", "Todavía no hay datos para mostrar.")); return; }
    const max = Math.max(1, ...filas.map(function (r) { return r.total; }));
    el.replaceChildren(...filas.map(function (r) {
      const fila = h(opciones.clic ? "button" : "div", "d-block w-100 text-start border-0 rounded-3 p-2 mb-1" + (opciones.clic ? " bg-transparent" : ""));
      if (opciones.clic) { fila.type = "button"; fila.addEventListener("click", function () { opciones.clic(r.nombre); }); }
      const sup = h("div", "d-flex justify-content-between gap-2 small mb-1");
      sup.append(h("span", "text-truncate fw-semibold", r.nombre), h("span", "fw-bold", nf.format(r.total)));
      const barra = h("div", "progress"); barra.style.height = "0.6rem";
      const relleno = h("div", "progress-bar " + (opciones.fondo || "bg-success")); relleno.style.width = (r.total / max * 100) + "%"; barra.append(relleno);
      fila.append(sup, barra); fila.title = r.nombre + ": " + r.total;
      return fila;
    }));
  }

  // ---------- detalle ----------
  const fmt = function (m, k) {
    if (k === "valor") return m.valor ? nf.format(m.valor) : "";
    if (k === "estado") return infoEstado(m.estado).label;
    return m[k] == null ? "" : String(m[k]);
  };
  function filasDetalle(base) {
    const regla = p.pendiente ? PENDIENTES.find(function (r) { return r.tema === p.pendiente; }) : null;
    let rs = base.filter(function (m) {
      if (p.tipo && m.tipo !== p.tipo) return false;
      if (regla && !regla.aplica(m)) return false;
      if (p.garantia && m.estadoGarantia !== p.garantia) return false;
      if (p.estado === "NOFUNC") return !esFuncional(m);
      return !p.estado || m.estado === p.estado;
    });
    if (st.q) rs = rs.filter(function (m) { return COLS.map(function (c) { return fmt(m, c[0]); }).join(" ").toLowerCase().indexOf(st.q) >= 0; });
    const k = st.sort;
    return rs.slice().sort(function (a, b) {
      const x = a[k], y = b[k];
      if (x == null || x === "") return 1;
      if (y == null || y === "") return -1;
      return (typeof x === "number" && typeof y === "number" ? x - y : String(x).localeCompare(String(y), "es", { numeric: true })) * st.dir;
    });
  }
  function dibujarEtiquetas() {
    $("sel-estado").value = p.estado || "";
    $("sel-gar").value = p.garantia || "";
    const tags = [];
    if (p.tipo) tags.push(["tipo", "Tipo: " + p.tipo]);
    if (p.pendiente) tags.push(["pendiente", "Pendiente: " + p.pendiente]);
    $("etiquetas").replaceChildren(...tags.map(function (t) {
      const b = h("button", "badge rounded-pill text-bg-success border-0 d-inline-flex align-items-center gap-1 py-2 px-3", t[1]);
      b.type = "button"; b.append(icono("close", "fs-6")); b.setAttribute("aria-label", "Quitar filtro " + t[1]);
      b.addEventListener("click", function () { cambiar(function () { p[t[0]] = null; }); });
      return b;
    }));
  }
  function dibujarColumnas() {
    $("cols-list").replaceChildren(...COLS.filter(function (c) { return c[0] !== "id"; }).map(function (c) {
      const d = h("div", "form-check");
      const i = h("input", "form-check-input"); i.type = "checkbox"; i.id = "col-" + c[0]; i.checked = st.vis.has(c[0]);
      i.addEventListener("change", function () { if (i.checked) st.vis.add(c[0]); else st.vis.delete(c[0]); render(); });
      const l = h("label", "form-check-label", c[1]); l.setAttribute("for", i.id);
      d.append(i, l); return d;
    }));
  }
  function celda(m, k) {
    if (k === "estado") { const i = infoEstado(m.estado); return h("span", "badge rounded-pill " + i.badge, i.label); }
    if (k === "estadoGarantia") return h("span", "badge rounded-pill " + (GARANTIAS[m.estadoGarantia] || "text-bg-secondary"), m.estadoGarantia || "—");
    if (k === "cuentadante" && !m.cuentadante) return h("span", "text-warning-emphasis fw-semibold", "Sin cuentadante");
    return document.createTextNode(fmt(m, k));
  }
  function dibujarDetalle(base) {
    dibujarEtiquetas();
    dibujarColumnas();
    const cols = COLS.filter(function (c) { return st.vis.has(c[0]); });
    $("thead-row").replaceChildren(...cols.map(function (c) {
      const th = h("th", "text-nowrap" + (c[3] === "num" || c[0] === "valor" ? " text-end" : "")); th.setAttribute("scope", "col"); th.setAttribute("role", "button");
      th.setAttribute("aria-sort", st.sort === c[0] ? (st.dir > 0 ? "ascending" : "descending") : "none");
      th.append(document.createTextNode(c[1]));
      if (st.sort === c[0]) th.append(icono(st.dir > 0 ? "arrow_upward" : "arrow_downward", "fs-6 align-middle ms-1"));
      th.addEventListener("click", function () { st.dir = st.sort === c[0] ? -st.dir : 1; st.sort = c[0]; st.limite = PAGINA; render(); });
      return th;
    }));
    const rs = filasDetalle(base);
    $("conteo").textContent = nf.format(rs.length) + (rs.length === 1 ? " máquina" : " máquinas") + (rs.length > st.limite ? " (mostrando " + st.limite + ")" : "");
    const visibles = rs.slice(0, st.limite);

    const filasTabla = visibles.map(function (m) {
      const tr = h("tr"); tr.style.cursor = "pointer"; tr.tabIndex = 0;
      tr.addEventListener("click", function () { abrirFicha(m); });
      tr.addEventListener("keydown", function (e) { if (e.key === "Enter") abrirFicha(m); });
      cols.forEach(function (c) {
        const td = h("td", c[0] === "valor" || c[0] === "id" ? "text-end text-nowrap" : c[0] === "descripcion" ? "text-truncate" : "text-nowrap");
        if (c[0] === "descripcion") { td.style.maxWidth = "16rem"; td.title = fmt(m, c[0]); }
        td.append(celda(m, c[0]));
        tr.append(td);
      });
      return tr;
    });
    if (!rs.length) { const tr = h("tr"); const td = h("td", "text-center text-secondary py-4", "No hay máquinas con estos filtros."); td.colSpan = Math.max(1, cols.length); tr.append(td); filasTabla.push(tr); }
    $("tabla").querySelector("tbody").replaceChildren(...filasTabla);

    const tarjetas = visibles.map(function (m) {
      const a = h("button", "list-group-item list-group-item-action text-start"); a.type = "button";
      const sup = h("div", "d-flex justify-content-between align-items-start gap-2");
      sup.append(h("span", "fw-bold", m.codigo || ("#" + m.id)), celda(m, "estado"));
      const l2 = h("div", "small", [m.tipo, m.marca, m.modelo].filter(Boolean).join(" · "));
      const l3 = h("div", "small text-secondary d-flex align-items-center gap-1");
      l3.append(icono("location_on", "fs-6"), document.createTextNode(m.ambiente || "Sin ambiente"));
      const l4 = h("div", "small d-flex align-items-center gap-1 mt-1");
      l4.append(celda(m, "estadoGarantia"));
      a.append(sup, l2, l3, l4);
      a.addEventListener("click", function () { abrirFicha(m); });
      return a;
    });
    if (!rs.length) tarjetas.push(h("div", "list-group-item text-center text-secondary py-4", "No hay máquinas con estos filtros."));
    $("tarjetas").replaceChildren(...tarjetas);

    $("mas-caja").hidden = rs.length <= st.limite;
    $("mas").textContent = "Ver " + Math.min(PAGINA, rs.length - st.limite) + " máquinas más";
  }

  // ---------- ficha de una maquina ----------
  function abrirFicha(m) {
    $("fichaTitulo").textContent = m.codigo ? "Máquina " + m.codigo : "Máquina #" + m.id;
    const i = infoEstado(m.estado); const e = $("fichaEstado"); e.textContent = i.label; e.className = "badge rounded-pill " + i.badge;
    $("fichaDatos").replaceChildren(...COLS.filter(function (c) { return c[0] !== "estado" && c[0] !== "id"; }).flatMap(function (c) {
      const v = fmt(m, c[0]);
      return [h("dt", "col-5 small text-secondary fw-semibold py-2 border-bottom", c[1]),
        h("dd", "col-7 py-2 border-bottom mb-0 text-break" + (v ? "" : " text-secondary fst-italic"), v || "Sin dato")];
    }));
    if (window.bootstrap) window.bootstrap.Offcanvas.getOrCreateInstance($("fichaPanel")).show();
  }

  function render() {
    const base = baseFiltrada();
    if (p.tipo && !base.some(function (m) { return m.tipo === p.tipo; })) p.tipo = null;
    dibujarFiltros();
    dibujarIndicadores(base);
    dibujarEstado(base);
    dibujarAnios(base);
    dibujarCalidad(base);
    barras($("tipo-lista"), contar(base, "tipo"), { clic: function (v) { irAlDetalle(function () { p.tipo = v; }); } });
    barras($("ubic-lista"), contar(base, "ambiente"), { fondo: "bg-success" });
    barras($("marca-lista"), contar(base, "marca").slice(0, 8), { fondo: "bg-dark" });
    dibujarDetalle(base);
  }

  // ---------- eventos ----------
  $("fSede").addEventListener("change", function (e) { cambiar(function () { p.sede = e.target.value || null; p.area = null; }); });
  $("fArea").addEventListener("change", function (e) { cambiar(function () { p.area = e.target.value || null; }); });
  $("sel-estado").addEventListener("change", function (e) { cambiar(function () { p.estado = e.target.value || null; }); });
  $("sel-gar").addEventListener("change", function (e) { cambiar(function () { p.garantia = e.target.value || null; }); });
  $("ver-nofunc").addEventListener("click", function (e) { e.preventDefault(); irAlDetalle(function () { p.estado = "NOFUNC"; }); });
  $("buscar").addEventListener("input", function (e) { st.q = e.target.value.trim().toLowerCase(); st.limite = PAGINA; render(); });
  $("mas").addEventListener("click", function () { st.limite += PAGINA; render(); });
  $("cols-all").addEventListener("click", function () { st.vis = new Set(COLS.map(function (c) { return c[0]; })); render(); });
  $("cols-basic").addEventListener("click", function () { st.vis = new Set(COLS.filter(function (c) { return c[2]; }).map(function (c) { return c[0]; })); render(); });

  // Se puede entrar directo a una pestaña con /gerencia#detalle
  const pestana = { resumen: "btn-tab-resumen", calidad: "btn-tab-calidad", distribucion: "btn-tab-distribucion", detalle: "btn-tab-detalle" }[location.hash.slice(1)];
  render();
  if (pestana && window.bootstrap) window.bootstrap.Tab.getOrCreateInstance($(pestana)).show();
})();
