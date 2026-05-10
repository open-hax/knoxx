shadow$provide.module$dist$bridge$knoxx_frontend_bridge_es = function(require, module, exports) {
  function Pe(e) {
    e = e.replace("#", "").trim();
    return e.length === 3 ? e.split("").map(a => `${a}${a}`).join("") : e;
  }
  function r(e, n) {
    var a = Pe(e);
    if (a.length !== 6) {
      return e;
    }
    e = parseInt(a.slice(0, 2), 16);
    const d = parseInt(a.slice(2, 4), 16);
    a = parseInt(a.slice(4, 6), 16);
    n = Number(n.toFixed(2));
    return `rgba(${e}, ${d}, ${a}, ${String(n)})`;
  }
  function $e(e, n) {
    if (!n) {
      return e;
    }
    e = {...e};
    for (const [l, d] of Object.entries(n)) {
      n = e[l], d && typeof d == "object" && !Array.isArray(d) && n && typeof n == "object" && !Array.isArray(n) ? e[l] = $e(n, d) : e[l] = d;
    }
    return e;
  }
  function ze(e) {
    return e.replace(/([A-Z])/g, "-$1").toLowerCase();
  }
  function Ge(e, n) {
    return `var(--uxx-${e.map(ze).join("-")}, ${n})`;
  }
  function Ie(e, n = []) {
    return typeof e == "string" ? Ge(n, e) : typeof e == "object" && e !== null ? (e = Object.entries(e).map(([l, d]) => [l, Ie(d, [...n, l])]), Object.fromEntries(e)) : e;
  }
  function pe(e, n, a$jscomp$0, l) {
    var JSCompiler_temp_const = (l == null ? void 0 : l.appearance) ?? "dark";
    l = l == null ? void 0 : l.colorOverrides;
    const a = {background:{default:a$jscomp$0.bg.default, surface:a$jscomp$0.bg.darker, elevated:a$jscomp$0.bg.tabInactive, highlight:a$jscomp$0.bg.lighter, overlay:"rgba(0, 0, 0, 0.6)"}, selection:{default:r(a$jscomp$0.bg.selection, 0.5)}, text:{default:a$jscomp$0.fg.default, bright:a$jscomp$0.fg.bright, panel:a$jscomp$0.fg.panel, soft:a$jscomp$0.fg.soft, muted:a$jscomp$0.fg.muted, subtle:a$jscomp$0.fg.subtle, inverse:a$jscomp$0.bg.default, secondary:a$jscomp$0.fg.muted}, interactive:{default:a$jscomp$0.accent.cyan, 
    hover:r(a$jscomp$0.accent.cyan, 0.88), active:r(a$jscomp$0.accent.cyan, 0.72), disabled:a$jscomp$0.fg.muted}, button:{primary:{bg:a$jscomp$0.accent.cyan, fg:a$jscomp$0.bg.default, hover:r(a$jscomp$0.accent.cyan, 0.88), active:r(a$jscomp$0.accent.cyan, 0.72)}, secondary:{bg:a$jscomp$0.bg.selection, fg:a$jscomp$0.fg.default, hover:r(a$jscomp$0.bg.selection, 0.88), active:r(a$jscomp$0.bg.selection, 0.72)}, ghost:{bg:"transparent", fg:a$jscomp$0.fg.default, hover:r(a$jscomp$0.fg.default, 0.08), active:r(a$jscomp$0.fg.default, 
    0.14)}, danger:{bg:a$jscomp$0.accent.red, fg:a$jscomp$0.fg.default, hover:r(a$jscomp$0.accent.red, 0.88), active:r(a$jscomp$0.accent.red, 0.72)}}, badge:{default:{bg:a$jscomp$0.fg.muted, fg:a$jscomp$0.fg.default}, success:{bg:r(a$jscomp$0.accent.green, 0.15), fg:a$jscomp$0.accent.green}, warning:{bg:r(a$jscomp$0.accent.orange, 0.15), fg:a$jscomp$0.accent.orange}, error:{bg:r(a$jscomp$0.accent.red, 0.15), fg:a$jscomp$0.accent.red}, info:{bg:r(a$jscomp$0.accent.blue, 0.15), fg:a$jscomp$0.accent.blue}}, 
    border:{default:a$jscomp$0.bg.groupBorder, subtle:a$jscomp$0.fg.subtle, focus:a$jscomp$0.accent.cyan, error:a$jscomp$0.accent.red}, accent:a$jscomp$0.accent, semantic:{error:a$jscomp$0.semantic.error, warning:a$jscomp$0.semantic.warning, success:a$jscomp$0.semantic.success, info:a$jscomp$0.semantic.info}, status:{alive:a$jscomp$0.accent.green, dead:a$jscomp$0.accent.red, open:a$jscomp$0.accent.green, closed:a$jscomp$0.fg.muted, merged:a$jscomp$0.accent.magenta, sleeping:a$jscomp$0.accent.blue, 
    eating:a$jscomp$0.accent.orange, working:a$jscomp$0.accent.yellow}, chart:{segment0:a$jscomp$0.accent.blue, segment1:a$jscomp$0.accent.green, segment2:a$jscomp$0.accent.yellow, segment3:a$jscomp$0.accent.orange, segment4:a$jscomp$0.accent.magenta, segment5:a$jscomp$0.fg.soft}, fill:{good:{start:a$jscomp$0.accent.green, end:r(a$jscomp$0.accent.green, 0.55)}, warn:{start:a$jscomp$0.accent.orange, end:r(a$jscomp$0.accent.orange, 0.55)}, danger:{start:a$jscomp$0.accent.red, end:r(a$jscomp$0.accent.red, 
    0.55)}, neutral:{start:a$jscomp$0.accent.blue, end:r(a$jscomp$0.accent.blue, 0.55)}}, surface:{panel:r(a$jscomp$0.bg.darker, 0.82), card:r(a$jscomp$0.bg.tabInactive, 0.65), cardAlt:r(a$jscomp$0.bg.lighter, 0.55), input:r(a$jscomp$0.bg.selection, 0.78), nav:r(a$jscomp$0.bg.darker, 0.6)}, alpha:{green:{_08:r(a$jscomp$0.accent.green, 0.08), _12:r(a$jscomp$0.accent.green, 0.12), _14:r(a$jscomp$0.accent.green, 0.14), _15:r(a$jscomp$0.accent.green, 0.15), _16:r(a$jscomp$0.accent.green, 0.16), _25:r(a$jscomp$0.accent.green, 
    0.25), _28:r(a$jscomp$0.accent.green, 0.28), _30:r(a$jscomp$0.accent.green, 0.3), _35:r(a$jscomp$0.accent.green, 0.35), _38:r(a$jscomp$0.accent.green, 0.38), _40:r(a$jscomp$0.accent.green, 0.4), _45:r(a$jscomp$0.accent.green, 0.45), _50:r(a$jscomp$0.accent.green, 0.5), _55:r(a$jscomp$0.accent.green, 0.55), _60:r(a$jscomp$0.accent.green, 0.6), _80:r(a$jscomp$0.accent.green, 0.8)}, red:{_12:r(a$jscomp$0.accent.red, 0.12), _14:r(a$jscomp$0.accent.red, 0.14), _15:r(a$jscomp$0.accent.red, 0.15), _25:r(a$jscomp$0.accent.red, 
    0.25), _30:r(a$jscomp$0.accent.red, 0.3), _38:r(a$jscomp$0.accent.red, 0.38), _40:r(a$jscomp$0.accent.red, 0.4), _45:r(a$jscomp$0.accent.red, 0.45), _46:r(a$jscomp$0.accent.red, 0.46), _50:r(a$jscomp$0.accent.red, 0.5)}, orange:{_12:r(a$jscomp$0.accent.orange, 0.12), _15:r(a$jscomp$0.accent.orange, 0.15), _32:r(a$jscomp$0.accent.orange, 0.32), _35:r(a$jscomp$0.accent.orange, 0.35), _40:r(a$jscomp$0.accent.orange, 0.4)}, blue:{_15:r(a$jscomp$0.accent.blue, 0.15), _20:r(a$jscomp$0.accent.blue, 
    0.2), _35:r(a$jscomp$0.accent.blue, 0.35), _45:r(a$jscomp$0.accent.blue, 0.45), _80:r(a$jscomp$0.accent.blue, 0.8), _95:r(a$jscomp$0.accent.blue, 0.95)}, magenta:{_08:r(a$jscomp$0.accent.magenta, 0.08), _14:r(a$jscomp$0.accent.magenta, 0.14), _30:r(a$jscomp$0.accent.magenta, 0.3)}, yellow:{_06:r(a$jscomp$0.accent.yellow, 0.06)}, bg:{_08:r(a$jscomp$0.bg.selection, 0.08), _10:r(a$jscomp$0.bg.selection, 0.1), _12:r(a$jscomp$0.bg.selection, 0.12), _14:r(a$jscomp$0.bg.selection, 0.14), _16:r(a$jscomp$0.bg.selection, 
    0.16), _18:r(a$jscomp$0.bg.selection, 0.18), _24:r(a$jscomp$0.bg.lighter, 0.24), _25:r(a$jscomp$0.bg.selection, 0.25), _28:r(a$jscomp$0.bg.selection, 0.28), _30:r(a$jscomp$0.bg.selection, 0.3), _46:r(a$jscomp$0.bg.darker, 0.46), _55:r(a$jscomp$0.bg.selection, 0.55), _60:r(a$jscomp$0.bg.darker, 0.6), _62:r(a$jscomp$0.bg.darker, 0.62), _68:r(a$jscomp$0.bg.darker, 0.68), _70:r(a$jscomp$0.bg.selection, 0.7), _72:r(a$jscomp$0.bg.darker, 0.72), _80:r(a$jscomp$0.bg.darker, 0.8), _85:r(a$jscomp$0.bg.lighter, 
    0.85), _88:r(a$jscomp$0.bg.lighter, 0.88), _88b:r(a$jscomp$0.bg.default, 0.88), _90:r(a$jscomp$0.bg.darker, 0.9), _95:r(a$jscomp$0.bg.darker, 0.95)}, warningBg:r(a$jscomp$0.accent.orange, 0.2), errorBg:r(a$jscomp$0.accent.red, 0.42), errorBgSolid:r(a$jscomp$0.accent.red, 0.9), federationError:r(a$jscomp$0.accent.red, 0.22), white:{_08:"rgba(255, 255, 255, 0.08)"}, shadow:"rgba(0, 0, 0, 0.35)", shadowLight:"rgba(0, 0, 0, 0.3)", shadowDeep:"rgba(15, 23, 42, 0.22)"}};
    l = $e(a, l);
    return {name:e, label:n, appearance:JSCompiler_temp_const, palette:a$jscomp$0, colors:l};
  }
  function se(e) {
    return Object.values(X).find(n => n.appearance === e) ?? X.monokai;
  }
  function Ze(e = "monokai") {
    return e === "dark" ? se("dark").name : e === "light" ? se("light").name : e === "auto" ? typeof window < "u" && typeof window.matchMedia == "function" ? window.matchMedia("(prefers-color-scheme: light)").matches ? se("light").name : se("dark").name : "monokai" : e;
  }
  function We(e = "monokai") {
    return X[e];
  }
  function He(e = "monokai") {
    return We(Ze(e));
  }
  function ye(e) {
    return typeof e == "object" && e !== null && !Array.isArray(e);
  }
  function Te(e, n) {
    if (!n) {
      return structuredClone(e);
    }
    e = structuredClone(e);
    for (const [l, d] of Object.entries(n)) {
      d !== void 0 && (n = e[l], ye(n) && ye(d) ? e[l] = Te(n, d) : e[l] = d);
    }
    return e;
  }
  function me(e, n) {
    return Te(e, n);
  }
  function J(e, n = []) {
    const a = {};
    for (const [l, d] of Object.entries(e)) {
      e = [...n, l], ye(d) ? a[l] = J(d, e) : a[l] = `var(${`--uxx-${e.map(ze).join("-")}`}, ${String(d)})`;
    }
    return a;
  }
  function dt({size:e}) {
    e = e === "sm" ? 14 : e === "lg" ? 20 : 16;
    return (0,require$react_SLASH_jsx_runtime.jsx)("svg", {width:e, height:e, viewBox:"0 0 24 24", fill:"none", stroke:"currentColor", strokeWidth:"2", strokeLinecap:"round", strokeLinejoin:"round", style:{animation:"spin 1s linear infinite"}, children:(0,require$react_SLASH_jsx_runtime.jsx)("path", {d:"M21 12a9 9 0 11-6.219-8.56"})});
  }
  function rr({title:e, message:n, actionLabel:a, onAction:l, icon:d = "\ud83d\udced"}) {
    return (0,require$react_SLASH_jsx_runtime.jsxs)("div", {style:{display:"flex", flexDirection:"column", alignItems:"center", justifyContent:"center", padding:"2rem", textAlign:"center", gap:"1rem"}, children:[(0,require$react_SLASH_jsx_runtime.jsx)("div", {style:{fontSize:"3rem", lineHeight:1, opacity:0.6}, children:d}), (0,require$react_SLASH_jsx_runtime.jsxs)("div", {children:[(0,require$react_SLASH_jsx_runtime.jsx)("h3", {style:{margin:"0 0 0.5rem 0", fontSize:"1.125rem", fontWeight:600, color:"var(--token-colors-text-default)"}, 
    children:e}), (0,require$react_SLASH_jsx_runtime.jsx)("p", {style:{margin:0, fontSize:"0.875rem", color:"var(--token-colors-text-muted)", maxWidth:"24rem"}, children:n})]}), a && l && (0,require$react_SLASH_jsx_runtime.jsx)(Re, {variant:"primary", onClick:l, children:a})]});
  }
  function Fe(e) {
    var n;
    if (typeof window > "u") {
      return null;
    }
    try {
      return ((n = localStorage.getItem(e)) == null ? void 0 : n.trim()) || null;
    } catch {
      return null;
    }
  }
  async function W(e$jscomp$0, n$jscomp$0) {
    let a$jscomp$0;
    try {
      var JSCompiler_temp_const = fetch, JSCompiler_temp_const$jscomp$0 = Object, JSCompiler_temp_const$jscomp$1 = JSCompiler_temp_const$jscomp$0.fromEntries;
      const n = new Headers({...((n$jscomp$0 == null ? void 0 : n$jscomp$0.body) != null ? {"Content-Type":"application/json"} : {}), ...((n$jscomp$0 == null ? void 0 : n$jscomp$0.headers) ?? {})}), a = Fe("knoxx_user_email") ?? Vn, l = Fe("knoxx_org_slug") ?? Xn;
      var JSCompiler_inline_result = (a && !n.has("x-knoxx-user-email") && n.set("x-knoxx-user-email", a), l && !n.has("x-knoxx-org-slug") && n.set("x-knoxx-org-slug", l), n);
      a$jscomp$0 = await JSCompiler_temp_const(`${Kn}${e$jscomp$0}`, {credentials:"include", headers:{...JSCompiler_temp_const$jscomp$1.call(JSCompiler_temp_const$jscomp$0, JSCompiler_inline_result.entries())}, ...n$jscomp$0});
    } catch (l) {
      throw Error(`Request to ${e$jscomp$0} failed before the server responded. This usually means the reverse proxy or upstream service reset the connection. ${l instanceof Error ? l.message : String(l)}`);
    }
    if (!a$jscomp$0.ok) {
      n$jscomp$0 = e$jscomp$0 = await a$jscomp$0.text();
      try {
        var e = JSON.parse(e$jscomp$0);
        if (e && typeof e == "object") {
          var a = [typeof e.error == "string" ? e.error : null, typeof e.detail == "string" ? e.detail : null, typeof e.message == "string" ? e.message : null, typeof e.error_code == "string" ? `code=${e.error_code}` : null, typeof e.model_error == "string" ? e.model_error : null].filter(Boolean);
          var JSCompiler_inline_result$jscomp$0 = a.length > 0 ? a.join(" | ") : null;
        } else {
          JSCompiler_inline_result$jscomp$0 = null;
        }
        n$jscomp$0 = JSCompiler_inline_result$jscomp$0 ?? e$jscomp$0;
      } catch {
      }
      throw Error(`${a$jscomp$0.status} ${a$jscomp$0.statusText}${n$jscomp$0 ? ` - ${n$jscomp$0}` : ""}`);
    }
    return await a$jscomp$0.json();
  }
  async function ar() {
    return W("/api/admin/config/discord");
  }
  async function or(e) {
    return W("/api/admin/config/discord", {method:"PUT", body:JSON.stringify({discordBotToken:e})});
  }
  async function sr() {
    return W("/api/admin/config/events");
  }
  async function lr(e) {
    return W("/api/admin/config/events", {method:"PUT", body:JSON.stringify(e)});
  }
  async function ir(e) {
    return W(`/api/admin/config/events/jobs/${encodeURIComponent(e)}/run`, {method:"POST"});
  }
  async function cr(e) {
    return W("/api/admin/config/events/dispatch", {method:"POST", body:JSON.stringify(e)});
  }
  async function ur() {
    return W("/api/admin/config/events/runtime/stop", {method:"POST"});
  }
  async function dr() {
    return W("/api/admin/config/events/runtime/start", {method:"POST"});
  }
  async function gr() {
    return W("/api/admin/config/events/runtime/reset", {method:"POST"});
  }
  Object.defineProperties(exports, {__esModule:{enumerable:!0, value:!0}, EmptyState:{enumerable:!0, get:function() {
    return rr;
  }}, dispatchEventAgentEvent:{enumerable:!0, get:function() {
    return cr;
  }}, getDiscordConfig:{enumerable:!0, get:function() {
    return ar;
  }}, getEventAgentControl:{enumerable:!0, get:function() {
    return sr;
  }}, resetEventAgentRuntime:{enumerable:!0, get:function() {
    return gr;
  }}, runEventAgentJob:{enumerable:!0, get:function() {
    return ir;
  }}, startEventAgentRuntime:{enumerable:!0, get:function() {
    return dr;
  }}, stopEventAgentRuntime:{enumerable:!0, get:function() {
    return ur;
  }}, updateDiscordConfig:{enumerable:!0, get:function() {
    return or;
  }}, updateEventAgentControl:{enumerable:!0, get:function() {
    return lr;
  }}});
  var require$react_SLASH_jsx_runtime = require("module$node_modules$react$jsx_runtime"), require$react = require("module$node_modules$react$index"), require$react_dom = require("module$node_modules$react_dom$index");
  const R = {bg:{default:"#272822", darker:"#1e1f1c", lighter:"#3e3d32", selection:"#414339", tabInactive:"#34352f", groupBorder:"#34352f"}, fg:{default:"#f8f8f2", bright:"#f8f8f2", panel:"#cccccc", soft:"#90908a", muted:"#75715e", subtle:"#464741"}, accent:{yellow:"#e6db74", orange:"#fd971f", red:"#f92672", magenta:"#ae81ff", blue:"#66d9ef", cyan:"#66d9ef", green:"#a6e22e"}, semantic:{error:"#f92672", warning:"#fd971f", success:"#a6e22e", info:"#66d9ef"}}, T = {bg:{default:"#011627", darker:"#01111d", 
  lighter:"#0b2942", selection:"#1d3b53", tabInactive:"#0b253a", groupBorder:"#5f7e97"}, fg:{default:"#d6deeb", bright:"#ffffff", panel:"#d2dee7", soft:"#89a4bb", muted:"#5f7e97", subtle:"#4b6479"}, accent:{yellow:"#ffeb95", orange:"#F78C6C", red:"#EF5350", magenta:"#c792ea", blue:"#82AAFF", cyan:"#80CBC4", green:"#c5e478"}, semantic:{error:"#EF5350", warning:"#FFCA28", success:"#c5e478", info:"#82AAFF"}}, p = {bg:{default:"#0A0C0F", darker:"#0F1318", lighter:"#1E2530", selection:"#131820", tabInactive:"#131820", 
  groupBorder:"#1E2530"}, fg:{default:"#E8ECF1", bright:"#F4F7FB", panel:"#D6DCE6", soft:"#A6B1C2", muted:"#8A94A6", subtle:"#5A6478"}, accent:{yellow:"#F5A623", orange:"#F5A623", red:"#FF4C4C", magenta:"#9B8CFF", blue:"#00D4FF", cyan:"#00D4FF", green:"#00E5A0"}, semantic:{error:"#FF4C4C", warning:"#F5A623", success:"#00E5A0", info:"#00D4FF"}}, X = {monokai:pe("monokai", "Monokai", R, {appearance:"dark", colorOverrides:{selection:{default:"rgba(135, 139, 145, 0.5)"}, interactive:{default:R.accent.green, 
  hover:"#8fce26", active:"#7cb824", disabled:R.fg.muted}, button:{primary:{bg:R.fg.muted, fg:R.fg.default, hover:"#8a856e", active:"#6a6654"}, secondary:{bg:R.bg.selection, fg:R.fg.default, hover:"#505248", active:"#3a3c33"}, ghost:{bg:"transparent", fg:R.fg.default, hover:R.bg.selection, active:R.bg.tabInactive}, danger:{bg:R.accent.red, fg:R.fg.default, hover:"#e61b63", active:"#d1155c"}}, border:{focus:"#99947c"}, chart:{segment5:"#7ca3b5"}, fill:{good:{start:R.accent.green, end:"#78efb7"}, warn:{start:R.accent.orange, 
  end:"#ffd280"}, danger:{start:R.accent.red, end:"#ff9e92"}, neutral:{start:"#7aa7bd", end:"#98bfd0"}}, alpha:{warningBg:"rgba(58, 41, 16, 0.88)", errorBg:"rgba(70, 24, 24, 0.42)", errorBgSolid:"rgba(70, 24, 24, 0.9)", federationError:"rgba(127, 29, 29, 0.22)"}}}), "night-owl":pe("night-owl", "Night Owl", T, {appearance:"dark", colorOverrides:{interactive:{default:T.accent.cyan, hover:"#7fdbca", active:"#21c7a8", disabled:T.fg.muted}, button:{primary:{bg:"#7e57c2cc", fg:"#ffffffcc", hover:"#7e57c2", 
  active:"#6747a4"}, secondary:{bg:T.bg.selection, fg:T.fg.default, hover:"#234d708c", active:"#0e293f"}, ghost:{bg:"transparent", fg:T.fg.default, hover:r(T.bg.selection, 0.55), active:T.bg.lighter}, danger:{bg:T.accent.red, fg:"#ffffffcc", hover:"#ec5f67", active:"#d3423e"}}, badge:{default:{bg:T.fg.muted, fg:"#ffffff"}}, border:{default:T.bg.groupBorder, subtle:T.fg.subtle, focus:T.accent.blue}, chart:{segment5:"#5f7e97"}, fill:{good:{start:T.accent.green, end:"#d9f5dd"}, warn:{start:T.accent.orange, 
  end:"#ffcb8b"}, danger:{start:T.accent.red, end:"#ff869a"}, neutral:{start:T.accent.blue, end:"#c5e4fd"}}, alpha:{warningBg:"#675700F2", errorBg:"rgba(171, 3, 0, 0.42)", errorBgSolid:"#AB0300F2", federationError:r(T.accent.red, 0.22), shadow:"rgba(1, 22, 39, 0.35)", shadowLight:"rgba(1, 11, 20, 0.3)", shadowDeep:"rgba(1, 17, 29, 0.45)"}}}), "proxy-console":pe("proxy-console", "Proxy Console", p, {appearance:"dark", colorOverrides:{interactive:{default:p.accent.cyan, hover:"#34DEFF", active:"#00B8DE", 
  disabled:p.fg.subtle}, button:{primary:{bg:"#00D4FF", fg:"#0A0C0F", hover:"#34DEFF", active:"#00B8DE"}, secondary:{bg:"#0F1318", fg:"#E8ECF1", hover:"#131820", active:"#1A212C"}, ghost:{bg:"transparent", fg:"#E8ECF1", hover:r(p.accent.cyan, 0.12), active:r(p.accent.cyan, 0.18)}, danger:{bg:"#FF4C4C", fg:"#FDFEFF", hover:"#FF6666", active:"#E64545"}}, badge:{default:{bg:r(p.fg.subtle, 0.2), fg:"#E8ECF1"}, success:{bg:r(p.accent.green, 0.12), fg:"#00E5A0"}, warning:{bg:r(p.accent.orange, 0.12), fg:"#F5A623"}, 
  error:{bg:r(p.accent.red, 0.12), fg:"#FF4C4C"}, info:{bg:r(p.accent.cyan, 0.12), fg:"#00D4FF"}}, border:{default:"#1E2530", subtle:"#171C25", focus:"#00D4FF", error:"#FF4C4C"}, chart:{segment0:"#00D4FF", segment1:"#00E5A0", segment2:"#F5A623", segment3:"#FF4C4C", segment4:"#9B8CFF", segment5:"#7A90AA"}, fill:{good:{start:"#00E5A0", end:"#57F0C0"}, warn:{start:"#F5A623", end:"#FFD07A"}, danger:{start:"#FF4C4C", end:"#FF8C8C"}, neutral:{start:"#00D4FF", end:"#8EEBFF"}}, surface:{panel:r(p.bg.darker, 
  0.92), card:r(p.bg.tabInactive, 0.82), cardAlt:r(p.bg.lighter, 0.62), input:r("#0A0D11", 0.95), nav:r(p.bg.default, 0.88)}, alpha:{green:{_08:r(p.accent.green, 0.08), _12:r(p.accent.green, 0.12), _14:r(p.accent.green, 0.14), _15:r(p.accent.green, 0.15), _16:r(p.accent.green, 0.16), _25:r(p.accent.green, 0.25), _28:r(p.accent.green, 0.28), _30:r(p.accent.green, 0.3), _35:r(p.accent.green, 0.35), _38:r(p.accent.green, 0.38), _40:r(p.accent.green, 0.4), _45:r(p.accent.green, 0.45), _50:r(p.accent.green, 
  0.5), _55:r(p.accent.green, 0.55), _60:r(p.accent.green, 0.6), _80:r(p.accent.green, 0.8)}, red:{_12:r(p.accent.red, 0.12), _14:r(p.accent.red, 0.14), _15:r(p.accent.red, 0.15), _25:r(p.accent.red, 0.25), _30:r(p.accent.red, 0.3), _38:r(p.accent.red, 0.38), _40:r(p.accent.red, 0.4), _45:r(p.accent.red, 0.45), _46:r(p.accent.red, 0.46), _50:r(p.accent.red, 0.5)}, orange:{_12:r(p.accent.orange, 0.12), _15:r(p.accent.orange, 0.15), _32:r(p.accent.orange, 0.32), _35:r(p.accent.orange, 0.35), _40:r(p.accent.orange, 
  0.4)}, blue:{_15:r(p.accent.blue, 0.15), _20:r(p.accent.blue, 0.2), _35:r(p.accent.blue, 0.35), _45:r(p.accent.blue, 0.45), _80:r(p.accent.blue, 0.8), _95:r(p.accent.blue, 0.95)}, magenta:{_08:r(p.accent.magenta, 0.08), _14:r(p.accent.magenta, 0.14), _30:r(p.accent.magenta, 0.3)}, yellow:{_06:r(p.accent.yellow, 0.06)}, bg:{_08:r(p.bg.tabInactive, 0.08), _10:r(p.bg.tabInactive, 0.1), _12:r(p.bg.tabInactive, 0.12), _14:r(p.bg.tabInactive, 0.14), _16:r(p.bg.tabInactive, 0.16), _18:r(p.bg.tabInactive, 
  0.18), _24:r(p.bg.tabInactive, 0.24), _25:r(p.bg.tabInactive, 0.25), _28:r(p.bg.tabInactive, 0.28), _30:r(p.bg.tabInactive, 0.3), _46:r(p.bg.default, 0.46), _55:r(p.bg.tabInactive, 0.55), _60:r(p.bg.default, 0.6), _62:r(p.bg.default, 0.62), _68:r(p.bg.default, 0.68), _70:r(p.bg.tabInactive, 0.7), _72:r(p.bg.default, 0.72), _80:r(p.bg.default, 0.8), _85:r(p.bg.tabInactive, 0.85), _88:r(p.bg.tabInactive, 0.88), _88b:r(p.bg.default, 0.88), _90:r(p.bg.default, 0.9), _95:r(p.bg.default, 0.95)}, warningBg:r("#442E0C", 
  0.88), errorBg:r("#5A1C1C", 0.42), errorBgSolid:r("#5A1C1C", 0.9), federationError:r("#7F1D1D", 0.22), white:{_08:r("#FFFFFF", 0.08)}, shadow:r("#000000", 0.4), shadowLight:r("#000000", 0.3), shadowDeep:r("#000000", 0.5)}}})};
  Ie(X.monokai.colors, ["colors"]);
  const be = {sans:"Inter, system-ui, -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, Helvetica Neue, Arial, sans-serif", mono:"JetBrains Mono, Fira Code, Monaco, Consolas, Liberation Mono, Courier New, monospace"}, D = {xs:"0.75rem", sm:"0.875rem", base:"1rem", lg:"1.125rem", xl:"1.25rem", "2xl":"1.5rem", "3xl":"1.875rem", "4xl":"2.25rem", "5xl":"3rem", inlineCode:"0.875em"};
  D["4xl"];
  D["3xl"];
  D["2xl"];
  D.xl;
  D.lg;
  D.base;
  D.base;
  D.sm;
  D.sm;
  D.xs;
  be.mono;
  D.sm;
  be.mono;
  D.inlineCode;
  const V = {xs:"0 1px 2px 0 rgba(0, 0, 0, 0.05)", sm:"0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px -1px rgba(0, 0, 0, 0.1)", md:"0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1)", lg:"0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -4px rgba(0, 0, 0, 0.1)", xl:"0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1)", "2xl":"0 25px 50px -12px rgba(0, 0, 0, 0.25)", inner:"inset 0 2px 4px 0 rgba(0, 0, 0, 0.05)", focus:"0 0 0 2px rgba(166, 226, 46, 0.5)", focusError:"0 0 0 2px rgba(249, 38, 114, 0.5)", 
  none:"none"};
  V.none;
  V.sm;
  V.md;
  V.lg;
  V.xl;
  V["2xl"];
  const fe = {colors:structuredClone(X.monokai.colors), palette:structuredClone(R), fontFamily:be, fontSize:D, shadow:V, radius:{none:"0px", xs:"2px", sm:"4px", md:"6px", lg:"8px", xl:"12px", full:"9999px"}}, Y = {monokai:fe, "night-owl":me(fe, {colors:structuredClone(X["night-owl"].colors), palette:structuredClone(T), shadow:{focus:"0 0 0 2px rgba(130, 170, 255, 0.35)", focusError:"0 0 0 2px rgba(239, 83, 80, 0.35)"}}), "proxy-console":me(fe, {colors:structuredClone(X["proxy-console"].colors), palette:structuredClone(p), 
  fontFamily:{sans:"IBM Plex Sans, Segoe UI, system-ui, sans-serif", mono:"JetBrains Mono, Fira Code, monospace"}, shadow:{xs:"0 1px 2px 0 rgba(0, 0, 0, 0.35)", sm:"0 1px 3px rgba(0, 0, 0, 0.4)", md:"0 4px 12px rgba(0, 0, 0, 0.5)", lg:"0 10px 24px rgba(0, 0, 0, 0.55)", xl:"0 20px 36px rgba(0, 0, 0, 0.6)", "2xl":"0 28px 64px rgba(0, 0, 0, 0.65)", inner:"inset 0 1px 2px rgba(0, 0, 0, 0.28)", focus:"0 0 0 2px rgba(0, 212, 255, 0.35)", focusError:"0 0 0 2px rgba(255, 76, 76, 0.35)", none:"none"}, radius:{none:"0px", 
  xs:"2px", sm:"4px", md:"4px", lg:"6px", xl:"8px", full:"9999px"}})}, nt = J(Y.monokai.colors, ["colors"]);
  J(Y.monokai.palette, ["palette"]);
  const he = J(Y.monokai.fontFamily, ["fontFamily"]), L = J(Y.monokai.fontSize, ["fontSize"]), rt = J(Y.monokai.shadow, ["shadow"]), at = J(Y.monokai.radius, ["radius"]);
  var JSCompiler_object_inline_spacing_1562 = {1:4, "1.5":6, 2:8, 3:12, 4:16, 5:20, 6:24, 8:32}, JSCompiler_object_inline_fontSize_2234 = L.base, JSCompiler_object_inline_fontSize_2235 = L.sm;
  (0,require$react.createContext)({theme:"monokai", themeName:"monokai", resolvedTheme:function(e, n) {
    const a = He(e);
    e = Te(Y[e] ?? Y.monokai, n);
    return {...a, palette:e.palette, colors:e.colors, fontFamily:e.fontFamily, fontSize:e.fontSize, shadow:e.shadow, radius:e.radius};
  }("monokai")});
  const it = {primary:{backgroundColor:nt.button.primary.bg, color:nt.button.primary.fg, border:"none"}, secondary:{backgroundColor:nt.button.secondary.bg, color:nt.button.secondary.fg, border:`1px solid ${nt.border.default}`}, ghost:{backgroundColor:"transparent", color:nt.button.ghost.fg, border:"none"}, danger:{backgroundColor:nt.button.danger.bg, color:nt.button.danger.fg, border:"none"}}, ct = {sm:{padding:`${JSCompiler_object_inline_spacing_1562[1.5]}px ${JSCompiler_object_inline_spacing_1562[3]}px`, 
  fontSize:JSCompiler_object_inline_fontSize_2235, gap:`${JSCompiler_object_inline_spacing_1562[1]}px`}, md:{padding:`${JSCompiler_object_inline_spacing_1562[2]}px ${JSCompiler_object_inline_spacing_1562[4]}px`, fontSize:JSCompiler_object_inline_fontSize_2234, gap:`${JSCompiler_object_inline_spacing_1562[2]}px`}, lg:{padding:`${JSCompiler_object_inline_spacing_1562[3]}px ${JSCompiler_object_inline_spacing_1562[6]}px`, fontSize:JSCompiler_object_inline_fontSize_2234, gap:`${JSCompiler_object_inline_spacing_1562[2]}px`}}, 
  ut = {display:"inline-flex", alignItems:"center", justifyContent:"center", fontWeight:500, lineHeight:1, borderRadius:at.md, cursor:"pointer", transition:"color, background-color, border-color 100ms cubic-bezier(0.4, 0, 0.2, 1)", outline:"none", fontFamily:he.sans}, Re = (0,require$react.forwardRef)(({variant:e = "secondary", size:n = "md", disabled:a = !1, loading:l = !1, fullWidth:d = !1, iconStart:h, iconEnd:x, children:w, type:y = "button", ...b}, S) => {
    a = a || l;
    return (0,require$react_SLASH_jsx_runtime.jsxs)("button", {ref:S, type:y, disabled:a, "data-component":"button", "data-variant":e, "data-size":n, "data-loading":l || void 0, "data-full-width":d || void 0, "aria-busy":l, style:{...ut, ...it[e], ...ct[n], width:d ? "100%" : void 0, opacity:a ? 0.6 : 1, cursor:a ? "not-allowed" : "pointer"}, ...b, children:[l && (0,require$react_SLASH_jsx_runtime.jsx)(dt, {size:n}), !l && h, w, !l && x]});
  });
  Re.displayName = "Button";
  nt.badge.default.bg;
  nt.badge.default.fg;
  nt.badge.success.bg;
  nt.badge.success.fg;
  nt.badge.warning.bg;
  nt.badge.warning.fg;
  nt.badge.error.bg;
  nt.badge.error.fg;
  nt.badge.info.bg;
  nt.badge.info.fg;
  nt.badge.success.bg;
  nt.badge.success.fg;
  nt.badge.default.bg;
  nt.badge.default.fg;
  nt.alpha.magenta._14;
  nt.accent.magenta;
  nt.alpha.green._12;
  nt.accent.green;
  nt.alpha.red._12;
  nt.accent.red;
  nt.alpha.blue._15;
  nt.accent.blue;
  nt.badge.default.bg;
  nt.text.muted;
  L.xs;
  L.xs;
  L.sm;
  at.sm;
  he.sans;
  if (typeof document < "u") {
    const e = document.createElement("style");
    e.textContent = "\n@keyframes devel-badge-pulse {\n  0%, 100% {\n    opacity: 1;\n    transform: scale(1);\n  }\n  50% {\n    opacity: 0.5;\n    transform: scale(1.2);\n  }\n}\n";
    document.head.appendChild(e);
  }
  const pt = {default:{backgroundColor:nt.background.surface, border:`1px solid ${nt.border.default}`, boxShadow:rt.sm}, outlined:{backgroundColor:"transparent", border:`1px solid ${nt.border.default}`, boxShadow:rt.none}, elevated:{backgroundColor:nt.background.elevated, border:`1px solid ${nt.border.subtle}`, boxShadow:rt.md}}, _e = {none:{padding:0}, sm:{padding:`${JSCompiler_object_inline_spacing_1562[2]}px ${JSCompiler_object_inline_spacing_1562[3]}px`}, md:{padding:`${JSCompiler_object_inline_spacing_1562[4]}px ${JSCompiler_object_inline_spacing_1562[5]}px`}, 
  lg:{padding:`${JSCompiler_object_inline_spacing_1562[6]}px ${JSCompiler_object_inline_spacing_1562[8]}px`}}, ft = {none:at.none, sm:at.sm, md:at.md, lg:at.lg, full:at.full}, bt = {display:"flex", flexDirection:"column", transition:"color, background-color, border-color 100ms cubic-bezier(0.4, 0, 0.2, 1)", fontFamily:he.sans, overflow:"hidden"}, yt = {display:"flex", alignItems:"center", justifyContent:"space-between", padding:`${JSCompiler_object_inline_spacing_1562[3]}px ${JSCompiler_object_inline_spacing_1562[4]}px`, 
  borderBottom:`1px solid ${nt.border.default}`, fontWeight:600, fontSize:L.lg}, mt = {flex:1, minHeight:0}, ht = {display:"flex", alignItems:"center", justifyContent:"flex-end", gap:`${JSCompiler_object_inline_spacing_1562[2]}px`, padding:`${JSCompiler_object_inline_spacing_1562[3]}px ${JSCompiler_object_inline_spacing_1562[4]}px`, borderTop:`1px solid ${nt.border.default}`};
  (0,require$react.forwardRef)(({variant:e = "default", interactive:n = !1, padding:a = "md", radius:l = "md", header:d, title:h, extra:x, children:w, footer:y, onClick:b, style:S}, g) => {
    const k = n || typeof b == "function";
    n = _e[a].padding;
    const s = u => {
      k && (u.key === "Enter" || u.key === " ") && (u.preventDefault(), b == null || b({...u, currentTarget:u.currentTarget}));
    }, c = d !== void 0 || h !== void 0 || x !== void 0;
    a = c || y ? _e[a].padding : void 0;
    return (0,require$react_SLASH_jsx_runtime.jsxs)("div", {ref:g, "data-component":"card", "data-variant":e, "data-interactive":k || void 0, role:k ? "button" : "region", tabIndex:k ? 0 : void 0, onClick:k ? b : void 0, onKeyDown:k ? s : void 0, style:{...bt, ...pt[e], padding:d || y ? void 0 : n, borderRadius:ft[l], cursor:k ? "pointer" : "default", ...S}, children:[c && (0,require$react_SLASH_jsx_runtime.jsxs)("div", {style:yt, children:[(0,require$react_SLASH_jsx_runtime.jsxs)("div", {style:{display:"flex", 
    alignItems:"center", gap:JSCompiler_object_inline_spacing_1562[2]}, children:[d, h && !d && (0,require$react_SLASH_jsx_runtime.jsx)("span", {children:h})]}), x && (0,require$react_SLASH_jsx_runtime.jsx)("div", {children:x})]}), (0,require$react_SLASH_jsx_runtime.jsx)("div", {style:{...mt, padding:a}, children:w}), y !== void 0 && (0,require$react_SLASH_jsx_runtime.jsx)("div", {style:ht, children:y})]});
  }).displayName = "Card";
  `${nt.border.default}`;
  L.lg;
  `${nt.border.default}`;
  const St = {sm:{maxWidth:"400px"}, md:{maxWidth:"600px"}, lg:{maxWidth:"800px"}, xl:{maxWidth:"1000px"}, full:{maxWidth:"calc(100vw - 64px)", maxHeight:"calc(100vh - 64px)"}}, kt = {position:"fixed", inset:0, backgroundColor:"var(--token-colors-background-overlay)", backdropFilter:"blur(4px)", zIndex:1400, display:"flex", alignItems:"center", justifyContent:"center", padding:"32px"}, vt = {backgroundColor:nt.background.elevated, borderRadius:at.lg, boxShadow:rt["2xl"], border:`1px solid ${nt.border.subtle}`, 
  display:"flex", flexDirection:"column", width:"100%", maxHeight:"calc(100vh - 64px)", fontFamily:he.sans}, wt = {display:"flex", alignItems:"center", justifyContent:"space-between", padding:`${JSCompiler_object_inline_spacing_1562[4]}px ${JSCompiler_object_inline_spacing_1562[5]}px`, borderBottom:`1px solid ${nt.border.default}`, fontWeight:600, fontSize:L.lg}, _t = {flex:1, padding:`${JSCompiler_object_inline_spacing_1562[4]}px ${JSCompiler_object_inline_spacing_1562[5]}px`, overflowY:"auto"}, 
  Et = {display:"flex", alignItems:"center", justifyContent:"flex-end", gap:`${JSCompiler_object_inline_spacing_1562[2]}px`, padding:`${JSCompiler_object_inline_spacing_1562[3]}px ${JSCompiler_object_inline_spacing_1562[5]}px`, borderTop:`1px solid ${nt.border.default}`}, Ft = {background:"none", border:"none", cursor:"pointer", padding:`${JSCompiler_object_inline_spacing_1562[1]}px`, fontSize:"20px", lineHeight:1, color:nt.text.muted, transition:"color, background-color, border-color 100ms cubic-bezier(0.4, 0, 0.2, 1)"};
  (0,require$react.forwardRef)(({open:e = !1, onClose:n, size:a = "md", closable:l = !0, closeOnBackdrop:d = !0, closeOnEscape:h = !0, header:y, title:b, children:S, footer:g}, k) => {
    const E = (0,require$react.useRef)(null), o = (0,require$react.useRef)(null), s = (0,require$react.useId)(), c = (0,require$react.useId)();
    (0,require$react.useEffect)(() => {
      var v;
      if (e) {
        return o.current = document.activeElement, (v = E.current) == null || v.focus(), document.body.style.overflow = "hidden", () => {
          var C;
          document.body.style.overflow = "";
          (C = o.current) == null || C.focus();
        };
      }
    }, [e]);
    const f = (0,require$react.useCallback)(v => {
      v.key === "Escape" && l && h && n();
    }, [l, h, n]), u = (0,require$react.useCallback)(v => {
      d && l && v.target === v.currentTarget && n();
    }, [d, l, n]), m = (0,require$react.useCallback)(v => {
      var C;
      if (v.key === "Tab") {
        var F = (C = E.current) == null ? void 0 : C.querySelectorAll('button, [href], input, select, textarea, [tabindex]:not([tabindex\x3d"-1"])');
        F && F.length !== 0 && (C = F[0], F = F[F.length - 1], v.shiftKey && document.activeElement === C ? (v.preventDefault(), F.focus()) : !v.shiftKey && document.activeElement === F && (v.preventDefault(), C.focus()));
      }
    }, []);
    if (!e) {
      return null;
    }
    a = (0,require$react_SLASH_jsx_runtime.jsx)("div", {"data-component":"modal", "data-size":a, "data-open":e, style:kt, onClick:u, onKeyDown:f, children:(0,require$react_SLASH_jsx_runtime.jsxs)("div", {ref:v => {
      E.current = v;
      typeof k == "function" ? k(v) : k && (k.current = v);
    }, role:"dialog", "aria-modal":"true", "aria-labelledby":b ? s : void 0, tabIndex:-1, style:{...vt, ...St[a]}, onKeyDown:m, children:[(y !== void 0 || b !== void 0) && (0,require$react_SLASH_jsx_runtime.jsxs)("div", {style:wt, children:[(0,require$react_SLASH_jsx_runtime.jsxs)("div", {id:s, style:{display:"flex", alignItems:"center", gap:JSCompiler_object_inline_spacing_1562[2]}, children:[y, b && !y && (0,require$react_SLASH_jsx_runtime.jsx)("span", {children:b})]}), l && (0,require$react_SLASH_jsx_runtime.jsx)("button", 
    {type:"button", style:Ft, onClick:n, "aria-label":"Close modal", children:"×"})]}), (0,require$react_SLASH_jsx_runtime.jsx)("div", {id:c, style:_t, children:S}), g !== void 0 && (0,require$react_SLASH_jsx_runtime.jsx)("div", {style:Et, children:g})]})});
    return (0,require$react_dom.createPortal)(a, document.body);
  }).displayName = "Modal";
  `${nt.border.default}`;
  L.lg;
  `${nt.border.default}`;
  nt.background.surface;
  nt.text.default;
  L.xs;
  at.sm;
  rt.md;
  const Ct = {sm:{padding:`${JSCompiler_object_inline_spacing_1562[1]}px ${JSCompiler_object_inline_spacing_1562[2]}px`, fontSize:L.sm}, md:{padding:`${JSCompiler_object_inline_spacing_1562[2]}px ${JSCompiler_object_inline_spacing_1562[3]}px`, fontSize:L.base}, lg:{padding:`${JSCompiler_object_inline_spacing_1562[3]}px ${JSCompiler_object_inline_spacing_1562[4]}px`, fontSize:L.lg}}, $t = {default:{backgroundColor:nt.background.default, border:`1px solid ${nt.border.default}`}, filled:{backgroundColor:nt.background.surface, 
  border:"1px solid transparent"}, unstyled:{backgroundColor:"transparent", border:"none", padding:0}}, zt = {width:"100%", fontFamily:he.sans, color:nt.text.default, borderRadius:at.sm, outline:"none", transition:"color, background-color, border-color 100ms cubic-bezier(0.4, 0, 0.2, 1)"}, It = {borderColor:nt.border.error}, Tt = {backgroundColor:nt.background.surface, color:nt.text.muted, cursor:"not-allowed", opacity:0.7}, Rt = {borderColor:nt.border.focus, boxShadow:`0 0 0 2px ${nt.alpha.blue._35}`}, 
  Ee = {position:"absolute", top:"50%", transform:"translateY(-50%)", display:"flex", alignItems:"center", justifyContent:"center", color:nt.text.muted, pointerEvents:"none"}, Ot = {fontSize:L.xs, color:nt.border.error, marginTop:`${JSCompiler_object_inline_spacing_1562[1]}px`};
  (0,require$react.forwardRef)(({type:e = "text", value:n, defaultValue:a, placeholder:l, size:d = "md", variant:h = "default", disabled:x = !1, readonly:w = !1, required:y = !1, error:b = !1, errorMessage:S, leftIcon:g, rightIcon:k, onChange:E, onFocus:o, onBlur:s, name:c, id:f, autoFocus:u, autoComplete:m, maxLength:_, minLength:v, pattern:C}, F) => {
    const [$, z] = (0,require$react.useState)(!1), N = {...zt, ...$t[h], ...(h !== "unstyled" ? Ct[d] : {}), ...(b ? It : {}), ...(x ? Tt : {}), ...($ && !x && h !== "unstyled" ? Rt : {}), paddingLeft:g && h !== "unstyled" ? "32px" : void 0, paddingRight:k && h !== "unstyled" ? "32px" : void 0};
    return (0,require$react_SLASH_jsx_runtime.jsxs)("div", {style:{position:"relative", width:"100%"}, children:[g && h !== "unstyled" && (0,require$react_SLASH_jsx_runtime.jsx)("div", {style:{...Ee, left:`${JSCompiler_object_inline_spacing_1562[2]}px`}, children:g}), (0,require$react_SLASH_jsx_runtime.jsx)("input", {ref:F, "data-component":"input", "data-size":d, "data-variant":h, "data-error":b || void 0, "data-disabled":x || void 0, type:e, value:n, defaultValue:a, placeholder:l, disabled:x, readOnly:w, 
    required:y, name:c, id:f, autoFocus:u, autoComplete:m, maxLength:_, minLength:v, pattern:C, style:N, onChange:E, onFocus:I => {
      z(!0);
      o == null || o(I);
    }, onBlur:I => {
      z(!1);
      s == null || s(I);
    }, "aria-invalid":b, "aria-describedby":b && S ? `${f}-error` : void 0}), k && h !== "unstyled" && (0,require$react_SLASH_jsx_runtime.jsx)("div", {style:{...Ee, right:`${JSCompiler_object_inline_spacing_1562[2]}px`}, children:k}), b && S && (0,require$react_SLASH_jsx_runtime.jsx)("div", {id:`${f}-error`, style:Ot, role:"alert", children:S})]});
  }).displayName = "Input";
  const Lt = {sm:{padding:`${JSCompiler_object_inline_spacing_1562[1]}px ${JSCompiler_object_inline_spacing_1562[2]}px`, fontSize:L.sm}, md:{padding:`${JSCompiler_object_inline_spacing_1562[2]}px ${JSCompiler_object_inline_spacing_1562[3]}px`, fontSize:L.base}, lg:{padding:`${JSCompiler_object_inline_spacing_1562[3]}px ${JSCompiler_object_inline_spacing_1562[4]}px`, fontSize:L.lg}}, Bt = {default:{backgroundColor:"transparent"}, filled:{backgroundColor:nt.background.surface}, unstyled:{backgroundColor:"transparent", 
  border:"none", padding:0}}, Nt = {display:"inline-flex", alignItems:"center", borderRadius:at.md, border:`1px solid ${nt.border.default}`, color:nt.text.default, fontFamily:he.sans, lineHeight:1.5, cursor:"pointer", appearance:"none", backgroundImage:"url(\"data:image/svg+xml,%3Csvg xmlns\x3d'http://www.w3.org/2000/svg' width\x3d'12' height\x3d'12' viewBox\x3d'0 0 12 12' fill\x3d'none'%3E%3Cpath d\x3d'M2.5 4.5L6 8L9.5 4.5' stroke\x3d'%2375715e' stroke-width\x3d'1.5' stroke-linecap\x3d'round' stroke-linejoin\x3d'round'/%3E%3C/svg%3E\")", 
  backgroundRepeat:"no-repeat", backgroundPosition:`right ${JSCompiler_object_inline_spacing_1562[2]}px center`, paddingRight:`${JSCompiler_object_inline_spacing_1562[6]}px`, transition:"color, background-color, border-color 100ms cubic-bezier(0.4, 0, 0.2, 1)"}, jt = {borderColor:nt.border.error}, Pt = {opacity:0.5, cursor:"not-allowed"};
  (0,require$react.forwardRef)(({size:e = "md", variant:n = "default", error:a = !1, errorMessage:l, options:d, placeholder:h, children:x, className:w, disabled:y, style:b, ...S}, g) => (0,require$react_SLASH_jsx_runtime.jsxs)("div", {"data-component":"select-wrapper", style:{display:"inline-flex", flexDirection:"column", gap:`${JSCompiler_object_inline_spacing_1562[1]}px`}, children:[(0,require$react_SLASH_jsx_runtime.jsxs)("select", {ref:g, "data-component":"select", "data-size":e, "data-variant":n, 
  "data-error":a || void 0, disabled:y, style:{...Nt, ...Lt[e], ...Bt[n], ...(a ? jt : {}), ...(y ? Pt : {}), ...b}, className:w, ...S, children:[h && (0,require$react_SLASH_jsx_runtime.jsx)("option", {value:"", disabled:!0, children:h}), d == null ? void 0 : d.map(E => (0,require$react_SLASH_jsx_runtime.jsx)("option", {value:E.value, disabled:E.disabled, children:E.label}, E.value)), x]}), a && l && (0,require$react_SLASH_jsx_runtime.jsx)("span", {style:{fontSize:L.xs, color:nt.accent.red}, children:l})]})).displayName = 
  "Select";
  const Ut = {sm:{padding:`${JSCompiler_object_inline_spacing_1562[1]}px ${JSCompiler_object_inline_spacing_1562[2]}px`, fontSize:L.sm}, md:{padding:`${JSCompiler_object_inline_spacing_1562[2]}px ${JSCompiler_object_inline_spacing_1562[3]}px`, fontSize:L.base}, lg:{padding:`${JSCompiler_object_inline_spacing_1562[3]}px ${JSCompiler_object_inline_spacing_1562[4]}px`, fontSize:L.lg}}, Gt = {default:{backgroundColor:"transparent"}, filled:{backgroundColor:nt.background.surface}, unstyled:{backgroundColor:"transparent", 
  border:"none", padding:0}}, Zt = {display:"block", width:"100%", borderRadius:at.md, border:`1px solid ${nt.border.default}`, color:nt.text.default, fontFamily:he.sans, lineHeight:1.5, resize:"vertical", transition:"color, background-color, border-color 100ms cubic-bezier(0.4, 0, 0.2, 1)"}, Wt = {borderColor:nt.border.error}, Ht = {opacity:0.5, cursor:"not-allowed"};
  (0,require$react.forwardRef)(({size:e = "md", variant:n = "default", error:a = !1, errorMessage:l, className:d, disabled:h, style:x, ...w}, y) => (0,require$react_SLASH_jsx_runtime.jsxs)("div", {"data-component":"textarea-wrapper", style:{display:"flex", flexDirection:"column", gap:`${JSCompiler_object_inline_spacing_1562[1]}px`}, children:[(0,require$react_SLASH_jsx_runtime.jsx)("textarea", {ref:y, "data-component":"textarea", "data-size":e, "data-variant":n, "data-error":a || void 0, disabled:h, 
  style:{...Zt, ...Ut[e], ...Gt[n], ...(a ? Wt : {}), ...(h ? Ht : {}), ...x}, className:d, ...w}), a && l && (0,require$react_SLASH_jsx_runtime.jsx)("span", {style:{fontSize:L.xs, color:nt.accent.red}, children:l})]})).displayName = "Textarea";
  `${nt.alpha.blue._80}${nt.accent.cyan}`;
  `${nt.alpha.green._80}${nt.accent.green}`;
  `${nt.alpha.orange._40}${nt.accent.orange}`;
  `${nt.alpha.red._45}${nt.accent.red}`;
  `${nt.alpha.blue._80}${nt.accent.blue}`;
  `${nt.alpha.blue._95}${nt.alpha.green._80}${nt.alpha.magenta._30}`;
  nt.background.surface;
  at.sm;
  at.sm;
  L.xs;
  nt.text.muted;
  L.xs;
  nt.text.muted;
  if (typeof document < "u") {
    const e = document.createElement("style");
    e.textContent = "\n@keyframes devel-progress-indeterminate {\n  0% { transform: translateX(-100%); }\n  100% { transform: translateX(200%); }\n}\n\n@keyframes devel-progress-striped {\n  0% { background-position: 1rem 0; }\n  100% { background-position: 0 0; }\n}\n";
    document.head.appendChild(e);
  }
  `${nt.border.default}`;
  nt.background.default;
  nt.text.default;
  nt.text.muted;
  nt.text.secondary;
  he.mono;
  `${nt.border.default}`;
  nt.background.default;
  nt.text.default;
  nt.text.secondary;
  `${nt.border.default}`;
  he.mono;
  nt.background.elevated;
  nt.text.default;
  (0,require$react.createContext)(null);
  nt.background.overlay;
  nt.background.elevated;
  rt["2xl"];
  `${nt.border.subtle}`;
  he.sans;
  `${nt.border.default}`;
  nt.text.default;
  L.lg;
  he.sans;
  L.xs;
  nt.text.muted;
  nt.background.surface;
  nt.background.surface;
  L.xs;
  nt.text.muted;
  he.mono;
  nt.text.muted;
  `${nt.border.default}`;
  nt.background.default;
  he.sans;
  `${nt.border.default}`;
  L.sm;
  nt.text.muted;
  nt.background.elevated;
  L.xs;
  nt.text.muted;
  `${nt.border.default}`;
  `${nt.border.default}`;
  nt.background.surface;
  nt.text.default;
  he.sans;
  L.sm;
  nt.button.primary.bg;
  nt.button.primary.fg;
  he.sans;
  L.sm;
  L.xs;
  nt.text.muted;
  nt.text.muted;
  L.sm;
  L.xs;
  nt.text.muted;
  (0,require$react.createContext)(null);
  nt.badge.info.bg;
  nt.semantic.info;
  nt.badge.success.bg;
  nt.semantic.success;
  nt.badge.warning.bg;
  nt.semantic.warning;
  nt.badge.error.bg;
  nt.semantic.error;
  at.md;
  rt.lg;
  he.sans;
  nt.text.muted;
  L.sm;
  nt.accent.cyan;
  he.sans;
  L.sm;
  nt.text.default;
  nt.background.surface;
  nt.background.elevated;
  nt.text.muted;
  nt.background.surface;
  nt.text.muted;
  nt.text.muted;
  L.sm;
  L.base;
  L.lg;
  nt.text.muted;
  at.xs;
  `${nt.border.default}`;
  at.sm;
  nt.text.muted;
  he.sans;
  L.sm;
  L.sm;
  L.base;
  L.lg;
  he.sans;
  he.sans;
  nt.text.default;
  nt.background.surface;
  `${nt.border.default}`;
  at.md;
  nt.border.focus;
  `${nt.alpha.blue._35}`;
  nt.background.surface;
  nt.text.muted;
  nt.background.elevated;
  `${nt.border.default}`;
  at.md;
  rt.lg;
  nt.text.default;
  nt.background.elevated;
  nt.background.surface;
  `${nt.border.focus}`;
  nt.text.muted;
  L.sm;
  L.xs;
  nt.text.muted;
  nt.border.default;
  nt.text.muted;
  nt.background.surface;
  nt.background.surface;
  `${nt.border.default}`;
  nt.background.elevated;
  rt.sm;
  he.sans;
  nt.background.surface;
  L.base;
  nt.text.default;
  L.sm;
  nt.text.muted;
  L.sm;
  nt.text.muted;
  nt.text.default;
  nt.text.muted;
  L.sm;
  L.base;
  L.lg;
  he.sans;
  nt.text.default;
  L.sm;
  nt.text.muted;
  L.sm;
  nt.text.default;
  L.xs;
  nt.text.muted;
  nt.background.surface;
  `${nt.border.default}`;
  L.sm;
  nt.text.muted;
  L.xs;
  nt.accent.cyan;
  L["2xl"];
  nt.text.default;
  nt.text.muted;
  L.base;
  L.xs;
  nt.accent.cyan;
  L.xl;
  nt.text.default;
  nt.text.muted;
  L.sm;
  nt.border.default;
  nt.badge.info.fg;
  nt.badge.success.fg;
  nt.badge.warning.fg;
  nt.badge.error.fg;
  nt.text.muted;
  L.sm;
  L["2xl"];
  nt.text.default;
  nt.text.muted;
  L.sm;
  `${nt.accent.blue}${nt.alpha.blue._35}`;
  he.sans;
  L.sm;
  L.xs;
  nt.text.muted;
  `${nt.border.default}`;
  nt.background.surface;
  `${nt.border.subtle}`;
  nt.text.default;
  nt.text.muted;
  L.sm;
  nt.text.muted;
  `${nt.border.default}`;
  nt.background.surface;
  `${nt.border.default}`;
  nt.background.default;
  nt.text.default;
  he.sans;
  L.sm;
  nt.text.muted;
  L.sm;
  nt.text.secondary;
  he.sans;
  var Vt = Object.create, Se = Object.defineProperty, Xt = Object.getOwnPropertyDescriptor, Oe = Object.getOwnPropertyNames, Jt = Object.getPrototypeOf, Qt = Object.prototype.hasOwnProperty, nn = (e, n, a, l) => {
    if (n && typeof n == "object" || typeof n == "function") {
      for (let d of Oe(n)) {
        !Qt.call(e, d) && d !== a && Se(e, d, {get:() => n[d], enumerable:!(l = Xt(n, d)) || l.enumerable});
      }
    }
    return e;
  }, i = ((e, n, a) => (a = e != null ? Vt(Jt(e)) : {}, nn(e && e.__esModule ? a : Se(a, "default", {value:e, enumerable:!0}), e)))(((e, n) => function() {
    return n || (0,e[Oe(e)[0]])((n = {exports:{}}).exports, n), n.exports;
  })({["../../node_modules/.pnpm/prismjs@1.29.0_patch_hash\x3dvrxx3pzkik6jpmgpayxfjunetu/node_modules/prismjs/prism.js"](e, n) {
    e = function() {
      function w(o, s, c, f) {
        this.type = o;
        this.content = s;
        this.alias = c;
        this.length = (f || "").length | 0;
      }
      function y(o, s, c, f) {
        o.lastIndex = s;
        (o = o.exec(c)) && f && o[1] && (f = o[1].length, o.index += f, o[0] = o[0].slice(f));
        return o;
      }
      function b(o, s$jscomp$0, c, f$jscomp$0, u$jscomp$0, m) {
        for (var _ in c) {
          if (c.hasOwnProperty(_) && c[_]) {
            var v = c[_];
            v = Array.isArray(v) ? v : [v];
            for (var C = 0; C < v.length; ++C) {
              if (m && m.cause == _ + "," + C) {
                return;
              }
              var F = v[C], $ = F.inside, z = !!F.lookbehind, M = !!F.greedy, j = F.alias;
              if (M && !F.pattern.global) {
                var N = F.pattern.toString().match(/[imsuy]*$/)[0];
                F.pattern = RegExp(F.pattern.source, N + "g");
              }
              F = F.pattern || F;
              N = f$jscomp$0.next;
              for (var U = u$jscomp$0; N !== s$jscomp$0.tail && !(m && U >= m.reach); U += N.value.length, N = N.next) {
                var Q = N.value;
                if (s$jscomp$0.length > o.length) {
                  return;
                }
                if (!(Q instanceof w)) {
                  var ne = 1, P;
                  if (M) {
                    if (P = y(F, U, o, z), !P || P.index >= o.length) {
                      break;
                    }
                    var re = P.index, De = P.index + P[0].length;
                    Q = U;
                    for (Q += N.value.length; re >= Q;) {
                      N = N.next, Q += N.value.length;
                    }
                    if (Q -= N.value.length, U = Q, N.value instanceof w) {
                      continue;
                    }
                    for (re = N; re !== s$jscomp$0.tail && (Q < De || typeof re.value == "string"); re = re.next) {
                      ne++, Q += re.value.length;
                    }
                    ne--;
                    Q = o.slice(U, Q);
                    P.index -= U;
                  } else if (P = y(F, 0, Q, z), !P) {
                    continue;
                  }
                  re = P.index;
                  De = P[0];
                  var ce = Q.slice(0, re);
                  re = Q.slice(re + De.length);
                  Q = U + Q.length;
                  m && Q > m.reach && (m.reach = Q);
                  var oe = N.prev;
                  ce && (oe = g(s$jscomp$0, oe, ce), U += ce.length);
                  ce = s$jscomp$0;
                  for (var s = oe, f = s.next, u = 0; u < ne && f !== ce.tail; u++) {
                    f = f.next;
                  }
                  s.next = f;
                  f.prev = s;
                  ce.length -= u;
                  De = new w(_, $ ? x.tokenize(De, $) : De, j, De);
                  if (N = g(s$jscomp$0, oe, De), re && g(s$jscomp$0, N, re), ne > 1) {
                    ne = {cause:_ + "," + C, reach:Q}, b(o, s$jscomp$0, c, N.prev, U, ne), m && ne.reach > m.reach && (m.reach = ne.reach);
                  }
                }
              }
            }
          }
        }
      }
      function S() {
        var o = {value:null, prev:null, next:null}, s = {value:null, prev:o, next:null};
        o.next = s;
        this.head = o;
        this.tail = s;
        this.length = 0;
      }
      function g(o, s, c) {
        var f = s.next;
        c = {value:c, prev:s, next:f};
        return s.next = c, f.prev = c, o.length++, c;
      }
      var l = /(?:^|\s)lang(?:uage)?-([\w-]+)(?=\s|$)/i, d = 0, h = {}, x = {util:{encode:function o(s) {
        return s instanceof w ? new w(s.type, o(s.content), s.alias) : Array.isArray(s) ? s.map(o) : s.replace(/&/g, "\x26amp;").replace(/</g, "\x26lt;").replace(/\u00a0/g, " ");
      }, type:function(o) {
        return Object.prototype.toString.call(o).slice(8, -1);
      }, objId:function(o) {
        return o.__id || Object.defineProperty(o, "__id", {value:++d}), o.__id;
      }, clone:function o(s, c) {
        c = c || {};
        var u;
        switch(x.util.type(s)) {
          case "Object":
            if (u = x.util.objId(s), c[u]) {
              return c[u];
            }
            var f = {};
            c[u] = f;
            for (var m in s) {
              s.hasOwnProperty(m) && (f[m] = o(s[m], c));
            }
            return f;
          case "Array":
            return u = x.util.objId(s), c[u] ? c[u] : (f = [], c[u] = f, s.forEach(function(_, v) {
              f[v] = o(_, c);
            }), f);
          default:
            return s;
        }
      }, getLanguage:function(o) {
        for (; o;) {
          var s = l.exec(o.className);
          if (s) {
            return s[1].toLowerCase();
          }
          o = o.parentElement;
        }
        return "none";
      }, setLanguage:function(o, s) {
        o.className = o.className.replace(RegExp(l, "gi"), "");
        o.classList.add("language-" + s);
      }, isActive:function(o, s, c) {
        for (var f = "no-" + s; o;) {
          var u = o.classList;
          if (u.contains(s)) {
            return !0;
          }
          if (u.contains(f)) {
            return !1;
          }
          o = o.parentElement;
        }
        return !!c;
      }}, languages:{plain:h, plaintext:h, text:h, txt:h, extend:function(o, s) {
        o = x.util.clone(x.languages[o]);
        for (var f in s) {
          o[f] = s[f];
        }
        return o;
      }, insertBefore:function(o, s, c, f) {
        f = f || x.languages;
        var u = f[o], m = {}, _;
        for (_ in u) {
          if (u.hasOwnProperty(_)) {
            if (_ == s) {
              for (var v in c) {
                c.hasOwnProperty(v) && (m[v] = c[v]);
              }
            }
            c.hasOwnProperty(_) || (m[_] = u[_]);
          }
        }
        var C = f[o];
        return f[o] = m, x.languages.DFS(x.languages, function(F, $) {
          $ === C && F != o && (this[F] = m);
        }), m;
      }, DFS:function o(s, c, f, u) {
        u = u || {};
        var m = x.util.objId, _;
        for (_ in s) {
          if (s.hasOwnProperty(_)) {
            c.call(s, _, s[_], f || _);
            var v = s[_], C = x.util.type(v);
            C !== "Object" || u[m(v)] ? C === "Array" && !u[m(v)] && (u[m(v)] = !0, o(v, c, _, u)) : (u[m(v)] = !0, o(v, c, null, u));
          }
        }
      }}, plugins:{}, highlight:function(o, s, c) {
        o = {code:o, grammar:s, language:c};
        if (x.hooks.run("before-tokenize", o), !o.grammar) {
          throw Error('The language "' + o.language + '" has no grammar.');
        }
        return o.tokens = x.tokenize(o.code, o.grammar), x.hooks.run("after-tokenize", o), w.stringify(x.util.encode(o.tokens), o.language);
      }, tokenize:function(o, s) {
        var c = s.rest;
        if (c) {
          for (var f in c) {
            s[f] = c[f];
          }
          delete s.rest;
        }
        c = new S();
        g(c, c.head, o);
        b(o, c, s, c.head, 0);
        o = [];
        for (s = c.head.next; s !== c.tail;) {
          o.push(s.value), s = s.next;
        }
        return o;
      }, hooks:{all:{}, add:function(o, s) {
        var c = x.hooks.all;
        c[o] = c[o] || [];
        c[o].push(s);
      }, run:function(o, s) {
        if ((o = x.hooks.all[o]) && o.length) {
          for (var f = 0, u; u = o[f++];) {
            u(s);
          }
        }
      }}, Token:w};
      w.stringify = function o(s, c) {
        if (typeof s == "string") {
          return s;
        }
        if (Array.isArray(s)) {
          var f = "";
          return s.forEach(function(C) {
            f += o(C, c);
          }), f;
        }
        var u = {type:s.type, content:o(s.content, c), tag:"span", classes:["token", s.type], attributes:{}, language:c};
        (s = s.alias) && (Array.isArray(s) ? Array.prototype.push.apply(u.classes, s) : u.classes.push(s));
        x.hooks.run("wrap", u);
        s = "";
        for (var v in u.attributes) {
          s += " " + v + '\x3d"' + (u.attributes[v] || "").replace(/"/g, "\x26quot;") + '"';
        }
        return "\x3c" + u.tag + ' class\x3d"' + u.classes.join(" ") + '"' + s + "\x3e" + u.content + "\x3c/" + u.tag + "\x3e";
      };
      return x;
    }();
    n.exports = e;
    e.default = e;
  }})());
  i.languages.markup = {comment:{pattern:/\x3c!--(?:(?!\x3c!--)[\s\S])*?--\x3e/, greedy:!0}, prolog:{pattern:/<\?[\s\S]+?\?>/, greedy:!0}, doctype:{pattern:/<!DOCTYPE(?:[^>"'[\]]|"[^"]*"|'[^']*')+(?:\[(?:[^<"'\]]|"[^"]*"|'[^']*'|<(?!!--)|\x3c!--(?:[^-]|-(?!->))*--\x3e)*\]\s*)?>/i, greedy:!0, inside:{"internal-subset":{pattern:/(^[^\[]*\[)[\s\S]+(?=\]>$)/, lookbehind:!0, greedy:!0, inside:null}, string:{pattern:/"[^"]*"|'[^']*'/, greedy:!0}, punctuation:/^<!|>$|[[\]]/, "doctype-tag":/^DOCTYPE/i, name:/[^\s<>'"]+/}}, 
  cdata:{pattern:/<!\[CDATA\[[\s\S]*?\]\]>/i, greedy:!0}, tag:{pattern:/<\/?(?!\d)[^\s>\/=$<%]+(?:\s(?:\s*[^\s>\/=]+(?:\s*=\s*(?:"[^"]*"|'[^']*'|[^\s'">=]+(?=[\s>]))|(?=[\s/>])))+)?\s*\/?>/, greedy:!0, inside:{tag:{pattern:/^<\/?[^\s>\/]+/, inside:{punctuation:/^<\/?/, namespace:/^[^\s>\/:]+:/}}, "special-attr":[], "attr-value":{pattern:/=\s*(?:"[^"]*"|'[^']*'|[^\s'">=]+)/, inside:{punctuation:[{pattern:/^=/, alias:"attr-equals"}, {pattern:/^(\s*)["']|["']$/, lookbehind:!0}]}}, punctuation:/\/?>/, 
  "attr-name":{pattern:/[^\s>\/]+/, inside:{namespace:/^[^\s>\/:]+:/}}}}, entity:[{pattern:/&[\da-z]{1,8};/i, alias:"named-entity"}, /&#x?[\da-f]{1,8};/i]};
  i.languages.markup.tag.inside["attr-value"].inside.entity = i.languages.markup.entity;
  i.languages.markup.doctype.inside["internal-subset"].inside = i.languages.markup;
  i.hooks.add("wrap", function(e) {
    e.type === "entity" && (e.attributes.title = e.content.replace(/&amp;/, "\x26"));
  });
  Object.defineProperty(i.languages.markup.tag, "addInlined", {value:function(e, l) {
    var a = {};
    a = (a["language-" + l] = {pattern:/(^<!\[CDATA\[)[\s\S]+?(?=\]\]>$)/i, lookbehind:!0, inside:i.languages[l]}, a.cdata = /^<!\[CDATA\[|\]\]>$/i, {"included-cdata":{pattern:/<!\[CDATA\[[\s\S]*?\]\]>/i, inside:a}});
    l = (a["language-" + l] = {pattern:/[\s\S]+/, inside:i.languages[l]}, {});
    l[e] = {pattern:RegExp(/(<__[^>]*>)(?:<!\[CDATA\[(?:[^\]]|\](?!\]>))*\]\]>|(?!<!\[CDATA\[)[\s\S])*?(?=<\/__>)/.source.replace(/__/g, function() {
      return e;
    }), "i"), lookbehind:!0, greedy:!0, inside:a};
    i.languages.insertBefore("markup", "cdata", l);
  }});
  Object.defineProperty(i.languages.markup.tag, "addAttribute", {value:function(e, n) {
    i.languages.markup.tag.inside["special-attr"].push({pattern:RegExp(/(^|["'\s])/.source + "(?:" + e + ")" + /\s*=\s*(?:"[^"]*"|'[^']*'|[^\s'">=]+(?=[\s>]))/.source, "i"), lookbehind:!0, inside:{"attr-name":/^[^\s=]+/, "attr-value":{pattern:/=[\s\S]+/, inside:{value:{pattern:/(^=\s*(["']|(?!["'])))\S[\s\S]*(?=\2$)/, lookbehind:!0, alias:[n, "language-" + n], inside:i.languages[n]}, punctuation:[{pattern:/^=/, alias:"attr-equals"}, /"|'/]}}}});
  }});
  i.languages.html = i.languages.markup;
  i.languages.mathml = i.languages.markup;
  i.languages.svg = i.languages.markup;
  i.languages.xml = i.languages.extend("markup", {});
  i.languages.ssml = i.languages.xml;
  i.languages.atom = i.languages.xml;
  i.languages.rss = i.languages.xml;
  (function(e) {
    var n = {pattern:/\\[\\(){}[\]^$+*?|.]/, alias:"escape"}, a = /\\(?:x[\da-fA-F]{2}|u[\da-fA-F]{4}|u\{[\da-fA-F]+\}|0[0-7]{0,2}|[123][0-7]{2}|c[a-zA-Z]|.)/, l = "(?:[^\\\\-]|" + a.source + ")";
    l = RegExp(l + "-" + l);
    var d = {pattern:/(<|')[^<>']+(?=[>']$)/, lookbehind:!0, alias:"variable"};
    e.languages.regex = {"char-class":{pattern:/((?:^|[^\\])(?:\\\\)*)\[(?:[^\\\]]|\\[\s\S])*\]/, lookbehind:!0, inside:{"char-class-negation":{pattern:/(^\[)\^/, lookbehind:!0, alias:"operator"}, "char-class-punctuation":{pattern:/^\[|\]$/, alias:"punctuation"}, range:{pattern:l, inside:{escape:a, "range-punctuation":{pattern:/-/, alias:"operator"}}}, "special-escape":n, "char-set":{pattern:/\\[wsd]|\\p\{[^{}]+\}/i, alias:"class-name"}, escape:a}}, "special-escape":n, "char-set":{pattern:/\.|\\[wsd]|\\p\{[^{}]+\}/i, 
    alias:"class-name"}, backreference:[{pattern:/\\(?![123][0-7]{2})[1-9]/, alias:"keyword"}, {pattern:/\\k<[^<>']+>/, alias:"keyword", inside:{"group-name":d}}], anchor:{pattern:/[$^]|\\[ABbGZz]/, alias:"function"}, escape:a, group:[{pattern:/\((?:\?(?:<[^<>']+>|'[^<>']+'|[>:]|<?[=!]|[idmnsuxU]+(?:-[idmnsuxU]+)?:?))?/, alias:"punctuation", inside:{"group-name":d}}, {pattern:/\)/, alias:"punctuation"}], quantifier:{pattern:/(?:[+*?]|\{\d+(?:,\d*)?\})[?+]?/, alias:"number"}, alternation:{pattern:/\|/, 
    alias:"keyword"}};
  })(i);
  i.languages.clike = {comment:[{pattern:/(^|[^\\])\/\*[\s\S]*?(?:\*\/|$)/, lookbehind:!0, greedy:!0}, {pattern:/(^|[^\\:])\/\/.*/, lookbehind:!0, greedy:!0}], string:{pattern:/(["'])(?:\\(?:\r\n|[\s\S])|(?!\1)[^\\\r\n])*\1/, greedy:!0}, "class-name":{pattern:/(\b(?:class|extends|implements|instanceof|interface|new|trait)\s+|\bcatch\s+\()[\w.\\]+/i, lookbehind:!0, inside:{punctuation:/[.\\]/}}, keyword:/\b(?:break|catch|continue|do|else|finally|for|function|if|in|instanceof|new|null|return|throw|try|while)\b/, 
  boolean:/\b(?:false|true)\b/, function:/\b\w+(?=\()/, number:/\b0x[\da-f]+\b|(?:\b\d+(?:\.\d*)?|\B\.\d+)(?:e[+-]?\d+)?/i, operator:/[<>]=?|[!=]=?=?|--?|\+\+?|&&?|\|\|?|[?*/~^%]/, punctuation:/[{}[\];(),.:]/};
  i.languages.javascript = i.languages.extend("clike", {"class-name":[i.languages.clike["class-name"], {pattern:/(^|[^$\w\xA0-\uFFFF])(?!\s)[_$A-Z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*(?=\.(?:constructor|prototype))/, lookbehind:!0}], keyword:[{pattern:/((?:^|\})\s*)catch\b/, lookbehind:!0}, {pattern:/(^|[^.]|\.\.\.\s*)\b(?:as|assert(?=\s*\{)|async(?=\s*(?:function\b|\(|[$\w\xA0-\uFFFF]|$))|await|break|case|class|const|continue|debugger|default|delete|do|else|enum|export|extends|finally(?=\s*(?:\{|$))|for|from(?=\s*(?:['"]|$))|function|(?:get|set)(?=\s*(?:[#\[$\w\xA0-\uFFFF]|$))|if|implements|import|in|instanceof|interface|let|new|null|of|package|private|protected|public|return|static|super|switch|this|throw|try|typeof|undefined|var|void|while|with|yield)\b/, 
  lookbehind:!0}], function:/#?(?!\s)[_$a-zA-Z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*(?=\s*(?:\.\s*(?:apply|bind|call)\s*)?\()/, number:{pattern:RegExp(/(^|[^\w$])/.source + "(?:" + /NaN|Infinity/.source + "|" + /0[bB][01]+(?:_[01]+)*n?/.source + "|" + /0[oO][0-7]+(?:_[0-7]+)*n?/.source + "|" + /0[xX][\dA-Fa-f]+(?:_[\dA-Fa-f]+)*n?/.source + "|" + /\d+(?:_\d+)*n/.source + "|" + /(?:\d+(?:_\d+)*(?:\.(?:\d+(?:_\d+)*)?)?|\.\d+(?:_\d+)*)(?:[Ee][+-]?\d+(?:_\d+)*)?/.source + ")" + /(?![\w$])/.source), lookbehind:!0}, 
  operator:/--|\+\+|\*\*=?|=>|&&=?|\|\|=?|[!=]==|<<=?|>>>?=?|[-+*/%&|^!=<>]=?|\.{3}|\?\?=?|\?\.?|[~:]/});
  i.languages.javascript["class-name"][0].pattern = /(\b(?:class|extends|implements|instanceof|interface|new)\s+)[\w.\\]+/;
  i.languages.insertBefore("javascript", "keyword", {regex:{pattern:RegExp(/((?:^|[^$\w\xA0-\uFFFF."'\])\s]|\b(?:return|yield))\s*)/.source + /\//.source + "(?:" + /(?:\[(?:[^\]\\\r\n]|\\.)*\]|\\.|[^/\\\[\r\n])+\/[dgimyus]{0,7}/.source + "|" + /(?:\[(?:[^[\]\\\r\n]|\\.|\[(?:[^[\]\\\r\n]|\\.|\[(?:[^[\]\\\r\n]|\\.)*\])*\])*\]|\\.|[^/\\\[\r\n])+\/[dgimyus]{0,7}v[dgimyus]{0,7}/.source + ")" + /(?=(?:\s|\/\*(?:[^*]|\*(?!\/))*\*\/)*(?:$|[\r\n,.;:})\]]|\/\/))/.source), lookbehind:!0, greedy:!0, inside:{"regex-source":{pattern:/^(\/)[\s\S]+(?=\/[a-z]*$)/, 
  lookbehind:!0, alias:"language-regex", inside:i.languages.regex}, "regex-delimiter":/^\/|\/$/, "regex-flags":/^[a-z]+$/}}, "function-variable":{pattern:/#?(?!\s)[_$a-zA-Z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*(?=\s*[=:]\s*(?:async\s*)?(?:\bfunction\b|(?:\((?:[^()]|\([^()]*\))*\)|(?!\s)[_$a-zA-Z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*)\s*=>))/, alias:"function"}, parameter:[{pattern:/(function(?:\s+(?!\s)[_$a-zA-Z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*)?\s*\(\s*)(?!\s)(?:[^()\s]|\s+(?![\s)])|\([^()]*\))+(?=\s*\))/, 
  lookbehind:!0, inside:i.languages.javascript}, {pattern:/(^|[^$\w\xA0-\uFFFF])(?!\s)[_$a-z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*(?=\s*=>)/i, lookbehind:!0, inside:i.languages.javascript}, {pattern:/(\(\s*)(?!\s)(?:[^()\s]|\s+(?![\s)])|\([^()]*\))+(?=\s*\)\s*=>)/, lookbehind:!0, inside:i.languages.javascript}, {pattern:/((?:\b|\s|^)(?!(?:as|async|await|break|case|catch|class|const|continue|debugger|default|delete|do|else|enum|export|extends|finally|for|from|function|get|if|implements|import|in|instanceof|interface|let|new|null|of|package|private|protected|public|return|set|static|super|switch|this|throw|try|typeof|undefined|var|void|while|with|yield)(?![$\w\xA0-\uFFFF]))(?:(?!\s)[_$a-zA-Z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*\s*)\(\s*|\]\s*\(\s*)(?!\s)(?:[^()\s]|\s+(?![\s)])|\([^()]*\))+(?=\s*\)\s*\{)/, 
  lookbehind:!0, inside:i.languages.javascript}], constant:/\b[A-Z](?:[A-Z_]|\dx?)*\b/});
  i.languages.insertBefore("javascript", "string", {hashbang:{pattern:/^#!.*/, greedy:!0, alias:"comment"}, "template-string":{pattern:/`(?:\\[\s\S]|\$\{(?:[^{}]|\{(?:[^{}]|\{[^}]*\})*\})+\}|(?!\$\{)[^\\`])*`/, greedy:!0, inside:{"template-punctuation":{pattern:/^`|`$/, alias:"string"}, interpolation:{pattern:/((?:^|[^\\])(?:\\{2})*)\$\{(?:[^{}]|\{(?:[^{}]|\{[^}]*\})*\})+\}/, lookbehind:!0, inside:{"interpolation-punctuation":{pattern:/^\$\{|\}$/, alias:"punctuation"}, rest:i.languages.javascript}}, 
  string:/[\s\S]+/}}, "string-property":{pattern:/((?:^|[,{])[ \t]*)(["'])(?:\\(?:\r\n|[\s\S])|(?!\2)[^\\\r\n])*\2(?=\s*:)/m, lookbehind:!0, greedy:!0, alias:"property"}});
  i.languages.insertBefore("javascript", "operator", {"literal-property":{pattern:/((?:^|[,{])[ \t]*)(?!\s)[_$a-zA-Z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*(?=\s*:)/m, lookbehind:!0, alias:"property"}});
  i.languages.markup && (i.languages.markup.tag.addInlined("script", "javascript"), i.languages.markup.tag.addAttribute(/on(?:abort|blur|change|click|composition(?:end|start|update)|dblclick|error|focus(?:in|out)?|key(?:down|up)|load|mouse(?:down|enter|leave|move|out|over|up)|reset|resize|scroll|select|slotchange|submit|unload|wheel)/.source, "javascript"));
  i.languages.js = i.languages.javascript;
  i.languages.actionscript = i.languages.extend("javascript", {keyword:/\b(?:as|break|case|catch|class|const|default|delete|do|dynamic|each|else|extends|final|finally|for|function|get|if|implements|import|in|include|instanceof|interface|internal|is|namespace|native|new|null|override|package|private|protected|public|return|set|static|super|switch|this|throw|try|typeof|use|var|void|while|with)\b/, operator:/\+\+|--|(?:[+\-*\/%^]|&&?|\|\|?|<<?|>>?>?|[!=]=?)=?|[~?@]/});
  i.languages.actionscript["class-name"].alias = "function";
  delete i.languages.actionscript.parameter;
  delete i.languages.actionscript["literal-property"];
  i.languages.markup && i.languages.insertBefore("actionscript", "string", {xml:{pattern:/(^|[^.])<\/?\w+(?:\s+[^\s>\/=]+=("|')(?:\\[\s\S]|(?!\2)[^\\])*\2)*\s*\/?>/, lookbehind:!0, inside:i.languages.markup}});
  (function(e) {
    var n = /#(?!\{).+/, a = {pattern:/#\{[^}]+\}/, alias:"variable"};
    e.languages.coffeescript = e.languages.extend("javascript", {comment:n, string:[{pattern:/'(?:\\[\s\S]|[^\\'])*'/, greedy:!0}, {pattern:/"(?:\\[\s\S]|[^\\"])*"/, greedy:!0, inside:{interpolation:a}}], keyword:/\b(?:and|break|by|catch|class|continue|debugger|delete|do|each|else|extend|extends|false|finally|for|if|in|instanceof|is|isnt|let|loop|namespace|new|no|not|null|of|off|on|or|own|return|super|switch|then|this|throw|true|try|typeof|undefined|unless|until|when|while|window|with|yes|yield)\b/, 
    "class-member":{pattern:/@(?!\d)\w+/, alias:"variable"}});
    e.languages.insertBefore("coffeescript", "comment", {"multiline-comment":{pattern:/###[\s\S]+?###/, alias:"comment"}, "block-regex":{pattern:/\/{3}[\s\S]*?\/{3}/, alias:"regex", inside:{comment:n, interpolation:a}}});
    e.languages.insertBefore("coffeescript", "string", {"inline-javascript":{pattern:/`(?:\\[\s\S]|[^\\`])*`/, inside:{delimiter:{pattern:/^`|`$/, alias:"punctuation"}, script:{pattern:/[\s\S]+/, alias:"language-javascript", inside:e.languages.javascript}}}, "multiline-string":[{pattern:/'''[\s\S]*?'''/, greedy:!0, alias:"string"}, {pattern:/"""[\s\S]*?"""/, greedy:!0, alias:"string", inside:{interpolation:a}}]});
    e.languages.insertBefore("coffeescript", "keyword", {property:/(?!\d)\w+(?=\s*:(?!:))/});
    delete e.languages.coffeescript["template-string"];
    e.languages.coffee = e.languages.coffeescript;
  })(i);
  (function(e) {
    var n = e.languages.javadoclike = {parameter:{pattern:/(^[\t ]*(?:\/{3}|\*|\/\*\*)\s*@(?:arg|arguments|param)\s+)\w+/m, lookbehind:!0}, keyword:{pattern:/(^[\t ]*(?:\/{3}|\*|\/\*\*)\s*|\{)@[a-z][a-zA-Z-]+\b/m, lookbehind:!0}, punctuation:/[{}]/};
    Object.defineProperty(n, "addSupport", {value:function(a, l) {
      (a = typeof a == "string" ? [a] : a).forEach(function(d) {
        if (w = e.languages[d]) {
          var w, y = w["doc-comment"];
          if ((y = y || (w = e.languages.insertBefore(d, "comment", {"doc-comment":{pattern:/(^|[^\\])\/\*\*[^/][\s\S]*?(?:\*\/|$)/, lookbehind:!0, alias:"comment"}}))["doc-comment"]) instanceof RegExp && (y = w["doc-comment"] = {pattern:y}), Array.isArray(y)) {
            for (d = 0, w = y.length; d < w; d++) {
              y[d] instanceof RegExp && (y[d] = {pattern:y[d]});
              var g = y[d];
              g.inside || (g.inside = {});
              g.inside.rest = l;
            }
          } else {
            y.inside || (y.inside = {}), y.inside.rest = l;
          }
        }
      });
    }});
    n.addSupport(["java", "javascript", "php"], n);
  })(i);
  (function(e) {
    var n = /(?:"(?:\\(?:\r\n|[\s\S])|[^"\\\r\n])*"|'(?:\\(?:\r\n|[\s\S])|[^'\\\r\n])*')/;
    (n = (e.languages.css = {comment:/\/\*[\s\S]*?\*\//, atrule:{pattern:RegExp("@[\\w-](?:" + /[^;{\s"']|\s+(?!\s)/.source + "|" + n.source + ")*?" + /(?:;|(?=\s*\{))/.source), inside:{rule:/^@[\w-]+/, "selector-function-argument":{pattern:/(\bselector\s*\(\s*(?![\s)]))(?:[^()\s]|\s+(?![\s)])|\((?:[^()]|\([^()]*\))*\))+(?=\s*\))/, lookbehind:!0, alias:"selector"}, keyword:{pattern:/(^|[^\w-])(?:and|not|only|or)(?![\w-])/, lookbehind:!0}}}, url:{pattern:RegExp("\\burl\\((?:" + n.source + "|" + /(?:[^\\\r\n()"']|\\[\s\S])*/.source + 
    ")\\)", "i"), greedy:!0, inside:{function:/^url/i, punctuation:/^\(|\)$/, string:{pattern:RegExp("^" + n.source + "$"), alias:"url"}}}, selector:{pattern:RegExp("(^|[{}\\s])[^{}\\s](?:[^{};\"'\\s]|\\s+(?![\\s{])|" + n.source + ")*(?\x3d\\s*\\{)"), lookbehind:!0}, string:{pattern:n, greedy:!0}, property:{pattern:/(^|[^-\w\xA0-\uFFFF])(?!\s)[-_a-z\xA0-\uFFFF](?:(?!\s)[-\w\xA0-\uFFFF])*(?=\s*:)/i, lookbehind:!0}, important:/!important\b/i, function:{pattern:/(^|[^-a-z0-9])[-a-z0-9]+(?=\()/i, lookbehind:!0}, 
    punctuation:/[(){};:,]/}, e.languages.css.atrule.inside.rest = e.languages.css, e.languages.markup)) && (n.tag.addInlined("style", "css"), n.tag.addAttribute("style", "css"));
  })(i);
  (function(e) {
    var n = /("|')(?:\\(?:\r\n|[\s\S])|(?!\1)[^\\\r\n])*\1/;
    n = (e.languages.css.selector = {pattern:e.languages.css.selector.pattern, lookbehind:!0, inside:n = {"pseudo-element":/:(?:after|before|first-letter|first-line|selection)|::[-\w]+/, "pseudo-class":/:[-\w]+/, class:/\.[-\w]+/, id:/#[-\w]+/, attribute:{pattern:RegExp("\\[(?:[^[\\]\"']|" + n.source + ")*\\]"), greedy:!0, inside:{punctuation:/^\[|\]$/, "case-sensitivity":{pattern:/(\s)[si]$/i, lookbehind:!0, alias:"keyword"}, namespace:{pattern:/^(\s*)(?:(?!\s)[-*\w\xA0-\uFFFF])*\|(?!=)/, lookbehind:!0, 
    inside:{punctuation:/\|$/}}, "attr-name":{pattern:/^(\s*)(?:(?!\s)[-\w\xA0-\uFFFF])+/, lookbehind:!0}, "attr-value":[n, {pattern:/(=\s*)(?:(?!\s)[-\w\xA0-\uFFFF])+(?=\s*$)/, lookbehind:!0}], operator:/[|~*^$]?=/}}, "n-th":[{pattern:/(\(\s*)[+-]?\d*[\dn](?:\s*[+-]\s*\d+)?(?=\s*\))/, lookbehind:!0, inside:{number:/[\dn]+/, operator:/[+-]/}}, {pattern:/(\(\s*)(?:even|odd)(?=\s*\))/i, lookbehind:!0}], combinator:/>|\+|~|\|\|/, punctuation:/[(),]/}}, e.languages.css.atrule.inside["selector-function-argument"].inside = 
    n, e.languages.insertBefore("css", "property", {variable:{pattern:/(^|[^-\w\xA0-\uFFFF])--(?!\s)[-_a-z\xA0-\uFFFF](?:(?!\s)[-\w\xA0-\uFFFF])*/i, lookbehind:!0}}), {pattern:/(\b\d+)(?:%|[a-z]+(?![\w-]))/, lookbehind:!0});
    var a = {pattern:/(^|[^\w.-])-?(?:\d+(?:\.\d+)?|\.\d+)/, lookbehind:!0};
    e.languages.insertBefore("css", "function", {operator:{pattern:/(\s)[+\-*\/](?=\s)/, lookbehind:!0}, hexcode:{pattern:/\B#[\da-f]{3,8}\b/i, alias:"color"}, color:[{pattern:/(^|[^\w-])(?:AliceBlue|AntiqueWhite|Aqua|Aquamarine|Azure|Beige|Bisque|Black|BlanchedAlmond|Blue|BlueViolet|Brown|BurlyWood|CadetBlue|Chartreuse|Chocolate|Coral|CornflowerBlue|Cornsilk|Crimson|Cyan|DarkBlue|DarkCyan|DarkGoldenRod|DarkGr[ae]y|DarkGreen|DarkKhaki|DarkMagenta|DarkOliveGreen|DarkOrange|DarkOrchid|DarkRed|DarkSalmon|DarkSeaGreen|DarkSlateBlue|DarkSlateGr[ae]y|DarkTurquoise|DarkViolet|DeepPink|DeepSkyBlue|DimGr[ae]y|DodgerBlue|FireBrick|FloralWhite|ForestGreen|Fuchsia|Gainsboro|GhostWhite|Gold|GoldenRod|Gr[ae]y|Green|GreenYellow|HoneyDew|HotPink|IndianRed|Indigo|Ivory|Khaki|Lavender|LavenderBlush|LawnGreen|LemonChiffon|LightBlue|LightCoral|LightCyan|LightGoldenRodYellow|LightGr[ae]y|LightGreen|LightPink|LightSalmon|LightSeaGreen|LightSkyBlue|LightSlateGr[ae]y|LightSteelBlue|LightYellow|Lime|LimeGreen|Linen|Magenta|Maroon|MediumAquaMarine|MediumBlue|MediumOrchid|MediumPurple|MediumSeaGreen|MediumSlateBlue|MediumSpringGreen|MediumTurquoise|MediumVioletRed|MidnightBlue|MintCream|MistyRose|Moccasin|NavajoWhite|Navy|OldLace|Olive|OliveDrab|Orange|OrangeRed|Orchid|PaleGoldenRod|PaleGreen|PaleTurquoise|PaleVioletRed|PapayaWhip|PeachPuff|Peru|Pink|Plum|PowderBlue|Purple|RebeccaPurple|Red|RosyBrown|RoyalBlue|SaddleBrown|Salmon|SandyBrown|SeaGreen|SeaShell|Sienna|Silver|SkyBlue|SlateBlue|SlateGr[ae]y|Snow|SpringGreen|SteelBlue|Tan|Teal|Thistle|Tomato|Transparent|Turquoise|Violet|Wheat|White|WhiteSmoke|Yellow|YellowGreen)(?![\w-])/i, 
    lookbehind:!0}, {pattern:/\b(?:hsl|rgb)\(\s*\d{1,3}\s*,\s*\d{1,3}%?\s*,\s*\d{1,3}%?\s*\)\B|\b(?:hsl|rgb)a\(\s*\d{1,3}\s*,\s*\d{1,3}%?\s*,\s*\d{1,3}%?\s*,\s*(?:0|0?\.\d+|1)\s*\)\B/i, inside:{unit:n, number:a, function:/[\w-]+(?=\()/, punctuation:/[(),]/}}], entity:/\\[\da-f]{1,8}/i, unit:n, number:a});
  })(i);
  (function(e) {
    function x(w, y) {
      y = (y || "").replace(/m/g, "") + "m";
      var b = /([:\-,[{]\s*(?:\s<<prop>>[ \t]+)?)(?:<<value>>)(?=[ \t]*(?:$|,|\]|\}|(?:[\r\n]\s*)?#))/.source.replace(/<<prop>>/g, function() {
        return l;
      }).replace(/<<value>>/g, function() {
        return w;
      });
      return RegExp(b, y);
    }
    var n = /[*&][^\s[\]{},]+/, a = /!(?:<[\w\-%#;/?:@&=+$,.!~*'()[\]]+>|(?:[a-zA-Z\d-]*!)?[\w\-%#;/?:@&=+$.~*'()]+)?/, l = "(?:" + a.source + "(?:[ \t]+" + n.source + ")?|" + n.source + "(?:[ \t]+" + a.source + ")?)", d = /(?:[^\s\x00-\x08\x0e-\x1f!"#%&'*,\-:>?@[\]`{|}\x7f-\x84\x86-\x9f\ud800-\udfff\ufffe\uffff]|[?:-]<PLAIN>)(?:[ \t]*(?:(?![#:])<PLAIN>|:<PLAIN>))*/.source.replace(/<PLAIN>/g, function() {
      return /[^\s\x00-\x08\x0e-\x1f,[\]{}\x7f-\x84\x86-\x9f\ud800-\udfff\ufffe\uffff]/.source;
    }), h = /"(?:[^"\\\r\n]|\\.)*"|'(?:[^'\\\r\n]|\\.)*'/.source;
    e.languages.yaml = {scalar:{pattern:RegExp(/([\-:]\s*(?:\s<<prop>>[ \t]+)?[|>])[ \t]*(?:((?:\r?\n|\r)[ \t]+)\S[^\r\n]*(?:\2[^\r\n]+)*)/.source.replace(/<<prop>>/g, function() {
      return l;
    })), lookbehind:!0, alias:"string"}, comment:/#.*/, key:{pattern:RegExp(/((?:^|[:\-,[{\r\n?])[ \t]*(?:<<prop>>[ \t]+)?)<<key>>(?=\s*:\s)/.source.replace(/<<prop>>/g, function() {
      return l;
    }).replace(/<<key>>/g, function() {
      return "(?:" + d + "|" + h + ")";
    })), lookbehind:!0, greedy:!0, alias:"atrule"}, directive:{pattern:/(^[ \t]*)%.+/m, lookbehind:!0, alias:"important"}, datetime:{pattern:x(/\d{4}-\d\d?-\d\d?(?:[tT]|[ \t]+)\d\d?:\d{2}:\d{2}(?:\.\d*)?(?:[ \t]*(?:Z|[-+]\d\d?(?::\d{2})?))?|\d{4}-\d{2}-\d{2}|\d\d?:\d{2}(?::\d{2}(?:\.\d*)?)?/.source), lookbehind:!0, alias:"number"}, boolean:{pattern:x(/false|true/.source, "i"), lookbehind:!0, alias:"important"}, null:{pattern:x(/null|~/.source, "i"), lookbehind:!0, alias:"important"}, string:{pattern:x(h), 
    lookbehind:!0, greedy:!0}, number:{pattern:x(/[+-]?(?:0x[\da-f]+|0o[0-7]+|(?:\d+(?:\.\d*)?|\.\d+)(?:e[+-]?\d+)?|\.inf|\.nan)/.source, "i"), lookbehind:!0}, tag:a, important:n, punctuation:/---|[:[\]{}\-,|>?]|\.\.\./};
    e.languages.yml = e.languages.yaml;
  })(i);
  (function(e) {
    function a(b) {
      return b = b.replace(/<inner>/g, function() {
        return n;
      }), RegExp(/((?:^|[^\\])(?:\\{2})*)/.source + "(?:" + b + ")");
    }
    var n = /(?:\\.|[^\\\n\r]|(?:\n|\r\n?)(?![\r\n]))/.source, l = /(?:\\.|``(?:[^`\r\n]|`(?!`))+``|`[^`\r\n]+`|[^\\|\r\n`])+/.source, d = /\|?__(?:\|__)+\|?(?:(?:\n|\r\n?)|(?![\s\S]))/.source.replace(/__/g, function() {
      return l;
    }), h = /\|?[ \t]*:?-{3,}:?[ \t]*(?:\|[ \t]*:?-{3,}:?[ \t]*)+\|?(?:\n|\r\n?)/.source, x = (e.languages.markdown = e.languages.extend("markup", {}), e.languages.insertBefore("markdown", "prolog", {"front-matter-block":{pattern:/(^(?:\s*[\r\n])?)---(?!.)[\s\S]*?[\r\n]---(?!.)/, lookbehind:!0, greedy:!0, inside:{punctuation:/^---|---$/, "front-matter":{pattern:/\S+(?:\s+\S+)*/, alias:["yaml", "language-yaml"], inside:e.languages.yaml}}}, blockquote:{pattern:/^>(?:[\t ]*>)*/m, alias:"punctuation"}, 
    table:{pattern:RegExp("^" + d + h + "(?:" + d + ")*", "m"), inside:{"table-data-rows":{pattern:RegExp("^(" + d + h + ")(?:" + d + ")*$"), lookbehind:!0, inside:{"table-data":{pattern:RegExp(l), inside:e.languages.markdown}, punctuation:/\|/}}, "table-line":{pattern:RegExp("^(" + d + ")" + h + "$"), lookbehind:!0, inside:{punctuation:/\||:?-{3,}:?/}}, "table-header-row":{pattern:RegExp("^" + d + "$"), inside:{"table-header":{pattern:RegExp(l), alias:"important", inside:e.languages.markdown}, punctuation:/\|/}}}}, 
    code:[{pattern:/((?:^|\n)[ \t]*\n|(?:^|\r\n?)[ \t]*\r\n?)(?: {4}|\t).+(?:(?:\n|\r\n?)(?: {4}|\t).+)*/, lookbehind:!0, alias:"keyword"}, {pattern:/^```[\s\S]*?^```$/m, greedy:!0, inside:{"code-block":{pattern:/^(```.*(?:\n|\r\n?))[\s\S]+?(?=(?:\n|\r\n?)^```$)/m, lookbehind:!0}, "code-language":{pattern:/^(```).+/, lookbehind:!0}, punctuation:/```/}}], title:[{pattern:/\S.*(?:\n|\r\n?)(?:==+|--+)(?=[ \t]*$)/m, alias:"important", inside:{punctuation:/==+$|--+$/}}, {pattern:/(^\s*)#.+/m, lookbehind:!0, 
    alias:"important", inside:{punctuation:/^#+|#+$/}}], hr:{pattern:/(^\s*)([*-])(?:[\t ]*\2){2,}(?=\s*$)/m, lookbehind:!0, alias:"punctuation"}, list:{pattern:/(^\s*)(?:[*+-]|\d+\.)(?=[\t ].)/m, lookbehind:!0, alias:"punctuation"}, "url-reference":{pattern:/!?\[[^\]]+\]:[\t ]+(?:\S+|<(?:\\.|[^>\\])+>)(?:[\t ]+(?:"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|\((?:\\.|[^)\\])*\)))?/, inside:{variable:{pattern:/^(!?\[)[^\]]+/, lookbehind:!0}, string:/(?:"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|\((?:\\.|[^)\\])*\))$/, 
    punctuation:/^[\[\]!:]|[<>]/}, alias:"url"}, bold:{pattern:a(/\b__(?:(?!_)<inner>|_(?:(?!_)<inner>)+_)+__\b|\*\*(?:(?!\*)<inner>|\*(?:(?!\*)<inner>)+\*)+\*\*/.source), lookbehind:!0, greedy:!0, inside:{content:{pattern:/(^..)[\s\S]+(?=..$)/, lookbehind:!0, inside:{}}, punctuation:/\*\*|__/}}, italic:{pattern:a(/\b_(?:(?!_)<inner>|__(?:(?!_)<inner>)+__)+_\b|\*(?:(?!\*)<inner>|\*\*(?:(?!\*)<inner>)+\*\*)+\*/.source), lookbehind:!0, greedy:!0, inside:{content:{pattern:/(^.)[\s\S]+(?=.$)/, lookbehind:!0, 
    inside:{}}, punctuation:/[*_]/}}, strike:{pattern:a(/(~~?)(?:(?!~)<inner>)+\2/.source), lookbehind:!0, greedy:!0, inside:{content:{pattern:/(^~~?)[\s\S]+(?=\1$)/, lookbehind:!0, inside:{}}, punctuation:/~~?/}}, "code-snippet":{pattern:/(^|[^\\`])(?:``[^`\r\n]+(?:`[^`\r\n]+)*``(?!`)|`[^`\r\n]+`(?!`))/, lookbehind:!0, greedy:!0, alias:["code", "keyword"]}, url:{pattern:a(/!?\[(?:(?!\])<inner>)+\](?:\([^\s)]+(?:[\t ]+"(?:\\.|[^"\\])*")?\)|[ \t]?\[(?:(?!\])<inner>)+\])/.source), lookbehind:!0, greedy:!0, 
    inside:{operator:/^!/, content:{pattern:/(^\[)[^\]]+(?=\])/, lookbehind:!0, inside:{}}, variable:{pattern:/(^\][ \t]?\[)[^\]]+(?=\]$)/, lookbehind:!0}, url:{pattern:/(^\]\()[^\s)]+/, lookbehind:!0}, string:{pattern:/(^[ \t]+)"(?:\\.|[^"\\])*"(?=\)$)/, lookbehind:!0}}}}), ["url", "bold", "italic", "strike"].forEach(function(b) {
      ["url", "bold", "italic", "strike", "code-snippet"].forEach(function(S) {
        b !== S && (e.languages.markdown[b].inside.content.inside[S] = e.languages.markdown[S]);
      });
    }), e.hooks.add("after-tokenize", function(b) {
      b.language !== "markdown" && b.language !== "md" || function S(g) {
        if (g && typeof g != "string") {
          for (var k = 0, E = g.length; k < E; k++) {
            var o, s = g[k];
            s.type !== "code" ? S(s.content) : (o = s.content[1], s = s.content[3], o && s && o.type === "code-language" && s.type === "code-block" && typeof o.content == "string" && (o = o.content.replace(/\b#/g, "sharp").replace(/\b\+\+/g, "pp"), o = "language-" + (o = (/[a-z][\w-]*/i.exec(o) || [""])[0].toLowerCase()), s.alias ? typeof s.alias == "string" ? s.alias = [s.alias, o] : s.alias.push(o) : s.alias = [o]));
          }
        }
      }(b.tokens);
    }), e.hooks.add("wrap", function(b) {
      if (b.type === "code-block") {
        for (var S = "", g = 0, k = b.classes.length; g < k; g++) {
          var E = b.classes[g];
          if (E = /language-(.+)/.exec(E)) {
            S = E[1];
            break;
          }
        }
        var o;
        (g = e.languages[S]) ? b.content = e.highlight(function(c) {
          return c = c.replace(x, ""), c = c.replace(/&(\w{1,8}|#x?[\da-f]{1,8});/gi, function(f, u) {
            var m;
            return (u = u.toLowerCase())[0] === "#" ? (m = u[1] === "x" ? parseInt(u.slice(2), 16) : Number(u.slice(1)), y(m)) : w[u] || f;
          });
        }(b.content), g, S) : S && S !== "none" && e.plugins.autoloader && (o = "md-" + (new Date()).valueOf() + "-" + Math.floor(1e16 * Math.random()), b.attributes.id = o, e.plugins.autoloader.loadLanguages(S, function() {
          var c = document.getElementById(o);
          c && (c.innerHTML = e.highlight(c.textContent, e.languages[S], S));
        }));
      }
    }), RegExp(e.languages.markup.tag.pattern.source, "gi")), w = {amp:"\x26", lt:"\x3c", gt:"\x3e", quot:'"'}, y = String.fromCodePoint || String.fromCharCode;
    e.languages.md = e.languages.markdown;
  })(i);
  i.languages.graphql = {comment:/#.*/, description:{pattern:/(?:"""(?:[^"]|(?!""")")*"""|"(?:\\.|[^\\"\r\n])*")(?=\s*[a-z_])/i, greedy:!0, alias:"string", inside:{"language-markdown":{pattern:/(^"(?:"")?)(?!\1)[\s\S]+(?=\1$)/, lookbehind:!0, inside:i.languages.markdown}}}, string:{pattern:/"""(?:[^"]|(?!""")")*"""|"(?:\\.|[^\\"\r\n])*"/, greedy:!0}, number:/(?:\B-|\b)\d+(?:\.\d+)?(?:e[+-]?\d+)?\b/i, boolean:/\b(?:false|true)\b/, variable:/\$[a-z_]\w*/i, directive:{pattern:/@[a-z_]\w*/i, alias:"function"}, 
  "attr-name":{pattern:/\b[a-z_]\w*(?=\s*(?:\((?:[^()"]|"(?:\\.|[^\\"\r\n])*")*\))?:)/i, greedy:!0}, "atom-input":{pattern:/\b[A-Z]\w*Input\b/, alias:"class-name"}, scalar:/\b(?:Boolean|Float|ID|Int|String)\b/, constant:/\b[A-Z][A-Z_\d]*\b/, "class-name":{pattern:/(\b(?:enum|implements|interface|on|scalar|type|union)\s+|&\s*|:\s*|\[)[A-Z_]\w*/, lookbehind:!0}, fragment:{pattern:/(\bfragment\s+|\.{3}\s*(?!on\b))[a-zA-Z_]\w*/, lookbehind:!0, alias:"function"}, "definition-mutation":{pattern:/(\bmutation\s+)[a-zA-Z_]\w*/, 
  lookbehind:!0, alias:"function"}, "definition-query":{pattern:/(\bquery\s+)[a-zA-Z_]\w*/, lookbehind:!0, alias:"function"}, keyword:/\b(?:directive|enum|extend|fragment|implements|input|interface|mutation|on|query|repeatable|scalar|schema|subscription|type|union)\b/, operator:/[!=|&]|\.{3}/, "property-query":/\w+(?=\s*\()/, object:/\w+(?=\s*\{)/, punctuation:/[!(){}\[\]:=,]/, property:/\w+/};
  i.hooks.add("after-tokenize", function(e) {
    function g(o, s) {
      s = s || 0;
      for (var c = 0; c < o.length; c++) {
        var f = n[a + (c + s)];
        if (!f || f.type !== o[c]) {
          return;
        }
      }
      return 1;
    }
    function k(o, s) {
      for (var c = 1, f = a; f < n.length; f++) {
        var u = n[f], m = u.content;
        if (u.type === "punctuation" && typeof m == "string") {
          if (o.test(m)) {
            c++;
          } else if (s.test(m) && --c === 0) {
            return f;
          }
        }
      }
      return -1;
    }
    function E(o, s) {
      var c = o.alias;
      c ? Array.isArray(c) || (o.alias = c = [c]) : o.alias = c = [];
      c.push(s);
    }
    if (e.language === "graphql") {
      for (var n = e.tokens.filter(function(o) {
        return typeof o != "string" && o.type !== "comment" && o.type !== "scalar";
      }), a = 0; a < n.length;) {
        if (e = n[a++], e.type === "keyword" && e.content === "mutation") {
          e = [];
          if (g(["definition-mutation", "punctuation"]) && n[a + 1].content === "(") {
            a += 2;
            var h = k(/^\($/, /^\)$/);
            if (h === -1) {
              continue;
            }
            for (; a < h; a++) {
              var x = n[a + 0];
              x.type === "variable" && (E(x, "variable-input"), e.push(x.content));
            }
            a = h + 1;
          }
          if (g(["punctuation", "property-query"]) && n[a + 0].content === "{" && (a++, E(n[a + 0], "property-mutation"), 0 < e.length) && (h = k(/^\{$/, /^\}$/), h !== -1)) {
            for (x = a; x < h; x++) {
              var b = n[x];
              b.type === "variable" && 0 <= e.indexOf(b.content) && E(b, "variable-input");
            }
          }
        }
      }
    }
  });
  i.languages.sql = {comment:{pattern:/(^|[^\\])(?:\/\*[\s\S]*?\*\/|(?:--|\/\/|#).*)/, lookbehind:!0}, variable:[{pattern:/@(["'`])(?:\\[\s\S]|(?!\1)[^\\])+\1/, greedy:!0}, /@[\w.$]+/], string:{pattern:/(^|[^@\\])("|')(?:\\[\s\S]|(?!\2)[^\\]|\2\2)*\2/, greedy:!0, lookbehind:!0}, identifier:{pattern:/(^|[^@\\])`(?:\\[\s\S]|[^`\\]|``)*`/, greedy:!0, lookbehind:!0, inside:{punctuation:/^`|`$/}}, function:/\b(?:AVG|COUNT|FIRST|FORMAT|LAST|LCASE|LEN|MAX|MID|MIN|MOD|NOW|ROUND|SUM|UCASE)(?=\s*\()/i, keyword:/\b(?:ACTION|ADD|AFTER|ALGORITHM|ALL|ALTER|ANALYZE|ANY|APPLY|AS|ASC|AUTHORIZATION|AUTO_INCREMENT|BACKUP|BDB|BEGIN|BERKELEYDB|BIGINT|BINARY|BIT|BLOB|BOOL|BOOLEAN|BREAK|BROWSE|BTREE|BULK|BY|CALL|CASCADED?|CASE|CHAIN|CHAR(?:ACTER|SET)?|CHECK(?:POINT)?|CLOSE|CLUSTERED|COALESCE|COLLATE|COLUMNS?|COMMENT|COMMIT(?:TED)?|COMPUTE|CONNECT|CONSISTENT|CONSTRAINT|CONTAINS(?:TABLE)?|CONTINUE|CONVERT|CREATE|CROSS|CURRENT(?:_DATE|_TIME|_TIMESTAMP|_USER)?|CURSOR|CYCLE|DATA(?:BASES?)?|DATE(?:TIME)?|DAY|DBCC|DEALLOCATE|DEC|DECIMAL|DECLARE|DEFAULT|DEFINER|DELAYED|DELETE|DELIMITERS?|DENY|DESC|DESCRIBE|DETERMINISTIC|DISABLE|DISCARD|DISK|DISTINCT|DISTINCTROW|DISTRIBUTED|DO|DOUBLE|DROP|DUMMY|DUMP(?:FILE)?|DUPLICATE|ELSE(?:IF)?|ENABLE|ENCLOSED|END|ENGINE|ENUM|ERRLVL|ERRORS|ESCAPED?|EXCEPT|EXEC(?:UTE)?|EXISTS|EXIT|EXPLAIN|EXTENDED|FETCH|FIELDS|FILE|FILLFACTOR|FIRST|FIXED|FLOAT|FOLLOWING|FOR(?: EACH ROW)?|FORCE|FOREIGN|FREETEXT(?:TABLE)?|FROM|FULL|FUNCTION|GEOMETRY(?:COLLECTION)?|GLOBAL|GOTO|GRANT|GROUP|HANDLER|HASH|HAVING|HOLDLOCK|HOUR|IDENTITY(?:COL|_INSERT)?|IF|IGNORE|IMPORT|INDEX|INFILE|INNER|INNODB|INOUT|INSERT|INT|INTEGER|INTERSECT|INTERVAL|INTO|INVOKER|ISOLATION|ITERATE|JOIN|KEYS?|KILL|LANGUAGE|LAST|LEAVE|LEFT|LEVEL|LIMIT|LINENO|LINES|LINESTRING|LOAD|LOCAL|LOCK|LONG(?:BLOB|TEXT)|LOOP|MATCH(?:ED)?|MEDIUM(?:BLOB|INT|TEXT)|MERGE|MIDDLEINT|MINUTE|MODE|MODIFIES|MODIFY|MONTH|MULTI(?:LINESTRING|POINT|POLYGON)|NATIONAL|NATURAL|NCHAR|NEXT|NO|NONCLUSTERED|NULLIF|NUMERIC|OFF?|OFFSETS?|ON|OPEN(?:DATASOURCE|QUERY|ROWSET)?|OPTIMIZE|OPTION(?:ALLY)?|ORDER|OUT(?:ER|FILE)?|OVER|PARTIAL|PARTITION|PERCENT|PIVOT|PLAN|POINT|POLYGON|PRECEDING|PRECISION|PREPARE|PREV|PRIMARY|PRINT|PRIVILEGES|PROC(?:EDURE)?|PUBLIC|PURGE|QUICK|RAISERROR|READS?|REAL|RECONFIGURE|REFERENCES|RELEASE|RENAME|REPEAT(?:ABLE)?|REPLACE|REPLICATION|REQUIRE|RESIGNAL|RESTORE|RESTRICT|RETURN(?:ING|S)?|REVOKE|RIGHT|ROLLBACK|ROUTINE|ROW(?:COUNT|GUIDCOL|S)?|RTREE|RULE|SAVE(?:POINT)?|SCHEMA|SECOND|SELECT|SERIAL(?:IZABLE)?|SESSION(?:_USER)?|SET(?:USER)?|SHARE|SHOW|SHUTDOWN|SIMPLE|SMALLINT|SNAPSHOT|SOME|SONAME|SQL|START(?:ING)?|STATISTICS|STATUS|STRIPED|SYSTEM_USER|TABLES?|TABLESPACE|TEMP(?:ORARY|TABLE)?|TERMINATED|TEXT(?:SIZE)?|THEN|TIME(?:STAMP)?|TINY(?:BLOB|INT|TEXT)|TOP?|TRAN(?:SACTIONS?)?|TRIGGER|TRUNCATE|TSEQUAL|TYPES?|UNBOUNDED|UNCOMMITTED|UNDEFINED|UNION|UNIQUE|UNLOCK|UNPIVOT|UNSIGNED|UPDATE(?:TEXT)?|USAGE|USE|USER|USING|VALUES?|VAR(?:BINARY|CHAR|CHARACTER|YING)|VIEW|WAITFOR|WARNINGS|WHEN|WHERE|WHILE|WITH(?: ROLLUP|IN)?|WORK|WRITE(?:TEXT)?|YEAR)\b/i, 
  boolean:/\b(?:FALSE|NULL|TRUE)\b/i, number:/\b0x[\da-f]+\b|\b\d+(?:\.\d*)?|\B\.\d+\b/i, operator:/[-+*\/=%^~]|&&?|\|\|?|!=?|<(?:=>?|<|>)?|>[>=]?|\b(?:AND|BETWEEN|DIV|ILIKE|IN|IS|LIKE|NOT|OR|REGEXP|RLIKE|SOUNDS LIKE|XOR)\b/i, punctuation:/[;[\]()`,.]/};
  (function(e) {
    function x(g, k) {
      if (e.languages[g]) {
        return {pattern:RegExp("((?:" + k + ")\\s*)" + a), lookbehind:!0, greedy:!0, inside:{"template-punctuation":{pattern:/^`|`$/, alias:"string"}, "embedded-code":{pattern:/[\s\S]+/, alias:g}}};
      }
    }
    function w(g, k, E) {
      return g = {code:g, grammar:k, language:E}, e.hooks.run("before-tokenize", g), g.tokens = e.tokenize(g.code, g.grammar), e.hooks.run("after-tokenize", g), g.tokens;
    }
    function y(g, k, E) {
      var c = e.tokenize(g, {interpolation:{pattern:RegExp(h), lookbehind:!0}}), o = 0, s = {};
      c = w(c.map(function(u) {
        if (typeof u == "string") {
          return u;
        }
        var m, _;
        for (u = u.content; g.indexOf((_ = o++, m = "___" + E.toUpperCase() + "_" + _ + "___")) !== -1;) {
        }
        return s[m] = u, m;
      }).join(""), k, E);
      var f = Object.keys(s);
      return o = 0, function u(m) {
        for (var _ = 0; _ < m.length && !(o >= f.length); _++) {
          var v, C, F, $, z, M, j, N = m[_];
          typeof N == "string" || typeof N.content == "string" ? (v = f[o], (j = (M = typeof N == "string" ? N : N.content).indexOf(v)) !== -1 && (++o, C = M.substring(0, j), z = s[v], F = void 0, ($ = {})["interpolation-punctuation"] = d, ($ = e.tokenize(z, $)).length === 3 && ((F = [1, 1]).push.apply(F, w($[1], e.languages.javascript, "javascript")), $.splice.apply($, F)), F = new e.Token("interpolation", $, l.alias, z), $ = M.substring(j + v.length), z = [], C && z.push(C), z.push(F), $ && (u(M = 
          [$]), z.push.apply(z, M)), typeof N == "string" ? (m.splice.apply(m, [_, 1].concat(z)), _ += z.length - 1) : N.content = z)) : (j = N.content, Array.isArray(j) ? u(j) : u([j]));
        }
      }(c), new e.Token(E, c, "language-" + E, g);
    }
    function S(g) {
      return typeof g == "string" ? g : Array.isArray(g) ? g.map(S).join("") : S(g.content);
    }
    var n = e.languages.javascript["template-string"], a = n.pattern.source, l = n.inside.interpolation, d = l.inside["interpolation-punctuation"], h = l.pattern.source;
    e.languages.javascript["template-string"] = [x("css", /\b(?:styled(?:\([^)]*\))?(?:\s*\.\s*\w+(?:\([^)]*\))*)*|css(?:\s*\.\s*(?:global|resolve))?|createGlobalStyle|keyframes)/.source), x("html", /\bhtml|\.\s*(?:inner|outer)HTML\s*\+?=/.source), x("svg", /\bsvg/.source), x("markdown", /\b(?:markdown|md)/.source), x("graphql", /\b(?:gql|graphql(?:\s*\.\s*experimental)?)/.source), x("sql", /\bsql/.source), n].filter(Boolean);
    var b = {javascript:!0, js:!0, typescript:!0, ts:!0, jsx:!0, tsx:!0};
    e.hooks.add("after-tokenize", function(g) {
      g.language in b && function k(E) {
        for (var o = 0, s = E.length; o < s; o++) {
          var c, f, u, m = E[o];
          typeof m != "string" && (c = m.content, Array.isArray(c) ? m.type === "template-string" ? (m = c[1], c.length === 3 && typeof m != "string" && m.type === "embedded-code" && (f = S(m), m = m.alias, m = Array.isArray(m) ? m[0] : m, u = e.languages[m]) && (c[1] = y(f, u, m))) : k(c) : typeof c != "string" && k([c]));
        }
      }(g.tokens);
    });
  })(i);
  (function(e) {
    e.languages.typescript = e.languages.extend("javascript", {"class-name":{pattern:/(\b(?:class|extends|implements|instanceof|interface|new|type)\s+)(?!keyof\b)(?!\s)[_$a-zA-Z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*(?:\s*<(?:[^<>]|<(?:[^<>]|<[^<>]*>)*>)*>)?/, lookbehind:!0, greedy:!0, inside:null}, builtin:/\b(?:Array|Function|Promise|any|boolean|console|never|number|string|symbol|unknown)\b/});
    e.languages.typescript.keyword.push(/\b(?:abstract|declare|is|keyof|readonly|require)\b/, /\b(?:asserts|infer|interface|module|namespace|type)\b(?=\s*(?:[{_$a-zA-Z\xA0-\uFFFF]|$))/, /\btype\b(?=\s*(?:[\{*]|$))/);
    delete e.languages.typescript.parameter;
    delete e.languages.typescript["literal-property"];
    var n = e.languages.extend("typescript", {});
    delete n["class-name"];
    e.languages.typescript["class-name"].inside = n;
    e.languages.insertBefore("typescript", "function", {decorator:{pattern:/@[$\w\xA0-\uFFFF]+/, inside:{at:{pattern:/^@/, alias:"operator"}, function:/^[\s\S]+/}}, "generic-function":{pattern:/#?(?!\s)[_$a-zA-Z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*\s*<(?:[^<>]|<(?:[^<>]|<[^<>]*>)*>)*>(?=\s*\()/, greedy:!0, inside:{function:/^#?(?!\s)[_$a-zA-Z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*/, generic:{pattern:/<[\s\S]+/, alias:"class-name", inside:n}}}});
    e.languages.ts = e.languages.typescript;
  })(i);
  (function(e) {
    var n = e.languages.javascript, a = /\{(?:[^{}]|\{(?:[^{}]|\{[^{}]*\})*\})+\}/.source, l = "(@(?:arg|argument|param|property)\\s+(?:" + a + "\\s+)?)";
    e.languages.jsdoc = e.languages.extend("javadoclike", {parameter:{pattern:RegExp(l + /(?:(?!\s)[$\w\xA0-\uFFFF.])+(?=\s|$)/.source), lookbehind:!0, inside:{punctuation:/\./}}});
    e.languages.insertBefore("jsdoc", "keyword", {"optional-parameter":{pattern:RegExp(l + /\[(?:(?!\s)[$\w\xA0-\uFFFF.])+(?:=[^[\]]+)?\](?=\s|$)/.source), lookbehind:!0, inside:{parameter:{pattern:/(^\[)[$\w\xA0-\uFFFF\.]+/, lookbehind:!0, inside:{punctuation:/\./}}, code:{pattern:/(=)[\s\S]*(?=\]$)/, lookbehind:!0, inside:n, alias:"language-javascript"}, punctuation:/[=[\]]/}}, "class-name":[{pattern:RegExp(/(@(?:augments|class|extends|interface|memberof!?|template|this|typedef)\s+(?:<TYPE>\s+)?)[A-Z]\w*(?:\.[A-Z]\w*)*/.source.replace(/<TYPE>/g, 
    function() {
      return a;
    })), lookbehind:!0, inside:{punctuation:/\./}}, {pattern:RegExp("(@[a-z]+\\s+)" + a), lookbehind:!0, inside:{string:n.string, number:n.number, boolean:n.boolean, keyword:e.languages.typescript.keyword, operator:/=>|\.\.\.|[&|?:*]/, punctuation:/[.,;=<>{}()[\]]/}}], example:{pattern:/(@example\s+(?!\s))(?:[^@\s]|\s+(?!\s))+?(?=\s*(?:\*\s*)?(?:@\w|\*\/))/, lookbehind:!0, inside:{code:{pattern:/^([\t ]*(?:\*\s*)?)\S.*$/m, lookbehind:!0, inside:n, alias:"language-javascript"}}}});
    e.languages.javadoclike.addSupport("javascript", e.languages.jsdoc);
  })(i);
  (function(e) {
    e.languages.flow = e.languages.extend("javascript", {});
    e.languages.insertBefore("flow", "keyword", {type:[{pattern:/\b(?:[Bb]oolean|Function|[Nn]umber|[Ss]tring|[Ss]ymbol|any|mixed|null|void)\b/, alias:"class-name"}]});
    e.languages.flow["function-variable"].pattern = /(?!\s)[_$a-z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*(?=\s*=\s*(?:function\b|(?:\([^()]*\)(?:\s*:\s*\w+)?|(?!\s)[_$a-z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*)\s*=>))/i;
    delete e.languages.flow.parameter;
    e.languages.insertBefore("flow", "operator", {"flow-punctuation":{pattern:/\{\||\|\}/, alias:"punctuation"}});
    Array.isArray(e.languages.flow.keyword) || (e.languages.flow.keyword = [e.languages.flow.keyword]);
    e.languages.flow.keyword.unshift({pattern:/(^|[^$]\b)(?:Class|declare|opaque|type)\b(?!\$)/, lookbehind:!0}, {pattern:/(^|[^$]\B)\$(?:Diff|Enum|Exact|Keys|ObjMap|PropertyType|Record|Shape|Subtype|Supertype|await)\b(?!\$)/, lookbehind:!0});
  })(i);
  i.languages.n4js = i.languages.extend("javascript", {keyword:/\b(?:Array|any|boolean|break|case|catch|class|const|constructor|continue|debugger|declare|default|delete|do|else|enum|export|extends|false|finally|for|from|function|get|if|implements|import|in|instanceof|interface|let|module|new|null|number|package|private|protected|public|return|set|static|string|super|switch|this|throw|true|try|typeof|var|void|while|with|yield)\b/});
  i.languages.insertBefore("n4js", "constant", {annotation:{pattern:/@+\w+/, alias:"operator"}});
  i.languages.n4jsd = i.languages.n4js;
  (function(e) {
    function n(x, w) {
      return RegExp(x.replace(/<ID>/g, function() {
        return /(?!\s)[_$a-zA-Z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*/.source;
      }), w);
    }
    e.languages.insertBefore("javascript", "function-variable", {"method-variable":{pattern:RegExp("(\\.\\s*)" + e.languages.javascript["function-variable"].pattern.source), lookbehind:!0, alias:["function-variable", "method", "function", "property-access"]}});
    e.languages.insertBefore("javascript", "function", {method:{pattern:RegExp("(\\.\\s*)" + e.languages.javascript.function.source), lookbehind:!0, alias:["function", "property-access"]}});
    e.languages.insertBefore("javascript", "constant", {"known-class-name":[{pattern:/\b(?:(?:Float(?:32|64)|(?:Int|Uint)(?:8|16|32)|Uint8Clamped)?Array|ArrayBuffer|BigInt|Boolean|DataView|Date|Error|Function|Intl|JSON|(?:Weak)?(?:Map|Set)|Math|Number|Object|Promise|Proxy|Reflect|RegExp|String|Symbol|WebAssembly)\b/, alias:"class-name"}, {pattern:/\b(?:[A-Z]\w*)Error\b/, alias:"class-name"}]});
    e.languages.insertBefore("javascript", "keyword", {imports:{pattern:n(/(\bimport\b\s*)(?:<ID>(?:\s*,\s*(?:\*\s*as\s+<ID>|\{[^{}]*\}))?|\*\s*as\s+<ID>|\{[^{}]*\})(?=\s*\bfrom\b)/.source), lookbehind:!0, inside:e.languages.javascript}, exports:{pattern:n(/(\bexport\b\s*)(?:\*(?:\s*as\s+<ID>)?(?=\s*\bfrom\b)|\{[^{}]*\})/.source), lookbehind:!0, inside:e.languages.javascript}});
    e.languages.javascript.keyword.unshift({pattern:/\b(?:as|default|export|from|import)\b/, alias:"module"}, {pattern:/\b(?:await|break|catch|continue|do|else|finally|for|if|return|switch|throw|try|while|yield)\b/, alias:"control-flow"}, {pattern:/\bnull\b/, alias:["null", "nil"]}, {pattern:/\bundefined\b/, alias:"nil"});
    e.languages.insertBefore("javascript", "operator", {spread:{pattern:/\.{3}/, alias:"operator"}, arrow:{pattern:/=>/, alias:"operator"}});
    e.languages.insertBefore("javascript", "punctuation", {"property-access":{pattern:n(/(\.\s*)#?<ID>/.source), lookbehind:!0}, "maybe-class-name":{pattern:/(^|[^$\w\xA0-\uFFFF])[A-Z][$\w\xA0-\uFFFF]+/, lookbehind:!0}, dom:{pattern:/\b(?:document|(?:local|session)Storage|location|navigator|performance|window)\b/, alias:"variable"}, console:{pattern:/\bconsole(?=\s*\.)/, alias:"class-name"}});
    for (var a = ["function", "function-variable", "method", "method-variable", "property-access"], l = 0; l < a.length; l++) {
      var h = a[l], d = e.languages.javascript[h];
      h = (d = e.util.type(d) === "RegExp" ? e.languages.javascript[h] = {pattern:d} : d).inside || {};
      (d.inside = h)["maybe-class-name"] = /^[A-Z][\s\S]*/;
    }
  })(i);
  (function(e) {
    function h(y, b) {
      return y = y.replace(/<S>/g, function() {
        return a;
      }).replace(/<BRACES>/g, function() {
        return l;
      }).replace(/<SPREAD>/g, function() {
        return d;
      }), RegExp(y, b);
    }
    function x(y) {
      for (var b = [], S = 0; S < y.length; S++) {
        var g = y[S], k = !1;
        typeof g != "string" && (g.type === "tag" && g.content[0] && g.content[0].type === "tag" ? g.content[0].content[0].content === "\x3c/" ? 0 < b.length && b[b.length - 1].tagName === w(g.content[0].content[1]) && b.pop() : g.content[g.content.length - 1].content !== "/\x3e" && b.push({tagName:w(g.content[0].content[1]), openedBraces:0}) : 0 < b.length && g.type === "punctuation" && g.content === "{" ? b[b.length - 1].openedBraces++ : 0 < b.length && 0 < b[b.length - 1].openedBraces && g.type === 
        "punctuation" && g.content === "}" ? b[b.length - 1].openedBraces-- : k = !0);
        (k || typeof g == "string") && 0 < b.length && b[b.length - 1].openedBraces === 0 && (k = w(g), S < y.length - 1 && (typeof y[S + 1] == "string" || y[S + 1].type === "plain-text") && (k += w(y[S + 1]), y.splice(S + 1, 1)), 0 < S && (typeof y[S - 1] == "string" || y[S - 1].type === "plain-text") && (k = w(y[S - 1]) + k, y.splice(S - 1, 1), S--), y[S] = new e.Token("plain-text", k, null, k));
        g.content && typeof g.content != "string" && x(g.content);
      }
    }
    var n = e.util.clone(e.languages.javascript), a = /(?:\s|\/\/.*(?!.)|\/\*(?:[^*]|\*(?!\/))\*\/)/.source, l = /(?:\{(?:\{(?:\{[^{}]*\}|[^{}])*\}|[^{}])*\})/.source, d = /(?:\{<S>*\.{3}(?:[^{}]|<BRACES>)*\})/.source;
    d = h(d).source;
    e.languages.jsx = e.languages.extend("markup", n);
    e.languages.jsx.tag.pattern = h(/<\/?(?:[\w.:-]+(?:<S>+(?:[\w.:$-]+(?:=(?:"(?:\\[\s\S]|[^\\"])*"|'(?:\\[\s\S]|[^\\'])*'|[^\s{'"/>=]+|<BRACES>))?|<SPREAD>))*<S>*\/?)?>/.source);
    e.languages.jsx.tag.inside.tag.pattern = /^<\/?[^\s>\/]*/;
    e.languages.jsx.tag.inside["attr-value"].pattern = /=(?!\{)(?:"(?:\\[\s\S]|[^\\"])*"|'(?:\\[\s\S]|[^\\'])*'|[^\s'">]+)/;
    e.languages.jsx.tag.inside.tag.inside["class-name"] = /^[A-Z]\w*(?:\.[A-Z]\w*)*$/;
    e.languages.jsx.tag.inside.comment = n.comment;
    e.languages.insertBefore("inside", "attr-name", {spread:{pattern:h(/<SPREAD>/.source), inside:e.languages.jsx}}, e.languages.jsx.tag);
    e.languages.insertBefore("inside", "special-attr", {script:{pattern:h(/=<BRACES>/.source), alias:"language-javascript", inside:{"script-punctuation":{pattern:/^=(?=\{)/, alias:"punctuation"}, rest:e.languages.jsx}}}, e.languages.jsx.tag);
    var w = function(y) {
      return y ? typeof y == "string" ? y : typeof y.content == "string" ? y.content : y.content.map(w).join("") : "";
    };
    e.hooks.add("after-tokenize", function(y) {
      y.language !== "jsx" && y.language !== "tsx" || x(y.tokens);
    });
  })(i);
  (function(e) {
    var n = e.util.clone(e.languages.typescript);
    n = (e.languages.tsx = e.languages.extend("jsx", n), delete e.languages.tsx.parameter, delete e.languages.tsx["literal-property"], e.languages.tsx.tag);
    n.pattern = RegExp(/(^|[^\w$]|(?=<\/))/.source + "(?:" + n.pattern.source + ")", n.pattern.flags);
    n.lookbehind = !0;
  })(i);
  i.languages.swift = {comment:{pattern:/(^|[^\\:])(?:\/\/.*|\/\*(?:[^/*]|\/(?!\*)|\*(?!\/)|\/\*(?:[^*]|\*(?!\/))*\*\/)*\*\/)/, lookbehind:!0, greedy:!0}, "string-literal":[{pattern:RegExp(/(^|[^"#])/.source + "(?:" + /"(?:\\(?:\((?:[^()]|\([^()]*\))*\)|\r\n|[^(])|[^\\\r\n"])*"/.source + "|" + /"""(?:\\(?:\((?:[^()]|\([^()]*\))*\)|[^(])|[^\\"]|"(?!""))*"""/.source + ")" + /(?!["#])/.source), lookbehind:!0, greedy:!0, inside:{interpolation:{pattern:/(\\\()(?:[^()]|\([^()]*\))*(?=\))/, lookbehind:!0, 
  inside:null}, "interpolation-punctuation":{pattern:/^\)|\\\($/, alias:"punctuation"}, punctuation:/\\(?=[\r\n])/, string:/[\s\S]+/}}, {pattern:RegExp(/(^|[^"#])(#+)/.source + "(?:" + /"(?:\\(?:#+\((?:[^()]|\([^()]*\))*\)|\r\n|[^#])|[^\\\r\n])*?"/.source + "|" + /"""(?:\\(?:#+\((?:[^()]|\([^()]*\))*\)|[^#])|[^\\])*?"""/.source + ")\\2"), lookbehind:!0, greedy:!0, inside:{interpolation:{pattern:/(\\#+\()(?:[^()]|\([^()]*\))*(?=\))/, lookbehind:!0, inside:null}, "interpolation-punctuation":{pattern:/^\)|\\#+\($/, 
  alias:"punctuation"}, string:/[\s\S]+/}}], directive:{pattern:RegExp(/#/.source + "(?:" + /(?:elseif|if)\b/.source + "(?:[ \t]*" + /(?:![ \t]*)?(?:\b\w+\b(?:[ \t]*\((?:[^()]|\([^()]*\))*\))?|\((?:[^()]|\([^()]*\))*\))(?:[ \t]*(?:&&|\|\|))?/.source + ")+|" + /(?:else|endif)\b/.source + ")"), alias:"property", inside:{"directive-name":/^#\w+/, boolean:/\b(?:false|true)\b/, number:/\b\d+(?:\.\d+)*\b/, operator:/!|&&|\|\||[<>]=?/, punctuation:/[(),]/}}, literal:{pattern:/#(?:colorLiteral|column|dsohandle|file(?:ID|Literal|Path)?|function|imageLiteral|line)\b/, 
  alias:"constant"}, "other-directive":{pattern:/#\w+\b/, alias:"property"}, attribute:{pattern:/@\w+/, alias:"atrule"}, "function-definition":{pattern:/(\bfunc\s+)\w+/, lookbehind:!0, alias:"function"}, label:{pattern:/\b(break|continue)\s+\w+|\b[a-zA-Z_]\w*(?=\s*:\s*(?:for|repeat|while)\b)/, lookbehind:!0, alias:"important"}, keyword:/\b(?:Any|Protocol|Self|Type|actor|as|assignment|associatedtype|associativity|async|await|break|case|catch|class|continue|convenience|default|defer|deinit|didSet|do|dynamic|else|enum|extension|fallthrough|fileprivate|final|for|func|get|guard|higherThan|if|import|in|indirect|infix|init|inout|internal|is|isolated|lazy|left|let|lowerThan|mutating|none|nonisolated|nonmutating|open|operator|optional|override|postfix|precedencegroup|prefix|private|protocol|public|repeat|required|rethrows|return|right|safe|self|set|some|static|struct|subscript|super|switch|throw|throws|try|typealias|unowned|unsafe|var|weak|where|while|willSet)\b/, 
  boolean:/\b(?:false|true)\b/, nil:{pattern:/\bnil\b/, alias:"constant"}, "short-argument":/\$\d+\b/, omit:{pattern:/\b_\b/, alias:"keyword"}, number:/\b(?:[\d_]+(?:\.[\de_]+)?|0x[a-f0-9_]+(?:\.[a-f0-9p_]+)?|0b[01_]+|0o[0-7_]+)\b/i, "class-name":/\b[A-Z](?:[A-Z_\d]*[a-z]\w*)?\b/, function:/\b[a-z_]\w*(?=\s*\()/i, constant:/\b(?:[A-Z_]{2,}|k[A-Z][A-Za-z_]+)\b/, operator:/[-+*/%=!<>&|^~?]+|\.[.\-+*/%=!<>&|^~?]+/, punctuation:/[{}[\]();,.:\\]/};
  i.languages.swift["string-literal"].forEach(function(e) {
    e.inside.interpolation.inside = i.languages.swift;
  });
  (function(e) {
    e.languages.kotlin = e.languages.extend("clike", {keyword:{pattern:/(^|[^.])\b(?:abstract|actual|annotation|as|break|by|catch|class|companion|const|constructor|continue|crossinline|data|do|dynamic|else|enum|expect|external|final|finally|for|fun|get|if|import|in|infix|init|inline|inner|interface|internal|is|lateinit|noinline|null|object|open|operator|out|override|package|private|protected|public|reified|return|sealed|set|super|suspend|tailrec|this|throw|to|try|typealias|val|var|vararg|when|where|while)\b/, 
    lookbehind:!0}, function:[{pattern:/(?:`[^\r\n`]+`|\b\w+)(?=\s*\()/, greedy:!0}, {pattern:/(\.)(?:`[^\r\n`]+`|\w+)(?=\s*\{)/, lookbehind:!0, greedy:!0}], number:/\b(?:0[xX][\da-fA-F]+(?:_[\da-fA-F]+)*|0[bB][01]+(?:_[01]+)*|\d+(?:_\d+)*(?:\.\d+(?:_\d+)*)?(?:[eE][+-]?\d+(?:_\d+)*)?[fFL]?)\b/, operator:/\+[+=]?|-[-=>]?|==?=?|!(?:!|==?)?|[\/*%<>]=?|[?:]:?|\.\.|&&|\|\||\b(?:and|inv|or|shl|shr|ushr|xor)\b/});
    delete e.languages.kotlin["class-name"];
    var n = {"interpolation-punctuation":{pattern:/^\$\{?|\}$/, alias:"punctuation"}, expression:{pattern:/[\s\S]+/, inside:e.languages.kotlin}};
    e.languages.insertBefore("kotlin", "string", {"string-literal":[{pattern:/"""(?:[^$]|\$(?:(?!\{)|\{[^{}]*\}))*?"""/, alias:"multiline", inside:{interpolation:{pattern:/\$(?:[a-z_]\w*|\{[^{}]*\})/i, inside:n}, string:/[\s\S]+/}}, {pattern:/"(?:[^"\\\r\n$]|\\.|\$(?:(?!\{)|\{[^{}]*\}))*"/, alias:"singleline", inside:{interpolation:{pattern:/((?:^|[^\\])(?:\\{2})*)\$(?:[a-z_]\w*|\{[^{}]*\})/i, lookbehind:!0, inside:n}, string:/[\s\S]+/}}], char:{pattern:/'(?:[^'\\\r\n]|\\(?:.|u[a-fA-F0-9]{0,4}))'/, 
    greedy:!0}});
    delete e.languages.kotlin.string;
    e.languages.insertBefore("kotlin", "keyword", {annotation:{pattern:/\B@(?:\w+:)?(?:[A-Z]\w*|\[[^\]]+\])/, alias:"builtin"}});
    e.languages.insertBefore("kotlin", "function", {label:{pattern:/\b\w+@|@\w+\b/, alias:"symbol"}});
    e.languages.kt = e.languages.kotlin;
    e.languages.kts = e.languages.kotlin;
  })(i);
  i.languages.c = i.languages.extend("clike", {comment:{pattern:/\/\/(?:[^\r\n\\]|\\(?:\r\n?|\n|(?![\r\n])))*|\/\*[\s\S]*?(?:\*\/|$)/, greedy:!0}, string:{pattern:/"(?:\\(?:\r\n|[\s\S])|[^"\\\r\n])*"/, greedy:!0}, "class-name":{pattern:/(\b(?:enum|struct)\s+(?:__attribute__\s*\(\([\s\S]*?\)\)\s*)?)\w+|\b[a-z]\w*_t\b/, lookbehind:!0}, keyword:/\b(?:_Alignas|_Alignof|_Atomic|_Bool|_Complex|_Generic|_Imaginary|_Noreturn|_Static_assert|_Thread_local|__attribute__|asm|auto|break|case|char|const|continue|default|do|double|else|enum|extern|float|for|goto|if|inline|int|long|register|return|short|signed|sizeof|static|struct|switch|typedef|typeof|union|unsigned|void|volatile|while)\b/, 
  function:/\b[a-z_]\w*(?=\s*\()/i, number:/(?:\b0x(?:[\da-f]+(?:\.[\da-f]*)?|\.[\da-f]+)(?:p[+-]?\d+)?|(?:\b\d+(?:\.\d*)?|\B\.\d+)(?:e[+-]?\d+)?)[ful]{0,4}/i, operator:/>>=?|<<=?|->|([-+&|:])\1|[?:~]|[-+*/%&|^!=<>]=?/});
  i.languages.insertBefore("c", "string", {char:{pattern:/'(?:\\(?:\r\n|[\s\S])|[^'\\\r\n]){0,32}'/, greedy:!0}});
  i.languages.insertBefore("c", "string", {macro:{pattern:/(^[\t ]*)#\s*[a-z](?:[^\r\n\\/]|\/(?!\*)|\/\*(?:[^*]|\*(?!\/))*\*\/|\\(?:\r\n|[\s\S]))*/im, lookbehind:!0, greedy:!0, alias:"property", inside:{string:[{pattern:/^(#\s*include\s*)<[^>]+>/, lookbehind:!0}, i.languages.c.string], char:i.languages.c.char, comment:i.languages.c.comment, "macro-name":[{pattern:/(^#\s*define\s+)\w+\b(?!\()/i, lookbehind:!0}, {pattern:/(^#\s*define\s+)\w+\b(?=\()/i, lookbehind:!0, alias:"function"}], directive:{pattern:/^(#\s*)[a-z]+/, 
  lookbehind:!0, alias:"keyword"}, "directive-hash":/^#/, punctuation:/##|\\(?=[\r\n])/, expression:{pattern:/\S[\s\S]*/, inside:i.languages.c}}}});
  i.languages.insertBefore("c", "function", {constant:/\b(?:EOF|NULL|SEEK_CUR|SEEK_END|SEEK_SET|__DATE__|__FILE__|__LINE__|__TIMESTAMP__|__TIME__|__func__|stderr|stdin|stdout)\b/});
  delete i.languages.c.boolean;
  i.languages.objectivec = i.languages.extend("c", {string:{pattern:/@?"(?:\\(?:\r\n|[\s\S])|[^"\\\r\n])*"/, greedy:!0}, keyword:/\b(?:asm|auto|break|case|char|const|continue|default|do|double|else|enum|extern|float|for|goto|if|in|inline|int|long|register|return|self|short|signed|sizeof|static|struct|super|switch|typedef|typeof|union|unsigned|void|volatile|while)\b|(?:@interface|@end|@implementation|@protocol|@class|@public|@protected|@private|@property|@try|@catch|@finally|@throw|@synthesize|@dynamic|@selector)\b/, 
  operator:/-[->]?|\+\+?|!=?|<<?=?|>>?=?|==?|&&?|\|\|?|[~^%?*\/@]/});
  delete i.languages.objectivec["class-name"];
  i.languages.objc = i.languages.objectivec;
  i.languages.reason = i.languages.extend("clike", {string:{pattern:/"(?:\\(?:\r\n|[\s\S])|[^\\\r\n"])*"/, greedy:!0}, "class-name":/\b[A-Z]\w*/, keyword:/\b(?:and|as|assert|begin|class|constraint|do|done|downto|else|end|exception|external|for|fun|function|functor|if|in|include|inherit|initializer|lazy|let|method|module|mutable|new|nonrec|object|of|open|or|private|rec|sig|struct|switch|then|to|try|type|val|virtual|when|while|with)\b/, operator:/\.{3}|:[:=]|\|>|->|=(?:==?|>)?|<=?|>=?|[|^?'#!~`]|[+\-*\/]\.?|\b(?:asr|land|lor|lsl|lsr|lxor|mod)\b/});
  i.languages.insertBefore("reason", "class-name", {char:{pattern:/'(?:\\x[\da-f]{2}|\\o[0-3][0-7][0-7]|\\\d{3}|\\.|[^'\\\r\n])'/, greedy:!0}, constructor:/\b[A-Z]\w*\b(?!\s*\.)/, label:{pattern:/\b[a-z]\w*(?=::)/, alias:"symbol"}});
  delete i.languages.reason.function;
  (function(e) {
    for (var n = /\/\*(?:[^*/]|\*(?!\/)|\/(?!\*)|<self>)*\*\//.source, a = 0; a < 2; a++) {
      n = n.replace(/<self>/g, function() {
        return n;
      });
    }
    n = n.replace(/<self>/g, function() {
      return /[^\s\S]/.source;
    });
    e.languages.rust = {comment:[{pattern:RegExp(/(^|[^\\])/.source + n), lookbehind:!0, greedy:!0}, {pattern:/(^|[^\\:])\/\/.*/, lookbehind:!0, greedy:!0}], string:{pattern:/b?"(?:\\[\s\S]|[^\\"])*"|b?r(#*)"(?:[^"]|"(?!\1))*"\1/, greedy:!0}, char:{pattern:/b?'(?:\\(?:x[0-7][\da-fA-F]|u\{(?:[\da-fA-F]_*){1,6}\}|.)|[^\\\r\n\t'])'/, greedy:!0}, attribute:{pattern:/#!?\[(?:[^\[\]"]|"(?:\\[\s\S]|[^\\"])*")*\]/, greedy:!0, alias:"attr-name", inside:{string:null}}, "closure-params":{pattern:/([=(,:]\s*|\bmove\s*)\|[^|]*\||\|[^|]*\|(?=\s*(?:\{|->))/, 
    lookbehind:!0, greedy:!0, inside:{"closure-punctuation":{pattern:/^\||\|$/, alias:"punctuation"}, rest:null}}, "lifetime-annotation":{pattern:/'\w+/, alias:"symbol"}, "fragment-specifier":{pattern:/(\$\w+:)[a-z]+/, lookbehind:!0, alias:"punctuation"}, variable:/\$\w+/, "function-definition":{pattern:/(\bfn\s+)\w+/, lookbehind:!0, alias:"function"}, "type-definition":{pattern:/(\b(?:enum|struct|trait|type|union)\s+)\w+/, lookbehind:!0, alias:"class-name"}, "module-declaration":[{pattern:/(\b(?:crate|mod)\s+)[a-z][a-z_\d]*/, 
    lookbehind:!0, alias:"namespace"}, {pattern:/(\b(?:crate|self|super)\s*)::\s*[a-z][a-z_\d]*\b(?:\s*::(?:\s*[a-z][a-z_\d]*\s*::)*)?/, lookbehind:!0, alias:"namespace", inside:{punctuation:/::/}}], keyword:[/\b(?:Self|abstract|as|async|await|become|box|break|const|continue|crate|do|dyn|else|enum|extern|final|fn|for|if|impl|in|let|loop|macro|match|mod|move|mut|override|priv|pub|ref|return|self|static|struct|super|trait|try|type|typeof|union|unsafe|unsized|use|virtual|where|while|yield)\b/, /\b(?:bool|char|f(?:32|64)|[ui](?:8|16|32|64|128|size)|str)\b/], 
    function:/\b[a-z_]\w*(?=\s*(?:::\s*<|\())/, macro:{pattern:/\b\w+!/, alias:"property"}, constant:/\b[A-Z_][A-Z_\d]+\b/, "class-name":/\b[A-Z]\w*\b/, namespace:{pattern:/(?:\b[a-z][a-z_\d]*\s*::\s*)*\b[a-z][a-z_\d]*\s*::(?!\s*<)/, inside:{punctuation:/::/}}, number:/\b(?:0x[\dA-Fa-f](?:_?[\dA-Fa-f])*|0o[0-7](?:_?[0-7])*|0b[01](?:_?[01])*|(?:(?:\d(?:_?\d)*)?\.)?\d(?:_?\d)*(?:[Ee][+-]?\d+)?)(?:_?(?:f32|f64|[iu](?:8|16|32|64|size)?))?\b/, boolean:/\b(?:false|true)\b/, punctuation:/->|\.\.=|\.{1,3}|::|[{}[\];(),:]/, 
    operator:/[-+*\/%!^]=?|=[=>]?|&[&=]?|\|[|=]?|<<?=?|>>?=?|[@?]/};
    e.languages.rust["closure-params"].inside.rest = e.languages.rust;
    e.languages.rust.attribute.inside.string = e.languages.rust.string;
  })(i);
  i.languages.go = i.languages.extend("clike", {string:{pattern:/(^|[^\\])"(?:\\.|[^"\\\r\n])*"|`[^`]*`/, lookbehind:!0, greedy:!0}, keyword:/\b(?:break|case|chan|const|continue|default|defer|else|fallthrough|for|func|go(?:to)?|if|import|interface|map|package|range|return|select|struct|switch|type|var)\b/, boolean:/\b(?:_|false|iota|nil|true)\b/, number:[/\b0(?:b[01_]+|o[0-7_]+)i?\b/i, /\b0x(?:[a-f\d_]+(?:\.[a-f\d_]*)?|\.[a-f\d_]+)(?:p[+-]?\d+(?:_\d+)*)?i?(?!\w)/i, /(?:\b\d[\d_]*(?:\.[\d_]*)?|\B\.\d[\d_]*)(?:e[+-]?[\d_]+)?i?(?!\w)/i], 
  operator:/[*\/%^!=]=?|\+[=+]?|-[=-]?|\|[=|]?|&(?:=|&|\^=?)?|>(?:>=?|=)?|<(?:<=?|=|-)?|:=|\.\.\./, builtin:/\b(?:append|bool|byte|cap|close|complex|complex(?:64|128)|copy|delete|error|float(?:32|64)|u?int(?:8|16|32|64)?|imag|len|make|new|panic|print(?:ln)?|real|recover|rune|string|uintptr)\b/});
  i.languages.insertBefore("go", "string", {char:{pattern:/'(?:\\.|[^'\\\r\n]){0,10}'/, greedy:!0}});
  delete i.languages.go["class-name"];
  (function(e) {
    var n = /\b(?:alignas|alignof|asm|auto|bool|break|case|catch|char|char16_t|char32_t|char8_t|class|co_await|co_return|co_yield|compl|concept|const|const_cast|consteval|constexpr|constinit|continue|decltype|default|delete|do|double|dynamic_cast|else|enum|explicit|export|extern|final|float|for|friend|goto|if|import|inline|int|int16_t|int32_t|int64_t|int8_t|long|module|mutable|namespace|new|noexcept|nullptr|operator|override|private|protected|public|register|reinterpret_cast|requires|return|short|signed|sizeof|static|static_assert|static_cast|struct|switch|template|this|thread_local|throw|try|typedef|typeid|typename|uint16_t|uint32_t|uint64_t|uint8_t|union|unsigned|using|virtual|void|volatile|wchar_t|while)\b/, 
    a = /\b(?!<keyword>)\w+(?:\s*\.\s*\w+)*\b/.source.replace(/<keyword>/g, function() {
      return n.source;
    });
    e.languages.cpp = e.languages.extend("c", {"class-name":[{pattern:RegExp(/(\b(?:class|concept|enum|struct|typename)\s+)(?!<keyword>)\w+/.source.replace(/<keyword>/g, function() {
      return n.source;
    })), lookbehind:!0}, /\b[A-Z]\w*(?=\s*::\s*\w+\s*\()/, /\b[A-Z_]\w*(?=\s*::\s*~\w+\s*\()/i, /\b\w+(?=\s*<(?:[^<>]|<(?:[^<>]|<[^<>]*>)*>)*>\s*::\s*\w+\s*\()/], keyword:n, number:{pattern:/(?:\b0b[01']+|\b0x(?:[\da-f']+(?:\.[\da-f']*)?|\.[\da-f']+)(?:p[+-]?[\d']+)?|(?:\b[\d']+(?:\.[\d']*)?|\B\.[\d']+)(?:e[+-]?[\d']+)?)[ful]{0,4}/i, greedy:!0}, operator:/>>=?|<<=?|->|--|\+\+|&&|\|\||[?:~]|<=>|[-+*/%&|^!=<>]=?|\b(?:and|and_eq|bitand|bitor|not|not_eq|or|or_eq|xor|xor_eq)\b/, boolean:/\b(?:false|true)\b/});
    e.languages.insertBefore("cpp", "string", {module:{pattern:RegExp(/(\b(?:import|module)\s+)/.source + "(?:" + /"(?:\\(?:\r\n|[\s\S])|[^"\\\r\n])*"|<[^<>\r\n]*>/.source + "|" + /<mod-name>(?:\s*:\s*<mod-name>)?|:\s*<mod-name>/.source.replace(/<mod-name>/g, function() {
      return a;
    }) + ")"), lookbehind:!0, greedy:!0, inside:{string:/^[<"][\s\S]+/, operator:/:/, punctuation:/\./}}, "raw-string":{pattern:/R"([^()\\ ]{0,16})\([\s\S]*?\)\1"/, alias:"string", greedy:!0}});
    e.languages.insertBefore("cpp", "keyword", {"generic-function":{pattern:/\b(?!operator\b)[a-z_]\w*\s*<(?:[^<>]|<[^<>]*>)*>(?=\s*\()/i, inside:{function:/^\w+/, generic:{pattern:/<[\s\S]+/, alias:"class-name", inside:e.languages.cpp}}}});
    e.languages.insertBefore("cpp", "operator", {"double-colon":{pattern:/::/, alias:"punctuation"}});
    e.languages.insertBefore("cpp", "class-name", {"base-clause":{pattern:/(\b(?:class|struct)\s+\w+\s*:\s*)[^;{}"'\s]+(?:\s+[^;{}"'\s]+)*(?=\s*[;{])/, lookbehind:!0, greedy:!0, inside:e.languages.extend("cpp", {})}});
    e.languages.insertBefore("inside", "double-colon", {"class-name":/\b[a-z_]\w*\b(?!\s*::)/i}, e.languages.cpp["base-clause"]);
  })(i);
  i.languages.python = {comment:{pattern:/(^|[^\\])#.*/, lookbehind:!0, greedy:!0}, "string-interpolation":{pattern:/(?:f|fr|rf)(?:("""|''')[\s\S]*?\1|("|')(?:\\.|(?!\2)[^\\\r\n])*\2)/i, greedy:!0, inside:{interpolation:{pattern:/((?:^|[^{])(?:\{\{)*)\{(?!\{)(?:[^{}]|\{(?!\{)(?:[^{}]|\{(?!\{)(?:[^{}])+\})+\})+\}/, lookbehind:!0, inside:{"format-spec":{pattern:/(:)[^:(){}]+(?=\}$)/, lookbehind:!0}, "conversion-option":{pattern:/![sra](?=[:}]$)/, alias:"punctuation"}, rest:null}}, string:/[\s\S]+/}}, 
  "triple-quoted-string":{pattern:/(?:[rub]|br|rb)?("""|''')[\s\S]*?\1/i, greedy:!0, alias:"string"}, string:{pattern:/(?:[rub]|br|rb)?("|')(?:\\.|(?!\1)[^\\\r\n])*\1/i, greedy:!0}, function:{pattern:/((?:^|\s)def[ \t]+)[a-zA-Z_]\w*(?=\s*\()/g, lookbehind:!0}, "class-name":{pattern:/(\bclass\s+)\w+/i, lookbehind:!0}, decorator:{pattern:/(^[\t ]*)@\w+(?:\.\w+)*/m, lookbehind:!0, alias:["annotation", "punctuation"], inside:{punctuation:/\./}}, keyword:/\b(?:_(?=\s*:)|and|as|assert|async|await|break|case|class|continue|def|del|elif|else|except|exec|finally|for|from|global|if|import|in|is|lambda|match|nonlocal|not|or|pass|print|raise|return|try|while|with|yield)\b/, 
  builtin:/\b(?:__import__|abs|all|any|apply|ascii|basestring|bin|bool|buffer|bytearray|bytes|callable|chr|classmethod|cmp|coerce|compile|complex|delattr|dict|dir|divmod|enumerate|eval|execfile|file|filter|float|format|frozenset|getattr|globals|hasattr|hash|help|hex|id|input|int|intern|isinstance|issubclass|iter|len|list|locals|long|map|max|memoryview|min|next|object|oct|open|ord|pow|property|range|raw_input|reduce|reload|repr|reversed|round|set|setattr|slice|sorted|staticmethod|str|sum|super|tuple|type|unichr|unicode|vars|xrange|zip)\b/, 
  boolean:/\b(?:False|None|True)\b/, number:/\b0(?:b(?:_?[01])+|o(?:_?[0-7])+|x(?:_?[a-f0-9])+)\b|(?:\b\d+(?:_\d+)*(?:\.(?:\d+(?:_\d+)*)?)?|\B\.\d+(?:_\d+)*)(?:e[+-]?\d+(?:_\d+)*)?j?(?!\w)/i, operator:/[-+%=]=?|!=|:=|\*\*?=?|\/\/?=?|<[<=>]?|>[=>]?|[&|^~]/, punctuation:/[{}[\];(),.:]/};
  i.languages.python["string-interpolation"].inside.interpolation.inside.rest = i.languages.python;
  i.languages.py = i.languages.python;
  i.languages.json = {property:{pattern:/(^|[^\\])"(?:\\.|[^\\"\r\n])*"(?=\s*:)/, lookbehind:!0, greedy:!0}, string:{pattern:/(^|[^\\])"(?:\\.|[^\\"\r\n])*"(?!\s*:)/, lookbehind:!0, greedy:!0}, comment:{pattern:/\/\/.*|\/\*[\s\S]*?(?:\*\/|$)/, greedy:!0}, number:/-?\b\d+(?:\.\d+)?(?:e[+-]?\d+)?\b/i, punctuation:/[{}[\],]/, operator:/:/, boolean:/\b(?:false|true)\b/, null:{pattern:/\bnull\b/, alias:"keyword"}};
  i.languages.webmanifest = i.languages.json;
  var le = {};
  ((e, n) => {
    for (var a in n) {
      Se(e, a, {get:n[a], enumerable:!0});
    }
  })(le, {dracula:() => sn, duotoneDark:() => cn, duotoneLight:() => dn, github:() => pn, gruvboxMaterialDark:() => Gn, gruvboxMaterialLight:() => Wn, jettwaveDark:() => Dn, jettwaveLight:() => Bn, nightOwl:() => bn, nightOwlLight:() => mn, oceanicNext:() => xn, okaidia:() => kn, oneDark:() => jn, oneLight:() => Mn, palenight:() => wn, shadesOfPurple:() => En, synthwave84:() => An, ultramin:() => $n, vsDark:() => In, vsLight:() => Rn});
  var sn = {plain:{color:"#F8F8F2", backgroundColor:"#282A36"}, styles:[{types:["prolog", "constant", "builtin"], style:{color:"rgb(189, 147, 249)"}}, {types:["inserted", "function"], style:{color:"rgb(80, 250, 123)"}}, {types:["deleted"], style:{color:"rgb(255, 85, 85)"}}, {types:["changed"], style:{color:"rgb(255, 184, 108)"}}, {types:["punctuation", "symbol"], style:{color:"rgb(248, 248, 242)"}}, {types:["string", "char", "tag", "selector"], style:{color:"rgb(255, 121, 198)"}}, {types:["keyword", 
  "variable"], style:{color:"rgb(189, 147, 249)", fontStyle:"italic"}}, {types:["comment"], style:{color:"rgb(98, 114, 164)"}}, {types:["attr-name"], style:{color:"rgb(241, 250, 140)"}}]}, cn = {plain:{backgroundColor:"#2a2734", color:"#9a86fd"}, styles:[{types:["comment", "prolog", "doctype", "cdata", "punctuation"], style:{color:"#6c6783"}}, {types:["namespace"], style:{opacity:0.7}}, {types:["tag", "operator", "number"], style:{color:"#e09142"}}, {types:["property", "function"], style:{color:"#9a86fd"}}, 
  {types:["tag-id", "selector", "atrule-id"], style:{color:"#eeebff"}}, {types:["attr-name"], style:{color:"#c4b9fe"}}, {types:"boolean string entity url attr-value keyword control directive unit statement regex atrule placeholder variable".split(" "), style:{color:"#ffcc99"}}, {types:["deleted"], style:{textDecorationLine:"line-through"}}, {types:["inserted"], style:{textDecorationLine:"underline"}}, {types:["italic"], style:{fontStyle:"italic"}}, {types:["important", "bold"], style:{fontWeight:"bold"}}, 
  {types:["important"], style:{color:"#c4b9fe"}}]}, dn = {plain:{backgroundColor:"#faf8f5", color:"#728fcb"}, styles:[{types:["comment", "prolog", "doctype", "cdata", "punctuation"], style:{color:"#b6ad9a"}}, {types:["namespace"], style:{opacity:0.7}}, {types:["tag", "operator", "number"], style:{color:"#063289"}}, {types:["property", "function"], style:{color:"#b29762"}}, {types:["tag-id", "selector", "atrule-id"], style:{color:"#2d2006"}}, {types:["attr-name"], style:{color:"#896724"}}, {types:"boolean string entity url attr-value keyword control directive unit statement regex atrule".split(" "), 
  style:{color:"#728fcb"}}, {types:["placeholder", "variable"], style:{color:"#93abdc"}}, {types:["deleted"], style:{textDecorationLine:"line-through"}}, {types:["inserted"], style:{textDecorationLine:"underline"}}, {types:["italic"], style:{fontStyle:"italic"}}, {types:["important", "bold"], style:{fontWeight:"bold"}}, {types:["important"], style:{color:"#896724"}}]}, pn = {plain:{color:"#393A34", backgroundColor:"#f6f8fa"}, styles:[{types:["comment", "prolog", "doctype", "cdata"], style:{color:"#999988", 
  fontStyle:"italic"}}, {types:["namespace"], style:{opacity:0.7}}, {types:["string", "attr-value"], style:{color:"#e3116c"}}, {types:["punctuation", "operator"], style:{color:"#393A34"}}, {types:"entity url symbol number boolean variable constant property regex inserted".split(" "), style:{color:"#36acaa"}}, {types:["atrule", "keyword", "attr-name", "selector"], style:{color:"#00a4db"}}, {types:["function", "deleted", "tag"], style:{color:"#d73a49"}}, {types:["function-variable"], style:{color:"#6f42c1"}}, 
  {types:["tag", "selector", "keyword"], style:{color:"#00009f"}}]}, bn = {plain:{color:"#d6deeb", backgroundColor:"#011627"}, styles:[{types:["changed"], style:{color:"rgb(162, 191, 252)", fontStyle:"italic"}}, {types:["deleted"], style:{color:"rgba(239, 83, 80, 0.56)", fontStyle:"italic"}}, {types:["inserted", "attr-name"], style:{color:"rgb(173, 219, 103)", fontStyle:"italic"}}, {types:["comment"], style:{color:"rgb(99, 119, 119)", fontStyle:"italic"}}, {types:["string", "url"], style:{color:"rgb(173, 219, 103)"}}, 
  {types:["variable"], style:{color:"rgb(214, 222, 235)"}}, {types:["number"], style:{color:"rgb(247, 140, 108)"}}, {types:["builtin", "char", "constant", "function"], style:{color:"rgb(130, 170, 255)"}}, {types:["punctuation"], style:{color:"rgb(199, 146, 234)"}}, {types:["selector", "doctype"], style:{color:"rgb(199, 146, 234)", fontStyle:"italic"}}, {types:["class-name"], style:{color:"rgb(255, 203, 139)"}}, {types:["tag", "operator", "keyword"], style:{color:"rgb(127, 219, 202)"}}, {types:["boolean"], 
  style:{color:"rgb(255, 88, 116)"}}, {types:["property"], style:{color:"rgb(128, 203, 196)"}}, {types:["namespace"], style:{color:"rgb(178, 204, 214)"}}]}, mn = {plain:{color:"#403f53", backgroundColor:"#FBFBFB"}, styles:[{types:["changed"], style:{color:"rgb(162, 191, 252)", fontStyle:"italic"}}, {types:["deleted"], style:{color:"rgba(239, 83, 80, 0.56)", fontStyle:"italic"}}, {types:["inserted", "attr-name"], style:{color:"rgb(72, 118, 214)", fontStyle:"italic"}}, {types:["comment"], style:{color:"rgb(152, 159, 177)", 
  fontStyle:"italic"}}, {types:["string", "builtin", "char", "constant", "url"], style:{color:"rgb(72, 118, 214)"}}, {types:["variable"], style:{color:"rgb(201, 103, 101)"}}, {types:["number"], style:{color:"rgb(170, 9, 130)"}}, {types:["punctuation"], style:{color:"rgb(153, 76, 195)"}}, {types:["function", "selector", "doctype"], style:{color:"rgb(153, 76, 195)", fontStyle:"italic"}}, {types:["class-name"], style:{color:"rgb(17, 17, 17)"}}, {types:["tag"], style:{color:"rgb(153, 76, 195)"}}, {types:["operator", 
  "property", "keyword", "namespace"], style:{color:"rgb(12, 150, 155)"}}, {types:["boolean"], style:{color:"rgb(188, 84, 84)"}}]}, xn = {plain:{backgroundColor:"#282c34", color:"#ffffff"}, styles:[{types:["attr-name"], style:{color:"#c5a5c5"}}, {types:["attr-value"], style:{color:"#8dc891"}}, {types:"comment block-comment prolog doctype cdata shebang".split(" "), style:{color:"#999999"}}, {types:"property number function-name constant symbol deleted".split(" "), style:{color:"#5a9bcf"}}, {types:["boolean"], 
  style:{color:"#ff8b50"}}, {types:["tag"], style:{color:"#fc929e"}}, {types:["string"], style:{color:"#8dc891"}}, {types:["punctuation"], style:{color:"#8dc891"}}, {types:["selector", "char", "builtin", "inserted"], style:{color:"#D8DEE9"}}, {types:["function"], style:{color:"#79b6f2"}}, {types:["operator", "entity", "url", "variable"], style:{color:"#d7deea"}}, {types:["keyword"], style:{color:"#c5a5c5"}}, {types:["atrule", "class-name"], style:{color:"#FAC863"}}, {types:["important"], style:{fontWeight:"400"}}, 
  {types:["bold"], style:{fontWeight:"bold"}}, {types:["italic"], style:{fontStyle:"italic"}}, {types:["namespace"], style:{opacity:0.7}}]}, kn = {plain:{color:"#f8f8f2", backgroundColor:"#272822"}, styles:[{types:["changed"], style:{color:"rgb(162, 191, 252)", fontStyle:"italic"}}, {types:["deleted"], style:{color:"#f92672", fontStyle:"italic"}}, {types:["inserted"], style:{color:"rgb(173, 219, 103)", fontStyle:"italic"}}, {types:["comment"], style:{color:"#8292a2", fontStyle:"italic"}}, {types:["string", 
  "url"], style:{color:"#a6e22e"}}, {types:["variable"], style:{color:"#f8f8f2"}}, {types:["number"], style:{color:"#ae81ff"}}, {types:["builtin", "char", "constant", "function", "class-name"], style:{color:"#e6db74"}}, {types:["punctuation"], style:{color:"#f8f8f2"}}, {types:["selector", "doctype"], style:{color:"#a6e22e", fontStyle:"italic"}}, {types:["tag", "operator", "keyword"], style:{color:"#66d9ef"}}, {types:["boolean"], style:{color:"#ae81ff"}}, {types:["namespace"], style:{color:"rgb(178, 204, 214)", 
  opacity:0.7}}, {types:["tag", "property"], style:{color:"#f92672"}}, {types:["attr-name"], style:{color:"#a6e22e !important"}}, {types:["doctype"], style:{color:"#8292a2"}}, {types:["rule"], style:{color:"#e6db74"}}]}, wn = {plain:{color:"#bfc7d5", backgroundColor:"#292d3e"}, styles:[{types:["comment"], style:{color:"rgb(105, 112, 152)", fontStyle:"italic"}}, {types:["string", "inserted"], style:{color:"rgb(195, 232, 141)"}}, {types:["number"], style:{color:"rgb(247, 140, 108)"}}, {types:["builtin", 
  "char", "constant", "function"], style:{color:"rgb(130, 170, 255)"}}, {types:["punctuation", "selector"], style:{color:"rgb(199, 146, 234)"}}, {types:["variable"], style:{color:"rgb(191, 199, 213)"}}, {types:["class-name", "attr-name"], style:{color:"rgb(255, 203, 107)"}}, {types:["tag", "deleted"], style:{color:"rgb(255, 85, 114)"}}, {types:["operator"], style:{color:"rgb(137, 221, 255)"}}, {types:["boolean"], style:{color:"rgb(255, 88, 116)"}}, {types:["keyword"], style:{fontStyle:"italic"}}, 
  {types:["doctype"], style:{color:"rgb(199, 146, 234)", fontStyle:"italic"}}, {types:["namespace"], style:{color:"rgb(178, 204, 214)"}}, {types:["url"], style:{color:"rgb(221, 221, 221)"}}]}, En = {plain:{color:"#9EFEFF", backgroundColor:"#2D2A55"}, styles:[{types:["changed"], style:{color:"rgb(255, 238, 128)"}}, {types:["deleted"], style:{color:"rgba(239, 83, 80, 0.56)"}}, {types:["inserted"], style:{color:"rgb(173, 219, 103)"}}, {types:["comment"], style:{color:"rgb(179, 98, 255)", fontStyle:"italic"}}, 
  {types:["punctuation"], style:{color:"rgb(255, 255, 255)"}}, {types:["constant"], style:{color:"rgb(255, 98, 140)"}}, {types:["string", "url"], style:{color:"rgb(165, 255, 144)"}}, {types:["variable"], style:{color:"rgb(255, 238, 128)"}}, {types:["number", "boolean"], style:{color:"rgb(255, 98, 140)"}}, {types:["attr-name"], style:{color:"rgb(255, 180, 84)"}}, {types:"keyword operator property namespace tag selector doctype".split(" "), style:{color:"rgb(255, 157, 0)"}}, {types:["builtin", "char", 
  "constant", "function", "class-name"], style:{color:"rgb(250, 208, 0)"}}]}, An = {plain:{backgroundColor:"linear-gradient(to bottom, #2a2139 75%, #34294f)", backgroundImage:"#34294f", color:"#f92aad", textShadow:"0 0 2px #100c0f, 0 0 5px #dc078e33, 0 0 10px #fff3"}, styles:[{types:["comment", "block-comment", "prolog", "doctype", "cdata"], style:{color:"#495495", fontStyle:"italic"}}, {types:["punctuation"], style:{color:"#ccc"}}, {types:"tag attr-name namespace number unit hexcode deleted".split(" "), 
  style:{color:"#e2777a"}}, {types:["property", "selector"], style:{color:"#72f1b8", textShadow:"0 0 2px #100c0f, 0 0 10px #257c5575, 0 0 35px #21272475"}}, {types:["function-name"], style:{color:"#6196cc"}}, {types:["boolean", "selector-id", "function"], style:{color:"#fdfdfd", textShadow:"0 0 2px #001716, 0 0 3px #03edf975, 0 0 5px #03edf975, 0 0 8px #03edf975"}}, {types:["class-name", "maybe-class-name", "builtin"], style:{color:"#fff5f6", textShadow:"0 0 2px #000, 0 0 10px #fc1f2c75, 0 0 5px #fc1f2c75, 0 0 25px #fc1f2c75"}}, 
  {types:["constant", "symbol"], style:{color:"#f92aad", textShadow:"0 0 2px #100c0f, 0 0 5px #dc078e33, 0 0 10px #fff3"}}, {types:["important", "atrule", "keyword", "selector-class"], style:{color:"#f4eee4", textShadow:"0 0 2px #393a33, 0 0 8px #f39f0575, 0 0 2px #f39f0575"}}, {types:["string", "char", "attr-value", "regex", "variable"], style:{color:"#f87c32"}}, {types:["parameter"], style:{fontStyle:"italic"}}, {types:["entity", "url"], style:{color:"#67cdcc"}}, {types:["operator"], style:{color:"ffffffee"}}, 
  {types:["important", "bold"], style:{fontWeight:"bold"}}, {types:["italic"], style:{fontStyle:"italic"}}, {types:["entity"], style:{cursor:"help"}}, {types:["inserted"], style:{color:"green"}}]}, $n = {plain:{color:"#282a2e", backgroundColor:"#ffffff"}, styles:[{types:["comment"], style:{color:"rgb(197, 200, 198)"}}, {types:["string", "number", "builtin", "variable"], style:{color:"rgb(150, 152, 150)"}}, {types:["class-name", "function", "tag", "attr-name"], style:{color:"rgb(40, 42, 46)"}}]}, 
  In = {plain:{color:"#9CDCFE", backgroundColor:"#1E1E1E"}, styles:[{types:["prolog"], style:{color:"rgb(0, 0, 128)"}}, {types:["comment"], style:{color:"rgb(106, 153, 85)"}}, {types:["builtin", "changed", "keyword", "interpolation-punctuation"], style:{color:"rgb(86, 156, 214)"}}, {types:["number", "inserted"], style:{color:"rgb(181, 206, 168)"}}, {types:["constant"], style:{color:"rgb(100, 102, 149)"}}, {types:["attr-name", "variable"], style:{color:"rgb(156, 220, 254)"}}, {types:["deleted", "string", 
  "attr-value", "template-punctuation"], style:{color:"rgb(206, 145, 120)"}}, {types:["selector"], style:{color:"rgb(215, 186, 125)"}}, {types:["tag"], style:{color:"rgb(78, 201, 176)"}}, {types:["tag"], languages:["markup"], style:{color:"rgb(86, 156, 214)"}}, {types:["punctuation", "operator"], style:{color:"rgb(212, 212, 212)"}}, {types:["punctuation"], languages:["markup"], style:{color:"#808080"}}, {types:["function"], style:{color:"rgb(220, 220, 170)"}}, {types:["class-name"], style:{color:"rgb(78, 201, 176)"}}, 
  {types:["char"], style:{color:"rgb(209, 105, 105)"}}]}, Rn = {plain:{color:"#000000", backgroundColor:"#ffffff"}, styles:[{types:["comment"], style:{color:"rgb(0, 128, 0)"}}, {types:["builtin"], style:{color:"rgb(0, 112, 193)"}}, {types:["number", "variable", "inserted"], style:{color:"rgb(9, 134, 88)"}}, {types:["operator"], style:{color:"rgb(0, 0, 0)"}}, {types:["constant", "char"], style:{color:"rgb(129, 31, 63)"}}, {types:["tag"], style:{color:"rgb(128, 0, 0)"}}, {types:["attr-name"], style:{color:"rgb(255, 0, 0)"}}, 
  {types:["deleted", "string"], style:{color:"rgb(163, 21, 21)"}}, {types:["changed", "punctuation"], style:{color:"rgb(4, 81, 165)"}}, {types:["function", "keyword"], style:{color:"rgb(0, 0, 255)"}}, {types:["class-name"], style:{color:"rgb(38, 127, 153)"}}]}, Dn = {plain:{color:"#f8fafc", backgroundColor:"#011627"}, styles:[{types:["prolog"], style:{color:"#000080"}}, {types:["comment"], style:{color:"#6A9955"}}, {types:["builtin", "changed", "keyword", "interpolation-punctuation"], style:{color:"#569CD6"}}, 
  {types:["number", "inserted"], style:{color:"#B5CEA8"}}, {types:["constant"], style:{color:"#f8fafc"}}, {types:["attr-name", "variable"], style:{color:"#9CDCFE"}}, {types:["deleted", "string", "attr-value", "template-punctuation"], style:{color:"#cbd5e1"}}, {types:["selector"], style:{color:"#D7BA7D"}}, {types:["tag"], style:{color:"#0ea5e9"}}, {types:["tag"], languages:["markup"], style:{color:"#0ea5e9"}}, {types:["punctuation", "operator"], style:{color:"#D4D4D4"}}, {types:["punctuation"], languages:["markup"], 
  style:{color:"#808080"}}, {types:["function"], style:{color:"#7dd3fc"}}, {types:["class-name"], style:{color:"#0ea5e9"}}, {types:["char"], style:{color:"#D16969"}}]}, Bn = {plain:{color:"#0f172a", backgroundColor:"#f1f5f9"}, styles:[{types:["prolog"], style:{color:"#000080"}}, {types:["comment"], style:{color:"#6A9955"}}, {types:["builtin", "changed", "keyword", "interpolation-punctuation"], style:{color:"#0c4a6e"}}, {types:["number", "inserted"], style:{color:"#B5CEA8"}}, {types:["constant"], 
  style:{color:"#0f172a"}}, {types:["attr-name", "variable"], style:{color:"#0c4a6e"}}, {types:["deleted", "string", "attr-value", "template-punctuation"], style:{color:"#64748b"}}, {types:["selector"], style:{color:"#D7BA7D"}}, {types:["tag"], style:{color:"#0ea5e9"}}, {types:["tag"], languages:["markup"], style:{color:"#0ea5e9"}}, {types:["punctuation", "operator"], style:{color:"#475569"}}, {types:["punctuation"], languages:["markup"], style:{color:"#808080"}}, {types:["function"], style:{color:"#0e7490"}}, 
  {types:["class-name"], style:{color:"#0ea5e9"}}, {types:["char"], style:{color:"#D16969"}}]}, jn = {plain:{backgroundColor:"hsl(220, 13%, 18%)", color:"hsl(220, 14%, 71%)", textShadow:"0 1px rgba(0, 0, 0, 0.3)"}, styles:[{types:["comment", "prolog", "cdata"], style:{color:"hsl(220, 10%, 40%)"}}, {types:["doctype", "punctuation", "entity"], style:{color:"hsl(220, 14%, 71%)"}}, {types:"attr-name class-name maybe-class-name boolean constant number atrule".split(" "), style:{color:"hsl(29, 54%, 61%)"}}, 
  {types:["keyword"], style:{color:"hsl(286, 60%, 67%)"}}, {types:["property", "tag", "symbol", "deleted", "important"], style:{color:"hsl(355, 65%, 65%)"}}, {types:"selector string char builtin inserted regex attr-value".split(" "), style:{color:"hsl(95, 38%, 62%)"}}, {types:["variable", "operator", "function"], style:{color:"hsl(207, 82%, 66%)"}}, {types:["url"], style:{color:"hsl(187, 47%, 55%)"}}, {types:["deleted"], style:{textDecorationLine:"line-through"}}, {types:["inserted"], style:{textDecorationLine:"underline"}}, 
  {types:["italic"], style:{fontStyle:"italic"}}, {types:["important", "bold"], style:{fontWeight:"bold"}}, {types:["important"], style:{color:"hsl(220, 14%, 71%)"}}]}, Mn = {plain:{backgroundColor:"hsl(230, 1%, 98%)", color:"hsl(230, 8%, 24%)"}, styles:[{types:["comment", "prolog", "cdata"], style:{color:"hsl(230, 4%, 64%)"}}, {types:["doctype", "punctuation", "entity"], style:{color:"hsl(230, 8%, 24%)"}}, {types:"attr-name class-name boolean constant number atrule".split(" "), style:{color:"hsl(35, 99%, 36%)"}}, 
  {types:["keyword"], style:{color:"hsl(301, 63%, 40%)"}}, {types:["property", "tag", "symbol", "deleted", "important"], style:{color:"hsl(5, 74%, 59%)"}}, {types:"selector string char builtin inserted regex attr-value punctuation".split(" "), style:{color:"hsl(119, 34%, 47%)"}}, {types:["variable", "operator", "function"], style:{color:"hsl(221, 87%, 60%)"}}, {types:["url"], style:{color:"hsl(198, 99%, 37%)"}}, {types:["deleted"], style:{textDecorationLine:"line-through"}}, {types:["inserted"], 
  style:{textDecorationLine:"underline"}}, {types:["italic"], style:{fontStyle:"italic"}}, {types:["important", "bold"], style:{fontWeight:"bold"}}, {types:["important"], style:{color:"hsl(230, 8%, 24%)"}}]}, Gn = {plain:{color:"#ebdbb2", backgroundColor:"#292828"}, styles:[{types:"imports class-name maybe-class-name constant doctype builtin function".split(" "), style:{color:"#d8a657"}}, {types:["property-access"], style:{color:"#7daea3"}}, {types:["tag"], style:{color:"#e78a4e"}}, {types:["attr-name", 
  "char", "url", "regex"], style:{color:"#a9b665"}}, {types:["attr-value", "string"], style:{color:"#89b482"}}, {types:["comment", "prolog", "cdata", "operator", "inserted"], style:{color:"#a89984"}}, {types:"delimiter boolean keyword selector important atrule property variable deleted".split(" "), style:{color:"#ea6962"}}, {types:["entity", "number", "symbol"], style:{color:"#d3869b"}}]}, Wn = {plain:{color:"#654735", backgroundColor:"#f9f5d7"}, styles:[{types:"delimiter boolean keyword selector important atrule property variable deleted".split(" "), 
  style:{color:"#af2528"}}, {types:"imports class-name maybe-class-name constant doctype builtin".split(" "), style:{color:"#b4730e"}}, {types:["string", "attr-value"], style:{color:"#477a5b"}}, {types:["property-access"], style:{color:"#266b79"}}, {types:["function", "attr-name", "char", "url"], style:{color:"#72761e"}}, {types:["tag"], style:{color:"#b94c07"}}, {types:["comment", "prolog", "cdata", "operator", "inserted"], style:{color:"#a89984"}}, {types:["entity", "number", "symbol"], style:{color:"#924f79"}}]};
  le.vsDark;
  le.nightOwl;
  le.vsDark;
  nt.background.surface;
  `${nt.border.default}`;
  `${nt.border.default}`;
  nt.background.elevated;
  rt.md;
  nt.background.elevated;
  rt.md;
  he.sans;
  `${nt.border.default}`;
  const Z = {}, Kn = (Z == null ? void 0 : Z.VITE_API_BASE) ?? "";
  var Ae;
  const Vn = ((Ae = Z == null ? void 0 : Z.VITE_KNOXX_DEV_USER_EMAIL) == null ? void 0 : Ae.trim()) ?? "";
  var Ce;
  const Xn = ((Ce = Z == null ? void 0 : Z.VITE_KNOXX_DEV_ORG_SLUG) == null ? void 0 : Ce.trim()) ?? "";
};

//# sourceMappingURL=module$dist$bridge$knoxx_frontend_bridge_es.js.map
