function Le$$module$dist$bridge$knoxx_frontend_bridge_es(e) {
  const r = e.replace("#", "").trim();
  return r.length === 3 ? r.split("").map(a => `${a}${a}`).join("") : r;
}
function Oe$$module$dist$bridge$knoxx_frontend_bridge_es(e) {
  const r = Number(e.toFixed(2));
  return String(r);
}
function n$$module$dist$bridge$knoxx_frontend_bridge_es(e, r) {
  const a = Le$$module$dist$bridge$knoxx_frontend_bridge_es(e);
  if (a.length !== 6) {
    return e;
  }
  const c = parseInt(a.slice(0, 2), 16);
  const d = parseInt(a.slice(2, 4), 16);
  const h = parseInt(a.slice(4, 6), 16);
  return `rgba(${c}, ${d}, ${h}, ${Oe$$module$dist$bridge$knoxx_frontend_bridge_es(r)})`;
}
function Fe$$module$dist$bridge$knoxx_frontend_bridge_es(e, r) {
  if (!r) {
    return e;
  }
  const a = {...e};
  for (const [c, d] of Object.entries(r)) {
    const h = a[c];
    if (d && typeof d == "object" && !Array.isArray(d) && h && typeof h == "object" && !Array.isArray(h)) {
      a[c] = Fe$$module$dist$bridge$knoxx_frontend_bridge_es(h, d);
      continue;
    }
    a[c] = d;
  }
  return a;
}
function Be$$module$dist$bridge$knoxx_frontend_bridge_es(e, r) {
  const a = {background:{default:e.bg.default, surface:e.bg.darker, elevated:e.bg.tabInactive, highlight:e.bg.lighter, overlay:"rgba(0, 0, 0, 0.6)"}, selection:{default:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.selection, 0.5)}, text:{default:e.fg.default, bright:e.fg.bright, panel:e.fg.panel, soft:e.fg.soft, muted:e.fg.muted, subtle:e.fg.subtle, inverse:e.bg.default, secondary:e.fg.muted}, interactive:{default:e.accent.cyan, hover:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.cyan, 
  0.88), active:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.cyan, 0.72), disabled:e.fg.muted}, button:{primary:{bg:e.accent.cyan, fg:e.bg.default, hover:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.cyan, 0.88), active:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.cyan, 0.72)}, secondary:{bg:e.bg.selection, fg:e.fg.default, hover:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.selection, 0.88), active:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.selection, 
  0.72)}, ghost:{bg:"transparent", fg:e.fg.default, hover:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.fg.default, 0.08), active:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.fg.default, 0.14)}, danger:{bg:e.accent.red, fg:e.fg.default, hover:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.red, 0.88), active:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.red, 0.72)}}, badge:{default:{bg:e.fg.muted, fg:e.fg.default}, success:{bg:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.green, 
  0.15), fg:e.accent.green}, warning:{bg:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.orange, 0.15), fg:e.accent.orange}, error:{bg:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.red, 0.15), fg:e.accent.red}, info:{bg:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.blue, 0.15), fg:e.accent.blue}}, border:{default:e.bg.groupBorder, subtle:e.fg.subtle, focus:e.accent.cyan, error:e.accent.red}, accent:e.accent, semantic:{error:e.semantic.error, warning:e.semantic.warning, 
  success:e.semantic.success, info:e.semantic.info}, status:{alive:e.accent.green, dead:e.accent.red, open:e.accent.green, closed:e.fg.muted, merged:e.accent.magenta, sleeping:e.accent.blue, eating:e.accent.orange, working:e.accent.yellow}, chart:{segment0:e.accent.blue, segment1:e.accent.green, segment2:e.accent.yellow, segment3:e.accent.orange, segment4:e.accent.magenta, segment5:e.fg.soft}, fill:{good:{start:e.accent.green, end:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.green, 0.55)}, 
  warn:{start:e.accent.orange, end:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.orange, 0.55)}, danger:{start:e.accent.red, end:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.red, 0.55)}, neutral:{start:e.accent.blue, end:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.blue, 0.55)}}, surface:{panel:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.darker, 0.82), card:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.tabInactive, 0.65), cardAlt:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.lighter, 
  0.55), input:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.selection, 0.78), nav:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.darker, 0.6)}, alpha:{green:{_08:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.green, 0.08), _12:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.green, 0.12), _14:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.green, 0.14), _15:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.green, 0.15), _16:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.green, 
  0.16), _25:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.green, 0.25), _28:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.green, 0.28), _30:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.green, 0.3), _35:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.green, 0.35), _38:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.green, 0.38), _40:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.green, 0.4), _45:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.green, 
  0.45), _50:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.green, 0.5), _55:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.green, 0.55), _60:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.green, 0.6), _80:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.green, 0.8)}, red:{_12:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.red, 0.12), _14:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.red, 0.14), _15:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.red, 
  0.15), _25:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.red, 0.25), _30:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.red, 0.3), _38:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.red, 0.38), _40:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.red, 0.4), _45:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.red, 0.45), _46:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.red, 0.46), _50:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.red, 
  0.5)}, orange:{_12:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.orange, 0.12), _15:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.orange, 0.15), _32:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.orange, 0.32), _35:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.orange, 0.35), _40:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.orange, 0.4)}, blue:{_15:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.blue, 0.15), _20:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.blue, 
  0.2), _35:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.blue, 0.35), _45:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.blue, 0.45), _80:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.blue, 0.8), _95:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.blue, 0.95)}, magenta:{_08:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.magenta, 0.08), _14:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.magenta, 0.14), _30:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.magenta, 
  0.3)}, yellow:{_06:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.yellow, 0.06)}, bg:{_08:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.selection, 0.08), _10:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.selection, 0.1), _12:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.selection, 0.12), _14:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.selection, 0.14), _16:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.selection, 0.16), _18:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.selection, 
  0.18), _24:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.lighter, 0.24), _25:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.selection, 0.25), _28:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.selection, 0.28), _30:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.selection, 0.3), _46:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.darker, 0.46), _55:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.selection, 0.55), _60:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.darker, 
  0.6), _62:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.darker, 0.62), _68:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.darker, 0.68), _70:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.selection, 0.7), _72:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.darker, 0.72), _80:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.darker, 0.8), _85:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.lighter, 0.85), _88:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.lighter, 
  0.88), _88b:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.default, 0.88), _90:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.darker, 0.9), _95:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.bg.darker, 0.95)}, warningBg:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.orange, 0.2), errorBg:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.red, 0.42), errorBgSolid:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.red, 0.9), federationError:n$$module$dist$bridge$knoxx_frontend_bridge_es(e.accent.red, 
  0.22), white:{_08:"rgba(255, 255, 255, 0.08)"}, shadow:"rgba(0, 0, 0, 0.35)", shadowLight:"rgba(0, 0, 0, 0.3)", shadowDeep:"rgba(15, 23, 42, 0.22)"}};
  return Fe$$module$dist$bridge$knoxx_frontend_bridge_es(a, r);
}
function Ee$$module$dist$bridge$knoxx_frontend_bridge_es(e) {
  return e.replace(/([A-Z])/g, "-$1").toLowerCase();
}
function Ne$$module$dist$bridge$knoxx_frontend_bridge_es(e, r) {
  return `var(--uxx-${e.map(Ee$$module$dist$bridge$knoxx_frontend_bridge_es).join("-")}, ${r})`;
}
function _e$$module$dist$bridge$knoxx_frontend_bridge_es(e, r = []) {
  if (typeof e == "string") {
    return Ne$$module$dist$bridge$knoxx_frontend_bridge_es(r, e);
  }
  if (typeof e == "object" && e !== null) {
    const a = Object.entries(e).map(([c, d]) => [c, _e$$module$dist$bridge$knoxx_frontend_bridge_es(d, [...r, c])]);
    return Object.fromEntries(a);
  }
  return e;
}
function de$$module$dist$bridge$knoxx_frontend_bridge_es(e, r, a, c) {
  return {name:e, label:r, appearance:(c == null ? void 0 : c.appearance) ?? "dark", palette:a, colors:Be$$module$dist$bridge$knoxx_frontend_bridge_es(a, c == null ? void 0 : c.colorOverrides)};
}
var R$$module$dist$bridge$knoxx_frontend_bridge_es = {bg:{default:"#272822", darker:"#1e1f1c", lighter:"#3e3d32", selection:"#414339", tabInactive:"#34352f", groupBorder:"#34352f"}, fg:{default:"#f8f8f2", bright:"#f8f8f2", panel:"#cccccc", soft:"#90908a", muted:"#75715e", subtle:"#464741"}, accent:{yellow:"#e6db74", orange:"#fd971f", red:"#f92672", magenta:"#ae81ff", blue:"#66d9ef", cyan:"#66d9ef", green:"#a6e22e"}, semantic:{error:"#f92672", warning:"#fd971f", success:"#a6e22e", info:"#66d9ef"}};
var T$$module$dist$bridge$knoxx_frontend_bridge_es = {bg:{default:"#011627", darker:"#01111d", lighter:"#0b2942", selection:"#1d3b53", tabInactive:"#0b253a", groupBorder:"#5f7e97"}, fg:{default:"#d6deeb", bright:"#ffffff", panel:"#d2dee7", soft:"#89a4bb", muted:"#5f7e97", subtle:"#4b6479"}, accent:{yellow:"#ffeb95", orange:"#F78C6C", red:"#EF5350", magenta:"#c792ea", blue:"#82AAFF", cyan:"#80CBC4", green:"#c5e478"}, semantic:{error:"#EF5350", warning:"#FFCA28", success:"#c5e478", info:"#82AAFF"}};
var p$$module$dist$bridge$knoxx_frontend_bridge_es = {bg:{default:"#0A0C0F", darker:"#0F1318", lighter:"#1E2530", selection:"#131820", tabInactive:"#131820", groupBorder:"#1E2530"}, fg:{default:"#E8ECF1", bright:"#F4F7FB", panel:"#D6DCE6", soft:"#A6B1C2", muted:"#8A94A6", subtle:"#5A6478"}, accent:{yellow:"#F5A623", orange:"#F5A623", red:"#FF4C4C", magenta:"#9B8CFF", blue:"#00D4FF", cyan:"#00D4FF", green:"#00E5A0"}, semantic:{error:"#FF4C4C", warning:"#F5A623", success:"#00E5A0", info:"#00D4FF"}};
var Y$$module$dist$bridge$knoxx_frontend_bridge_es = {monokai:de$$module$dist$bridge$knoxx_frontend_bridge_es("monokai", "Monokai", R$$module$dist$bridge$knoxx_frontend_bridge_es, {appearance:"dark", colorOverrides:{selection:{default:"rgba(135, 139, 145, 0.5)"}, interactive:{default:R$$module$dist$bridge$knoxx_frontend_bridge_es.accent.green, hover:"#8fce26", active:"#7cb824", disabled:R$$module$dist$bridge$knoxx_frontend_bridge_es.fg.muted}, button:{primary:{bg:R$$module$dist$bridge$knoxx_frontend_bridge_es.fg.muted, 
fg:R$$module$dist$bridge$knoxx_frontend_bridge_es.fg.default, hover:"#8a856e", active:"#6a6654"}, secondary:{bg:R$$module$dist$bridge$knoxx_frontend_bridge_es.bg.selection, fg:R$$module$dist$bridge$knoxx_frontend_bridge_es.fg.default, hover:"#505248", active:"#3a3c33"}, ghost:{bg:"transparent", fg:R$$module$dist$bridge$knoxx_frontend_bridge_es.fg.default, hover:R$$module$dist$bridge$knoxx_frontend_bridge_es.bg.selection, active:R$$module$dist$bridge$knoxx_frontend_bridge_es.bg.tabInactive}, danger:{bg:R$$module$dist$bridge$knoxx_frontend_bridge_es.accent.red, 
fg:R$$module$dist$bridge$knoxx_frontend_bridge_es.fg.default, hover:"#e61b63", active:"#d1155c"}}, border:{focus:"#99947c"}, chart:{segment5:"#7ca3b5"}, fill:{good:{start:R$$module$dist$bridge$knoxx_frontend_bridge_es.accent.green, end:"#78efb7"}, warn:{start:R$$module$dist$bridge$knoxx_frontend_bridge_es.accent.orange, end:"#ffd280"}, danger:{start:R$$module$dist$bridge$knoxx_frontend_bridge_es.accent.red, end:"#ff9e92"}, neutral:{start:"#7aa7bd", end:"#98bfd0"}}, alpha:{warningBg:"rgba(58, 41, 16, 0.88)", 
errorBg:"rgba(70, 24, 24, 0.42)", errorBgSolid:"rgba(70, 24, 24, 0.9)", federationError:"rgba(127, 29, 29, 0.22)"}}}), "night-owl":de$$module$dist$bridge$knoxx_frontend_bridge_es("night-owl", "Night Owl", T$$module$dist$bridge$knoxx_frontend_bridge_es, {appearance:"dark", colorOverrides:{interactive:{default:T$$module$dist$bridge$knoxx_frontend_bridge_es.accent.cyan, hover:"#7fdbca", active:"#21c7a8", disabled:T$$module$dist$bridge$knoxx_frontend_bridge_es.fg.muted}, button:{primary:{bg:"#7e57c2cc", 
fg:"#ffffffcc", hover:"#7e57c2", active:"#6747a4"}, secondary:{bg:T$$module$dist$bridge$knoxx_frontend_bridge_es.bg.selection, fg:T$$module$dist$bridge$knoxx_frontend_bridge_es.fg.default, hover:"#234d708c", active:"#0e293f"}, ghost:{bg:"transparent", fg:T$$module$dist$bridge$knoxx_frontend_bridge_es.fg.default, hover:n$$module$dist$bridge$knoxx_frontend_bridge_es(T$$module$dist$bridge$knoxx_frontend_bridge_es.bg.selection, 0.55), active:T$$module$dist$bridge$knoxx_frontend_bridge_es.bg.lighter}, 
danger:{bg:T$$module$dist$bridge$knoxx_frontend_bridge_es.accent.red, fg:"#ffffffcc", hover:"#ec5f67", active:"#d3423e"}}, badge:{default:{bg:T$$module$dist$bridge$knoxx_frontend_bridge_es.fg.muted, fg:"#ffffff"}}, border:{default:T$$module$dist$bridge$knoxx_frontend_bridge_es.bg.groupBorder, subtle:T$$module$dist$bridge$knoxx_frontend_bridge_es.fg.subtle, focus:T$$module$dist$bridge$knoxx_frontend_bridge_es.accent.blue}, chart:{segment5:"#5f7e97"}, fill:{good:{start:T$$module$dist$bridge$knoxx_frontend_bridge_es.accent.green, 
end:"#d9f5dd"}, warn:{start:T$$module$dist$bridge$knoxx_frontend_bridge_es.accent.orange, end:"#ffcb8b"}, danger:{start:T$$module$dist$bridge$knoxx_frontend_bridge_es.accent.red, end:"#ff869a"}, neutral:{start:T$$module$dist$bridge$knoxx_frontend_bridge_es.accent.blue, end:"#c5e4fd"}}, alpha:{warningBg:"#675700F2", errorBg:"rgba(171, 3, 0, 0.42)", errorBgSolid:"#AB0300F2", federationError:n$$module$dist$bridge$knoxx_frontend_bridge_es(T$$module$dist$bridge$knoxx_frontend_bridge_es.accent.red, 0.22), 
shadow:"rgba(1, 22, 39, 0.35)", shadowLight:"rgba(1, 11, 20, 0.3)", shadowDeep:"rgba(1, 17, 29, 0.45)"}}}), "proxy-console":de$$module$dist$bridge$knoxx_frontend_bridge_es("proxy-console", "Proxy Console", p$$module$dist$bridge$knoxx_frontend_bridge_es, {appearance:"dark", colorOverrides:{interactive:{default:p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.cyan, hover:"#34DEFF", active:"#00B8DE", disabled:p$$module$dist$bridge$knoxx_frontend_bridge_es.fg.subtle}, button:{primary:{bg:"#00D4FF", 
fg:"#0A0C0F", hover:"#34DEFF", active:"#00B8DE"}, secondary:{bg:"#0F1318", fg:"#E8ECF1", hover:"#131820", active:"#1A212C"}, ghost:{bg:"transparent", fg:"#E8ECF1", hover:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.cyan, 0.12), active:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.cyan, 0.18)}, danger:{bg:"#FF4C4C", fg:"#FDFEFF", hover:"#FF6666", active:"#E64545"}}, badge:{default:{bg:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.fg.subtle, 
0.2), fg:"#E8ECF1"}, success:{bg:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.green, 0.12), fg:"#00E5A0"}, warning:{bg:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.orange, 0.12), fg:"#F5A623"}, error:{bg:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.red, 0.12), fg:"#FF4C4C"}, info:{bg:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.cyan, 
0.12), fg:"#00D4FF"}}, border:{default:"#1E2530", subtle:"#171C25", focus:"#00D4FF", error:"#FF4C4C"}, chart:{segment0:"#00D4FF", segment1:"#00E5A0", segment2:"#F5A623", segment3:"#FF4C4C", segment4:"#9B8CFF", segment5:"#7A90AA"}, fill:{good:{start:"#00E5A0", end:"#57F0C0"}, warn:{start:"#F5A623", end:"#FFD07A"}, danger:{start:"#FF4C4C", end:"#FF8C8C"}, neutral:{start:"#00D4FF", end:"#8EEBFF"}}, surface:{panel:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.bg.darker, 
0.92), card:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.bg.tabInactive, 0.82), cardAlt:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.bg.lighter, 0.62), input:n$$module$dist$bridge$knoxx_frontend_bridge_es("#0A0D11", 0.95), nav:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.bg.default, 0.88)}, alpha:{green:{_08:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.green, 
0.08), _12:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.green, 0.12), _14:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.green, 0.14), _15:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.green, 0.15), _16:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.green, 0.16), _25:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.green, 
0.25), _28:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.green, 0.28), _30:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.green, 0.3), _35:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.green, 0.35), _38:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.green, 0.38), _40:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.green, 
0.4), _45:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.green, 0.45), _50:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.green, 0.5), _55:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.green, 0.55), _60:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.green, 0.6), _80:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.green, 
0.8)}, red:{_12:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.red, 0.12), _14:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.red, 0.14), _15:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.red, 0.15), _25:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.red, 0.25), _30:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.red, 
0.3), _38:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.red, 0.38), _40:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.red, 0.4), _45:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.red, 0.45), _46:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.red, 0.46), _50:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.red, 
0.5)}, orange:{_12:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.orange, 0.12), _15:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.orange, 0.15), _32:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.orange, 0.32), _35:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.orange, 0.35), _40:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.orange, 
0.4)}, blue:{_15:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.blue, 0.15), _20:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.blue, 0.2), _35:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.blue, 0.35), _45:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.blue, 0.45), _80:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.blue, 
0.8), _95:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.blue, 0.95)}, magenta:{_08:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.magenta, 0.08), _14:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.magenta, 0.14), _30:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.magenta, 0.3)}, yellow:{_06:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.accent.yellow, 
0.06)}, bg:{_08:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.bg.tabInactive, 0.08), _10:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.bg.tabInactive, 0.1), _12:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.bg.tabInactive, 0.12), _14:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.bg.tabInactive, 0.14), _16:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.bg.tabInactive, 
0.16), _18:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.bg.tabInactive, 0.18), _24:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.bg.tabInactive, 0.24), _25:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.bg.tabInactive, 0.25), _28:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.bg.tabInactive, 0.28), _30:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.bg.tabInactive, 
0.3), _46:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.bg.default, 0.46), _55:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.bg.tabInactive, 0.55), _60:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.bg.default, 0.6), _62:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.bg.default, 0.62), _68:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.bg.default, 
0.68), _70:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.bg.tabInactive, 0.7), _72:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.bg.default, 0.72), _80:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.bg.default, 0.8), _85:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.bg.tabInactive, 0.85), _88:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.bg.tabInactive, 
0.88), _88b:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.bg.default, 0.88), _90:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.bg.default, 0.9), _95:n$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es.bg.default, 0.95)}, warningBg:n$$module$dist$bridge$knoxx_frontend_bridge_es("#442E0C", 0.88), errorBg:n$$module$dist$bridge$knoxx_frontend_bridge_es("#5A1C1C", 0.42), 
errorBgSolid:n$$module$dist$bridge$knoxx_frontend_bridge_es("#5A1C1C", 0.9), federationError:n$$module$dist$bridge$knoxx_frontend_bridge_es("#7F1D1D", 0.22), white:{_08:n$$module$dist$bridge$knoxx_frontend_bridge_es("#FFFFFF", 0.08)}, shadow:n$$module$dist$bridge$knoxx_frontend_bridge_es("#000000", 0.4), shadowLight:n$$module$dist$bridge$knoxx_frontend_bridge_es("#000000", 0.3), shadowDeep:n$$module$dist$bridge$knoxx_frontend_bridge_es("#000000", 0.5)}}})};
var G$$module$dist$bridge$knoxx_frontend_bridge_es = "monokai";
function ae$$module$dist$bridge$knoxx_frontend_bridge_es(e) {
  return Object.values(Y$$module$dist$bridge$knoxx_frontend_bridge_es).find(r => r.appearance === e) ?? Y$$module$dist$bridge$knoxx_frontend_bridge_es[G$$module$dist$bridge$knoxx_frontend_bridge_es];
}
function je$$module$dist$bridge$knoxx_frontend_bridge_es(e = G$$module$dist$bridge$knoxx_frontend_bridge_es) {
  return e === "dark" ? ae$$module$dist$bridge$knoxx_frontend_bridge_es("dark").name : e === "light" ? ae$$module$dist$bridge$knoxx_frontend_bridge_es("light").name : e === "auto" ? typeof window < "u" && typeof window.matchMedia == "function" ? window.matchMedia("(prefers-color-scheme: light)").matches ? ae$$module$dist$bridge$knoxx_frontend_bridge_es("light").name : ae$$module$dist$bridge$knoxx_frontend_bridge_es("dark").name : G$$module$dist$bridge$knoxx_frontend_bridge_es : e;
}
function Pe$$module$dist$bridge$knoxx_frontend_bridge_es(e = G$$module$dist$bridge$knoxx_frontend_bridge_es) {
  return Y$$module$dist$bridge$knoxx_frontend_bridge_es[e];
}
function Me$$module$dist$bridge$knoxx_frontend_bridge_es(e = G$$module$dist$bridge$knoxx_frontend_bridge_es) {
  return Pe$$module$dist$bridge$knoxx_frontend_bridge_es(je$$module$dist$bridge$knoxx_frontend_bridge_es(e));
}
_e$$module$dist$bridge$knoxx_frontend_bridge_es(Y$$module$dist$bridge$knoxx_frontend_bridge_es[G$$module$dist$bridge$knoxx_frontend_bridge_es].colors, ["colors"]);
var W$$module$dist$bridge$knoxx_frontend_bridge_es = 4;
var Ue$$module$dist$bridge$knoxx_frontend_bridge_es = {1:W$$module$dist$bridge$knoxx_frontend_bridge_es * 1, "1.5":W$$module$dist$bridge$knoxx_frontend_bridge_es * 1.5, 2:W$$module$dist$bridge$knoxx_frontend_bridge_es * 2, 3:W$$module$dist$bridge$knoxx_frontend_bridge_es * 3, 4:W$$module$dist$bridge$knoxx_frontend_bridge_es * 4, 5:W$$module$dist$bridge$knoxx_frontend_bridge_es * 5, 6:W$$module$dist$bridge$knoxx_frontend_bridge_es * 6, 8:W$$module$dist$bridge$knoxx_frontend_bridge_es * 8};
var pe$$module$dist$bridge$knoxx_frontend_bridge_es = {sans:["Inter", "system-ui", "-apple-system", "BlinkMacSystemFont", "Segoe UI", "Roboto", "Helvetica Neue", "Arial", "sans-serif"].join(", "), mono:["JetBrains Mono", "Fira Code", "Monaco", "Consolas", "Liberation Mono", "Courier New", "monospace"].join(", ")};
var L$$module$dist$bridge$knoxx_frontend_bridge_es = {xs:"0.75rem", sm:"0.875rem", base:"1rem", lg:"1.125rem", xl:"1.25rem", "2xl":"1.5rem", "3xl":"1.875rem", "4xl":"2.25rem", "5xl":"3rem", inlineCode:"0.875em"};
var Ge$$module$dist$bridge$knoxx_frontend_bridge_es = {medium:500, semibold:600};
var Ze$$module$dist$bridge$knoxx_frontend_bridge_es = {none:1, normal:1.5};
L$$module$dist$bridge$knoxx_frontend_bridge_es["4xl"], L$$module$dist$bridge$knoxx_frontend_bridge_es["3xl"], L$$module$dist$bridge$knoxx_frontend_bridge_es["2xl"], L$$module$dist$bridge$knoxx_frontend_bridge_es.xl, L$$module$dist$bridge$knoxx_frontend_bridge_es.lg, L$$module$dist$bridge$knoxx_frontend_bridge_es.base, L$$module$dist$bridge$knoxx_frontend_bridge_es.base, L$$module$dist$bridge$knoxx_frontend_bridge_es.sm, L$$module$dist$bridge$knoxx_frontend_bridge_es.sm, L$$module$dist$bridge$knoxx_frontend_bridge_es.xs, 
pe$$module$dist$bridge$knoxx_frontend_bridge_es.mono, L$$module$dist$bridge$knoxx_frontend_bridge_es.sm, pe$$module$dist$bridge$knoxx_frontend_bridge_es.mono, L$$module$dist$bridge$knoxx_frontend_bridge_es.inlineCode;
var We$$module$dist$bridge$knoxx_frontend_bridge_es = {fast:"100ms"};
var He$$module$dist$bridge$knoxx_frontend_bridge_es = {easeInOut:"cubic-bezier(0.4, 0, 0.2, 1)"};
var qe$$module$dist$bridge$knoxx_frontend_bridge_es = {colors:`color, background-color, border-color ${We$$module$dist$bridge$knoxx_frontend_bridge_es.fast} ${He$$module$dist$bridge$knoxx_frontend_bridge_es.easeInOut}`};
var K$$module$dist$bridge$knoxx_frontend_bridge_es = {xs:"0 1px 2px 0 rgba(0, 0, 0, 0.05)", sm:"0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px -1px rgba(0, 0, 0, 0.1)", md:"0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1)", lg:"0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -4px rgba(0, 0, 0, 0.1)", xl:"0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1)", "2xl":"0 25px 50px -12px rgba(0, 0, 0, 0.25)", inner:"inset 0 2px 4px 0 rgba(0, 0, 0, 0.05)", focus:"0 0 0 2px rgba(166, 226, 46, 0.5)", 
focusError:"0 0 0 2px rgba(249, 38, 114, 0.5)", none:"none"};
K$$module$dist$bridge$knoxx_frontend_bridge_es.none, K$$module$dist$bridge$knoxx_frontend_bridge_es.sm, K$$module$dist$bridge$knoxx_frontend_bridge_es.md, K$$module$dist$bridge$knoxx_frontend_bridge_es.lg, K$$module$dist$bridge$knoxx_frontend_bridge_es.xl, K$$module$dist$bridge$knoxx_frontend_bridge_es["2xl"];
var Ke$$module$dist$bridge$knoxx_frontend_bridge_es = {modal:1400};
var Ye$$module$dist$bridge$knoxx_frontend_bridge_es = {none:"0px", xs:"2px", sm:"4px", md:"6px", lg:"8px", xl:"12px", full:"9999px"};
function H$$module$dist$bridge$knoxx_frontend_bridge_es(e) {
  return structuredClone(e);
}
function fe$$module$dist$bridge$knoxx_frontend_bridge_es(e) {
  return typeof e == "object" && e !== null && !Array.isArray(e);
}
function Ae$$module$dist$bridge$knoxx_frontend_bridge_es(e, r) {
  if (!r) {
    return H$$module$dist$bridge$knoxx_frontend_bridge_es(e);
  }
  const a = H$$module$dist$bridge$knoxx_frontend_bridge_es(e);
  for (const [c, d] of Object.entries(r)) {
    if (d === void 0) {
      continue;
    }
    const h = a[c];
    if (fe$$module$dist$bridge$knoxx_frontend_bridge_es(h) && fe$$module$dist$bridge$knoxx_frontend_bridge_es(d)) {
      a[c] = Ae$$module$dist$bridge$knoxx_frontend_bridge_es(h, d);
      continue;
    }
    a[c] = d;
  }
  return a;
}
function be$$module$dist$bridge$knoxx_frontend_bridge_es(e, r) {
  return Ae$$module$dist$bridge$knoxx_frontend_bridge_es(e, r);
}
var ge$$module$dist$bridge$knoxx_frontend_bridge_es = {colors:H$$module$dist$bridge$knoxx_frontend_bridge_es(Y$$module$dist$bridge$knoxx_frontend_bridge_es.monokai.colors), palette:H$$module$dist$bridge$knoxx_frontend_bridge_es(R$$module$dist$bridge$knoxx_frontend_bridge_es), fontFamily:pe$$module$dist$bridge$knoxx_frontend_bridge_es, fontSize:L$$module$dist$bridge$knoxx_frontend_bridge_es, shadow:K$$module$dist$bridge$knoxx_frontend_bridge_es, radius:Ye$$module$dist$bridge$knoxx_frontend_bridge_es};
var q$$module$dist$bridge$knoxx_frontend_bridge_es = {monokai:ge$$module$dist$bridge$knoxx_frontend_bridge_es, "night-owl":be$$module$dist$bridge$knoxx_frontend_bridge_es(ge$$module$dist$bridge$knoxx_frontend_bridge_es, {colors:H$$module$dist$bridge$knoxx_frontend_bridge_es(Y$$module$dist$bridge$knoxx_frontend_bridge_es["night-owl"].colors), palette:H$$module$dist$bridge$knoxx_frontend_bridge_es(T$$module$dist$bridge$knoxx_frontend_bridge_es), shadow:{focus:"0 0 0 2px rgba(130, 170, 255, 0.35)", 
focusError:"0 0 0 2px rgba(239, 83, 80, 0.35)"}}), "proxy-console":be$$module$dist$bridge$knoxx_frontend_bridge_es(ge$$module$dist$bridge$knoxx_frontend_bridge_es, {colors:H$$module$dist$bridge$knoxx_frontend_bridge_es(Y$$module$dist$bridge$knoxx_frontend_bridge_es["proxy-console"].colors), palette:H$$module$dist$bridge$knoxx_frontend_bridge_es(p$$module$dist$bridge$knoxx_frontend_bridge_es), fontFamily:{sans:["IBM Plex Sans", "Segoe UI", "system-ui", "sans-serif"].join(", "), mono:["JetBrains Mono", 
"Fira Code", "monospace"].join(", ")}, shadow:{xs:"0 1px 2px 0 rgba(0, 0, 0, 0.35)", sm:"0 1px 3px rgba(0, 0, 0, 0.4)", md:"0 4px 12px rgba(0, 0, 0, 0.5)", lg:"0 10px 24px rgba(0, 0, 0, 0.55)", xl:"0 20px 36px rgba(0, 0, 0, 0.6)", "2xl":"0 28px 64px rgba(0, 0, 0, 0.65)", inner:"inset 0 1px 2px rgba(0, 0, 0, 0.28)", focus:"0 0 0 2px rgba(0, 212, 255, 0.35)", focusError:"0 0 0 2px rgba(255, 76, 76, 0.35)", none:"none"}, radius:{none:"0px", xs:"2px", sm:"4px", md:"4px", lg:"6px", xl:"8px", full:"9999px"}})};
function Ve$$module$dist$bridge$knoxx_frontend_bridge_es(e) {
  return `--uxx-${e.map(Ee$$module$dist$bridge$knoxx_frontend_bridge_es).join("-")}`;
}
function V$$module$dist$bridge$knoxx_frontend_bridge_es(e, r = []) {
  const a = {};
  for (const [c, d] of Object.entries(e)) {
    const h = [...r, c];
    if (fe$$module$dist$bridge$knoxx_frontend_bridge_es(d)) {
      a[c] = V$$module$dist$bridge$knoxx_frontend_bridge_es(d, h);
      continue;
    }
    a[c] = `var(${Ve$$module$dist$bridge$knoxx_frontend_bridge_es(h)}, ${String(d)})`;
  }
  return a;
}
var Xe$$module$dist$bridge$knoxx_frontend_bridge_es = V$$module$dist$bridge$knoxx_frontend_bridge_es(q$$module$dist$bridge$knoxx_frontend_bridge_es.monokai.colors, ["colors"]);
V$$module$dist$bridge$knoxx_frontend_bridge_es(q$$module$dist$bridge$knoxx_frontend_bridge_es.monokai.palette, ["palette"]);
var ye$$module$dist$bridge$knoxx_frontend_bridge_es = V$$module$dist$bridge$knoxx_frontend_bridge_es(q$$module$dist$bridge$knoxx_frontend_bridge_es.monokai.fontFamily, ["fontFamily"]);
var O$$module$dist$bridge$knoxx_frontend_bridge_es = V$$module$dist$bridge$knoxx_frontend_bridge_es(q$$module$dist$bridge$knoxx_frontend_bridge_es.monokai.fontSize, ["fontSize"]);
var Qe$$module$dist$bridge$knoxx_frontend_bridge_es = V$$module$dist$bridge$knoxx_frontend_bridge_es(q$$module$dist$bridge$knoxx_frontend_bridge_es.monokai.shadow, ["shadow"]);
var Je$$module$dist$bridge$knoxx_frontend_bridge_es = V$$module$dist$bridge$knoxx_frontend_bridge_es(q$$module$dist$bridge$knoxx_frontend_bridge_es.monokai.radius, ["radius"]);
var et$$module$dist$bridge$knoxx_frontend_bridge_es = {h1:{fontSize:O$$module$dist$bridge$knoxx_frontend_bridge_es["4xl"]}, h2:{fontSize:O$$module$dist$bridge$knoxx_frontend_bridge_es["3xl"]}, h3:{fontSize:O$$module$dist$bridge$knoxx_frontend_bridge_es["2xl"]}, h4:{fontSize:O$$module$dist$bridge$knoxx_frontend_bridge_es.xl}, h5:{fontSize:O$$module$dist$bridge$knoxx_frontend_bridge_es.lg}, h6:{fontSize:O$$module$dist$bridge$knoxx_frontend_bridge_es.base}, body:{fontSize:O$$module$dist$bridge$knoxx_frontend_bridge_es.base}, 
bodySm:{fontSize:O$$module$dist$bridge$knoxx_frontend_bridge_es.sm}, label:{fontSize:O$$module$dist$bridge$knoxx_frontend_bridge_es.sm}, caption:{fontSize:O$$module$dist$bridge$knoxx_frontend_bridge_es.xs}, code:{fontFamily:ye$$module$dist$bridge$knoxx_frontend_bridge_es.mono, fontSize:O$$module$dist$bridge$knoxx_frontend_bridge_es.sm}, codeInline:{fontFamily:ye$$module$dist$bridge$knoxx_frontend_bridge_es.mono, fontSize:O$$module$dist$bridge$knoxx_frontend_bridge_es.inlineCode}};
var t$$module$dist$bridge$knoxx_frontend_bridge_es = {colors:Xe$$module$dist$bridge$knoxx_frontend_bridge_es, spacing:Ue$$module$dist$bridge$knoxx_frontend_bridge_es, fontFamily:ye$$module$dist$bridge$knoxx_frontend_bridge_es, fontSize:O$$module$dist$bridge$knoxx_frontend_bridge_es, fontWeight:Ge$$module$dist$bridge$knoxx_frontend_bridge_es, lineHeight:Ze$$module$dist$bridge$knoxx_frontend_bridge_es, typography:et$$module$dist$bridge$knoxx_frontend_bridge_es, transitions:qe$$module$dist$bridge$knoxx_frontend_bridge_es, 
shadow:Qe$$module$dist$bridge$knoxx_frontend_bridge_es, zIndex:Ke$$module$dist$bridge$knoxx_frontend_bridge_es, radius:Je$$module$dist$bridge$knoxx_frontend_bridge_es};
function tt$$module$dist$bridge$knoxx_frontend_bridge_es(e) {
  return q$$module$dist$bridge$knoxx_frontend_bridge_es[e] ?? q$$module$dist$bridge$knoxx_frontend_bridge_es[G$$module$dist$bridge$knoxx_frontend_bridge_es];
}
function nt$$module$dist$bridge$knoxx_frontend_bridge_es(e, r) {
  const a = Me$$module$dist$bridge$knoxx_frontend_bridge_es(e);
  const c = be$$module$dist$bridge$knoxx_frontend_bridge_es(tt$$module$dist$bridge$knoxx_frontend_bridge_es(e), r);
  return {...a, palette:c.palette, colors:c.colors, fontFamily:c.fontFamily, fontSize:c.fontSize, shadow:c.shadow, radius:c.radius};
}
(0,shadow.esm.esm_import$react.createContext)({theme:G$$module$dist$bridge$knoxx_frontend_bridge_es, themeName:G$$module$dist$bridge$knoxx_frontend_bridge_es, resolvedTheme:nt$$module$dist$bridge$knoxx_frontend_bridge_es(G$$module$dist$bridge$knoxx_frontend_bridge_es)});
var rt$$module$dist$bridge$knoxx_frontend_bridge_es = {primary:{backgroundColor:t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.button.primary.bg, color:t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.button.primary.fg, border:"none"}, secondary:{backgroundColor:t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.button.secondary.bg, color:t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.button.secondary.fg, border:`1px solid ${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`}, 
ghost:{backgroundColor:"transparent", color:t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.button.ghost.fg, border:"none"}, danger:{backgroundColor:t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.button.danger.bg, color:t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.button.danger.fg, border:"none"}};
var at$$module$dist$bridge$knoxx_frontend_bridge_es = {sm:{padding:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[1.5]}px ${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[3]}px`, fontSize:t$$module$dist$bridge$knoxx_frontend_bridge_es.typography.bodySm.fontSize, gap:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[1]}px`}, md:{padding:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[2]}px ${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[4]}px`, fontSize:t$$module$dist$bridge$knoxx_frontend_bridge_es.typography.body.fontSize, 
gap:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[2]}px`}, lg:{padding:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[3]}px ${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[6]}px`, fontSize:t$$module$dist$bridge$knoxx_frontend_bridge_es.typography.body.fontSize, gap:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[2]}px`}};
var ot$$module$dist$bridge$knoxx_frontend_bridge_es = {display:"inline-flex", alignItems:"center", justifyContent:"center", fontWeight:t$$module$dist$bridge$knoxx_frontend_bridge_es.fontWeight.medium, lineHeight:t$$module$dist$bridge$knoxx_frontend_bridge_es.lineHeight.none, borderRadius:t$$module$dist$bridge$knoxx_frontend_bridge_es.radius.md, cursor:"pointer", transition:t$$module$dist$bridge$knoxx_frontend_bridge_es.transitions.colors, outline:"none", fontFamily:t$$module$dist$bridge$knoxx_frontend_bridge_es.fontFamily.sans};
function st$$module$dist$bridge$knoxx_frontend_bridge_es({size:e}) {
  const r = e === "sm" ? 14 : e === "lg" ? 20 : 16;
  return (0,shadow.esm.esm_import$react$jsx_runtime.jsx)("svg", {width:r, height:r, viewBox:"0 0 24 24", fill:"none", stroke:"currentColor", strokeWidth:"2", strokeLinecap:"round", strokeLinejoin:"round", style:{animation:"spin 1s linear infinite"}, children:(0,shadow.esm.esm_import$react$jsx_runtime.jsx)("path", {d:"M21 12a9 9 0 11-6.219-8.56"})});
}
var Ce$$module$dist$bridge$knoxx_frontend_bridge_es = (0,shadow.esm.esm_import$react.forwardRef)(({variant:e = "secondary", size:r = "md", disabled:a = !1, loading:c = !1, fullWidth:d = !1, iconStart:h, iconEnd:x, children:w, type:y = "button", ...b}, k) => {
  const g = a || c;
  const S = {...ot$$module$dist$bridge$knoxx_frontend_bridge_es, ...rt$$module$dist$bridge$knoxx_frontend_bridge_es[e], ...at$$module$dist$bridge$knoxx_frontend_bridge_es[r], width:d ? "100%" : void 0, opacity:g ? 0.6 : 1, cursor:g ? "not-allowed" : "pointer"};
  return (0,shadow.esm.esm_import$react$jsx_runtime.jsxs)("button", {ref:k, type:y, disabled:g, "data-component":"button", "data-variant":e, "data-size":r, "data-loading":c || void 0, "data-full-width":d || void 0, "aria-busy":c, style:S, ...b, children:[c && (0,shadow.esm.esm_import$react$jsx_runtime.jsx)(st$$module$dist$bridge$knoxx_frontend_bridge_es, {size:r}), !c && h, w, !c && x]});
});
Ce$$module$dist$bridge$knoxx_frontend_bridge_es.displayName = "Button";
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.badge.default.bg, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.badge.default.fg, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.badge.success.bg, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.badge.success.fg, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.badge.warning.bg, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.badge.warning.fg, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.badge.error.bg, 
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.badge.error.fg, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.badge.info.bg, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.badge.info.fg, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.badge.success.bg, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.badge.success.fg, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.badge.default.bg, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.badge.default.fg, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.alpha.magenta._14, 
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.accent.magenta, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.alpha.green._12, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.accent.green, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.alpha.red._12, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.accent.red, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.alpha.blue._15, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.accent.blue, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.badge.default.bg, 
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.xs, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.xs, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.sm;
t$$module$dist$bridge$knoxx_frontend_bridge_es.radius.sm, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontFamily.sans;
var lt$$module$dist$bridge$knoxx_frontend_bridge_es = `
@keyframes devel-badge-pulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(1.2);
  }
}
`;
if (typeof document < "u") {
  const e = document.createElement("style");
  e.textContent = lt$$module$dist$bridge$knoxx_frontend_bridge_es, document.head.appendChild(e);
}
var it$$module$dist$bridge$knoxx_frontend_bridge_es = {default:{backgroundColor:t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.surface, border:`1px solid ${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`, boxShadow:t$$module$dist$bridge$knoxx_frontend_bridge_es.shadow.sm}, outlined:{backgroundColor:"transparent", border:`1px solid ${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`, boxShadow:t$$module$dist$bridge$knoxx_frontend_bridge_es.shadow.none}, 
elevated:{backgroundColor:t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.elevated, border:`1px solid ${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.subtle}`, boxShadow:t$$module$dist$bridge$knoxx_frontend_bridge_es.shadow.md}};
var ve$$module$dist$bridge$knoxx_frontend_bridge_es = {none:{padding:0}, sm:{padding:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[2]}px ${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[3]}px`}, md:{padding:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[4]}px ${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[5]}px`}, lg:{padding:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[6]}px ${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[8]}px`}};
var ct$$module$dist$bridge$knoxx_frontend_bridge_es = {none:t$$module$dist$bridge$knoxx_frontend_bridge_es.radius.none, sm:t$$module$dist$bridge$knoxx_frontend_bridge_es.radius.sm, md:t$$module$dist$bridge$knoxx_frontend_bridge_es.radius.md, lg:t$$module$dist$bridge$knoxx_frontend_bridge_es.radius.lg, full:t$$module$dist$bridge$knoxx_frontend_bridge_es.radius.full};
var ut$$module$dist$bridge$knoxx_frontend_bridge_es = {display:"flex", flexDirection:"column", transition:t$$module$dist$bridge$knoxx_frontend_bridge_es.transitions.colors, fontFamily:t$$module$dist$bridge$knoxx_frontend_bridge_es.fontFamily.sans, overflow:"hidden"};
var dt$$module$dist$bridge$knoxx_frontend_bridge_es = {display:"flex", alignItems:"center", justifyContent:"space-between", padding:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[3]}px ${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[4]}px`, borderBottom:`1px solid ${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`, fontWeight:t$$module$dist$bridge$knoxx_frontend_bridge_es.fontWeight.semibold, fontSize:t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.lg};
var gt$$module$dist$bridge$knoxx_frontend_bridge_es = {flex:1, minHeight:0};
var pt$$module$dist$bridge$knoxx_frontend_bridge_es = {display:"flex", alignItems:"center", justifyContent:"flex-end", gap:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[2]}px`, padding:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[3]}px ${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[4]}px`, borderTop:`1px solid ${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`};
var ft$$module$dist$bridge$knoxx_frontend_bridge_es = (0,shadow.esm.esm_import$react.forwardRef)(({variant:e = "default", interactive:r = !1, padding:a = "md", radius:c = "md", header:d, title:h, extra:x, children:w, footer:y, onClick:b, style:k}, g) => {
  const S = r || typeof b == "function";
  const E = ve$$module$dist$bridge$knoxx_frontend_bridge_es[a].padding;
  const o = {...ut$$module$dist$bridge$knoxx_frontend_bridge_es, ...it$$module$dist$bridge$knoxx_frontend_bridge_es[e], padding:d || y ? void 0 : E, borderRadius:ct$$module$dist$bridge$knoxx_frontend_bridge_es[c], cursor:S ? "pointer" : "default"};
  const s = u => {
    S && (u.key === "Enter" || u.key === " ") && (u.preventDefault(), b == null || b({...u, currentTarget:u.currentTarget}));
  };
  const i = d !== void 0 || h !== void 0 || x !== void 0;
  const f = i || y ? ve$$module$dist$bridge$knoxx_frontend_bridge_es[a].padding : void 0;
  return (0,shadow.esm.esm_import$react$jsx_runtime.jsxs)("div", {ref:g, "data-component":"card", "data-variant":e, "data-interactive":S || void 0, role:S ? "button" : "region", tabIndex:S ? 0 : void 0, onClick:S ? b : void 0, onKeyDown:S ? s : void 0, style:{...o, ...k}, children:[i && (0,shadow.esm.esm_import$react$jsx_runtime.jsxs)("div", {style:dt$$module$dist$bridge$knoxx_frontend_bridge_es, children:[(0,shadow.esm.esm_import$react$jsx_runtime.jsxs)("div", {style:{display:"flex", alignItems:"center", 
  gap:t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[2]}, children:[d, h && !d && (0,shadow.esm.esm_import$react$jsx_runtime.jsx)("span", {children:h})]}), x && (0,shadow.esm.esm_import$react$jsx_runtime.jsx)("div", {children:x})]}), (0,shadow.esm.esm_import$react$jsx_runtime.jsx)("div", {style:{...gt$$module$dist$bridge$knoxx_frontend_bridge_es, padding:f}, children:w}), y !== void 0 && (0,shadow.esm.esm_import$react$jsx_runtime.jsx)("div", {style:pt$$module$dist$bridge$knoxx_frontend_bridge_es, 
  children:y})]});
});
ft$$module$dist$bridge$knoxx_frontend_bridge_es.displayName = "Card";
`${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.lg;
`${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`;
var bt$$module$dist$bridge$knoxx_frontend_bridge_es = {sm:{maxWidth:"400px"}, md:{maxWidth:"600px"}, lg:{maxWidth:"800px"}, xl:{maxWidth:"1000px"}, full:{maxWidth:"calc(100vw - 64px)", maxHeight:"calc(100vh - 64px)"}};
var yt$$module$dist$bridge$knoxx_frontend_bridge_es = {position:"fixed", inset:0, backgroundColor:"var(--token-colors-background-overlay)", backdropFilter:"blur(4px)", zIndex:t$$module$dist$bridge$knoxx_frontend_bridge_es.zIndex.modal, display:"flex", alignItems:"center", justifyContent:"center", padding:"32px"};
var mt$$module$dist$bridge$knoxx_frontend_bridge_es = {backgroundColor:t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.elevated, borderRadius:t$$module$dist$bridge$knoxx_frontend_bridge_es.radius.lg, boxShadow:t$$module$dist$bridge$knoxx_frontend_bridge_es.shadow["2xl"], border:`1px solid ${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.subtle}`, display:"flex", flexDirection:"column", width:"100%", maxHeight:"calc(100vh - 64px)", fontFamily:t$$module$dist$bridge$knoxx_frontend_bridge_es.fontFamily.sans};
var ht$$module$dist$bridge$knoxx_frontend_bridge_es = {display:"flex", alignItems:"center", justifyContent:"space-between", padding:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[4]}px ${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[5]}px`, borderBottom:`1px solid ${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`, fontWeight:t$$module$dist$bridge$knoxx_frontend_bridge_es.fontWeight.semibold, fontSize:t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.lg};
var xt$$module$dist$bridge$knoxx_frontend_bridge_es = {flex:1, padding:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[4]}px ${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[5]}px`, overflowY:"auto"};
var kt$$module$dist$bridge$knoxx_frontend_bridge_es = {display:"flex", alignItems:"center", justifyContent:"flex-end", gap:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[2]}px`, padding:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[3]}px ${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[5]}px`, borderTop:`1px solid ${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`};
var St$$module$dist$bridge$knoxx_frontend_bridge_es = {background:"none", border:"none", cursor:"pointer", padding:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[1]}px`, fontSize:"20px", lineHeight:1, color:t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted, transition:t$$module$dist$bridge$knoxx_frontend_bridge_es.transitions.colors};
var vt$$module$dist$bridge$knoxx_frontend_bridge_es = (0,shadow.esm.esm_import$react.forwardRef)(({open:e = !1, onClose:r, size:a = "md", closable:c = !0, closeOnBackdrop:d = !0, closeOnEscape:h = !0, centered:x = !0, scrollBehavior:w = "inside", header:y, title:b, children:k, footer:g}, S) => {
  const E = (0,shadow.esm.esm_import$react.useRef)(null);
  const o = (0,shadow.esm.esm_import$react.useRef)(null);
  const s = (0,shadow.esm.esm_import$react.useId)();
  const i = (0,shadow.esm.esm_import$react.useId)();
  (0,shadow.esm.esm_import$react.useEffect)(() => {
    var v;
    if (e) {
      return o.current = document.activeElement, (v = E.current) == null || v.focus(), document.body.style.overflow = "hidden", () => {
        var C;
        document.body.style.overflow = "", (C = o.current) == null || C.focus();
      };
    }
  }, [e]);
  const f = (0,shadow.esm.esm_import$react.useCallback)(v => {
    v.key === "Escape" && c && h && r();
  }, [c, h, r]);
  const u = (0,shadow.esm.esm_import$react.useCallback)(v => {
    d && c && v.target === v.currentTarget && r();
  }, [d, c, r]);
  const m = (0,shadow.esm.esm_import$react.useCallback)(v => {
    var C;
    if (v.key === "Tab") {
      const _ = (C = E.current) == null ? void 0 : C.querySelectorAll('button, [href], input, select, textarea, [tabindex]:not([tabindex\x3d"-1"])');
      if (!_ || _.length === 0) {
        return;
      }
      const $ = _[0];
      const z = _[_.length - 1];
      v.shiftKey && document.activeElement === $ ? (v.preventDefault(), z.focus()) : !v.shiftKey && document.activeElement === z && (v.preventDefault(), $.focus());
    }
  }, []);
  if (!e) {
    return null;
  }
  const F = (0,shadow.esm.esm_import$react$jsx_runtime.jsx)("div", {"data-component":"modal", "data-size":a, "data-open":e, style:yt$$module$dist$bridge$knoxx_frontend_bridge_es, onClick:u, onKeyDown:f, children:(0,shadow.esm.esm_import$react$jsx_runtime.jsxs)("div", {ref:v => {
    E.current = v, typeof S == "function" ? S(v) : S && (S.current = v);
  }, role:"dialog", "aria-modal":"true", "aria-labelledby":b ? s : void 0, tabIndex:-1, style:{...mt$$module$dist$bridge$knoxx_frontend_bridge_es, ...bt$$module$dist$bridge$knoxx_frontend_bridge_es[a]}, onKeyDown:m, children:[(y !== void 0 || b !== void 0) && (0,shadow.esm.esm_import$react$jsx_runtime.jsxs)("div", {style:ht$$module$dist$bridge$knoxx_frontend_bridge_es, children:[(0,shadow.esm.esm_import$react$jsx_runtime.jsxs)("div", {id:s, style:{display:"flex", alignItems:"center", gap:t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[2]}, 
  children:[y, b && !y && (0,shadow.esm.esm_import$react$jsx_runtime.jsx)("span", {children:b})]}), c && (0,shadow.esm.esm_import$react$jsx_runtime.jsx)("button", {type:"button", style:St$$module$dist$bridge$knoxx_frontend_bridge_es, onClick:r, "aria-label":"Close modal", children:"×"})]}), (0,shadow.esm.esm_import$react$jsx_runtime.jsx)("div", {id:i, style:xt$$module$dist$bridge$knoxx_frontend_bridge_es, children:k}), g !== void 0 && (0,shadow.esm.esm_import$react$jsx_runtime.jsx)("div", {style:kt$$module$dist$bridge$knoxx_frontend_bridge_es, 
  children:g})]})});
  return (0,shadow.esm.esm_import$react_dom.createPortal)(F, document.body);
});
vt$$module$dist$bridge$knoxx_frontend_bridge_es.displayName = "Modal";
`${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.lg;
`${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.surface, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.default, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.xs, t$$module$dist$bridge$knoxx_frontend_bridge_es.radius.sm, t$$module$dist$bridge$knoxx_frontend_bridge_es.shadow.md;
var wt$$module$dist$bridge$knoxx_frontend_bridge_es = {sm:{padding:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[1]}px ${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[2]}px`, fontSize:t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.sm}, md:{padding:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[2]}px ${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[3]}px`, fontSize:t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.base}, lg:{padding:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[3]}px ${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[4]}px`, 
fontSize:t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.lg}};
var Ft$$module$dist$bridge$knoxx_frontend_bridge_es = {default:{backgroundColor:t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.default, border:`1px solid ${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`}, filled:{backgroundColor:t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.surface, border:"1px solid transparent"}, unstyled:{backgroundColor:"transparent", border:"none", padding:0}};
var Et$$module$dist$bridge$knoxx_frontend_bridge_es = {width:"100%", fontFamily:t$$module$dist$bridge$knoxx_frontend_bridge_es.fontFamily.sans, color:t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.default, borderRadius:t$$module$dist$bridge$knoxx_frontend_bridge_es.radius.sm, outline:"none", transition:t$$module$dist$bridge$knoxx_frontend_bridge_es.transitions.colors};
var _t$$module$dist$bridge$knoxx_frontend_bridge_es = {borderColor:t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.error};
var At$$module$dist$bridge$knoxx_frontend_bridge_es = {backgroundColor:t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.surface, color:t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted, cursor:"not-allowed", opacity:0.7};
var Ct$$module$dist$bridge$knoxx_frontend_bridge_es = {borderColor:t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.focus, boxShadow:`0 0 0 2px ${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.alpha.blue._35}`};
var we$$module$dist$bridge$knoxx_frontend_bridge_es = {position:"absolute", top:"50%", transform:"translateY(-50%)", display:"flex", alignItems:"center", justifyContent:"center", color:t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted, pointerEvents:"none"};
var $t$$module$dist$bridge$knoxx_frontend_bridge_es = {fontSize:t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.xs, color:t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.error, marginTop:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[1]}px`};
var zt$$module$dist$bridge$knoxx_frontend_bridge_es = (0,shadow.esm.esm_import$react.forwardRef)(({type:e = "text", value:r, defaultValue:a, placeholder:c, size:d = "md", variant:h = "default", disabled:x = !1, readonly:w = !1, required:y = !1, error:b = !1, errorMessage:k, leftIcon:g, rightIcon:S, onChange:E, onFocus:o, onBlur:s, name:i, id:f, autoFocus:u, autoComplete:m, maxLength:F, minLength:v, pattern:C}, _) => {
  const [$, z] = (0,shadow.esm.esm_import$react.useState)(!1);
  const M = I => {
    z(!0), o == null || o(I);
  };
  const j = I => {
    z(!1), s == null || s(I);
  };
  const N = {...Et$$module$dist$bridge$knoxx_frontend_bridge_es, ...Ft$$module$dist$bridge$knoxx_frontend_bridge_es[h], ...(h !== "unstyled" ? wt$$module$dist$bridge$knoxx_frontend_bridge_es[d] : {}), ...(b ? _t$$module$dist$bridge$knoxx_frontend_bridge_es : {}), ...(x ? At$$module$dist$bridge$knoxx_frontend_bridge_es : {}), ...($ && !x && h !== "unstyled" ? Ct$$module$dist$bridge$knoxx_frontend_bridge_es : {}), paddingLeft:g && h !== "unstyled" ? "32px" : void 0, paddingRight:S && h !== "unstyled" ? 
  "32px" : void 0};
  return (0,shadow.esm.esm_import$react$jsx_runtime.jsxs)("div", {style:{position:"relative", width:"100%"}, children:[g && h !== "unstyled" && (0,shadow.esm.esm_import$react$jsx_runtime.jsx)("div", {style:{...we$$module$dist$bridge$knoxx_frontend_bridge_es, left:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[2]}px`}, children:g}), (0,shadow.esm.esm_import$react$jsx_runtime.jsx)("input", {ref:_, "data-component":"input", "data-size":d, "data-variant":h, "data-error":b || void 0, "data-disabled":x || 
  void 0, type:e, value:r, defaultValue:a, placeholder:c, disabled:x, readOnly:w, required:y, name:i, id:f, autoFocus:u, autoComplete:m, maxLength:F, minLength:v, pattern:C, style:N, onChange:E, onFocus:M, onBlur:j, "aria-invalid":b, "aria-describedby":b && k ? `${f}-error` : void 0}), S && h !== "unstyled" && (0,shadow.esm.esm_import$react$jsx_runtime.jsx)("div", {style:{...we$$module$dist$bridge$knoxx_frontend_bridge_es, right:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[2]}px`}, children:S}), 
  b && k && (0,shadow.esm.esm_import$react$jsx_runtime.jsx)("div", {id:`${f}-error`, style:$t$$module$dist$bridge$knoxx_frontend_bridge_es, role:"alert", children:k})]});
});
zt$$module$dist$bridge$knoxx_frontend_bridge_es.displayName = "Input";
var It$$module$dist$bridge$knoxx_frontend_bridge_es = {sm:{padding:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[1]}px ${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[2]}px`, fontSize:t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.sm}, md:{padding:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[2]}px ${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[3]}px`, fontSize:t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.base}, lg:{padding:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[3]}px ${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[4]}px`, 
fontSize:t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.lg}};
var Tt$$module$dist$bridge$knoxx_frontend_bridge_es = {default:{backgroundColor:"transparent"}, filled:{backgroundColor:t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.surface}, unstyled:{backgroundColor:"transparent", border:"none", padding:0}};
var Rt$$module$dist$bridge$knoxx_frontend_bridge_es = {display:"inline-flex", alignItems:"center", borderRadius:t$$module$dist$bridge$knoxx_frontend_bridge_es.radius.md, border:`1px solid ${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`, color:t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.default, fontFamily:t$$module$dist$bridge$knoxx_frontend_bridge_es.fontFamily.sans, lineHeight:t$$module$dist$bridge$knoxx_frontend_bridge_es.lineHeight.normal, cursor:"pointer", 
appearance:"none", backgroundImage:`url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12' fill='none'%3E%3Cpath d='M2.5 4.5L6 8L9.5 4.5' stroke='%2375715e' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E")`, backgroundRepeat:"no-repeat", backgroundPosition:`right ${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[2]}px center`, paddingRight:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[6]}px`, 
transition:t$$module$dist$bridge$knoxx_frontend_bridge_es.transitions.colors};
var Dt$$module$dist$bridge$knoxx_frontend_bridge_es = {borderColor:t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.error};
var Lt$$module$dist$bridge$knoxx_frontend_bridge_es = {opacity:0.5, cursor:"not-allowed"};
var Ot$$module$dist$bridge$knoxx_frontend_bridge_es = (0,shadow.esm.esm_import$react.forwardRef)(({size:e = "md", variant:r = "default", error:a = !1, errorMessage:c, options:d, placeholder:h, children:x, className:w, disabled:y, style:b, ...k}, g) => {
  const S = {...Rt$$module$dist$bridge$knoxx_frontend_bridge_es, ...It$$module$dist$bridge$knoxx_frontend_bridge_es[e], ...Tt$$module$dist$bridge$knoxx_frontend_bridge_es[r], ...(a ? Dt$$module$dist$bridge$knoxx_frontend_bridge_es : {}), ...(y ? Lt$$module$dist$bridge$knoxx_frontend_bridge_es : {}), ...b};
  return (0,shadow.esm.esm_import$react$jsx_runtime.jsxs)("div", {"data-component":"select-wrapper", style:{display:"inline-flex", flexDirection:"column", gap:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[1]}px`}, children:[(0,shadow.esm.esm_import$react$jsx_runtime.jsxs)("select", {ref:g, "data-component":"select", "data-size":e, "data-variant":r, "data-error":a || void 0, disabled:y, style:S, className:w, ...k, children:[h && (0,shadow.esm.esm_import$react$jsx_runtime.jsx)("option", 
  {value:"", disabled:!0, children:h}), d == null ? void 0 : d.map(E => (0,shadow.esm.esm_import$react$jsx_runtime.jsx)("option", {value:E.value, disabled:E.disabled, children:E.label}, E.value)), x]}), a && c && (0,shadow.esm.esm_import$react$jsx_runtime.jsx)("span", {style:{fontSize:t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.xs, color:t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.accent.red}, children:c})]});
});
Ot$$module$dist$bridge$knoxx_frontend_bridge_es.displayName = "Select";
var Bt$$module$dist$bridge$knoxx_frontend_bridge_es = {sm:{padding:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[1]}px ${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[2]}px`, fontSize:t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.sm}, md:{padding:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[2]}px ${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[3]}px`, fontSize:t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.base}, lg:{padding:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[3]}px ${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[4]}px`, 
fontSize:t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.lg}};
var Nt$$module$dist$bridge$knoxx_frontend_bridge_es = {default:{backgroundColor:"transparent"}, filled:{backgroundColor:t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.surface}, unstyled:{backgroundColor:"transparent", border:"none", padding:0}};
var jt$$module$dist$bridge$knoxx_frontend_bridge_es = {display:"block", width:"100%", borderRadius:t$$module$dist$bridge$knoxx_frontend_bridge_es.radius.md, border:`1px solid ${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`, color:t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.default, fontFamily:t$$module$dist$bridge$knoxx_frontend_bridge_es.fontFamily.sans, lineHeight:t$$module$dist$bridge$knoxx_frontend_bridge_es.lineHeight.normal, resize:"vertical", transition:t$$module$dist$bridge$knoxx_frontend_bridge_es.transitions.colors};
var Pt$$module$dist$bridge$knoxx_frontend_bridge_es = {borderColor:t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.error};
var Mt$$module$dist$bridge$knoxx_frontend_bridge_es = {opacity:0.5, cursor:"not-allowed"};
var Ut$$module$dist$bridge$knoxx_frontend_bridge_es = (0,shadow.esm.esm_import$react.forwardRef)(({size:e = "md", variant:r = "default", error:a = !1, errorMessage:c, className:d, disabled:h, style:x, ...w}, y) => {
  const b = {...jt$$module$dist$bridge$knoxx_frontend_bridge_es, ...Bt$$module$dist$bridge$knoxx_frontend_bridge_es[e], ...Nt$$module$dist$bridge$knoxx_frontend_bridge_es[r], ...(a ? Pt$$module$dist$bridge$knoxx_frontend_bridge_es : {}), ...(h ? Mt$$module$dist$bridge$knoxx_frontend_bridge_es : {}), ...x};
  return (0,shadow.esm.esm_import$react$jsx_runtime.jsxs)("div", {"data-component":"textarea-wrapper", style:{display:"flex", flexDirection:"column", gap:`${t$$module$dist$bridge$knoxx_frontend_bridge_es.spacing[1]}px`}, children:[(0,shadow.esm.esm_import$react$jsx_runtime.jsx)("textarea", {ref:y, "data-component":"textarea", "data-size":e, "data-variant":r, "data-error":a || void 0, disabled:h, style:b, className:d, ...w}), a && c && (0,shadow.esm.esm_import$react$jsx_runtime.jsx)("span", {style:{fontSize:t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.xs, 
  color:t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.accent.red}, children:c})]});
});
Ut$$module$dist$bridge$knoxx_frontend_bridge_es.displayName = "Textarea";
`${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.alpha.blue._80}${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.accent.cyan}`, `${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.alpha.green._80}${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.accent.green}`, `${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.alpha.orange._40}${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.accent.orange}`, `${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.alpha.red._45}${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.accent.red}`, 
`${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.alpha.blue._80}${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.accent.blue}`, `${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.alpha.blue._95}${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.alpha.green._80}${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.alpha.magenta._30}`;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.surface, t$$module$dist$bridge$knoxx_frontend_bridge_es.radius.sm;
t$$module$dist$bridge$knoxx_frontend_bridge_es.radius.sm;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.xs, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.xs, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted;
var Gt$$module$dist$bridge$knoxx_frontend_bridge_es = `
@keyframes devel-progress-indeterminate {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(200%); }
}
`;
var Zt$$module$dist$bridge$knoxx_frontend_bridge_es = `
@keyframes devel-progress-striped {
  0% { background-position: 1rem 0; }
  100% { background-position: 0 0; }
}
`;
if (typeof document < "u") {
  const e = document.createElement("style");
  e.textContent = Gt$$module$dist$bridge$knoxx_frontend_bridge_es + Zt$$module$dist$bridge$knoxx_frontend_bridge_es, document.head.appendChild(e);
}
`${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.default;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.default;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.secondary, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontFamily.mono;
`${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.default;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.default;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.secondary;
`${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontFamily.mono, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.elevated, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.default;
(0,shadow.esm.esm_import$react.createContext)(null);
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.overlay;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.elevated, t$$module$dist$bridge$knoxx_frontend_bridge_es.shadow["2xl"], `${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.subtle}`, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontFamily.sans;
`${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.default, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.lg, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontFamily.sans;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.xs, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.surface;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.surface, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.xs, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontFamily.mono;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted;
`${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.default, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontFamily.sans;
`${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.sm, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.elevated, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.xs, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted;
`${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`;
`${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.surface, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.default, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontFamily.sans, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.sm;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.button.primary.bg, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.button.primary.fg, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontFamily.sans, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.sm;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.xs, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.sm;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.xs, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted;
(0,shadow.esm.esm_import$react.createContext)(null);
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.badge.info.bg, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.semantic.info, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.badge.success.bg, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.semantic.success, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.badge.warning.bg, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.semantic.warning, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.badge.error.bg, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.semantic.error;
t$$module$dist$bridge$knoxx_frontend_bridge_es.radius.md, t$$module$dist$bridge$knoxx_frontend_bridge_es.shadow.lg, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontFamily.sans;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.sm, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.accent.cyan;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontFamily.sans, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.sm, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.default;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.surface;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.elevated;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.surface, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.sm, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.base, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.lg;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted, t$$module$dist$bridge$knoxx_frontend_bridge_es.radius.xs;
`${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`, t$$module$dist$bridge$knoxx_frontend_bridge_es.radius.sm, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontFamily.sans, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.sm;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.sm, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.base, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.lg;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontFamily.sans;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontFamily.sans, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.default, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.surface, `${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`, t$$module$dist$bridge$knoxx_frontend_bridge_es.radius.md;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.focus, `${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.alpha.blue._35}`;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.surface, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.elevated, `${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`, t$$module$dist$bridge$knoxx_frontend_bridge_es.radius.md, t$$module$dist$bridge$knoxx_frontend_bridge_es.shadow.lg;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.default;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.elevated;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.surface, `${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.focus}`;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.sm;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.xs, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.surface, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.surface, `${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.elevated, t$$module$dist$bridge$knoxx_frontend_bridge_es.shadow.sm;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontFamily.sans;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.surface;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.base, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.default;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.sm, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.sm;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.default;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.sm, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.base, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.lg;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontFamily.sans;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.default;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.sm, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.sm, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.default;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.xs, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.surface;
`${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.sm, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.xs, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.accent.cyan;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize["2xl"], t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.default;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.base;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.xs, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.accent.cyan;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.xl, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.default;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.sm;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.badge.info.fg, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.badge.success.fg, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.badge.warning.fg, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.badge.error.fg;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.sm;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize["2xl"], t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.default;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.sm;
`${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.accent.blue}${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.alpha.blue._35}`;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontFamily.sans, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.sm;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.xs;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted, `${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.surface;
`${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.subtle}`, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.default;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.sm;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted;
`${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.surface;
`${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.default, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.default, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontFamily.sans, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.sm;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.muted;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontSize.sm, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.text.secondary, t$$module$dist$bridge$knoxx_frontend_bridge_es.fontFamily.sans;
var Wt$$module$dist$bridge$knoxx_frontend_bridge_es = Object.create;
var he$$module$dist$bridge$knoxx_frontend_bridge_es = Object.defineProperty;
var Ht$$module$dist$bridge$knoxx_frontend_bridge_es = Object.getOwnPropertyDescriptor;
var $e$$module$dist$bridge$knoxx_frontend_bridge_es = Object.getOwnPropertyNames;
var qt$$module$dist$bridge$knoxx_frontend_bridge_es = Object.getPrototypeOf;
var Kt$$module$dist$bridge$knoxx_frontend_bridge_es = Object.prototype.hasOwnProperty;
var Yt$$module$dist$bridge$knoxx_frontend_bridge_es = (e, r) => function() {
  return r || (0,e[$e$$module$dist$bridge$knoxx_frontend_bridge_es(e)[0]])((r = {exports:{}}).exports, r), r.exports;
};
var Vt$$module$dist$bridge$knoxx_frontend_bridge_es = (e, r) => {
  for (var a in r) {
    he$$module$dist$bridge$knoxx_frontend_bridge_es(e, a, {get:r[a], enumerable:!0});
  }
};
var Xt$$module$dist$bridge$knoxx_frontend_bridge_es = (e, r, a, c) => {
  if (r && typeof r == "object" || typeof r == "function") {
    for (let d of $e$$module$dist$bridge$knoxx_frontend_bridge_es(r)) {
      !Kt$$module$dist$bridge$knoxx_frontend_bridge_es.call(e, d) && d !== a && he$$module$dist$bridge$knoxx_frontend_bridge_es(e, d, {get:() => r[d], enumerable:!(c = Ht$$module$dist$bridge$knoxx_frontend_bridge_es(r, d)) || c.enumerable});
    }
  }
  return e;
};
var Qt$$module$dist$bridge$knoxx_frontend_bridge_es = (e, r, a) => (a = e != null ? Wt$$module$dist$bridge$knoxx_frontend_bridge_es(qt$$module$dist$bridge$knoxx_frontend_bridge_es(e)) : {}, Xt$$module$dist$bridge$knoxx_frontend_bridge_es(!e || !e.__esModule ? he$$module$dist$bridge$knoxx_frontend_bridge_es(a, "default", {value:e, enumerable:!0}) : a, e));
var Jt$$module$dist$bridge$knoxx_frontend_bridge_es = Yt$$module$dist$bridge$knoxx_frontend_bridge_es({["../../node_modules/.pnpm/prismjs@1.29.0_patch_hash\x3dvrxx3pzkik6jpmgpayxfjunetu/node_modules/prismjs/prism.js"](e, r) {
  var a = function() {
    function w(o, s, i, f) {
      this.type = o, this.content = s, this.alias = i, this.length = (f || "").length | 0;
    }
    function y(o, s, i, f) {
      o.lastIndex = s;
      var u = o.exec(i);
      if (u && f && u[1]) {
        var m = u[1].length;
        u.index += m, u[0] = u[0].slice(m);
      }
      return u;
    }
    function b(o, s, i, f, u, m) {
      for (var F in i) {
        if (!(!i.hasOwnProperty(F) || !i[F])) {
          var v = i[F];
          v = Array.isArray(v) ? v : [v];
          for (var C = 0; C < v.length; ++C) {
            if (m && m.cause == F + "," + C) {
              return;
            }
            var _ = v[C];
            var $ = _.inside;
            var z = !!_.lookbehind;
            var M = !!_.greedy;
            var j = _.alias;
            if (M && !_.pattern.global) {
              var N = _.pattern.toString().match(/[imsuy]*$/)[0];
              _.pattern = RegExp(_.pattern.source, N + "g");
            }
            var se = _.pattern || _;
            var I = f.next;
            for (var U = u; I !== s.tail && !(m && U >= m.reach); U += I.value.length, I = I.next) {
              var X = I.value;
              if (s.length > o.length) {
                return;
              }
              if (!(X instanceof w)) {
                var ee = 1;
                var P;
                if (M) {
                  if (P = y(se, U, o, z), !P || P.index >= o.length) {
                    break;
                  }
                  var te = P.index;
                  var ze = P.index + P[0].length;
                  var Z = U;
                  for (Z += I.value.length; te >= Z;) {
                    I = I.next, Z += I.value.length;
                  }
                  if (Z -= I.value.length, U = Z, I.value instanceof w) {
                    continue;
                  }
                  for (var J = I; J !== s.tail && (Z < ze || typeof J.value == "string"); J = J.next) {
                    ee++, Z += J.value.length;
                  }
                  ee--, X = o.slice(U, Z), P.index -= U;
                } else if (P = y(se, 0, X, z), !P) {
                  continue;
                }
                te = P.index;
                var ne = P[0];
                var le = X.slice(0, te);
                var xe = X.slice(te + ne.length);
                var ie = U + X.length;
                m && ie > m.reach && (m.reach = ie);
                var re = I.prev;
                le && (re = g(s, re, le), U += le.length), S(s, re, ee);
                var Ie = new w(F, $ ? x.tokenize(ne, $) : ne, j, ne);
                if (I = g(s, re, Ie), xe && g(s, I, xe), ee > 1) {
                  var ce = {cause:F + "," + C, reach:ie};
                  b(o, s, i, I.prev, U, ce), m && ce.reach > m.reach && (m.reach = ce.reach);
                }
              }
            }
          }
        }
      }
    }
    function k() {
      var o = {value:null, prev:null, next:null};
      var s = {value:null, prev:o, next:null};
      o.next = s, this.head = o, this.tail = s, this.length = 0;
    }
    function g(o, s, i) {
      var f = s.next;
      var u = {value:i, prev:s, next:f};
      return s.next = u, f.prev = u, o.length++, u;
    }
    function S(o, s, i) {
      var f = s.next;
      for (var u = 0; u < i && f !== o.tail; u++) {
        f = f.next;
      }
      s.next = f, f.prev = s, o.length -= u;
    }
    function E(o) {
      var s = [];
      for (var i = o.head.next; i !== o.tail;) {
        s.push(i.value), i = i.next;
      }
      return s;
    }
    var c = /(?:^|\s)lang(?:uage)?-([\w-]+)(?=\s|$)/i;
    var d = 0;
    var h = {};
    var x = {util:{encode:function o(s) {
      return s instanceof w ? new w(s.type, o(s.content), s.alias) : Array.isArray(s) ? s.map(o) : s.replace(/&/g, "\x26amp;").replace(/</g, "\x26lt;").replace(/\u00a0/g, " ");
    }, type:function(o) {
      return Object.prototype.toString.call(o).slice(8, -1);
    }, objId:function(o) {
      return o.__id || Object.defineProperty(o, "__id", {value:++d}), o.__id;
    }, clone:function o(s, i) {
      i = i || {};
      var f;
      var u;
      switch(x.util.type(s)) {
        case "Object":
          if (u = x.util.objId(s), i[u]) {
            return i[u];
          }
          f = {}, i[u] = f;
          for (var m in s) {
            s.hasOwnProperty(m) && (f[m] = o(s[m], i));
          }
          return f;
        case "Array":
          return u = x.util.objId(s), i[u] ? i[u] : (f = [], i[u] = f, s.forEach(function(F, v) {
            f[v] = o(F, i);
          }), f);
        default:
          return s;
      }
    }, getLanguage:function(o) {
      for (; o;) {
        var s = c.exec(o.className);
        if (s) {
          return s[1].toLowerCase();
        }
        o = o.parentElement;
      }
      return "none";
    }, setLanguage:function(o, s) {
      o.className = o.className.replace(RegExp(c, "gi"), ""), o.classList.add("language-" + s);
    }, isActive:function(o, s, i) {
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
      return !!i;
    }}, languages:{plain:h, plaintext:h, text:h, txt:h, extend:function(o, s) {
      var i = x.util.clone(x.languages[o]);
      for (var f in s) {
        i[f] = s[f];
      }
      return i;
    }, insertBefore:function(o, s, i, f) {
      f = f || x.languages;
      var u = f[o];
      var m = {};
      for (var F in u) {
        if (u.hasOwnProperty(F)) {
          if (F == s) {
            for (var v in i) {
              i.hasOwnProperty(v) && (m[v] = i[v]);
            }
          }
          i.hasOwnProperty(F) || (m[F] = u[F]);
        }
      }
      var C = f[o];
      return f[o] = m, x.languages.DFS(x.languages, function(_, $) {
        $ === C && _ != o && (this[_] = m);
      }), m;
    }, DFS:function o(s, i, f, u) {
      u = u || {};
      var m = x.util.objId;
      for (var F in s) {
        if (s.hasOwnProperty(F)) {
          i.call(s, F, s[F], f || F);
          var v = s[F];
          var C = x.util.type(v);
          C === "Object" && !u[m(v)] ? (u[m(v)] = !0, o(v, i, null, u)) : C === "Array" && !u[m(v)] && (u[m(v)] = !0, o(v, i, F, u));
        }
      }
    }}, plugins:{}, highlight:function(o, s, i) {
      var f = {code:o, grammar:s, language:i};
      if (x.hooks.run("before-tokenize", f), !f.grammar) {
        throw new Error('The language "' + f.language + '" has no grammar.');
      }
      return f.tokens = x.tokenize(f.code, f.grammar), x.hooks.run("after-tokenize", f), w.stringify(x.util.encode(f.tokens), f.language);
    }, tokenize:function(o, s) {
      var i = s.rest;
      if (i) {
        for (var f in i) {
          s[f] = i[f];
        }
        delete s.rest;
      }
      var u = new k();
      return g(u, u.head, o), b(o, u, s, u.head, 0), E(u);
    }, hooks:{all:{}, add:function(o, s) {
      var i = x.hooks.all;
      i[o] = i[o] || [], i[o].push(s);
    }, run:function(o, s) {
      var i = x.hooks.all[o];
      if (!(!i || !i.length)) {
        var f = 0;
        for (var u; u = i[f++];) {
          u(s);
        }
      }
    }}, Token:w};
    w.stringify = function o(s, i) {
      if (typeof s == "string") {
        return s;
      }
      if (Array.isArray(s)) {
        var f = "";
        return s.forEach(function(C) {
          f += o(C, i);
        }), f;
      }
      var u = {type:s.type, content:o(s.content, i), tag:"span", classes:["token", s.type], attributes:{}, language:i};
      var m = s.alias;
      m && (Array.isArray(m) ? Array.prototype.push.apply(u.classes, m) : u.classes.push(m)), x.hooks.run("wrap", u);
      var F = "";
      for (var v in u.attributes) {
        F += " " + v + '\x3d"' + (u.attributes[v] || "").replace(/"/g, "\x26quot;") + '"';
      }
      return "\x3c" + u.tag + ' class\x3d"' + u.classes.join(" ") + '"' + F + "\x3e" + u.content + "\x3c/" + u.tag + "\x3e";
    };
    return x;
  }();
  r.exports = a, a.default = a;
}});
var l$$module$dist$bridge$knoxx_frontend_bridge_es = Qt$$module$dist$bridge$knoxx_frontend_bridge_es(Jt$$module$dist$bridge$knoxx_frontend_bridge_es());
l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.markup = {comment:{pattern:/\x3c!--(?:(?!\x3c!--)[\s\S])*?--\x3e/, greedy:!0}, prolog:{pattern:/<\?[\s\S]+?\?>/, greedy:!0}, doctype:{pattern:/<!DOCTYPE(?:[^>"'[\]]|"[^"]*"|'[^']*')+(?:\[(?:[^<"'\]]|"[^"]*"|'[^']*'|<(?!!--)|\x3c!--(?:[^-]|-(?!->))*--\x3e)*\]\s*)?>/i, greedy:!0, inside:{"internal-subset":{pattern:/(^[^\[]*\[)[\s\S]+(?=\]>$)/, lookbehind:!0, greedy:!0, inside:null}, string:{pattern:/"[^"]*"|'[^']*'/, greedy:!0}, punctuation:/^<!|>$|[[\]]/, 
"doctype-tag":/^DOCTYPE/i, name:/[^\s<>'"]+/}}, cdata:{pattern:/<!\[CDATA\[[\s\S]*?\]\]>/i, greedy:!0}, tag:{pattern:/<\/?(?!\d)[^\s>\/=$<%]+(?:\s(?:\s*[^\s>\/=]+(?:\s*=\s*(?:"[^"]*"|'[^']*'|[^\s'">=]+(?=[\s>]))|(?=[\s/>])))+)?\s*\/?>/, greedy:!0, inside:{tag:{pattern:/^<\/?[^\s>\/]+/, inside:{punctuation:/^<\/?/, namespace:/^[^\s>\/:]+:/}}, "special-attr":[], "attr-value":{pattern:/=\s*(?:"[^"]*"|'[^']*'|[^\s'">=]+)/, inside:{punctuation:[{pattern:/^=/, alias:"attr-equals"}, {pattern:/^(\s*)["']|["']$/, 
lookbehind:!0}]}}, punctuation:/\/?>/, "attr-name":{pattern:/[^\s>\/]+/, inside:{namespace:/^[^\s>\/:]+:/}}}}, entity:[{pattern:/&[\da-z]{1,8};/i, alias:"named-entity"}, /&#x?[\da-f]{1,8};/i]}, l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.markup.tag.inside["attr-value"].inside.entity = l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.markup.entity, l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.markup.doctype.inside["internal-subset"].inside = l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.markup, 
l$$module$dist$bridge$knoxx_frontend_bridge_es.hooks.add("wrap", function(e) {
  e.type === "entity" && (e.attributes.title = e.content.replace(/&amp;/, "\x26"));
}), Object.defineProperty(l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.markup.tag, "addInlined", {value:function(e, c) {
  var a = {};
  a = (a["language-" + c] = {pattern:/(^<!\[CDATA\[)[\s\S]+?(?=\]\]>$)/i, lookbehind:!0, inside:l$$module$dist$bridge$knoxx_frontend_bridge_es.languages[c]}, a.cdata = /^<!\[CDATA\[|\]\]>$/i, {"included-cdata":{pattern:/<!\[CDATA\[[\s\S]*?\]\]>/i, inside:a}});
  c = (a["language-" + c] = {pattern:/[\s\S]+/, inside:l$$module$dist$bridge$knoxx_frontend_bridge_es.languages[c]}, {});
  c[e] = {pattern:RegExp(/(<__[^>]*>)(?:<!\[CDATA\[(?:[^\]]|\](?!\]>))*\]\]>|(?!<!\[CDATA\[)[\s\S])*?(?=<\/__>)/.source.replace(/__/g, function() {
    return e;
  }), "i"), lookbehind:!0, greedy:!0, inside:a}, l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.insertBefore("markup", "cdata", c);
}}), Object.defineProperty(l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.markup.tag, "addAttribute", {value:function(e, r) {
  l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.markup.tag.inside["special-attr"].push({pattern:RegExp(/(^|["'\s])/.source + "(?:" + e + ")" + /\s*=\s*(?:"[^"]*"|'[^']*'|[^\s'">=]+(?=[\s>]))/.source, "i"), lookbehind:!0, inside:{"attr-name":/^[^\s=]+/, "attr-value":{pattern:/=[\s\S]+/, inside:{value:{pattern:/(^=\s*(["']|(?!["'])))\S[\s\S]*(?=\2$)/, lookbehind:!0, alias:[r, "language-" + r], inside:l$$module$dist$bridge$knoxx_frontend_bridge_es.languages[r]}, punctuation:[{pattern:/^=/, 
  alias:"attr-equals"}, /"|'/]}}}});
}}), l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.html = l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.markup, l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.mathml = l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.markup, l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.svg = l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.markup, l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.xml = l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.extend("markup", 
{}), l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.ssml = l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.xml, l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.atom = l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.xml, l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.rss = l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.xml, function(e) {
  var r = {pattern:/\\[\\(){}[\]^$+*?|.]/, alias:"escape"};
  var a = /\\(?:x[\da-fA-F]{2}|u[\da-fA-F]{4}|u\{[\da-fA-F]+\}|0[0-7]{0,2}|[123][0-7]{2}|c[a-zA-Z]|.)/;
  var c = "(?:[^\\\\-]|" + a.source + ")";
  c = RegExp(c + "-" + c);
  var d = {pattern:/(<|')[^<>']+(?=[>']$)/, lookbehind:!0, alias:"variable"};
  e.languages.regex = {"char-class":{pattern:/((?:^|[^\\])(?:\\\\)*)\[(?:[^\\\]]|\\[\s\S])*\]/, lookbehind:!0, inside:{"char-class-negation":{pattern:/(^\[)\^/, lookbehind:!0, alias:"operator"}, "char-class-punctuation":{pattern:/^\[|\]$/, alias:"punctuation"}, range:{pattern:c, inside:{escape:a, "range-punctuation":{pattern:/-/, alias:"operator"}}}, "special-escape":r, "char-set":{pattern:/\\[wsd]|\\p\{[^{}]+\}/i, alias:"class-name"}, escape:a}}, "special-escape":r, "char-set":{pattern:/\.|\\[wsd]|\\p\{[^{}]+\}/i, 
  alias:"class-name"}, backreference:[{pattern:/\\(?![123][0-7]{2})[1-9]/, alias:"keyword"}, {pattern:/\\k<[^<>']+>/, alias:"keyword", inside:{"group-name":d}}], anchor:{pattern:/[$^]|\\[ABbGZz]/, alias:"function"}, escape:a, group:[{pattern:/\((?:\?(?:<[^<>']+>|'[^<>']+'|[>:]|<?[=!]|[idmnsuxU]+(?:-[idmnsuxU]+)?:?))?/, alias:"punctuation", inside:{"group-name":d}}, {pattern:/\)/, alias:"punctuation"}], quantifier:{pattern:/(?:[+*?]|\{\d+(?:,\d*)?\})[?+]?/, alias:"number"}, alternation:{pattern:/\|/, 
  alias:"keyword"}};
}(l$$module$dist$bridge$knoxx_frontend_bridge_es), l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.clike = {comment:[{pattern:/(^|[^\\])\/\*[\s\S]*?(?:\*\/|$)/, lookbehind:!0, greedy:!0}, {pattern:/(^|[^\\:])\/\/.*/, lookbehind:!0, greedy:!0}], string:{pattern:/(["'])(?:\\(?:\r\n|[\s\S])|(?!\1)[^\\\r\n])*\1/, greedy:!0}, "class-name":{pattern:/(\b(?:class|extends|implements|instanceof|interface|new|trait)\s+|\bcatch\s+\()[\w.\\]+/i, lookbehind:!0, inside:{punctuation:/[.\\]/}}, keyword:/\b(?:break|catch|continue|do|else|finally|for|function|if|in|instanceof|new|null|return|throw|try|while)\b/, 
boolean:/\b(?:false|true)\b/, function:/\b\w+(?=\()/, number:/\b0x[\da-f]+\b|(?:\b\d+(?:\.\d*)?|\B\.\d+)(?:e[+-]?\d+)?/i, operator:/[<>]=?|[!=]=?=?|--?|\+\+?|&&?|\|\|?|[?*/~^%]/, punctuation:/[{}[\];(),.:]/}, l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.javascript = l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.extend("clike", {"class-name":[l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.clike["class-name"], {pattern:/(^|[^$\w\xA0-\uFFFF])(?!\s)[_$A-Z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*(?=\.(?:constructor|prototype))/, 
lookbehind:!0}], keyword:[{pattern:/((?:^|\})\s*)catch\b/, lookbehind:!0}, {pattern:/(^|[^.]|\.\.\.\s*)\b(?:as|assert(?=\s*\{)|async(?=\s*(?:function\b|\(|[$\w\xA0-\uFFFF]|$))|await|break|case|class|const|continue|debugger|default|delete|do|else|enum|export|extends|finally(?=\s*(?:\{|$))|for|from(?=\s*(?:['"]|$))|function|(?:get|set)(?=\s*(?:[#\[$\w\xA0-\uFFFF]|$))|if|implements|import|in|instanceof|interface|let|new|null|of|package|private|protected|public|return|static|super|switch|this|throw|try|typeof|undefined|var|void|while|with|yield)\b/, 
lookbehind:!0}], function:/#?(?!\s)[_$a-zA-Z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*(?=\s*(?:\.\s*(?:apply|bind|call)\s*)?\()/, number:{pattern:RegExp(/(^|[^\w$])/.source + "(?:" + /NaN|Infinity/.source + "|" + /0[bB][01]+(?:_[01]+)*n?/.source + "|" + /0[oO][0-7]+(?:_[0-7]+)*n?/.source + "|" + /0[xX][\dA-Fa-f]+(?:_[\dA-Fa-f]+)*n?/.source + "|" + /\d+(?:_\d+)*n/.source + "|" + /(?:\d+(?:_\d+)*(?:\.(?:\d+(?:_\d+)*)?)?|\.\d+(?:_\d+)*)(?:[Ee][+-]?\d+(?:_\d+)*)?/.source + ")" + /(?![\w$])/.source), lookbehind:!0}, 
operator:/--|\+\+|\*\*=?|=>|&&=?|\|\|=?|[!=]==|<<=?|>>>?=?|[-+*/%&|^!=<>]=?|\.{3}|\?\?=?|\?\.?|[~:]/}), l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.javascript["class-name"][0].pattern = /(\b(?:class|extends|implements|instanceof|interface|new)\s+)[\w.\\]+/, l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.insertBefore("javascript", "keyword", {regex:{pattern:RegExp(/((?:^|[^$\w\xA0-\uFFFF."'\])\s]|\b(?:return|yield))\s*)/.source + /\//.source + "(?:" + /(?:\[(?:[^\]\\\r\n]|\\.)*\]|\\.|[^/\\\[\r\n])+\/[dgimyus]{0,7}/.source + 
"|" + /(?:\[(?:[^[\]\\\r\n]|\\.|\[(?:[^[\]\\\r\n]|\\.|\[(?:[^[\]\\\r\n]|\\.)*\])*\])*\]|\\.|[^/\\\[\r\n])+\/[dgimyus]{0,7}v[dgimyus]{0,7}/.source + ")" + /(?=(?:\s|\/\*(?:[^*]|\*(?!\/))*\*\/)*(?:$|[\r\n,.;:})\]]|\/\/))/.source), lookbehind:!0, greedy:!0, inside:{"regex-source":{pattern:/^(\/)[\s\S]+(?=\/[a-z]*$)/, lookbehind:!0, alias:"language-regex", inside:l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.regex}, "regex-delimiter":/^\/|\/$/, "regex-flags":/^[a-z]+$/}}, "function-variable":{pattern:/#?(?!\s)[_$a-zA-Z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*(?=\s*[=:]\s*(?:async\s*)?(?:\bfunction\b|(?:\((?:[^()]|\([^()]*\))*\)|(?!\s)[_$a-zA-Z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*)\s*=>))/, 
alias:"function"}, parameter:[{pattern:/(function(?:\s+(?!\s)[_$a-zA-Z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*)?\s*\(\s*)(?!\s)(?:[^()\s]|\s+(?![\s)])|\([^()]*\))+(?=\s*\))/, lookbehind:!0, inside:l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.javascript}, {pattern:/(^|[^$\w\xA0-\uFFFF])(?!\s)[_$a-z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*(?=\s*=>)/i, lookbehind:!0, inside:l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.javascript}, {pattern:/(\(\s*)(?!\s)(?:[^()\s]|\s+(?![\s)])|\([^()]*\))+(?=\s*\)\s*=>)/, 
lookbehind:!0, inside:l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.javascript}, {pattern:/((?:\b|\s|^)(?!(?:as|async|await|break|case|catch|class|const|continue|debugger|default|delete|do|else|enum|export|extends|finally|for|from|function|get|if|implements|import|in|instanceof|interface|let|new|null|of|package|private|protected|public|return|set|static|super|switch|this|throw|try|typeof|undefined|var|void|while|with|yield)(?![$\w\xA0-\uFFFF]))(?:(?!\s)[_$a-zA-Z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*\s*)\(\s*|\]\s*\(\s*)(?!\s)(?:[^()\s]|\s+(?![\s)])|\([^()]*\))+(?=\s*\)\s*\{)/, 
lookbehind:!0, inside:l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.javascript}], constant:/\b[A-Z](?:[A-Z_]|\dx?)*\b/}), l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.insertBefore("javascript", "string", {hashbang:{pattern:/^#!.*/, greedy:!0, alias:"comment"}, "template-string":{pattern:/`(?:\\[\s\S]|\$\{(?:[^{}]|\{(?:[^{}]|\{[^}]*\})*\})+\}|(?!\$\{)[^\\`])*`/, greedy:!0, inside:{"template-punctuation":{pattern:/^`|`$/, alias:"string"}, interpolation:{pattern:/((?:^|[^\\])(?:\\{2})*)\$\{(?:[^{}]|\{(?:[^{}]|\{[^}]*\})*\})+\}/, 
lookbehind:!0, inside:{"interpolation-punctuation":{pattern:/^\$\{|\}$/, alias:"punctuation"}, rest:l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.javascript}}, string:/[\s\S]+/}}, "string-property":{pattern:/((?:^|[,{])[ \t]*)(["'])(?:\\(?:\r\n|[\s\S])|(?!\2)[^\\\r\n])*\2(?=\s*:)/m, lookbehind:!0, greedy:!0, alias:"property"}}), l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.insertBefore("javascript", "operator", {"literal-property":{pattern:/((?:^|[,{])[ \t]*)(?!\s)[_$a-zA-Z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*(?=\s*:)/m, 
lookbehind:!0, alias:"property"}}), l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.markup && (l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.markup.tag.addInlined("script", "javascript"), l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.markup.tag.addAttribute(/on(?:abort|blur|change|click|composition(?:end|start|update)|dblclick|error|focus(?:in|out)?|key(?:down|up)|load|mouse(?:down|enter|leave|move|out|over|up)|reset|resize|scroll|select|slotchange|submit|unload|wheel)/.source, 
"javascript")), l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.js = l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.javascript, l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.actionscript = l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.extend("javascript", {keyword:/\b(?:as|break|case|catch|class|const|default|delete|do|dynamic|each|else|extends|final|finally|for|function|get|if|implements|import|in|include|instanceof|interface|internal|is|namespace|native|new|null|override|package|private|protected|public|return|set|static|super|switch|this|throw|try|typeof|use|var|void|while|with)\b/, 
operator:/\+\+|--|(?:[+\-*\/%^]|&&?|\|\|?|<<?|>>?>?|[!=]=?)=?|[~?@]/}), l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.actionscript["class-name"].alias = "function", delete l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.actionscript.parameter, delete l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.actionscript["literal-property"], l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.markup && l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.insertBefore("actionscript", 
"string", {xml:{pattern:/(^|[^.])<\/?\w+(?:\s+[^\s>\/=]+=("|')(?:\\[\s\S]|(?!\2)[^\\])*\2)*\s*\/?>/, lookbehind:!0, inside:l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.markup}}), function(e) {
  var r = /#(?!\{).+/;
  var a = {pattern:/#\{[^}]+\}/, alias:"variable"};
  e.languages.coffeescript = e.languages.extend("javascript", {comment:r, string:[{pattern:/'(?:\\[\s\S]|[^\\'])*'/, greedy:!0}, {pattern:/"(?:\\[\s\S]|[^\\"])*"/, greedy:!0, inside:{interpolation:a}}], keyword:/\b(?:and|break|by|catch|class|continue|debugger|delete|do|each|else|extend|extends|false|finally|for|if|in|instanceof|is|isnt|let|loop|namespace|new|no|not|null|of|off|on|or|own|return|super|switch|then|this|throw|true|try|typeof|undefined|unless|until|when|while|window|with|yes|yield)\b/, 
  "class-member":{pattern:/@(?!\d)\w+/, alias:"variable"}}), e.languages.insertBefore("coffeescript", "comment", {"multiline-comment":{pattern:/###[\s\S]+?###/, alias:"comment"}, "block-regex":{pattern:/\/{3}[\s\S]*?\/{3}/, alias:"regex", inside:{comment:r, interpolation:a}}}), e.languages.insertBefore("coffeescript", "string", {"inline-javascript":{pattern:/`(?:\\[\s\S]|[^\\`])*`/, inside:{delimiter:{pattern:/^`|`$/, alias:"punctuation"}, script:{pattern:/[\s\S]+/, alias:"language-javascript", inside:e.languages.javascript}}}, 
  "multiline-string":[{pattern:/'''[\s\S]*?'''/, greedy:!0, alias:"string"}, {pattern:/"""[\s\S]*?"""/, greedy:!0, alias:"string", inside:{interpolation:a}}]}), e.languages.insertBefore("coffeescript", "keyword", {property:/(?!\d)\w+(?=\s*:(?!:))/}), delete e.languages.coffeescript["template-string"], e.languages.coffee = e.languages.coffeescript;
}(l$$module$dist$bridge$knoxx_frontend_bridge_es), function(e) {
  var r = e.languages.javadoclike = {parameter:{pattern:/(^[\t ]*(?:\/{3}|\*|\/\*\*)\s*@(?:arg|arguments|param)\s+)\w+/m, lookbehind:!0}, keyword:{pattern:/(^[\t ]*(?:\/{3}|\*|\/\*\*)\s*|\{)@[a-z][a-zA-Z-]+\b/m, lookbehind:!0}, punctuation:/[{}]/};
  Object.defineProperty(r, "addSupport", {value:function(a, c) {
    (a = typeof a == "string" ? [a] : a).forEach(function(d) {
      var h = function(g) {
        g.inside || (g.inside = {}), g.inside.rest = c;
      };
      var x = "doc-comment";
      if (w = e.languages[d]) {
        var w;
        var y = w[x];
        if ((y = y || (w = e.languages.insertBefore(d, "comment", {"doc-comment":{pattern:/(^|[^\\])\/\*\*[^/][\s\S]*?(?:\*\/|$)/, lookbehind:!0, alias:"comment"}}))[x]) instanceof RegExp && (y = w[x] = {pattern:y}), Array.isArray(y)) {
          var b = 0;
          for (var k = y.length; b < k; b++) {
            y[b] instanceof RegExp && (y[b] = {pattern:y[b]}), h(y[b]);
          }
        } else {
          h(y);
        }
      }
    });
  }}), r.addSupport(["java", "javascript", "php"], r);
}(l$$module$dist$bridge$knoxx_frontend_bridge_es), function(e) {
  var r = /(?:"(?:\\(?:\r\n|[\s\S])|[^"\\\r\n])*"|'(?:\\(?:\r\n|[\s\S])|[^'\\\r\n])*')/;
  r = (e.languages.css = {comment:/\/\*[\s\S]*?\*\//, atrule:{pattern:RegExp("@[\\w-](?:" + /[^;{\s"']|\s+(?!\s)/.source + "|" + r.source + ")*?" + /(?:;|(?=\s*\{))/.source), inside:{rule:/^@[\w-]+/, "selector-function-argument":{pattern:/(\bselector\s*\(\s*(?![\s)]))(?:[^()\s]|\s+(?![\s)])|\((?:[^()]|\([^()]*\))*\))+(?=\s*\))/, lookbehind:!0, alias:"selector"}, keyword:{pattern:/(^|[^\w-])(?:and|not|only|or)(?![\w-])/, lookbehind:!0}}}, url:{pattern:RegExp("\\burl\\((?:" + r.source + "|" + /(?:[^\\\r\n()"']|\\[\s\S])*/.source + 
  ")\\)", "i"), greedy:!0, inside:{function:/^url/i, punctuation:/^\(|\)$/, string:{pattern:RegExp("^" + r.source + "$"), alias:"url"}}}, selector:{pattern:RegExp(`(^|[{}\\s])[^{}\\s](?:[^{};"'\\s]|\\s+(?![\\s{])|` + r.source + ")*(?\x3d\\s*\\{)"), lookbehind:!0}, string:{pattern:r, greedy:!0}, property:{pattern:/(^|[^-\w\xA0-\uFFFF])(?!\s)[-_a-z\xA0-\uFFFF](?:(?!\s)[-\w\xA0-\uFFFF])*(?=\s*:)/i, lookbehind:!0}, important:/!important\b/i, function:{pattern:/(^|[^-a-z0-9])[-a-z0-9]+(?=\()/i, lookbehind:!0}, 
  punctuation:/[(){};:,]/}, e.languages.css.atrule.inside.rest = e.languages.css, e.languages.markup);
  r && (r.tag.addInlined("style", "css"), r.tag.addAttribute("style", "css"));
}(l$$module$dist$bridge$knoxx_frontend_bridge_es), function(e) {
  var r = /("|')(?:\\(?:\r\n|[\s\S])|(?!\1)[^\\\r\n])*\1/;
  r = (e.languages.css.selector = {pattern:e.languages.css.selector.pattern, lookbehind:!0, inside:r = {"pseudo-element":/:(?:after|before|first-letter|first-line|selection)|::[-\w]+/, "pseudo-class":/:[-\w]+/, class:/\.[-\w]+/, id:/#[-\w]+/, attribute:{pattern:RegExp(`\\[(?:[^[\\]"']|` + r.source + ")*\\]"), greedy:!0, inside:{punctuation:/^\[|\]$/, "case-sensitivity":{pattern:/(\s)[si]$/i, lookbehind:!0, alias:"keyword"}, namespace:{pattern:/^(\s*)(?:(?!\s)[-*\w\xA0-\uFFFF])*\|(?!=)/, lookbehind:!0, 
  inside:{punctuation:/\|$/}}, "attr-name":{pattern:/^(\s*)(?:(?!\s)[-\w\xA0-\uFFFF])+/, lookbehind:!0}, "attr-value":[r, {pattern:/(=\s*)(?:(?!\s)[-\w\xA0-\uFFFF])+(?=\s*$)/, lookbehind:!0}], operator:/[|~*^$]?=/}}, "n-th":[{pattern:/(\(\s*)[+-]?\d*[\dn](?:\s*[+-]\s*\d+)?(?=\s*\))/, lookbehind:!0, inside:{number:/[\dn]+/, operator:/[+-]/}}, {pattern:/(\(\s*)(?:even|odd)(?=\s*\))/i, lookbehind:!0}], combinator:/>|\+|~|\|\|/, punctuation:/[(),]/}}, e.languages.css.atrule.inside["selector-function-argument"].inside = 
  r, e.languages.insertBefore("css", "property", {variable:{pattern:/(^|[^-\w\xA0-\uFFFF])--(?!\s)[-_a-z\xA0-\uFFFF](?:(?!\s)[-\w\xA0-\uFFFF])*/i, lookbehind:!0}}), {pattern:/(\b\d+)(?:%|[a-z]+(?![\w-]))/, lookbehind:!0});
  var a = {pattern:/(^|[^\w.-])-?(?:\d+(?:\.\d+)?|\.\d+)/, lookbehind:!0};
  e.languages.insertBefore("css", "function", {operator:{pattern:/(\s)[+\-*\/](?=\s)/, lookbehind:!0}, hexcode:{pattern:/\B#[\da-f]{3,8}\b/i, alias:"color"}, color:[{pattern:/(^|[^\w-])(?:AliceBlue|AntiqueWhite|Aqua|Aquamarine|Azure|Beige|Bisque|Black|BlanchedAlmond|Blue|BlueViolet|Brown|BurlyWood|CadetBlue|Chartreuse|Chocolate|Coral|CornflowerBlue|Cornsilk|Crimson|Cyan|DarkBlue|DarkCyan|DarkGoldenRod|DarkGr[ae]y|DarkGreen|DarkKhaki|DarkMagenta|DarkOliveGreen|DarkOrange|DarkOrchid|DarkRed|DarkSalmon|DarkSeaGreen|DarkSlateBlue|DarkSlateGr[ae]y|DarkTurquoise|DarkViolet|DeepPink|DeepSkyBlue|DimGr[ae]y|DodgerBlue|FireBrick|FloralWhite|ForestGreen|Fuchsia|Gainsboro|GhostWhite|Gold|GoldenRod|Gr[ae]y|Green|GreenYellow|HoneyDew|HotPink|IndianRed|Indigo|Ivory|Khaki|Lavender|LavenderBlush|LawnGreen|LemonChiffon|LightBlue|LightCoral|LightCyan|LightGoldenRodYellow|LightGr[ae]y|LightGreen|LightPink|LightSalmon|LightSeaGreen|LightSkyBlue|LightSlateGr[ae]y|LightSteelBlue|LightYellow|Lime|LimeGreen|Linen|Magenta|Maroon|MediumAquaMarine|MediumBlue|MediumOrchid|MediumPurple|MediumSeaGreen|MediumSlateBlue|MediumSpringGreen|MediumTurquoise|MediumVioletRed|MidnightBlue|MintCream|MistyRose|Moccasin|NavajoWhite|Navy|OldLace|Olive|OliveDrab|Orange|OrangeRed|Orchid|PaleGoldenRod|PaleGreen|PaleTurquoise|PaleVioletRed|PapayaWhip|PeachPuff|Peru|Pink|Plum|PowderBlue|Purple|RebeccaPurple|Red|RosyBrown|RoyalBlue|SaddleBrown|Salmon|SandyBrown|SeaGreen|SeaShell|Sienna|Silver|SkyBlue|SlateBlue|SlateGr[ae]y|Snow|SpringGreen|SteelBlue|Tan|Teal|Thistle|Tomato|Transparent|Turquoise|Violet|Wheat|White|WhiteSmoke|Yellow|YellowGreen)(?![\w-])/i, 
  lookbehind:!0}, {pattern:/\b(?:hsl|rgb)\(\s*\d{1,3}\s*,\s*\d{1,3}%?\s*,\s*\d{1,3}%?\s*\)\B|\b(?:hsl|rgb)a\(\s*\d{1,3}\s*,\s*\d{1,3}%?\s*,\s*\d{1,3}%?\s*,\s*(?:0|0?\.\d+|1)\s*\)\B/i, inside:{unit:r, number:a, function:/[\w-]+(?=\()/, punctuation:/[(),]/}}], entity:/\\[\da-f]{1,8}/i, unit:r, number:a});
}(l$$module$dist$bridge$knoxx_frontend_bridge_es), function(e) {
  function x(w, y) {
    y = (y || "").replace(/m/g, "") + "m";
    var b = /([:\-,[{]\s*(?:\s<<prop>>[ \t]+)?)(?:<<value>>)(?=[ \t]*(?:$|,|\]|\}|(?:[\r\n]\s*)?#))/.source.replace(/<<prop>>/g, function() {
      return c;
    }).replace(/<<value>>/g, function() {
      return w;
    });
    return RegExp(b, y);
  }
  var r = /[*&][^\s[\]{},]+/;
  var a = /!(?:<[\w\-%#;/?:@&=+$,.!~*'()[\]]+>|(?:[a-zA-Z\d-]*!)?[\w\-%#;/?:@&=+$.~*'()]+)?/;
  var c = "(?:" + a.source + "(?:[ \t]+" + r.source + ")?|" + r.source + "(?:[ \t]+" + a.source + ")?)";
  var d = /(?:[^\s\x00-\x08\x0e-\x1f!"#%&'*,\-:>?@[\]`{|}\x7f-\x84\x86-\x9f\ud800-\udfff\ufffe\uffff]|[?:-]<PLAIN>)(?:[ \t]*(?:(?![#:])<PLAIN>|:<PLAIN>))*/.source.replace(/<PLAIN>/g, function() {
    return /[^\s\x00-\x08\x0e-\x1f,[\]{}\x7f-\x84\x86-\x9f\ud800-\udfff\ufffe\uffff]/.source;
  });
  var h = /"(?:[^"\\\r\n]|\\.)*"|'(?:[^'\\\r\n]|\\.)*'/.source;
  e.languages.yaml = {scalar:{pattern:RegExp(/([\-:]\s*(?:\s<<prop>>[ \t]+)?[|>])[ \t]*(?:((?:\r?\n|\r)[ \t]+)\S[^\r\n]*(?:\2[^\r\n]+)*)/.source.replace(/<<prop>>/g, function() {
    return c;
  })), lookbehind:!0, alias:"string"}, comment:/#.*/, key:{pattern:RegExp(/((?:^|[:\-,[{\r\n?])[ \t]*(?:<<prop>>[ \t]+)?)<<key>>(?=\s*:\s)/.source.replace(/<<prop>>/g, function() {
    return c;
  }).replace(/<<key>>/g, function() {
    return "(?:" + d + "|" + h + ")";
  })), lookbehind:!0, greedy:!0, alias:"atrule"}, directive:{pattern:/(^[ \t]*)%.+/m, lookbehind:!0, alias:"important"}, datetime:{pattern:x(/\d{4}-\d\d?-\d\d?(?:[tT]|[ \t]+)\d\d?:\d{2}:\d{2}(?:\.\d*)?(?:[ \t]*(?:Z|[-+]\d\d?(?::\d{2})?))?|\d{4}-\d{2}-\d{2}|\d\d?:\d{2}(?::\d{2}(?:\.\d*)?)?/.source), lookbehind:!0, alias:"number"}, boolean:{pattern:x(/false|true/.source, "i"), lookbehind:!0, alias:"important"}, null:{pattern:x(/null|~/.source, "i"), lookbehind:!0, alias:"important"}, string:{pattern:x(h), 
  lookbehind:!0, greedy:!0}, number:{pattern:x(/[+-]?(?:0x[\da-f]+|0o[0-7]+|(?:\d+(?:\.\d*)?|\.\d+)(?:e[+-]?\d+)?|\.inf|\.nan)/.source, "i"), lookbehind:!0}, tag:a, important:r, punctuation:/---|[:[\]{}\-,|>?]|\.\.\./}, e.languages.yml = e.languages.yaml;
}(l$$module$dist$bridge$knoxx_frontend_bridge_es), function(e) {
  function a(b) {
    return b = b.replace(/<inner>/g, function() {
      return r;
    }), RegExp(/((?:^|[^\\])(?:\\{2})*)/.source + "(?:" + b + ")");
  }
  var r = /(?:\\.|[^\\\n\r]|(?:\n|\r\n?)(?![\r\n]))/.source;
  var c = /(?:\\.|``(?:[^`\r\n]|`(?!`))+``|`[^`\r\n]+`|[^\\|\r\n`])+/.source;
  var d = /\|?__(?:\|__)+\|?(?:(?:\n|\r\n?)|(?![\s\S]))/.source.replace(/__/g, function() {
    return c;
  });
  var h = /\|?[ \t]*:?-{3,}:?[ \t]*(?:\|[ \t]*:?-{3,}:?[ \t]*)+\|?(?:\n|\r\n?)/.source;
  var x = (e.languages.markdown = e.languages.extend("markup", {}), e.languages.insertBefore("markdown", "prolog", {"front-matter-block":{pattern:/(^(?:\s*[\r\n])?)---(?!.)[\s\S]*?[\r\n]---(?!.)/, lookbehind:!0, greedy:!0, inside:{punctuation:/^---|---$/, "front-matter":{pattern:/\S+(?:\s+\S+)*/, alias:["yaml", "language-yaml"], inside:e.languages.yaml}}}, blockquote:{pattern:/^>(?:[\t ]*>)*/m, alias:"punctuation"}, table:{pattern:RegExp("^" + d + h + "(?:" + d + ")*", "m"), inside:{"table-data-rows":{pattern:RegExp("^(" + 
  d + h + ")(?:" + d + ")*$"), lookbehind:!0, inside:{"table-data":{pattern:RegExp(c), inside:e.languages.markdown}, punctuation:/\|/}}, "table-line":{pattern:RegExp("^(" + d + ")" + h + "$"), lookbehind:!0, inside:{punctuation:/\||:?-{3,}:?/}}, "table-header-row":{pattern:RegExp("^" + d + "$"), inside:{"table-header":{pattern:RegExp(c), alias:"important", inside:e.languages.markdown}, punctuation:/\|/}}}}, code:[{pattern:/((?:^|\n)[ \t]*\n|(?:^|\r\n?)[ \t]*\r\n?)(?: {4}|\t).+(?:(?:\n|\r\n?)(?: {4}|\t).+)*/, 
  lookbehind:!0, alias:"keyword"}, {pattern:/^```[\s\S]*?^```$/m, greedy:!0, inside:{"code-block":{pattern:/^(```.*(?:\n|\r\n?))[\s\S]+?(?=(?:\n|\r\n?)^```$)/m, lookbehind:!0}, "code-language":{pattern:/^(```).+/, lookbehind:!0}, punctuation:/```/}}], title:[{pattern:/\S.*(?:\n|\r\n?)(?:==+|--+)(?=[ \t]*$)/m, alias:"important", inside:{punctuation:/==+$|--+$/}}, {pattern:/(^\s*)#.+/m, lookbehind:!0, alias:"important", inside:{punctuation:/^#+|#+$/}}], hr:{pattern:/(^\s*)([*-])(?:[\t ]*\2){2,}(?=\s*$)/m, 
  lookbehind:!0, alias:"punctuation"}, list:{pattern:/(^\s*)(?:[*+-]|\d+\.)(?=[\t ].)/m, lookbehind:!0, alias:"punctuation"}, "url-reference":{pattern:/!?\[[^\]]+\]:[\t ]+(?:\S+|<(?:\\.|[^>\\])+>)(?:[\t ]+(?:"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|\((?:\\.|[^)\\])*\)))?/, inside:{variable:{pattern:/^(!?\[)[^\]]+/, lookbehind:!0}, string:/(?:"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|\((?:\\.|[^)\\])*\))$/, punctuation:/^[\[\]!:]|[<>]/}, alias:"url"}, bold:{pattern:a(/\b__(?:(?!_)<inner>|_(?:(?!_)<inner>)+_)+__\b|\*\*(?:(?!\*)<inner>|\*(?:(?!\*)<inner>)+\*)+\*\*/.source), 
  lookbehind:!0, greedy:!0, inside:{content:{pattern:/(^..)[\s\S]+(?=..$)/, lookbehind:!0, inside:{}}, punctuation:/\*\*|__/}}, italic:{pattern:a(/\b_(?:(?!_)<inner>|__(?:(?!_)<inner>)+__)+_\b|\*(?:(?!\*)<inner>|\*\*(?:(?!\*)<inner>)+\*\*)+\*/.source), lookbehind:!0, greedy:!0, inside:{content:{pattern:/(^.)[\s\S]+(?=.$)/, lookbehind:!0, inside:{}}, punctuation:/[*_]/}}, strike:{pattern:a(/(~~?)(?:(?!~)<inner>)+\2/.source), lookbehind:!0, greedy:!0, inside:{content:{pattern:/(^~~?)[\s\S]+(?=\1$)/, 
  lookbehind:!0, inside:{}}, punctuation:/~~?/}}, "code-snippet":{pattern:/(^|[^\\`])(?:``[^`\r\n]+(?:`[^`\r\n]+)*``(?!`)|`[^`\r\n]+`(?!`))/, lookbehind:!0, greedy:!0, alias:["code", "keyword"]}, url:{pattern:a(/!?\[(?:(?!\])<inner>)+\](?:\([^\s)]+(?:[\t ]+"(?:\\.|[^"\\])*")?\)|[ \t]?\[(?:(?!\])<inner>)+\])/.source), lookbehind:!0, greedy:!0, inside:{operator:/^!/, content:{pattern:/(^\[)[^\]]+(?=\])/, lookbehind:!0, inside:{}}, variable:{pattern:/(^\][ \t]?\[)[^\]]+(?=\]$)/, lookbehind:!0}, url:{pattern:/(^\]\()[^\s)]+/, 
  lookbehind:!0}, string:{pattern:/(^[ \t]+)"(?:\\.|[^"\\])*"(?=\)$)/, lookbehind:!0}}}}), ["url", "bold", "italic", "strike"].forEach(function(b) {
    ["url", "bold", "italic", "strike", "code-snippet"].forEach(function(k) {
      b !== k && (e.languages.markdown[b].inside.content.inside[k] = e.languages.markdown[k]);
    });
  }), e.hooks.add("after-tokenize", function(b) {
    b.language !== "markdown" && b.language !== "md" || function k(g) {
      if (g && typeof g != "string") {
        var S = 0;
        for (var E = g.length; S < E; S++) {
          var o;
          var s = g[S];
          s.type !== "code" ? k(s.content) : (o = s.content[1], s = s.content[3], o && s && o.type === "code-language" && s.type === "code-block" && typeof o.content == "string" && (o = o.content.replace(/\b#/g, "sharp").replace(/\b\+\+/g, "pp"), o = "language-" + (o = (/[a-z][\w-]*/i.exec(o) || [""])[0].toLowerCase()), s.alias ? typeof s.alias == "string" ? s.alias = [s.alias, o] : s.alias.push(o) : s.alias = [o]));
        }
      }
    }(b.tokens);
  }), e.hooks.add("wrap", function(b) {
    if (b.type === "code-block") {
      var k = "";
      var g = 0;
      for (var S = b.classes.length; g < S; g++) {
        var E = b.classes[g];
        E = /language-(.+)/.exec(E);
        if (E) {
          k = E[1];
          break;
        }
      }
      var o;
      var s = e.languages[k];
      s ? b.content = e.highlight(function(i) {
        return i = i.replace(x, ""), i = i.replace(/&(\w{1,8}|#x?[\da-f]{1,8});/gi, function(f, u) {
          var m;
          return (u = u.toLowerCase())[0] === "#" ? (m = u[1] === "x" ? parseInt(u.slice(2), 16) : Number(u.slice(1)), y(m)) : w[u] || f;
        });
      }(b.content), s, k) : k && k !== "none" && e.plugins.autoloader && (o = "md-" + (new Date()).valueOf() + "-" + Math.floor(1e16 * Math.random()), b.attributes.id = o, e.plugins.autoloader.loadLanguages(k, function() {
        var i = document.getElementById(o);
        i && (i.innerHTML = e.highlight(i.textContent, e.languages[k], k));
      }));
    }
  }), RegExp(e.languages.markup.tag.pattern.source, "gi"));
  var w = {amp:"\x26", lt:"\x3c", gt:"\x3e", quot:'"'};
  var y = String.fromCodePoint || String.fromCharCode;
  e.languages.md = e.languages.markdown;
}(l$$module$dist$bridge$knoxx_frontend_bridge_es), l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.graphql = {comment:/#.*/, description:{pattern:/(?:"""(?:[^"]|(?!""")")*"""|"(?:\\.|[^\\"\r\n])*")(?=\s*[a-z_])/i, greedy:!0, alias:"string", inside:{"language-markdown":{pattern:/(^"(?:"")?)(?!\1)[\s\S]+(?=\1$)/, lookbehind:!0, inside:l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.markdown}}}, string:{pattern:/"""(?:[^"]|(?!""")")*"""|"(?:\\.|[^\\"\r\n])*"/, greedy:!0}, number:/(?:\B-|\b)\d+(?:\.\d+)?(?:e[+-]?\d+)?\b/i, 
boolean:/\b(?:false|true)\b/, variable:/\$[a-z_]\w*/i, directive:{pattern:/@[a-z_]\w*/i, alias:"function"}, "attr-name":{pattern:/\b[a-z_]\w*(?=\s*(?:\((?:[^()"]|"(?:\\.|[^\\"\r\n])*")*\))?:)/i, greedy:!0}, "atom-input":{pattern:/\b[A-Z]\w*Input\b/, alias:"class-name"}, scalar:/\b(?:Boolean|Float|ID|Int|String)\b/, constant:/\b[A-Z][A-Z_\d]*\b/, "class-name":{pattern:/(\b(?:enum|implements|interface|on|scalar|type|union)\s+|&\s*|:\s*|\[)[A-Z_]\w*/, lookbehind:!0}, fragment:{pattern:/(\bfragment\s+|\.{3}\s*(?!on\b))[a-zA-Z_]\w*/, 
lookbehind:!0, alias:"function"}, "definition-mutation":{pattern:/(\bmutation\s+)[a-zA-Z_]\w*/, lookbehind:!0, alias:"function"}, "definition-query":{pattern:/(\bquery\s+)[a-zA-Z_]\w*/, lookbehind:!0, alias:"function"}, keyword:/\b(?:directive|enum|extend|fragment|implements|input|interface|mutation|on|query|repeatable|scalar|schema|subscription|type|union)\b/, operator:/[!=|&]|\.{3}/, "property-query":/\w+(?=\s*\()/, object:/\w+(?=\s*\{)/, punctuation:/[!(){}\[\]:=,]/, property:/\w+/}, l$$module$dist$bridge$knoxx_frontend_bridge_es.hooks.add("after-tokenize", 
function(e) {
  function k(o) {
    return r[a + o];
  }
  function g(o, s) {
    s = s || 0;
    for (var i = 0; i < o.length; i++) {
      var f = k(i + s);
      if (!f || f.type !== o[i]) {
        return;
      }
    }
    return 1;
  }
  function S(o, s) {
    var i = 1;
    for (var f = a; f < r.length; f++) {
      var u = r[f];
      var m = u.content;
      if (u.type === "punctuation" && typeof m == "string") {
        if (o.test(m)) {
          i++;
        } else if (s.test(m) && --i === 0) {
          return f;
        }
      }
    }
    return -1;
  }
  function E(o, s) {
    var i = o.alias;
    i ? Array.isArray(i) || (o.alias = i = [i]) : o.alias = i = [], i.push(s);
  }
  if (e.language === "graphql") {
    var r = e.tokens.filter(function(o) {
      return typeof o != "string" && o.type !== "comment" && o.type !== "scalar";
    });
    for (var a = 0; a < r.length;) {
      var c = r[a++];
      if (c.type === "keyword" && c.content === "mutation") {
        var d = [];
        if (g(["definition-mutation", "punctuation"]) && k(1).content === "(") {
          a += 2;
          var h = S(/^\($/, /^\)$/);
          if (h === -1) {
            continue;
          }
          for (; a < h; a++) {
            var x = k(0);
            x.type === "variable" && (E(x, "variable-input"), d.push(x.content));
          }
          a = h + 1;
        }
        if (g(["punctuation", "property-query"]) && k(0).content === "{" && (a++, E(k(0), "property-mutation"), 0 < d.length)) {
          var w = S(/^\{$/, /^\}$/);
          if (w !== -1) {
            for (var y = a; y < w; y++) {
              var b = r[y];
              b.type === "variable" && 0 <= d.indexOf(b.content) && E(b, "variable-input");
            }
          }
        }
      }
    }
  }
}), l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.sql = {comment:{pattern:/(^|[^\\])(?:\/\*[\s\S]*?\*\/|(?:--|\/\/|#).*)/, lookbehind:!0}, variable:[{pattern:/@(["'`])(?:\\[\s\S]|(?!\1)[^\\])+\1/, greedy:!0}, /@[\w.$]+/], string:{pattern:/(^|[^@\\])("|')(?:\\[\s\S]|(?!\2)[^\\]|\2\2)*\2/, greedy:!0, lookbehind:!0}, identifier:{pattern:/(^|[^@\\])`(?:\\[\s\S]|[^`\\]|``)*`/, greedy:!0, lookbehind:!0, inside:{punctuation:/^`|`$/}}, function:/\b(?:AVG|COUNT|FIRST|FORMAT|LAST|LCASE|LEN|MAX|MID|MIN|MOD|NOW|ROUND|SUM|UCASE)(?=\s*\()/i, 
keyword:/\b(?:ACTION|ADD|AFTER|ALGORITHM|ALL|ALTER|ANALYZE|ANY|APPLY|AS|ASC|AUTHORIZATION|AUTO_INCREMENT|BACKUP|BDB|BEGIN|BERKELEYDB|BIGINT|BINARY|BIT|BLOB|BOOL|BOOLEAN|BREAK|BROWSE|BTREE|BULK|BY|CALL|CASCADED?|CASE|CHAIN|CHAR(?:ACTER|SET)?|CHECK(?:POINT)?|CLOSE|CLUSTERED|COALESCE|COLLATE|COLUMNS?|COMMENT|COMMIT(?:TED)?|COMPUTE|CONNECT|CONSISTENT|CONSTRAINT|CONTAINS(?:TABLE)?|CONTINUE|CONVERT|CREATE|CROSS|CURRENT(?:_DATE|_TIME|_TIMESTAMP|_USER)?|CURSOR|CYCLE|DATA(?:BASES?)?|DATE(?:TIME)?|DAY|DBCC|DEALLOCATE|DEC|DECIMAL|DECLARE|DEFAULT|DEFINER|DELAYED|DELETE|DELIMITERS?|DENY|DESC|DESCRIBE|DETERMINISTIC|DISABLE|DISCARD|DISK|DISTINCT|DISTINCTROW|DISTRIBUTED|DO|DOUBLE|DROP|DUMMY|DUMP(?:FILE)?|DUPLICATE|ELSE(?:IF)?|ENABLE|ENCLOSED|END|ENGINE|ENUM|ERRLVL|ERRORS|ESCAPED?|EXCEPT|EXEC(?:UTE)?|EXISTS|EXIT|EXPLAIN|EXTENDED|FETCH|FIELDS|FILE|FILLFACTOR|FIRST|FIXED|FLOAT|FOLLOWING|FOR(?: EACH ROW)?|FORCE|FOREIGN|FREETEXT(?:TABLE)?|FROM|FULL|FUNCTION|GEOMETRY(?:COLLECTION)?|GLOBAL|GOTO|GRANT|GROUP|HANDLER|HASH|HAVING|HOLDLOCK|HOUR|IDENTITY(?:COL|_INSERT)?|IF|IGNORE|IMPORT|INDEX|INFILE|INNER|INNODB|INOUT|INSERT|INT|INTEGER|INTERSECT|INTERVAL|INTO|INVOKER|ISOLATION|ITERATE|JOIN|KEYS?|KILL|LANGUAGE|LAST|LEAVE|LEFT|LEVEL|LIMIT|LINENO|LINES|LINESTRING|LOAD|LOCAL|LOCK|LONG(?:BLOB|TEXT)|LOOP|MATCH(?:ED)?|MEDIUM(?:BLOB|INT|TEXT)|MERGE|MIDDLEINT|MINUTE|MODE|MODIFIES|MODIFY|MONTH|MULTI(?:LINESTRING|POINT|POLYGON)|NATIONAL|NATURAL|NCHAR|NEXT|NO|NONCLUSTERED|NULLIF|NUMERIC|OFF?|OFFSETS?|ON|OPEN(?:DATASOURCE|QUERY|ROWSET)?|OPTIMIZE|OPTION(?:ALLY)?|ORDER|OUT(?:ER|FILE)?|OVER|PARTIAL|PARTITION|PERCENT|PIVOT|PLAN|POINT|POLYGON|PRECEDING|PRECISION|PREPARE|PREV|PRIMARY|PRINT|PRIVILEGES|PROC(?:EDURE)?|PUBLIC|PURGE|QUICK|RAISERROR|READS?|REAL|RECONFIGURE|REFERENCES|RELEASE|RENAME|REPEAT(?:ABLE)?|REPLACE|REPLICATION|REQUIRE|RESIGNAL|RESTORE|RESTRICT|RETURN(?:ING|S)?|REVOKE|RIGHT|ROLLBACK|ROUTINE|ROW(?:COUNT|GUIDCOL|S)?|RTREE|RULE|SAVE(?:POINT)?|SCHEMA|SECOND|SELECT|SERIAL(?:IZABLE)?|SESSION(?:_USER)?|SET(?:USER)?|SHARE|SHOW|SHUTDOWN|SIMPLE|SMALLINT|SNAPSHOT|SOME|SONAME|SQL|START(?:ING)?|STATISTICS|STATUS|STRIPED|SYSTEM_USER|TABLES?|TABLESPACE|TEMP(?:ORARY|TABLE)?|TERMINATED|TEXT(?:SIZE)?|THEN|TIME(?:STAMP)?|TINY(?:BLOB|INT|TEXT)|TOP?|TRAN(?:SACTIONS?)?|TRIGGER|TRUNCATE|TSEQUAL|TYPES?|UNBOUNDED|UNCOMMITTED|UNDEFINED|UNION|UNIQUE|UNLOCK|UNPIVOT|UNSIGNED|UPDATE(?:TEXT)?|USAGE|USE|USER|USING|VALUES?|VAR(?:BINARY|CHAR|CHARACTER|YING)|VIEW|WAITFOR|WARNINGS|WHEN|WHERE|WHILE|WITH(?: ROLLUP|IN)?|WORK|WRITE(?:TEXT)?|YEAR)\b/i, 
boolean:/\b(?:FALSE|NULL|TRUE)\b/i, number:/\b0x[\da-f]+\b|\b\d+(?:\.\d*)?|\B\.\d+\b/i, operator:/[-+*\/=%^~]|&&?|\|\|?|!=?|<(?:=>?|<|>)?|>[>=]?|\b(?:AND|BETWEEN|DIV|ILIKE|IN|IS|LIKE|NOT|OR|REGEXP|RLIKE|SOUNDS LIKE|XOR)\b/i, punctuation:/[;[\]()`,.]/}, function(e) {
  function x(g, S) {
    if (e.languages[g]) {
      return {pattern:RegExp("((?:" + S + ")\\s*)" + a), lookbehind:!0, greedy:!0, inside:{"template-punctuation":{pattern:/^`|`$/, alias:"string"}, "embedded-code":{pattern:/[\s\S]+/, alias:g}}};
    }
  }
  function w(g, S, E) {
    return g = {code:g, grammar:S, language:E}, e.hooks.run("before-tokenize", g), g.tokens = e.tokenize(g.code, g.grammar), e.hooks.run("after-tokenize", g), g.tokens;
  }
  function y(g, S, E) {
    var i = e.tokenize(g, {interpolation:{pattern:RegExp(h), lookbehind:!0}});
    var o = 0;
    var s = {};
    i = w(i.map(function(u) {
      if (typeof u == "string") {
        return u;
      }
      var m;
      var F;
      for (u = u.content; g.indexOf((F = o++, m = "___" + E.toUpperCase() + "_" + F + "___")) !== -1;) {
      }
      return s[m] = u, m;
    }).join(""), S, E);
    var f = Object.keys(s);
    return o = 0, function u(m) {
      for (var F = 0; F < m.length; F++) {
        if (o >= f.length) {
          return;
        }
        var v;
        var C;
        var _;
        var $;
        var z;
        var M;
        var j;
        var N = m[F];
        typeof N == "string" || typeof N.content == "string" ? (v = f[o], (j = (M = typeof N == "string" ? N : N.content).indexOf(v)) !== -1 && (++o, C = M.substring(0, j), z = s[v], _ = void 0, ($ = {})["interpolation-punctuation"] = d, ($ = e.tokenize(z, $)).length === 3 && ((_ = [1, 1]).push.apply(_, w($[1], e.languages.javascript, "javascript")), $.splice.apply($, _)), _ = new e.Token("interpolation", $, c.alias, z), $ = M.substring(j + v.length), z = [], C && z.push(C), z.push(_), $ && (u(M = 
        [$]), z.push.apply(z, M)), typeof N == "string" ? (m.splice.apply(m, [F, 1].concat(z)), F += z.length - 1) : N.content = z)) : (j = N.content, Array.isArray(j) ? u(j) : u([j]));
      }
    }(i), new e.Token(E, i, "language-" + E, g);
  }
  function k(g) {
    return typeof g == "string" ? g : Array.isArray(g) ? g.map(k).join("") : k(g.content);
  }
  var r = e.languages.javascript["template-string"];
  var a = r.pattern.source;
  var c = r.inside.interpolation;
  var d = c.inside["interpolation-punctuation"];
  var h = c.pattern.source;
  e.languages.javascript["template-string"] = [x("css", /\b(?:styled(?:\([^)]*\))?(?:\s*\.\s*\w+(?:\([^)]*\))*)*|css(?:\s*\.\s*(?:global|resolve))?|createGlobalStyle|keyframes)/.source), x("html", /\bhtml|\.\s*(?:inner|outer)HTML\s*\+?=/.source), x("svg", /\bsvg/.source), x("markdown", /\b(?:markdown|md)/.source), x("graphql", /\b(?:gql|graphql(?:\s*\.\s*experimental)?)/.source), x("sql", /\bsql/.source), r].filter(Boolean);
  var b = {javascript:!0, js:!0, typescript:!0, ts:!0, jsx:!0, tsx:!0};
  e.hooks.add("after-tokenize", function(g) {
    g.language in b && function S(E) {
      var o = 0;
      for (var s = E.length; o < s; o++) {
        var i;
        var f;
        var u;
        var m = E[o];
        typeof m != "string" && (i = m.content, Array.isArray(i) ? m.type === "template-string" ? (m = i[1], i.length === 3 && typeof m != "string" && m.type === "embedded-code" && (f = k(m), m = m.alias, m = Array.isArray(m) ? m[0] : m, u = e.languages[m]) && (i[1] = y(f, u, m))) : S(i) : typeof i != "string" && S([i]));
      }
    }(g.tokens);
  });
}(l$$module$dist$bridge$knoxx_frontend_bridge_es), function(e) {
  e.languages.typescript = e.languages.extend("javascript", {"class-name":{pattern:/(\b(?:class|extends|implements|instanceof|interface|new|type)\s+)(?!keyof\b)(?!\s)[_$a-zA-Z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*(?:\s*<(?:[^<>]|<(?:[^<>]|<[^<>]*>)*>)*>)?/, lookbehind:!0, greedy:!0, inside:null}, builtin:/\b(?:Array|Function|Promise|any|boolean|console|never|number|string|symbol|unknown)\b/}), e.languages.typescript.keyword.push(/\b(?:abstract|declare|is|keyof|readonly|require)\b/, /\b(?:asserts|infer|interface|module|namespace|type)\b(?=\s*(?:[{_$a-zA-Z\xA0-\uFFFF]|$))/, 
  /\btype\b(?=\s*(?:[\{*]|$))/), delete e.languages.typescript.parameter, delete e.languages.typescript["literal-property"];
  var r = e.languages.extend("typescript", {});
  delete r["class-name"], e.languages.typescript["class-name"].inside = r, e.languages.insertBefore("typescript", "function", {decorator:{pattern:/@[$\w\xA0-\uFFFF]+/, inside:{at:{pattern:/^@/, alias:"operator"}, function:/^[\s\S]+/}}, "generic-function":{pattern:/#?(?!\s)[_$a-zA-Z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*\s*<(?:[^<>]|<(?:[^<>]|<[^<>]*>)*>)*>(?=\s*\()/, greedy:!0, inside:{function:/^#?(?!\s)[_$a-zA-Z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*/, generic:{pattern:/<[\s\S]+/, alias:"class-name", 
  inside:r}}}}), e.languages.ts = e.languages.typescript;
}(l$$module$dist$bridge$knoxx_frontend_bridge_es), function(e) {
  var r = e.languages.javascript;
  var a = /\{(?:[^{}]|\{(?:[^{}]|\{[^{}]*\})*\})+\}/.source;
  var c = "(@(?:arg|argument|param|property)\\s+(?:" + a + "\\s+)?)";
  e.languages.jsdoc = e.languages.extend("javadoclike", {parameter:{pattern:RegExp(c + /(?:(?!\s)[$\w\xA0-\uFFFF.])+(?=\s|$)/.source), lookbehind:!0, inside:{punctuation:/\./}}}), e.languages.insertBefore("jsdoc", "keyword", {"optional-parameter":{pattern:RegExp(c + /\[(?:(?!\s)[$\w\xA0-\uFFFF.])+(?:=[^[\]]+)?\](?=\s|$)/.source), lookbehind:!0, inside:{parameter:{pattern:/(^\[)[$\w\xA0-\uFFFF\.]+/, lookbehind:!0, inside:{punctuation:/\./}}, code:{pattern:/(=)[\s\S]*(?=\]$)/, lookbehind:!0, inside:r, 
  alias:"language-javascript"}, punctuation:/[=[\]]/}}, "class-name":[{pattern:RegExp(/(@(?:augments|class|extends|interface|memberof!?|template|this|typedef)\s+(?:<TYPE>\s+)?)[A-Z]\w*(?:\.[A-Z]\w*)*/.source.replace(/<TYPE>/g, function() {
    return a;
  })), lookbehind:!0, inside:{punctuation:/\./}}, {pattern:RegExp("(@[a-z]+\\s+)" + a), lookbehind:!0, inside:{string:r.string, number:r.number, boolean:r.boolean, keyword:e.languages.typescript.keyword, operator:/=>|\.\.\.|[&|?:*]/, punctuation:/[.,;=<>{}()[\]]/}}], example:{pattern:/(@example\s+(?!\s))(?:[^@\s]|\s+(?!\s))+?(?=\s*(?:\*\s*)?(?:@\w|\*\/))/, lookbehind:!0, inside:{code:{pattern:/^([\t ]*(?:\*\s*)?)\S.*$/m, lookbehind:!0, inside:r, alias:"language-javascript"}}}}), e.languages.javadoclike.addSupport("javascript", 
  e.languages.jsdoc);
}(l$$module$dist$bridge$knoxx_frontend_bridge_es), function(e) {
  e.languages.flow = e.languages.extend("javascript", {}), e.languages.insertBefore("flow", "keyword", {type:[{pattern:/\b(?:[Bb]oolean|Function|[Nn]umber|[Ss]tring|[Ss]ymbol|any|mixed|null|void)\b/, alias:"class-name"}]}), e.languages.flow["function-variable"].pattern = /(?!\s)[_$a-z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*(?=\s*=\s*(?:function\b|(?:\([^()]*\)(?:\s*:\s*\w+)?|(?!\s)[_$a-z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*)\s*=>))/i, delete e.languages.flow.parameter, e.languages.insertBefore("flow", 
  "operator", {"flow-punctuation":{pattern:/\{\||\|\}/, alias:"punctuation"}}), Array.isArray(e.languages.flow.keyword) || (e.languages.flow.keyword = [e.languages.flow.keyword]), e.languages.flow.keyword.unshift({pattern:/(^|[^$]\b)(?:Class|declare|opaque|type)\b(?!\$)/, lookbehind:!0}, {pattern:/(^|[^$]\B)\$(?:Diff|Enum|Exact|Keys|ObjMap|PropertyType|Record|Shape|Subtype|Supertype|await)\b(?!\$)/, lookbehind:!0});
}(l$$module$dist$bridge$knoxx_frontend_bridge_es), l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.n4js = l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.extend("javascript", {keyword:/\b(?:Array|any|boolean|break|case|catch|class|const|constructor|continue|debugger|declare|default|delete|do|else|enum|export|extends|false|finally|for|from|function|get|if|implements|import|in|instanceof|interface|let|module|new|null|number|package|private|protected|public|return|set|static|string|super|switch|this|throw|true|try|typeof|var|void|while|with|yield)\b/}), 
l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.insertBefore("n4js", "constant", {annotation:{pattern:/@+\w+/, alias:"operator"}}), l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.n4jsd = l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.n4js, function(e) {
  function r(x, w) {
    return RegExp(x.replace(/<ID>/g, function() {
      return /(?!\s)[_$a-zA-Z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*/.source;
    }), w);
  }
  e.languages.insertBefore("javascript", "function-variable", {"method-variable":{pattern:RegExp("(\\.\\s*)" + e.languages.javascript["function-variable"].pattern.source), lookbehind:!0, alias:["function-variable", "method", "function", "property-access"]}}), e.languages.insertBefore("javascript", "function", {method:{pattern:RegExp("(\\.\\s*)" + e.languages.javascript.function.source), lookbehind:!0, alias:["function", "property-access"]}}), e.languages.insertBefore("javascript", "constant", {"known-class-name":[{pattern:/\b(?:(?:Float(?:32|64)|(?:Int|Uint)(?:8|16|32)|Uint8Clamped)?Array|ArrayBuffer|BigInt|Boolean|DataView|Date|Error|Function|Intl|JSON|(?:Weak)?(?:Map|Set)|Math|Number|Object|Promise|Proxy|Reflect|RegExp|String|Symbol|WebAssembly)\b/, 
  alias:"class-name"}, {pattern:/\b(?:[A-Z]\w*)Error\b/, alias:"class-name"}]}), e.languages.insertBefore("javascript", "keyword", {imports:{pattern:r(/(\bimport\b\s*)(?:<ID>(?:\s*,\s*(?:\*\s*as\s+<ID>|\{[^{}]*\}))?|\*\s*as\s+<ID>|\{[^{}]*\})(?=\s*\bfrom\b)/.source), lookbehind:!0, inside:e.languages.javascript}, exports:{pattern:r(/(\bexport\b\s*)(?:\*(?:\s*as\s+<ID>)?(?=\s*\bfrom\b)|\{[^{}]*\})/.source), lookbehind:!0, inside:e.languages.javascript}}), e.languages.javascript.keyword.unshift({pattern:/\b(?:as|default|export|from|import)\b/, 
  alias:"module"}, {pattern:/\b(?:await|break|catch|continue|do|else|finally|for|if|return|switch|throw|try|while|yield)\b/, alias:"control-flow"}, {pattern:/\bnull\b/, alias:["null", "nil"]}, {pattern:/\bundefined\b/, alias:"nil"}), e.languages.insertBefore("javascript", "operator", {spread:{pattern:/\.{3}/, alias:"operator"}, arrow:{pattern:/=>/, alias:"operator"}}), e.languages.insertBefore("javascript", "punctuation", {"property-access":{pattern:r(/(\.\s*)#?<ID>/.source), lookbehind:!0}, "maybe-class-name":{pattern:/(^|[^$\w\xA0-\uFFFF])[A-Z][$\w\xA0-\uFFFF]+/, 
  lookbehind:!0}, dom:{pattern:/\b(?:document|(?:local|session)Storage|location|navigator|performance|window)\b/, alias:"variable"}, console:{pattern:/\bconsole(?=\s*\.)/, alias:"class-name"}});
  var a = ["function", "function-variable", "method", "method-variable", "property-access"];
  for (var c = 0; c < a.length; c++) {
    var h = a[c];
    var d = e.languages.javascript[h];
    h = (d = e.util.type(d) === "RegExp" ? e.languages.javascript[h] = {pattern:d} : d).inside || {};
    (d.inside = h)["maybe-class-name"] = /^[A-Z][\s\S]*/;
  }
}(l$$module$dist$bridge$knoxx_frontend_bridge_es), function(e) {
  function h(y, b) {
    return y = y.replace(/<S>/g, function() {
      return a;
    }).replace(/<BRACES>/g, function() {
      return c;
    }).replace(/<SPREAD>/g, function() {
      return d;
    }), RegExp(y, b);
  }
  function x(y) {
    var b = [];
    for (var k = 0; k < y.length; k++) {
      var g = y[k];
      var S = !1;
      typeof g != "string" && (g.type === "tag" && g.content[0] && g.content[0].type === "tag" ? g.content[0].content[0].content === "\x3c/" ? 0 < b.length && b[b.length - 1].tagName === w(g.content[0].content[1]) && b.pop() : g.content[g.content.length - 1].content !== "/\x3e" && b.push({tagName:w(g.content[0].content[1]), openedBraces:0}) : 0 < b.length && g.type === "punctuation" && g.content === "{" ? b[b.length - 1].openedBraces++ : 0 < b.length && 0 < b[b.length - 1].openedBraces && g.type === 
      "punctuation" && g.content === "}" ? b[b.length - 1].openedBraces-- : S = !0), (S || typeof g == "string") && 0 < b.length && b[b.length - 1].openedBraces === 0 && (S = w(g), k < y.length - 1 && (typeof y[k + 1] == "string" || y[k + 1].type === "plain-text") && (S += w(y[k + 1]), y.splice(k + 1, 1)), 0 < k && (typeof y[k - 1] == "string" || y[k - 1].type === "plain-text") && (S = w(y[k - 1]) + S, y.splice(k - 1, 1), k--), y[k] = new e.Token("plain-text", S, null, S)), g.content && typeof g.content != 
      "string" && x(g.content);
    }
  }
  var r = e.util.clone(e.languages.javascript);
  var a = /(?:\s|\/\/.*(?!.)|\/\*(?:[^*]|\*(?!\/))\*\/)/.source;
  var c = /(?:\{(?:\{(?:\{[^{}]*\}|[^{}])*\}|[^{}])*\})/.source;
  var d = /(?:\{<S>*\.{3}(?:[^{}]|<BRACES>)*\})/.source;
  d = h(d).source, e.languages.jsx = e.languages.extend("markup", r), e.languages.jsx.tag.pattern = h(/<\/?(?:[\w.:-]+(?:<S>+(?:[\w.:$-]+(?:=(?:"(?:\\[\s\S]|[^\\"])*"|'(?:\\[\s\S]|[^\\'])*'|[^\s{'"/>=]+|<BRACES>))?|<SPREAD>))*<S>*\/?)?>/.source), e.languages.jsx.tag.inside.tag.pattern = /^<\/?[^\s>\/]*/, e.languages.jsx.tag.inside["attr-value"].pattern = /=(?!\{)(?:"(?:\\[\s\S]|[^\\"])*"|'(?:\\[\s\S]|[^\\'])*'|[^\s'">]+)/, e.languages.jsx.tag.inside.tag.inside["class-name"] = /^[A-Z]\w*(?:\.[A-Z]\w*)*$/, 
  e.languages.jsx.tag.inside.comment = r.comment, e.languages.insertBefore("inside", "attr-name", {spread:{pattern:h(/<SPREAD>/.source), inside:e.languages.jsx}}, e.languages.jsx.tag), e.languages.insertBefore("inside", "special-attr", {script:{pattern:h(/=<BRACES>/.source), alias:"language-javascript", inside:{"script-punctuation":{pattern:/^=(?=\{)/, alias:"punctuation"}, rest:e.languages.jsx}}}, e.languages.jsx.tag);
  var w = function(y) {
    return y ? typeof y == "string" ? y : typeof y.content == "string" ? y.content : y.content.map(w).join("") : "";
  };
  e.hooks.add("after-tokenize", function(y) {
    y.language !== "jsx" && y.language !== "tsx" || x(y.tokens);
  });
}(l$$module$dist$bridge$knoxx_frontend_bridge_es), function(e) {
  var r = e.util.clone(e.languages.typescript);
  r = (e.languages.tsx = e.languages.extend("jsx", r), delete e.languages.tsx.parameter, delete e.languages.tsx["literal-property"], e.languages.tsx.tag);
  r.pattern = RegExp(/(^|[^\w$]|(?=<\/))/.source + "(?:" + r.pattern.source + ")", r.pattern.flags), r.lookbehind = !0;
}(l$$module$dist$bridge$knoxx_frontend_bridge_es), l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.swift = {comment:{pattern:/(^|[^\\:])(?:\/\/.*|\/\*(?:[^/*]|\/(?!\*)|\*(?!\/)|\/\*(?:[^*]|\*(?!\/))*\*\/)*\*\/)/, lookbehind:!0, greedy:!0}, "string-literal":[{pattern:RegExp(/(^|[^"#])/.source + "(?:" + /"(?:\\(?:\((?:[^()]|\([^()]*\))*\)|\r\n|[^(])|[^\\\r\n"])*"/.source + "|" + /"""(?:\\(?:\((?:[^()]|\([^()]*\))*\)|[^(])|[^\\"]|"(?!""))*"""/.source + ")" + /(?!["#])/.source), lookbehind:!0, 
greedy:!0, inside:{interpolation:{pattern:/(\\\()(?:[^()]|\([^()]*\))*(?=\))/, lookbehind:!0, inside:null}, "interpolation-punctuation":{pattern:/^\)|\\\($/, alias:"punctuation"}, punctuation:/\\(?=[\r\n])/, string:/[\s\S]+/}}, {pattern:RegExp(/(^|[^"#])(#+)/.source + "(?:" + /"(?:\\(?:#+\((?:[^()]|\([^()]*\))*\)|\r\n|[^#])|[^\\\r\n])*?"/.source + "|" + /"""(?:\\(?:#+\((?:[^()]|\([^()]*\))*\)|[^#])|[^\\])*?"""/.source + ")\\2"), lookbehind:!0, greedy:!0, inside:{interpolation:{pattern:/(\\#+\()(?:[^()]|\([^()]*\))*(?=\))/, 
lookbehind:!0, inside:null}, "interpolation-punctuation":{pattern:/^\)|\\#+\($/, alias:"punctuation"}, string:/[\s\S]+/}}], directive:{pattern:RegExp(/#/.source + "(?:" + /(?:elseif|if)\b/.source + "(?:[ \t]*" + /(?:![ \t]*)?(?:\b\w+\b(?:[ \t]*\((?:[^()]|\([^()]*\))*\))?|\((?:[^()]|\([^()]*\))*\))(?:[ \t]*(?:&&|\|\|))?/.source + ")+|" + /(?:else|endif)\b/.source + ")"), alias:"property", inside:{"directive-name":/^#\w+/, boolean:/\b(?:false|true)\b/, number:/\b\d+(?:\.\d+)*\b/, operator:/!|&&|\|\||[<>]=?/, 
punctuation:/[(),]/}}, literal:{pattern:/#(?:colorLiteral|column|dsohandle|file(?:ID|Literal|Path)?|function|imageLiteral|line)\b/, alias:"constant"}, "other-directive":{pattern:/#\w+\b/, alias:"property"}, attribute:{pattern:/@\w+/, alias:"atrule"}, "function-definition":{pattern:/(\bfunc\s+)\w+/, lookbehind:!0, alias:"function"}, label:{pattern:/\b(break|continue)\s+\w+|\b[a-zA-Z_]\w*(?=\s*:\s*(?:for|repeat|while)\b)/, lookbehind:!0, alias:"important"}, keyword:/\b(?:Any|Protocol|Self|Type|actor|as|assignment|associatedtype|associativity|async|await|break|case|catch|class|continue|convenience|default|defer|deinit|didSet|do|dynamic|else|enum|extension|fallthrough|fileprivate|final|for|func|get|guard|higherThan|if|import|in|indirect|infix|init|inout|internal|is|isolated|lazy|left|let|lowerThan|mutating|none|nonisolated|nonmutating|open|operator|optional|override|postfix|precedencegroup|prefix|private|protocol|public|repeat|required|rethrows|return|right|safe|self|set|some|static|struct|subscript|super|switch|throw|throws|try|typealias|unowned|unsafe|var|weak|where|while|willSet)\b/, 
boolean:/\b(?:false|true)\b/, nil:{pattern:/\bnil\b/, alias:"constant"}, "short-argument":/\$\d+\b/, omit:{pattern:/\b_\b/, alias:"keyword"}, number:/\b(?:[\d_]+(?:\.[\de_]+)?|0x[a-f0-9_]+(?:\.[a-f0-9p_]+)?|0b[01_]+|0o[0-7_]+)\b/i, "class-name":/\b[A-Z](?:[A-Z_\d]*[a-z]\w*)?\b/, function:/\b[a-z_]\w*(?=\s*\()/i, constant:/\b(?:[A-Z_]{2,}|k[A-Z][A-Za-z_]+)\b/, operator:/[-+*/%=!<>&|^~?]+|\.[.\-+*/%=!<>&|^~?]+/, punctuation:/[{}[\]();,.:\\]/}, l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.swift["string-literal"].forEach(function(e) {
  e.inside.interpolation.inside = l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.swift;
}), function(e) {
  e.languages.kotlin = e.languages.extend("clike", {keyword:{pattern:/(^|[^.])\b(?:abstract|actual|annotation|as|break|by|catch|class|companion|const|constructor|continue|crossinline|data|do|dynamic|else|enum|expect|external|final|finally|for|fun|get|if|import|in|infix|init|inline|inner|interface|internal|is|lateinit|noinline|null|object|open|operator|out|override|package|private|protected|public|reified|return|sealed|set|super|suspend|tailrec|this|throw|to|try|typealias|val|var|vararg|when|where|while)\b/, 
  lookbehind:!0}, function:[{pattern:/(?:`[^\r\n`]+`|\b\w+)(?=\s*\()/, greedy:!0}, {pattern:/(\.)(?:`[^\r\n`]+`|\w+)(?=\s*\{)/, lookbehind:!0, greedy:!0}], number:/\b(?:0[xX][\da-fA-F]+(?:_[\da-fA-F]+)*|0[bB][01]+(?:_[01]+)*|\d+(?:_\d+)*(?:\.\d+(?:_\d+)*)?(?:[eE][+-]?\d+(?:_\d+)*)?[fFL]?)\b/, operator:/\+[+=]?|-[-=>]?|==?=?|!(?:!|==?)?|[\/*%<>]=?|[?:]:?|\.\.|&&|\|\||\b(?:and|inv|or|shl|shr|ushr|xor)\b/}), delete e.languages.kotlin["class-name"];
  var r = {"interpolation-punctuation":{pattern:/^\$\{?|\}$/, alias:"punctuation"}, expression:{pattern:/[\s\S]+/, inside:e.languages.kotlin}};
  e.languages.insertBefore("kotlin", "string", {"string-literal":[{pattern:/"""(?:[^$]|\$(?:(?!\{)|\{[^{}]*\}))*?"""/, alias:"multiline", inside:{interpolation:{pattern:/\$(?:[a-z_]\w*|\{[^{}]*\})/i, inside:r}, string:/[\s\S]+/}}, {pattern:/"(?:[^"\\\r\n$]|\\.|\$(?:(?!\{)|\{[^{}]*\}))*"/, alias:"singleline", inside:{interpolation:{pattern:/((?:^|[^\\])(?:\\{2})*)\$(?:[a-z_]\w*|\{[^{}]*\})/i, lookbehind:!0, inside:r}, string:/[\s\S]+/}}], char:{pattern:/'(?:[^'\\\r\n]|\\(?:.|u[a-fA-F0-9]{0,4}))'/, 
  greedy:!0}}), delete e.languages.kotlin.string, e.languages.insertBefore("kotlin", "keyword", {annotation:{pattern:/\B@(?:\w+:)?(?:[A-Z]\w*|\[[^\]]+\])/, alias:"builtin"}}), e.languages.insertBefore("kotlin", "function", {label:{pattern:/\b\w+@|@\w+\b/, alias:"symbol"}}), e.languages.kt = e.languages.kotlin, e.languages.kts = e.languages.kotlin;
}(l$$module$dist$bridge$knoxx_frontend_bridge_es), l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.c = l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.extend("clike", {comment:{pattern:/\/\/(?:[^\r\n\\]|\\(?:\r\n?|\n|(?![\r\n])))*|\/\*[\s\S]*?(?:\*\/|$)/, greedy:!0}, string:{pattern:/"(?:\\(?:\r\n|[\s\S])|[^"\\\r\n])*"/, greedy:!0}, "class-name":{pattern:/(\b(?:enum|struct)\s+(?:__attribute__\s*\(\([\s\S]*?\)\)\s*)?)\w+|\b[a-z]\w*_t\b/, lookbehind:!0}, keyword:/\b(?:_Alignas|_Alignof|_Atomic|_Bool|_Complex|_Generic|_Imaginary|_Noreturn|_Static_assert|_Thread_local|__attribute__|asm|auto|break|case|char|const|continue|default|do|double|else|enum|extern|float|for|goto|if|inline|int|long|register|return|short|signed|sizeof|static|struct|switch|typedef|typeof|union|unsigned|void|volatile|while)\b/, 
function:/\b[a-z_]\w*(?=\s*\()/i, number:/(?:\b0x(?:[\da-f]+(?:\.[\da-f]*)?|\.[\da-f]+)(?:p[+-]?\d+)?|(?:\b\d+(?:\.\d*)?|\B\.\d+)(?:e[+-]?\d+)?)[ful]{0,4}/i, operator:/>>=?|<<=?|->|([-+&|:])\1|[?:~]|[-+*/%&|^!=<>]=?/}), l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.insertBefore("c", "string", {char:{pattern:/'(?:\\(?:\r\n|[\s\S])|[^'\\\r\n]){0,32}'/, greedy:!0}}), l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.insertBefore("c", "string", {macro:{pattern:/(^[\t ]*)#\s*[a-z](?:[^\r\n\\/]|\/(?!\*)|\/\*(?:[^*]|\*(?!\/))*\*\/|\\(?:\r\n|[\s\S]))*/im, 
lookbehind:!0, greedy:!0, alias:"property", inside:{string:[{pattern:/^(#\s*include\s*)<[^>]+>/, lookbehind:!0}, l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.c.string], char:l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.c.char, comment:l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.c.comment, "macro-name":[{pattern:/(^#\s*define\s+)\w+\b(?!\()/i, lookbehind:!0}, {pattern:/(^#\s*define\s+)\w+\b(?=\()/i, lookbehind:!0, alias:"function"}], directive:{pattern:/^(#\s*)[a-z]+/, 
lookbehind:!0, alias:"keyword"}, "directive-hash":/^#/, punctuation:/##|\\(?=[\r\n])/, expression:{pattern:/\S[\s\S]*/, inside:l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.c}}}}), l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.insertBefore("c", "function", {constant:/\b(?:EOF|NULL|SEEK_CUR|SEEK_END|SEEK_SET|__DATE__|__FILE__|__LINE__|__TIMESTAMP__|__TIME__|__func__|stderr|stdin|stdout)\b/}), delete l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.c.boolean, l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.objectivec = 
l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.extend("c", {string:{pattern:/@?"(?:\\(?:\r\n|[\s\S])|[^"\\\r\n])*"/, greedy:!0}, keyword:/\b(?:asm|auto|break|case|char|const|continue|default|do|double|else|enum|extern|float|for|goto|if|in|inline|int|long|register|return|self|short|signed|sizeof|static|struct|super|switch|typedef|typeof|union|unsigned|void|volatile|while)\b|(?:@interface|@end|@implementation|@protocol|@class|@public|@protected|@private|@property|@try|@catch|@finally|@throw|@synthesize|@dynamic|@selector)\b/, 
operator:/-[->]?|\+\+?|!=?|<<?=?|>>?=?|==?|&&?|\|\|?|[~^%?*\/@]/}), delete l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.objectivec["class-name"], l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.objc = l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.objectivec, l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.reason = l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.extend("clike", {string:{pattern:/"(?:\\(?:\r\n|[\s\S])|[^\\\r\n"])*"/, greedy:!0}, "class-name":/\b[A-Z]\w*/, 
keyword:/\b(?:and|as|assert|begin|class|constraint|do|done|downto|else|end|exception|external|for|fun|function|functor|if|in|include|inherit|initializer|lazy|let|method|module|mutable|new|nonrec|object|of|open|or|private|rec|sig|struct|switch|then|to|try|type|val|virtual|when|while|with)\b/, operator:/\.{3}|:[:=]|\|>|->|=(?:==?|>)?|<=?|>=?|[|^?'#!~`]|[+\-*\/]\.?|\b(?:asr|land|lor|lsl|lsr|lxor|mod)\b/}), l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.insertBefore("reason", "class-name", 
{char:{pattern:/'(?:\\x[\da-f]{2}|\\o[0-3][0-7][0-7]|\\\d{3}|\\.|[^'\\\r\n])'/, greedy:!0}, constructor:/\b[A-Z]\w*\b(?!\s*\.)/, label:{pattern:/\b[a-z]\w*(?=::)/, alias:"symbol"}}), delete l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.reason.function, function(e) {
  var r = /\/\*(?:[^*/]|\*(?!\/)|\/(?!\*)|<self>)*\*\//.source;
  for (var a = 0; a < 2; a++) {
    r = r.replace(/<self>/g, function() {
      return r;
    });
  }
  r = r.replace(/<self>/g, function() {
    return /[^\s\S]/.source;
  }), e.languages.rust = {comment:[{pattern:RegExp(/(^|[^\\])/.source + r), lookbehind:!0, greedy:!0}, {pattern:/(^|[^\\:])\/\/.*/, lookbehind:!0, greedy:!0}], string:{pattern:/b?"(?:\\[\s\S]|[^\\"])*"|b?r(#*)"(?:[^"]|"(?!\1))*"\1/, greedy:!0}, char:{pattern:/b?'(?:\\(?:x[0-7][\da-fA-F]|u\{(?:[\da-fA-F]_*){1,6}\}|.)|[^\\\r\n\t'])'/, greedy:!0}, attribute:{pattern:/#!?\[(?:[^\[\]"]|"(?:\\[\s\S]|[^\\"])*")*\]/, greedy:!0, alias:"attr-name", inside:{string:null}}, "closure-params":{pattern:/([=(,:]\s*|\bmove\s*)\|[^|]*\||\|[^|]*\|(?=\s*(?:\{|->))/, 
  lookbehind:!0, greedy:!0, inside:{"closure-punctuation":{pattern:/^\||\|$/, alias:"punctuation"}, rest:null}}, "lifetime-annotation":{pattern:/'\w+/, alias:"symbol"}, "fragment-specifier":{pattern:/(\$\w+:)[a-z]+/, lookbehind:!0, alias:"punctuation"}, variable:/\$\w+/, "function-definition":{pattern:/(\bfn\s+)\w+/, lookbehind:!0, alias:"function"}, "type-definition":{pattern:/(\b(?:enum|struct|trait|type|union)\s+)\w+/, lookbehind:!0, alias:"class-name"}, "module-declaration":[{pattern:/(\b(?:crate|mod)\s+)[a-z][a-z_\d]*/, 
  lookbehind:!0, alias:"namespace"}, {pattern:/(\b(?:crate|self|super)\s*)::\s*[a-z][a-z_\d]*\b(?:\s*::(?:\s*[a-z][a-z_\d]*\s*::)*)?/, lookbehind:!0, alias:"namespace", inside:{punctuation:/::/}}], keyword:[/\b(?:Self|abstract|as|async|await|become|box|break|const|continue|crate|do|dyn|else|enum|extern|final|fn|for|if|impl|in|let|loop|macro|match|mod|move|mut|override|priv|pub|ref|return|self|static|struct|super|trait|try|type|typeof|union|unsafe|unsized|use|virtual|where|while|yield)\b/, /\b(?:bool|char|f(?:32|64)|[ui](?:8|16|32|64|128|size)|str)\b/], 
  function:/\b[a-z_]\w*(?=\s*(?:::\s*<|\())/, macro:{pattern:/\b\w+!/, alias:"property"}, constant:/\b[A-Z_][A-Z_\d]+\b/, "class-name":/\b[A-Z]\w*\b/, namespace:{pattern:/(?:\b[a-z][a-z_\d]*\s*::\s*)*\b[a-z][a-z_\d]*\s*::(?!\s*<)/, inside:{punctuation:/::/}}, number:/\b(?:0x[\dA-Fa-f](?:_?[\dA-Fa-f])*|0o[0-7](?:_?[0-7])*|0b[01](?:_?[01])*|(?:(?:\d(?:_?\d)*)?\.)?\d(?:_?\d)*(?:[Ee][+-]?\d+)?)(?:_?(?:f32|f64|[iu](?:8|16|32|64|size)?))?\b/, boolean:/\b(?:false|true)\b/, punctuation:/->|\.\.=|\.{1,3}|::|[{}[\];(),:]/, 
  operator:/[-+*\/%!^]=?|=[=>]?|&[&=]?|\|[|=]?|<<?=?|>>?=?|[@?]/}, e.languages.rust["closure-params"].inside.rest = e.languages.rust, e.languages.rust.attribute.inside.string = e.languages.rust.string;
}(l$$module$dist$bridge$knoxx_frontend_bridge_es), l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.go = l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.extend("clike", {string:{pattern:/(^|[^\\])"(?:\\.|[^"\\\r\n])*"|`[^`]*`/, lookbehind:!0, greedy:!0}, keyword:/\b(?:break|case|chan|const|continue|default|defer|else|fallthrough|for|func|go(?:to)?|if|import|interface|map|package|range|return|select|struct|switch|type|var)\b/, boolean:/\b(?:_|false|iota|nil|true)\b/, number:[/\b0(?:b[01_]+|o[0-7_]+)i?\b/i, 
/\b0x(?:[a-f\d_]+(?:\.[a-f\d_]*)?|\.[a-f\d_]+)(?:p[+-]?\d+(?:_\d+)*)?i?(?!\w)/i, /(?:\b\d[\d_]*(?:\.[\d_]*)?|\B\.\d[\d_]*)(?:e[+-]?[\d_]+)?i?(?!\w)/i], operator:/[*\/%^!=]=?|\+[=+]?|-[=-]?|\|[=|]?|&(?:=|&|\^=?)?|>(?:>=?|=)?|<(?:<=?|=|-)?|:=|\.\.\./, builtin:/\b(?:append|bool|byte|cap|close|complex|complex(?:64|128)|copy|delete|error|float(?:32|64)|u?int(?:8|16|32|64)?|imag|len|make|new|panic|print(?:ln)?|real|recover|rune|string|uintptr)\b/}), l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.insertBefore("go", 
"string", {char:{pattern:/'(?:\\.|[^'\\\r\n]){0,10}'/, greedy:!0}}), delete l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.go["class-name"], function(e) {
  var r = /\b(?:alignas|alignof|asm|auto|bool|break|case|catch|char|char16_t|char32_t|char8_t|class|co_await|co_return|co_yield|compl|concept|const|const_cast|consteval|constexpr|constinit|continue|decltype|default|delete|do|double|dynamic_cast|else|enum|explicit|export|extern|final|float|for|friend|goto|if|import|inline|int|int16_t|int32_t|int64_t|int8_t|long|module|mutable|namespace|new|noexcept|nullptr|operator|override|private|protected|public|register|reinterpret_cast|requires|return|short|signed|sizeof|static|static_assert|static_cast|struct|switch|template|this|thread_local|throw|try|typedef|typeid|typename|uint16_t|uint32_t|uint64_t|uint8_t|union|unsigned|using|virtual|void|volatile|wchar_t|while)\b/;
  var a = /\b(?!<keyword>)\w+(?:\s*\.\s*\w+)*\b/.source.replace(/<keyword>/g, function() {
    return r.source;
  });
  e.languages.cpp = e.languages.extend("c", {"class-name":[{pattern:RegExp(/(\b(?:class|concept|enum|struct|typename)\s+)(?!<keyword>)\w+/.source.replace(/<keyword>/g, function() {
    return r.source;
  })), lookbehind:!0}, /\b[A-Z]\w*(?=\s*::\s*\w+\s*\()/, /\b[A-Z_]\w*(?=\s*::\s*~\w+\s*\()/i, /\b\w+(?=\s*<(?:[^<>]|<(?:[^<>]|<[^<>]*>)*>)*>\s*::\s*\w+\s*\()/], keyword:r, number:{pattern:/(?:\b0b[01']+|\b0x(?:[\da-f']+(?:\.[\da-f']*)?|\.[\da-f']+)(?:p[+-]?[\d']+)?|(?:\b[\d']+(?:\.[\d']*)?|\B\.[\d']+)(?:e[+-]?[\d']+)?)[ful]{0,4}/i, greedy:!0}, operator:/>>=?|<<=?|->|--|\+\+|&&|\|\||[?:~]|<=>|[-+*/%&|^!=<>]=?|\b(?:and|and_eq|bitand|bitor|not|not_eq|or|or_eq|xor|xor_eq)\b/, boolean:/\b(?:false|true)\b/}), 
  e.languages.insertBefore("cpp", "string", {module:{pattern:RegExp(/(\b(?:import|module)\s+)/.source + "(?:" + /"(?:\\(?:\r\n|[\s\S])|[^"\\\r\n])*"|<[^<>\r\n]*>/.source + "|" + /<mod-name>(?:\s*:\s*<mod-name>)?|:\s*<mod-name>/.source.replace(/<mod-name>/g, function() {
    return a;
  }) + ")"), lookbehind:!0, greedy:!0, inside:{string:/^[<"][\s\S]+/, operator:/:/, punctuation:/\./}}, "raw-string":{pattern:/R"([^()\\ ]{0,16})\([\s\S]*?\)\1"/, alias:"string", greedy:!0}}), e.languages.insertBefore("cpp", "keyword", {"generic-function":{pattern:/\b(?!operator\b)[a-z_]\w*\s*<(?:[^<>]|<[^<>]*>)*>(?=\s*\()/i, inside:{function:/^\w+/, generic:{pattern:/<[\s\S]+/, alias:"class-name", inside:e.languages.cpp}}}}), e.languages.insertBefore("cpp", "operator", {"double-colon":{pattern:/::/, 
  alias:"punctuation"}}), e.languages.insertBefore("cpp", "class-name", {"base-clause":{pattern:/(\b(?:class|struct)\s+\w+\s*:\s*)[^;{}"'\s]+(?:\s+[^;{}"'\s]+)*(?=\s*[;{])/, lookbehind:!0, greedy:!0, inside:e.languages.extend("cpp", {})}}), e.languages.insertBefore("inside", "double-colon", {"class-name":/\b[a-z_]\w*\b(?!\s*::)/i}, e.languages.cpp["base-clause"]);
}(l$$module$dist$bridge$knoxx_frontend_bridge_es), l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.python = {comment:{pattern:/(^|[^\\])#.*/, lookbehind:!0, greedy:!0}, "string-interpolation":{pattern:/(?:f|fr|rf)(?:("""|''')[\s\S]*?\1|("|')(?:\\.|(?!\2)[^\\\r\n])*\2)/i, greedy:!0, inside:{interpolation:{pattern:/((?:^|[^{])(?:\{\{)*)\{(?!\{)(?:[^{}]|\{(?!\{)(?:[^{}]|\{(?!\{)(?:[^{}])+\})+\})+\}/, lookbehind:!0, inside:{"format-spec":{pattern:/(:)[^:(){}]+(?=\}$)/, lookbehind:!0}, "conversion-option":{pattern:/![sra](?=[:}]$)/, 
alias:"punctuation"}, rest:null}}, string:/[\s\S]+/}}, "triple-quoted-string":{pattern:/(?:[rub]|br|rb)?("""|''')[\s\S]*?\1/i, greedy:!0, alias:"string"}, string:{pattern:/(?:[rub]|br|rb)?("|')(?:\\.|(?!\1)[^\\\r\n])*\1/i, greedy:!0}, function:{pattern:/((?:^|\s)def[ \t]+)[a-zA-Z_]\w*(?=\s*\()/g, lookbehind:!0}, "class-name":{pattern:/(\bclass\s+)\w+/i, lookbehind:!0}, decorator:{pattern:/(^[\t ]*)@\w+(?:\.\w+)*/m, lookbehind:!0, alias:["annotation", "punctuation"], inside:{punctuation:/\./}}, keyword:/\b(?:_(?=\s*:)|and|as|assert|async|await|break|case|class|continue|def|del|elif|else|except|exec|finally|for|from|global|if|import|in|is|lambda|match|nonlocal|not|or|pass|print|raise|return|try|while|with|yield)\b/, 
builtin:/\b(?:__import__|abs|all|any|apply|ascii|basestring|bin|bool|buffer|bytearray|bytes|callable|chr|classmethod|cmp|coerce|compile|complex|delattr|dict|dir|divmod|enumerate|eval|execfile|file|filter|float|format|frozenset|getattr|globals|hasattr|hash|help|hex|id|input|int|intern|isinstance|issubclass|iter|len|list|locals|long|map|max|memoryview|min|next|object|oct|open|ord|pow|property|range|raw_input|reduce|reload|repr|reversed|round|set|setattr|slice|sorted|staticmethod|str|sum|super|tuple|type|unichr|unicode|vars|xrange|zip)\b/, 
boolean:/\b(?:False|None|True)\b/, number:/\b0(?:b(?:_?[01])+|o(?:_?[0-7])+|x(?:_?[a-f0-9])+)\b|(?:\b\d+(?:_\d+)*(?:\.(?:\d+(?:_\d+)*)?)?|\B\.\d+(?:_\d+)*)(?:e[+-]?\d+(?:_\d+)*)?j?(?!\w)/i, operator:/[-+%=]=?|!=|:=|\*\*?=?|\/\/?=?|<[<=>]?|>[=>]?|[&|^~]/, punctuation:/[{}[\];(),.:]/}, l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.python["string-interpolation"].inside.interpolation.inside.rest = l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.python, l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.py = 
l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.python, l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.json = {property:{pattern:/(^|[^\\])"(?:\\.|[^\\"\r\n])*"(?=\s*:)/, lookbehind:!0, greedy:!0}, string:{pattern:/(^|[^\\])"(?:\\.|[^\\"\r\n])*"(?!\s*:)/, lookbehind:!0, greedy:!0}, comment:{pattern:/\/\/.*|\/\*[\s\S]*?(?:\*\/|$)/, greedy:!0}, number:/-?\b\d+(?:\.\d+)?(?:e[+-]?\d+)?\b/i, punctuation:/[{}[\],]/, operator:/:/, boolean:/\b(?:false|true)\b/, null:{pattern:/\bnull\b/, 
alias:"keyword"}}, l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.webmanifest = l$$module$dist$bridge$knoxx_frontend_bridge_es.languages.json;
var oe$$module$dist$bridge$knoxx_frontend_bridge_es = {};
Vt$$module$dist$bridge$knoxx_frontend_bridge_es(oe$$module$dist$bridge$knoxx_frontend_bridge_es, {dracula:() => tn$$module$dist$bridge$knoxx_frontend_bridge_es, duotoneDark:() => rn$$module$dist$bridge$knoxx_frontend_bridge_es, duotoneLight:() => on$$module$dist$bridge$knoxx_frontend_bridge_es, github:() => ln$$module$dist$bridge$knoxx_frontend_bridge_es, gruvboxMaterialDark:() => Nn$$module$dist$bridge$knoxx_frontend_bridge_es, gruvboxMaterialLight:() => Pn$$module$dist$bridge$knoxx_frontend_bridge_es, 
jettwaveDark:() => zn$$module$dist$bridge$knoxx_frontend_bridge_es, jettwaveLight:() => Tn$$module$dist$bridge$knoxx_frontend_bridge_es, nightOwl:() => un$$module$dist$bridge$knoxx_frontend_bridge_es, nightOwlLight:() => gn$$module$dist$bridge$knoxx_frontend_bridge_es, oceanicNext:() => fn$$module$dist$bridge$knoxx_frontend_bridge_es, okaidia:() => yn$$module$dist$bridge$knoxx_frontend_bridge_es, oneDark:() => Dn$$module$dist$bridge$knoxx_frontend_bridge_es, oneLight:() => On$$module$dist$bridge$knoxx_frontend_bridge_es, 
palenight:() => hn$$module$dist$bridge$knoxx_frontend_bridge_es, shadesOfPurple:() => kn$$module$dist$bridge$knoxx_frontend_bridge_es, synthwave84:() => vn$$module$dist$bridge$knoxx_frontend_bridge_es, ultramin:() => Fn$$module$dist$bridge$knoxx_frontend_bridge_es, vsDark:() => _n$$module$dist$bridge$knoxx_frontend_bridge_es, vsLight:() => Cn$$module$dist$bridge$knoxx_frontend_bridge_es});
var en$$module$dist$bridge$knoxx_frontend_bridge_es = {plain:{color:"#F8F8F2", backgroundColor:"#282A36"}, styles:[{types:["prolog", "constant", "builtin"], style:{color:"rgb(189, 147, 249)"}}, {types:["inserted", "function"], style:{color:"rgb(80, 250, 123)"}}, {types:["deleted"], style:{color:"rgb(255, 85, 85)"}}, {types:["changed"], style:{color:"rgb(255, 184, 108)"}}, {types:["punctuation", "symbol"], style:{color:"rgb(248, 248, 242)"}}, {types:["string", "char", "tag", "selector"], style:{color:"rgb(255, 121, 198)"}}, 
{types:["keyword", "variable"], style:{color:"rgb(189, 147, 249)", fontStyle:"italic"}}, {types:["comment"], style:{color:"rgb(98, 114, 164)"}}, {types:["attr-name"], style:{color:"rgb(241, 250, 140)"}}]};
var tn$$module$dist$bridge$knoxx_frontend_bridge_es = en$$module$dist$bridge$knoxx_frontend_bridge_es;
var nn$$module$dist$bridge$knoxx_frontend_bridge_es = {plain:{backgroundColor:"#2a2734", color:"#9a86fd"}, styles:[{types:["comment", "prolog", "doctype", "cdata", "punctuation"], style:{color:"#6c6783"}}, {types:["namespace"], style:{opacity:0.7}}, {types:["tag", "operator", "number"], style:{color:"#e09142"}}, {types:["property", "function"], style:{color:"#9a86fd"}}, {types:["tag-id", "selector", "atrule-id"], style:{color:"#eeebff"}}, {types:["attr-name"], style:{color:"#c4b9fe"}}, {types:["boolean", 
"string", "entity", "url", "attr-value", "keyword", "control", "directive", "unit", "statement", "regex", "atrule", "placeholder", "variable"], style:{color:"#ffcc99"}}, {types:["deleted"], style:{textDecorationLine:"line-through"}}, {types:["inserted"], style:{textDecorationLine:"underline"}}, {types:["italic"], style:{fontStyle:"italic"}}, {types:["important", "bold"], style:{fontWeight:"bold"}}, {types:["important"], style:{color:"#c4b9fe"}}]};
var rn$$module$dist$bridge$knoxx_frontend_bridge_es = nn$$module$dist$bridge$knoxx_frontend_bridge_es;
var an$$module$dist$bridge$knoxx_frontend_bridge_es = {plain:{backgroundColor:"#faf8f5", color:"#728fcb"}, styles:[{types:["comment", "prolog", "doctype", "cdata", "punctuation"], style:{color:"#b6ad9a"}}, {types:["namespace"], style:{opacity:0.7}}, {types:["tag", "operator", "number"], style:{color:"#063289"}}, {types:["property", "function"], style:{color:"#b29762"}}, {types:["tag-id", "selector", "atrule-id"], style:{color:"#2d2006"}}, {types:["attr-name"], style:{color:"#896724"}}, {types:["boolean", 
"string", "entity", "url", "attr-value", "keyword", "control", "directive", "unit", "statement", "regex", "atrule"], style:{color:"#728fcb"}}, {types:["placeholder", "variable"], style:{color:"#93abdc"}}, {types:["deleted"], style:{textDecorationLine:"line-through"}}, {types:["inserted"], style:{textDecorationLine:"underline"}}, {types:["italic"], style:{fontStyle:"italic"}}, {types:["important", "bold"], style:{fontWeight:"bold"}}, {types:["important"], style:{color:"#896724"}}]};
var on$$module$dist$bridge$knoxx_frontend_bridge_es = an$$module$dist$bridge$knoxx_frontend_bridge_es;
var sn$$module$dist$bridge$knoxx_frontend_bridge_es = {plain:{color:"#393A34", backgroundColor:"#f6f8fa"}, styles:[{types:["comment", "prolog", "doctype", "cdata"], style:{color:"#999988", fontStyle:"italic"}}, {types:["namespace"], style:{opacity:0.7}}, {types:["string", "attr-value"], style:{color:"#e3116c"}}, {types:["punctuation", "operator"], style:{color:"#393A34"}}, {types:["entity", "url", "symbol", "number", "boolean", "variable", "constant", "property", "regex", "inserted"], style:{color:"#36acaa"}}, 
{types:["atrule", "keyword", "attr-name", "selector"], style:{color:"#00a4db"}}, {types:["function", "deleted", "tag"], style:{color:"#d73a49"}}, {types:["function-variable"], style:{color:"#6f42c1"}}, {types:["tag", "selector", "keyword"], style:{color:"#00009f"}}]};
var ln$$module$dist$bridge$knoxx_frontend_bridge_es = sn$$module$dist$bridge$knoxx_frontend_bridge_es;
var cn$$module$dist$bridge$knoxx_frontend_bridge_es = {plain:{color:"#d6deeb", backgroundColor:"#011627"}, styles:[{types:["changed"], style:{color:"rgb(162, 191, 252)", fontStyle:"italic"}}, {types:["deleted"], style:{color:"rgba(239, 83, 80, 0.56)", fontStyle:"italic"}}, {types:["inserted", "attr-name"], style:{color:"rgb(173, 219, 103)", fontStyle:"italic"}}, {types:["comment"], style:{color:"rgb(99, 119, 119)", fontStyle:"italic"}}, {types:["string", "url"], style:{color:"rgb(173, 219, 103)"}}, 
{types:["variable"], style:{color:"rgb(214, 222, 235)"}}, {types:["number"], style:{color:"rgb(247, 140, 108)"}}, {types:["builtin", "char", "constant", "function"], style:{color:"rgb(130, 170, 255)"}}, {types:["punctuation"], style:{color:"rgb(199, 146, 234)"}}, {types:["selector", "doctype"], style:{color:"rgb(199, 146, 234)", fontStyle:"italic"}}, {types:["class-name"], style:{color:"rgb(255, 203, 139)"}}, {types:["tag", "operator", "keyword"], style:{color:"rgb(127, 219, 202)"}}, {types:["boolean"], 
style:{color:"rgb(255, 88, 116)"}}, {types:["property"], style:{color:"rgb(128, 203, 196)"}}, {types:["namespace"], style:{color:"rgb(178, 204, 214)"}}]};
var un$$module$dist$bridge$knoxx_frontend_bridge_es = cn$$module$dist$bridge$knoxx_frontend_bridge_es;
var dn$$module$dist$bridge$knoxx_frontend_bridge_es = {plain:{color:"#403f53", backgroundColor:"#FBFBFB"}, styles:[{types:["changed"], style:{color:"rgb(162, 191, 252)", fontStyle:"italic"}}, {types:["deleted"], style:{color:"rgba(239, 83, 80, 0.56)", fontStyle:"italic"}}, {types:["inserted", "attr-name"], style:{color:"rgb(72, 118, 214)", fontStyle:"italic"}}, {types:["comment"], style:{color:"rgb(152, 159, 177)", fontStyle:"italic"}}, {types:["string", "builtin", "char", "constant", "url"], style:{color:"rgb(72, 118, 214)"}}, 
{types:["variable"], style:{color:"rgb(201, 103, 101)"}}, {types:["number"], style:{color:"rgb(170, 9, 130)"}}, {types:["punctuation"], style:{color:"rgb(153, 76, 195)"}}, {types:["function", "selector", "doctype"], style:{color:"rgb(153, 76, 195)", fontStyle:"italic"}}, {types:["class-name"], style:{color:"rgb(17, 17, 17)"}}, {types:["tag"], style:{color:"rgb(153, 76, 195)"}}, {types:["operator", "property", "keyword", "namespace"], style:{color:"rgb(12, 150, 155)"}}, {types:["boolean"], style:{color:"rgb(188, 84, 84)"}}]};
var gn$$module$dist$bridge$knoxx_frontend_bridge_es = dn$$module$dist$bridge$knoxx_frontend_bridge_es;
var D$$module$dist$bridge$knoxx_frontend_bridge_es = {char:"#D8DEE9", comment:"#999999", keyword:"#c5a5c5", primitive:"#5a9bcf", string:"#8dc891", variable:"#d7deea", boolean:"#ff8b50", tag:"#fc929e", function:"#79b6f2", className:"#FAC863"};
var pn$$module$dist$bridge$knoxx_frontend_bridge_es = {plain:{backgroundColor:"#282c34", color:"#ffffff"}, styles:[{types:["attr-name"], style:{color:D$$module$dist$bridge$knoxx_frontend_bridge_es.keyword}}, {types:["attr-value"], style:{color:D$$module$dist$bridge$knoxx_frontend_bridge_es.string}}, {types:["comment", "block-comment", "prolog", "doctype", "cdata", "shebang"], style:{color:D$$module$dist$bridge$knoxx_frontend_bridge_es.comment}}, {types:["property", "number", "function-name", "constant", 
"symbol", "deleted"], style:{color:D$$module$dist$bridge$knoxx_frontend_bridge_es.primitive}}, {types:["boolean"], style:{color:D$$module$dist$bridge$knoxx_frontend_bridge_es.boolean}}, {types:["tag"], style:{color:D$$module$dist$bridge$knoxx_frontend_bridge_es.tag}}, {types:["string"], style:{color:D$$module$dist$bridge$knoxx_frontend_bridge_es.string}}, {types:["punctuation"], style:{color:D$$module$dist$bridge$knoxx_frontend_bridge_es.string}}, {types:["selector", "char", "builtin", "inserted"], 
style:{color:D$$module$dist$bridge$knoxx_frontend_bridge_es.char}}, {types:["function"], style:{color:D$$module$dist$bridge$knoxx_frontend_bridge_es.function}}, {types:["operator", "entity", "url", "variable"], style:{color:D$$module$dist$bridge$knoxx_frontend_bridge_es.variable}}, {types:["keyword"], style:{color:D$$module$dist$bridge$knoxx_frontend_bridge_es.keyword}}, {types:["atrule", "class-name"], style:{color:D$$module$dist$bridge$knoxx_frontend_bridge_es.className}}, {types:["important"], 
style:{fontWeight:"400"}}, {types:["bold"], style:{fontWeight:"bold"}}, {types:["italic"], style:{fontStyle:"italic"}}, {types:["namespace"], style:{opacity:0.7}}]};
var fn$$module$dist$bridge$knoxx_frontend_bridge_es = pn$$module$dist$bridge$knoxx_frontend_bridge_es;
var bn$$module$dist$bridge$knoxx_frontend_bridge_es = {plain:{color:"#f8f8f2", backgroundColor:"#272822"}, styles:[{types:["changed"], style:{color:"rgb(162, 191, 252)", fontStyle:"italic"}}, {types:["deleted"], style:{color:"#f92672", fontStyle:"italic"}}, {types:["inserted"], style:{color:"rgb(173, 219, 103)", fontStyle:"italic"}}, {types:["comment"], style:{color:"#8292a2", fontStyle:"italic"}}, {types:["string", "url"], style:{color:"#a6e22e"}}, {types:["variable"], style:{color:"#f8f8f2"}}, 
{types:["number"], style:{color:"#ae81ff"}}, {types:["builtin", "char", "constant", "function", "class-name"], style:{color:"#e6db74"}}, {types:["punctuation"], style:{color:"#f8f8f2"}}, {types:["selector", "doctype"], style:{color:"#a6e22e", fontStyle:"italic"}}, {types:["tag", "operator", "keyword"], style:{color:"#66d9ef"}}, {types:["boolean"], style:{color:"#ae81ff"}}, {types:["namespace"], style:{color:"rgb(178, 204, 214)", opacity:0.7}}, {types:["tag", "property"], style:{color:"#f92672"}}, 
{types:["attr-name"], style:{color:"#a6e22e !important"}}, {types:["doctype"], style:{color:"#8292a2"}}, {types:["rule"], style:{color:"#e6db74"}}]};
var yn$$module$dist$bridge$knoxx_frontend_bridge_es = bn$$module$dist$bridge$knoxx_frontend_bridge_es;
var mn$$module$dist$bridge$knoxx_frontend_bridge_es = {plain:{color:"#bfc7d5", backgroundColor:"#292d3e"}, styles:[{types:["comment"], style:{color:"rgb(105, 112, 152)", fontStyle:"italic"}}, {types:["string", "inserted"], style:{color:"rgb(195, 232, 141)"}}, {types:["number"], style:{color:"rgb(247, 140, 108)"}}, {types:["builtin", "char", "constant", "function"], style:{color:"rgb(130, 170, 255)"}}, {types:["punctuation", "selector"], style:{color:"rgb(199, 146, 234)"}}, {types:["variable"], style:{color:"rgb(191, 199, 213)"}}, 
{types:["class-name", "attr-name"], style:{color:"rgb(255, 203, 107)"}}, {types:["tag", "deleted"], style:{color:"rgb(255, 85, 114)"}}, {types:["operator"], style:{color:"rgb(137, 221, 255)"}}, {types:["boolean"], style:{color:"rgb(255, 88, 116)"}}, {types:["keyword"], style:{fontStyle:"italic"}}, {types:["doctype"], style:{color:"rgb(199, 146, 234)", fontStyle:"italic"}}, {types:["namespace"], style:{color:"rgb(178, 204, 214)"}}, {types:["url"], style:{color:"rgb(221, 221, 221)"}}]};
var hn$$module$dist$bridge$knoxx_frontend_bridge_es = mn$$module$dist$bridge$knoxx_frontend_bridge_es;
var xn$$module$dist$bridge$knoxx_frontend_bridge_es = {plain:{color:"#9EFEFF", backgroundColor:"#2D2A55"}, styles:[{types:["changed"], style:{color:"rgb(255, 238, 128)"}}, {types:["deleted"], style:{color:"rgba(239, 83, 80, 0.56)"}}, {types:["inserted"], style:{color:"rgb(173, 219, 103)"}}, {types:["comment"], style:{color:"rgb(179, 98, 255)", fontStyle:"italic"}}, {types:["punctuation"], style:{color:"rgb(255, 255, 255)"}}, {types:["constant"], style:{color:"rgb(255, 98, 140)"}}, {types:["string", 
"url"], style:{color:"rgb(165, 255, 144)"}}, {types:["variable"], style:{color:"rgb(255, 238, 128)"}}, {types:["number", "boolean"], style:{color:"rgb(255, 98, 140)"}}, {types:["attr-name"], style:{color:"rgb(255, 180, 84)"}}, {types:["keyword", "operator", "property", "namespace", "tag", "selector", "doctype"], style:{color:"rgb(255, 157, 0)"}}, {types:["builtin", "char", "constant", "function", "class-name"], style:{color:"rgb(250, 208, 0)"}}]};
var kn$$module$dist$bridge$knoxx_frontend_bridge_es = xn$$module$dist$bridge$knoxx_frontend_bridge_es;
var Sn$$module$dist$bridge$knoxx_frontend_bridge_es = {plain:{backgroundColor:"linear-gradient(to bottom, #2a2139 75%, #34294f)", backgroundImage:"#34294f", color:"#f92aad", textShadow:"0 0 2px #100c0f, 0 0 5px #dc078e33, 0 0 10px #fff3"}, styles:[{types:["comment", "block-comment", "prolog", "doctype", "cdata"], style:{color:"#495495", fontStyle:"italic"}}, {types:["punctuation"], style:{color:"#ccc"}}, {types:["tag", "attr-name", "namespace", "number", "unit", "hexcode", "deleted"], style:{color:"#e2777a"}}, 
{types:["property", "selector"], style:{color:"#72f1b8", textShadow:"0 0 2px #100c0f, 0 0 10px #257c5575, 0 0 35px #21272475"}}, {types:["function-name"], style:{color:"#6196cc"}}, {types:["boolean", "selector-id", "function"], style:{color:"#fdfdfd", textShadow:"0 0 2px #001716, 0 0 3px #03edf975, 0 0 5px #03edf975, 0 0 8px #03edf975"}}, {types:["class-name", "maybe-class-name", "builtin"], style:{color:"#fff5f6", textShadow:"0 0 2px #000, 0 0 10px #fc1f2c75, 0 0 5px #fc1f2c75, 0 0 25px #fc1f2c75"}}, 
{types:["constant", "symbol"], style:{color:"#f92aad", textShadow:"0 0 2px #100c0f, 0 0 5px #dc078e33, 0 0 10px #fff3"}}, {types:["important", "atrule", "keyword", "selector-class"], style:{color:"#f4eee4", textShadow:"0 0 2px #393a33, 0 0 8px #f39f0575, 0 0 2px #f39f0575"}}, {types:["string", "char", "attr-value", "regex", "variable"], style:{color:"#f87c32"}}, {types:["parameter"], style:{fontStyle:"italic"}}, {types:["entity", "url"], style:{color:"#67cdcc"}}, {types:["operator"], style:{color:"ffffffee"}}, 
{types:["important", "bold"], style:{fontWeight:"bold"}}, {types:["italic"], style:{fontStyle:"italic"}}, {types:["entity"], style:{cursor:"help"}}, {types:["inserted"], style:{color:"green"}}]};
var vn$$module$dist$bridge$knoxx_frontend_bridge_es = Sn$$module$dist$bridge$knoxx_frontend_bridge_es;
var wn$$module$dist$bridge$knoxx_frontend_bridge_es = {plain:{color:"#282a2e", backgroundColor:"#ffffff"}, styles:[{types:["comment"], style:{color:"rgb(197, 200, 198)"}}, {types:["string", "number", "builtin", "variable"], style:{color:"rgb(150, 152, 150)"}}, {types:["class-name", "function", "tag", "attr-name"], style:{color:"rgb(40, 42, 46)"}}]};
var Fn$$module$dist$bridge$knoxx_frontend_bridge_es = wn$$module$dist$bridge$knoxx_frontend_bridge_es;
var En$$module$dist$bridge$knoxx_frontend_bridge_es = {plain:{color:"#9CDCFE", backgroundColor:"#1E1E1E"}, styles:[{types:["prolog"], style:{color:"rgb(0, 0, 128)"}}, {types:["comment"], style:{color:"rgb(106, 153, 85)"}}, {types:["builtin", "changed", "keyword", "interpolation-punctuation"], style:{color:"rgb(86, 156, 214)"}}, {types:["number", "inserted"], style:{color:"rgb(181, 206, 168)"}}, {types:["constant"], style:{color:"rgb(100, 102, 149)"}}, {types:["attr-name", "variable"], style:{color:"rgb(156, 220, 254)"}}, 
{types:["deleted", "string", "attr-value", "template-punctuation"], style:{color:"rgb(206, 145, 120)"}}, {types:["selector"], style:{color:"rgb(215, 186, 125)"}}, {types:["tag"], style:{color:"rgb(78, 201, 176)"}}, {types:["tag"], languages:["markup"], style:{color:"rgb(86, 156, 214)"}}, {types:["punctuation", "operator"], style:{color:"rgb(212, 212, 212)"}}, {types:["punctuation"], languages:["markup"], style:{color:"#808080"}}, {types:["function"], style:{color:"rgb(220, 220, 170)"}}, {types:["class-name"], 
style:{color:"rgb(78, 201, 176)"}}, {types:["char"], style:{color:"rgb(209, 105, 105)"}}]};
var _n$$module$dist$bridge$knoxx_frontend_bridge_es = En$$module$dist$bridge$knoxx_frontend_bridge_es;
var An$$module$dist$bridge$knoxx_frontend_bridge_es = {plain:{color:"#000000", backgroundColor:"#ffffff"}, styles:[{types:["comment"], style:{color:"rgb(0, 128, 0)"}}, {types:["builtin"], style:{color:"rgb(0, 112, 193)"}}, {types:["number", "variable", "inserted"], style:{color:"rgb(9, 134, 88)"}}, {types:["operator"], style:{color:"rgb(0, 0, 0)"}}, {types:["constant", "char"], style:{color:"rgb(129, 31, 63)"}}, {types:["tag"], style:{color:"rgb(128, 0, 0)"}}, {types:["attr-name"], style:{color:"rgb(255, 0, 0)"}}, 
{types:["deleted", "string"], style:{color:"rgb(163, 21, 21)"}}, {types:["changed", "punctuation"], style:{color:"rgb(4, 81, 165)"}}, {types:["function", "keyword"], style:{color:"rgb(0, 0, 255)"}}, {types:["class-name"], style:{color:"rgb(38, 127, 153)"}}]};
var Cn$$module$dist$bridge$knoxx_frontend_bridge_es = An$$module$dist$bridge$knoxx_frontend_bridge_es;
var $n$$module$dist$bridge$knoxx_frontend_bridge_es = {plain:{color:"#f8fafc", backgroundColor:"#011627"}, styles:[{types:["prolog"], style:{color:"#000080"}}, {types:["comment"], style:{color:"#6A9955"}}, {types:["builtin", "changed", "keyword", "interpolation-punctuation"], style:{color:"#569CD6"}}, {types:["number", "inserted"], style:{color:"#B5CEA8"}}, {types:["constant"], style:{color:"#f8fafc"}}, {types:["attr-name", "variable"], style:{color:"#9CDCFE"}}, {types:["deleted", "string", "attr-value", 
"template-punctuation"], style:{color:"#cbd5e1"}}, {types:["selector"], style:{color:"#D7BA7D"}}, {types:["tag"], style:{color:"#0ea5e9"}}, {types:["tag"], languages:["markup"], style:{color:"#0ea5e9"}}, {types:["punctuation", "operator"], style:{color:"#D4D4D4"}}, {types:["punctuation"], languages:["markup"], style:{color:"#808080"}}, {types:["function"], style:{color:"#7dd3fc"}}, {types:["class-name"], style:{color:"#0ea5e9"}}, {types:["char"], style:{color:"#D16969"}}]};
var zn$$module$dist$bridge$knoxx_frontend_bridge_es = $n$$module$dist$bridge$knoxx_frontend_bridge_es;
var In$$module$dist$bridge$knoxx_frontend_bridge_es = {plain:{color:"#0f172a", backgroundColor:"#f1f5f9"}, styles:[{types:["prolog"], style:{color:"#000080"}}, {types:["comment"], style:{color:"#6A9955"}}, {types:["builtin", "changed", "keyword", "interpolation-punctuation"], style:{color:"#0c4a6e"}}, {types:["number", "inserted"], style:{color:"#B5CEA8"}}, {types:["constant"], style:{color:"#0f172a"}}, {types:["attr-name", "variable"], style:{color:"#0c4a6e"}}, {types:["deleted", "string", "attr-value", 
"template-punctuation"], style:{color:"#64748b"}}, {types:["selector"], style:{color:"#D7BA7D"}}, {types:["tag"], style:{color:"#0ea5e9"}}, {types:["tag"], languages:["markup"], style:{color:"#0ea5e9"}}, {types:["punctuation", "operator"], style:{color:"#475569"}}, {types:["punctuation"], languages:["markup"], style:{color:"#808080"}}, {types:["function"], style:{color:"#0e7490"}}, {types:["class-name"], style:{color:"#0ea5e9"}}, {types:["char"], style:{color:"#D16969"}}]};
var Tn$$module$dist$bridge$knoxx_frontend_bridge_es = In$$module$dist$bridge$knoxx_frontend_bridge_es;
var Rn$$module$dist$bridge$knoxx_frontend_bridge_es = {plain:{backgroundColor:"hsl(220, 13%, 18%)", color:"hsl(220, 14%, 71%)", textShadow:"0 1px rgba(0, 0, 0, 0.3)"}, styles:[{types:["comment", "prolog", "cdata"], style:{color:"hsl(220, 10%, 40%)"}}, {types:["doctype", "punctuation", "entity"], style:{color:"hsl(220, 14%, 71%)"}}, {types:["attr-name", "class-name", "maybe-class-name", "boolean", "constant", "number", "atrule"], style:{color:"hsl(29, 54%, 61%)"}}, {types:["keyword"], style:{color:"hsl(286, 60%, 67%)"}}, 
{types:["property", "tag", "symbol", "deleted", "important"], style:{color:"hsl(355, 65%, 65%)"}}, {types:["selector", "string", "char", "builtin", "inserted", "regex", "attr-value"], style:{color:"hsl(95, 38%, 62%)"}}, {types:["variable", "operator", "function"], style:{color:"hsl(207, 82%, 66%)"}}, {types:["url"], style:{color:"hsl(187, 47%, 55%)"}}, {types:["deleted"], style:{textDecorationLine:"line-through"}}, {types:["inserted"], style:{textDecorationLine:"underline"}}, {types:["italic"], style:{fontStyle:"italic"}}, 
{types:["important", "bold"], style:{fontWeight:"bold"}}, {types:["important"], style:{color:"hsl(220, 14%, 71%)"}}]};
var Dn$$module$dist$bridge$knoxx_frontend_bridge_es = Rn$$module$dist$bridge$knoxx_frontend_bridge_es;
var Ln$$module$dist$bridge$knoxx_frontend_bridge_es = {plain:{backgroundColor:"hsl(230, 1%, 98%)", color:"hsl(230, 8%, 24%)"}, styles:[{types:["comment", "prolog", "cdata"], style:{color:"hsl(230, 4%, 64%)"}}, {types:["doctype", "punctuation", "entity"], style:{color:"hsl(230, 8%, 24%)"}}, {types:["attr-name", "class-name", "boolean", "constant", "number", "atrule"], style:{color:"hsl(35, 99%, 36%)"}}, {types:["keyword"], style:{color:"hsl(301, 63%, 40%)"}}, {types:["property", "tag", "symbol", "deleted", 
"important"], style:{color:"hsl(5, 74%, 59%)"}}, {types:["selector", "string", "char", "builtin", "inserted", "regex", "attr-value", "punctuation"], style:{color:"hsl(119, 34%, 47%)"}}, {types:["variable", "operator", "function"], style:{color:"hsl(221, 87%, 60%)"}}, {types:["url"], style:{color:"hsl(198, 99%, 37%)"}}, {types:["deleted"], style:{textDecorationLine:"line-through"}}, {types:["inserted"], style:{textDecorationLine:"underline"}}, {types:["italic"], style:{fontStyle:"italic"}}, {types:["important", 
"bold"], style:{fontWeight:"bold"}}, {types:["important"], style:{color:"hsl(230, 8%, 24%)"}}]};
var On$$module$dist$bridge$knoxx_frontend_bridge_es = Ln$$module$dist$bridge$knoxx_frontend_bridge_es;
var Bn$$module$dist$bridge$knoxx_frontend_bridge_es = {plain:{color:"#ebdbb2", backgroundColor:"#292828"}, styles:[{types:["imports", "class-name", "maybe-class-name", "constant", "doctype", "builtin", "function"], style:{color:"#d8a657"}}, {types:["property-access"], style:{color:"#7daea3"}}, {types:["tag"], style:{color:"#e78a4e"}}, {types:["attr-name", "char", "url", "regex"], style:{color:"#a9b665"}}, {types:["attr-value", "string"], style:{color:"#89b482"}}, {types:["comment", "prolog", "cdata", 
"operator", "inserted"], style:{color:"#a89984"}}, {types:["delimiter", "boolean", "keyword", "selector", "important", "atrule", "property", "variable", "deleted"], style:{color:"#ea6962"}}, {types:["entity", "number", "symbol"], style:{color:"#d3869b"}}]};
var Nn$$module$dist$bridge$knoxx_frontend_bridge_es = Bn$$module$dist$bridge$knoxx_frontend_bridge_es;
var jn$$module$dist$bridge$knoxx_frontend_bridge_es = {plain:{color:"#654735", backgroundColor:"#f9f5d7"}, styles:[{types:["delimiter", "boolean", "keyword", "selector", "important", "atrule", "property", "variable", "deleted"], style:{color:"#af2528"}}, {types:["imports", "class-name", "maybe-class-name", "constant", "doctype", "builtin"], style:{color:"#b4730e"}}, {types:["string", "attr-value"], style:{color:"#477a5b"}}, {types:["property-access"], style:{color:"#266b79"}}, {types:["function", 
"attr-name", "char", "url"], style:{color:"#72761e"}}, {types:["tag"], style:{color:"#b94c07"}}, {types:["comment", "prolog", "cdata", "operator", "inserted"], style:{color:"#a89984"}}, {types:["entity", "number", "symbol"], style:{color:"#924f79"}}]};
var Pn$$module$dist$bridge$knoxx_frontend_bridge_es = jn$$module$dist$bridge$knoxx_frontend_bridge_es;
oe$$module$dist$bridge$knoxx_frontend_bridge_es.vsDark, oe$$module$dist$bridge$knoxx_frontend_bridge_es.nightOwl, oe$$module$dist$bridge$knoxx_frontend_bridge_es.vsDark;
t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.surface, `${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`, `${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.elevated, t$$module$dist$bridge$knoxx_frontend_bridge_es.shadow.md, t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.background.elevated, t$$module$dist$bridge$knoxx_frontend_bridge_es.shadow.md;
t$$module$dist$bridge$knoxx_frontend_bridge_es.fontFamily.sans;
`${t$$module$dist$bridge$knoxx_frontend_bridge_es.colors.border.default}`;
function Zn$$module$dist$bridge$knoxx_frontend_bridge_es({title:e, message:r, actionLabel:a, onAction:c, icon:d = "\ud83d\udced"}) {
  return (0,shadow.esm.esm_import$react$jsx_runtime.jsxs)("div", {style:{display:"flex", flexDirection:"column", alignItems:"center", justifyContent:"center", padding:"2rem", textAlign:"center", gap:"1rem"}, children:[(0,shadow.esm.esm_import$react$jsx_runtime.jsx)("div", {style:{fontSize:"3rem", lineHeight:1, opacity:0.6}, children:d}), (0,shadow.esm.esm_import$react$jsx_runtime.jsxs)("div", {children:[(0,shadow.esm.esm_import$react$jsx_runtime.jsx)("h3", {style:{margin:"0 0 0.5rem 0", fontSize:"1.125rem", 
  fontWeight:600, color:"var(--token-colors-text-default)"}, children:e}), (0,shadow.esm.esm_import$react$jsx_runtime.jsx)("p", {style:{margin:0, fontSize:"0.875rem", color:"var(--token-colors-text-muted)", maxWidth:"24rem"}, children:r})]}), a && c && (0,shadow.esm.esm_import$react$jsx_runtime.jsx)(Ce$$module$dist$bridge$knoxx_frontend_bridge_es, {variant:"primary", onClick:c, children:a})]});
}
/** @const */ 
var module$dist$bridge$knoxx_frontend_bridge_es = {};
/** @const */ 
module$dist$bridge$knoxx_frontend_bridge_es.EmptyState = Zn$$module$dist$bridge$knoxx_frontend_bridge_es;

$CLJS.module$dist$bridge$knoxx_frontend_bridge_es=module$dist$bridge$knoxx_frontend_bridge_es;
//# sourceMappingURL=module$dist$bridge$knoxx_frontend_bridge_es.js.map
