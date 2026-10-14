try{ document.getElementById("favicon").href=document.querySelector(".back img").src; }catch(e){}
/* ================= Палитры — единый источник для карточек, preview, команд и сохранения ================= */
const PALETTES = [
  {name:"fspirat",         colors:["7FBF3A","D4FF9A"]},
  {name:"Особая",          colors:["B04DFF","FF8FE0"]},
  {name:"Закат",           colors:["FAE8F3","FF8F5A"]},
  {name:"Пламя",           colors:["FFE259","FF7A00","D10000"]},
  {name:"Океан",           colors:["00F0FF","0066FF"]},
  {name:"Изумруд",         colors:["C6FF8A","1FAA59"]},
  {name:"Лёд",             colors:["FFFFFF","9BE7FF","4A9BFF"]},
  {name:"Эндер",           colors:["1B0033","8A2BE2","E0B0FF"]},
  {name:"Золото",          colors:["FFF6B7","FFC300","B8860B"]},
  {name:"Радуга",          colors:["FF4D4D","FFD24D","4DFF88","4DB8FF","B84DFF"]},
  {name:"Легендарная",     colors:["FFB000","FF6A00"]},
  {name:"Мифическая",      colors:["FF3CAC","784BA0","2B86C5"]},
  {name:"Сакура",          colors:["FFD1E8","FF7EB9"]},
  {name:"Северное сияние", colors:["43E97B","38F9D7","7F7FFF"]},
  {name:"Незер",           colors:["FF4E00","7A0000"]},
  {name:"Неон",            colors:["00FFF0","FF00E5"]},
  {name:"Киберпанк",       colors:["FCEE09","FF2A6D","05D9E8"]},
  // Новые палитры — идут после исходных 17.
  {name:"Персик",          colors:["FFE0C2","FF9A8B"]},
  {name:"Янтарь",          colors:["FFD86F","FC6262"]},
  {name:"Медь",            colors:["FFB88C","DE6262"]},
  {name:"Кровь",           colors:["FF5E62","8E0E00"]},
  {name:"Аква",            colors:["13F1FC","0470DC"]},
  {name:"Сапфир",          colors:["6DD5FA","2A5298","1E3C72"]},
  {name:"Иней",            colors:["FFFFFF","B8E2FF"]},
  {name:"Полночь",         colors:["4CA1AF","2C3E50"]},
  {name:"Лес",             colors:["A8E063","56AB2F"]},
  {name:"Мята",            colors:["D4FFEC","3EECAC"]},
  {name:"Лайм",            colors:["EAFF00","6BFF00"]},
  {name:"Джунгли",         colors:["93F9B9","1D976C"]},
  {name:"Токсик",          colors:["B6FF00","00FF87"]},
  {name:"Плазма",          colors:["7F00FF","E100FF"]},
  {name:"Электро",         colors:["00C3FF","FFFF1C"]},
  {name:"Вейпорвейв",      colors:["FF71CE","01CDFE","05FFA1"]},
  {name:"Зефир",           colors:["FBC2EB","A6C1EE"]},
  {name:"Лаванда",         colors:["E0C3FC","8EC5FC"]},
  {name:"Сладость",        colors:["FFDEE9","B5FFFC"]},
  {name:"Аметист",         colors:["E4B0FF","9D4EDD","5A189A"]},
  {name:"Пустота",         colors:["1CB5E0","000046"]},
  {name:"Серебро",         colors:["FFFFFF","BDC3C7","7F8C8D"]},
  {name:"Тень",            colors:["8E9EAB","3A3A3A"]},
  {name:"Алмаз",           colors:["D6FFFB","4AEDD9","1AA39A"]},
  {name:"Незерит",         colors:["A59A9A","5A4E4E","2E2828"]},
  {name:"Редстоун",        colors:["FF4D4D","D10000","6B0000"]},
  {name:"Лазурит",         colors:["5B8CFF","2346C4","102A78"]},
  {name:"Опыт",            colors:["F4FF6B","7CFC00","3FA000"]},
  {name:"Слизь",           colors:["B8FF9C","6FD65B","3E9E3A"]},
  {name:"Призмарин",       colors:["B5FFE6","5FC9B2","2E7F74"]},
  {name:"Хорус",           colors:["E8C6FF","A374C9","5E3F7A"]},
  {name:"Искажённый лес",  colors:["5CFFE0","14B485","0B4F4F"]},
  {name:"Багровый лес",    colors:["FF8A8A","B52B2B","5C0F1C"]},
  {name:"Скалк",           colors:["3FE6FF","0D6B7A","051A24"]},
  {name:"Огонь душ",       colors:["C8FFFF","38E8F2","0F8C9E"]},
  {name:"Патина",          colors:["E8A07C","5FAF9A","3E8C7E"]},
  {name:"Светокамень",     colors:["FFF7B0","FFCF4A","C78A1F"]},
  {name:"Портал",          colors:["D88CFF","8A2BE2","3A0078"]},
  {name:"Апельсин",        colors:["FFC371","FF5F6D"]},
  {name:"Мандарин",        colors:["FFB347","FF7B00"]},
  {name:"Карамель",        colors:["F5D7A1","C68B59"]},
  {name:"Коралл",          colors:["FFB199","FF0844"]},
  {name:"Вулкан",          colors:["FFD000","FF4000","5A0000"]},
  {name:"Рассвет",         colors:["FFF1A8","FFA9B8","A18CD1"]},
  {name:"Ледник",          colors:["E0FFFF","7FD4FF","3A7BD5"]},
  {name:"Бриз",            colors:["A8EDEA","70B8FF"]},
  {name:"Штиль",           colors:["C9FFE5","6DD5ED"]},
  {name:"Глубина",         colors:["00B4DB","003366"]},
  {name:"Небо",            colors:["87CEFA","1E90FF"]},
  {name:"Весна",           colors:["FDFC47","24FE41"]},
  {name:"Осень",           colors:["F7B733","FC4A1A","7A1E0A"]},
  {name:"Мох",             colors:["C2D97A","5B7F3A"]},
  {name:"Цветение",        colors:["FFE3F1","FF9CCB","B4F8C8"]},
  {name:"Море",            colors:["43C6AC","191654"]},
  {name:"Кислота",         colors:["F9FF00","00FFA3"]},
  {name:"Фуксия",          colors:["FF00CC","333399"]},
  {name:"Диско",           colors:["FF0080","FFD700","00E5FF"]},
  {name:"Синтвейв",        colors:["F72585","7209B7","3A0CA3","4CC9F0"]},
  {name:"Лава-лампа",      colors:["FF6FD8","FFB36B"]},
  {name:"Пастельная радуга", colors:["FFB3BA","FFDFBA","FFFFBA","BAFFC9","BAE1FF"]},
  {name:"Мороженое",       colors:["FFF5D1","FFC4D6","C1F0FF"]},
  {name:"Персиковый чай",  colors:["FFE5B4","FFB5A7"]},
  {name:"Сирень",          colors:["F3E5F5","CE93D8"]},
  {name:"Мятный крем",     colors:["E0FFF4","A8E6CF"]},
  {name:"Кровавая луна",   colors:["FF3D3D","6B0000","1A0000"]},
  {name:"Бездна",          colors:["8A2BE2","4B0082","1A0033"]},
  {name:"Графит",          colors:["B0B0B0","505050","1E1E1E"]},
  {name:"Ночной город",    colors:["F72585","3A0CA3","0B0033"]},
  {name:"Яд",              colors:["9DFF00","2F6B00","0A1F00"]},
  {name:"Чёрно-белый",     colors:["FFFFFF","000000"]},
];
/** Категории для фильтра. Палитра без категории видна только во «Все». */
const CATS=[["all","Все"],["mc","Minecraft"],["warm","Тёплые"],["cold","Холодные"],["nature","Природа"],["bright","Яркие"],["pastel","Пастель"],["dark","Тёмные"]];
const PAL_CAT={
  "fspirat":"nature","Особая":"bright","Закат":"warm","Пламя":"warm","Океан":"cold","Изумруд":"nature","Лёд":"cold","Эндер":"dark",
  "Золото":"warm","Радуга":"bright","Легендарная":"warm","Мифическая":"bright","Сакура":"pastel","Северное сияние":"cold",
  "Незер":"dark","Неон":"bright","Киберпанк":"bright",
  "Персик":"pastel","Янтарь":"warm","Медь":"warm","Кровь":"dark","Аква":"cold","Сапфир":"cold","Иней":"cold","Полночь":"dark",
  "Лес":"nature","Мята":"nature","Лайм":"nature","Джунгли":"nature","Токсик":"bright","Плазма":"bright","Электро":"bright",
  "Вейпорвейв":"bright","Зефир":"pastel","Лаванда":"pastel","Сладость":"pastel","Аметист":"dark","Пустота":"dark","Серебро":"cold","Тень":"dark",
  "Алмаз":"mc","Незерит":"mc","Редстоун":"mc","Лазурит":"mc","Опыт":"mc","Слизь":"mc","Призмарин":"mc","Хорус":"mc","Искажённый лес":"mc","Багровый лес":"mc","Скалк":"mc","Огонь душ":"mc","Патина":"mc","Светокамень":"mc","Портал":"mc","Апельсин":"warm","Мандарин":"warm","Карамель":"warm","Коралл":"warm","Вулкан":"warm","Рассвет":"warm","Ледник":"cold","Бриз":"cold","Штиль":"cold","Глубина":"cold","Небо":"cold","Весна":"nature","Осень":"nature","Мох":"nature","Цветение":"nature","Море":"nature","Кислота":"bright","Фуксия":"bright","Диско":"bright","Синтвейв":"bright","Лава-лампа":"bright","Пастельная радуга":"pastel","Мороженое":"pastel","Персиковый чай":"pastel","Сирень":"pastel","Мятный крем":"pastel","Кровавая луна":"dark","Бездна":"dark","Графит":"dark","Ночной город":"dark","Яд":"dark","Чёрно-белый":"dark"
};
const COLLAPSED=18;   // сколько палитр видно во «Все», пока список свёрнут
const DEFAULT_TPL="&#$1$2$3$4$5$6$f$c";
const MAX_LORE=20;
const SP_PREFIX="sponsor";
/* Какие поля нужны каждому формату */
const isClassic = () => S.cmd.startsWith("c:");
/** Формат без пометки режима: c:item → item, c:raw → am-raw (текст без команды). */
const base = () => { if(!isClassic()) return S.cmd; const b=S.cmd.slice(2); return b==="raw"?"am-raw":b; };
const isSponsor = () => S.cmd==="sponsor";
const isSponsorEdit = () => S.cmd==="sponsoredit";
const otherKind = () => S.cmd.startsWith("fmt:") ? S.cmd.slice(4) : null;
const showName = () => base()!=="/itemlore" && !isSponsor();
const showLore = () => !["/itemname","sponsor","sponsoredit"].includes(base());
const minStops = () => isSponsor()?1:2;
const maxStops = () => isSponsor()?7:6;

/* ================= Цвет: та же логика, что в HEX-генераторе ================= */
const $ = id => document.getElementById(id);
/** Анонимная статистика сайта (js/stats.js): только действия, без введённых текстов. */
const track = (e,d) => { try{ if(window.fsTrack) window.fsTrack(e,d); }catch(_){} };
const clean = h => (h||"").replace(/[^0-9a-f]/gi,"").slice(0,6).toUpperCase();
const valid = h => /^[0-9A-F]{6}$/.test(h);
const toRgb = h => [0,2,4].map(i=>parseInt(h.slice(i,i+2),16));
const mix = (a,b,t) => { const A=toRgb(a),B=toRgb(b);
  return A.map((v,i)=>Math.round(v+(B[i]-v)*t).toString(16).padStart(2,"0")).join("").toUpperCase(); };
const shade = h => "#"+toRgb(h).map(v=>Math.floor(v/4).toString(16).padStart(2,"0")).join("");
const css = cols => cols.length===1 ? "#"+cols[0] : `linear-gradient(90deg,${cols.map(c=>"#"+c).join(",")})`;
function segments(chars,n){
  const parts=n-1, out=[];
  for(let k=0;k<parts;k++){
    const a=Math.round(chars.length*k/parts), b=Math.round(chars.length*(k+1)/parts);
    out.push(chars.slice(a,b));
  }
  return out;
}
/** Цвет каждого символа: текст делится на равные части, каждая переливается к следующему цвету (как в генераторе). */
function glyphColors(text,style){
  const chars=[...text];
  if(isClassic()){
    const cs=legOf(style); if(!cs.length) return chars.map(c=>({c,hex:"FFFFFF"}));
    if(cs.length===1) return chars.map(c=>({c,hex:LEG[cs[0]]}));
    const out=[]; segments(chars,cs.length+1).forEach((seg,k)=>seg.forEach(c=>out.push({c,hex:LEG[cs[k]]}))); return out;
  }
  if(style.mode==="solid") return chars.map(c=>({c,hex:style.stops[0]}));
  const st=style.stops, out=[];
  segments(chars,st.length).forEach((seg,k)=>{
    const n=seg.length;
    seg.forEach((c,i)=>out.push({c,hex:mix(st[k],st[k+1],n>1?i/(n-1):0)}));
  });
  return out;
}
/* Классические цвета Minecraft (/colors): код → HEX и название */
const LEG={"0":"000000","1":"0000AA","2":"00AA00","3":"00AAAA","4":"AA0000","5":"AA00AA","6":"FFAA00","7":"AAAAAA",
  "8":"555555","9":"5555FF","a":"55FF55","b":"55FFFF","c":"FF5555","d":"FF55FF","e":"FFFF55","f":"FFFFFF"};
const LEG_NAME={"0":"Чёрный","1":"Тёмно-синий","2":"Тёмно-зелёный","3":"Бирюзовый","4":"Тёмно-красный","5":"Фиолетовый","6":"Золотой","7":"Серый",
  "8":"Тёмно-серый","9":"Синий","a":"Зелёный","b":"Голубой","c":"Красный","d":"Розовый","e":"Жёлтый","f":"Белый"};
const LEG_MAX=8;
const CLASSIC=[
  {name:"Радуга",codes:"c6eab9d"},{name:"Огонь",codes:"4c6e"},{name:"Пламя",codes:"e6c"},{name:"Океан",codes:"19b"},
  {name:"Лёд",codes:"fb3"},{name:"Небо",codes:"bf"},{name:"Лес",codes:"2a"},{name:"Мята",codes:"ab"},
  {name:"Закат",codes:"5dc6"},{name:"Золото",codes:"6e"},{name:"Солнце",codes:"ef"},{name:"Неон",codes:"db"},
  {name:"Эндер",codes:"5d"},{name:"Кровь",codes:"4c"},{name:"Ночь",codes:"87f"},{name:"Тень",codes:"087"},
].map(p=>({name:p.name,codes:[...p.codes],colors:[...p.codes].map(c=>LEG[c])}));
const nearestLeg = h => { const A=toRgb(h); let best="f",bd=1e9;
  for(const [c,hx] of Object.entries(LEG)){ const B=toRgb(hx), d=A.reduce((s,v,i)=>s+(v-B[i])**2,0); if(d<bd){bd=d;best=c;} } return best; };
/** Коды /colors для цели: если их ещё нет — берём ближайшие к её HEX-цветам. */
function legOf(st){
  if(!Array.isArray(st.legacy)||!st.legacy.every(c=>LEG[c])){
    const src=(st.mode==="solid"?[st.stops[0]]:st.stops).filter(valid).map(nearestLeg);
    st.legacy=src.filter((c,i)=>i===0||c!==src[i-1]).slice(0,LEG_MAX);
    if(!st.legacy.length) st.legacy=["f"];
  }
  return st.legacy;
}
/** Резкие границы между цветами — так классические цвета и выглядят в игре. */
const cssHard = codes => codes.length===1 ? "#"+LEG[codes[0]] :
  "linear-gradient(90deg,"+codes.map((c,i)=>`#${LEG[c]} ${i/codes.length*100}%,#${LEG[c]} ${(i+1)/codes.length*100}%`).join(",")+")";
function legacyName(st){
  const cs=legOf(st); if(!cs.length) return "Цвета не выбраны";
  const p=CLASSIC.find(p=>p.codes.join("")===cs.join(""));
  return p ? p.name : cs.length===1 ? LEG_NAME[cs[0]] : "Свои цвета";
}
/** Соседние символы одного цвета склеиваются — команда короче. */
function runs(glyphs){
  const out=[];
  glyphs.forEach(g=>{ const last=out[out.length-1];
    if(last&&(last.hex===g.hex||g.c===" ")) last.t+=g.c; else out.push({t:g.c,hex:g.hex}); });
  return out;
}

/* ================= Состояние ================= */
const newStyle = (colors,mode) => ({mode:mode||"gradient",stops:[...colors],fmt:{l:false,o:false,n:false,m:false,k:false}});
const DEFAULT_STATE = () => ({
  cmd:"item",font:"normal",cmdName:"",cmdLore:"",tpl:DEFAULT_TPL,
  name:{text:"Пример текста",style:newStyle(PALETTES[0].colors)},
  lore:[],
  sponsor:{text:SP_PREFIX,style:newStyle(PALETTES[0].colors)},
  nick:"nickname",nickHex:"D4FF9A",
  nickT:{text:"",style:newStyle(["AAAAAA","FFFFFF"])},   // градиент ника для /sponsor editprefix
});
const store={
  get(k,d){ try{ const v=localStorage.getItem(k); return v===null?d:v; }catch(e){ return d; } },
  set(k,v){ try{ localStorage.setItem(k,v); return true; }catch(e){ return false; } }
};
const STATE_KEY="fspirat_item_editor", TPL_KEY="fspirat_hex_templates"; // свои палитры общие с HEX-генератором
let S;
try{ S=JSON.parse(store.get(STATE_KEY,"null")); }catch(e){ S=null; }
if(!S||!S.name||!Array.isArray(S.lore)) S=DEFAULT_STATE();
{ const d=DEFAULT_STATE(); for(const k in d) if(S[k]===undefined||typeof S[k]!==typeof d[k]) S[k]=d[k]; }
/* Испорченные вложенные поля (стиль без fmt, строка без style и т. п.) чиним на месте: добавляем только недостающее,
   остальное сохранённое состояние не трогаем. Раньше редактор падал с TypeError и оставался пустым. */
function fixStyle(st,colors){
  if(!st||typeof st!=="object"||Array.isArray(st)) return newStyle(colors);
  if(!Array.isArray(st.stops)||!st.stops.length) st.stops=[...colors];
  if(!st.fmt||typeof st.fmt!=="object"||Array.isArray(st.fmt)) st.fmt={l:false,o:false,n:false,m:false,k:false};
  return st;
}
function fixText(t,colors){
  if(!t||typeof t!=="object"||Array.isArray(t)) t={text:typeof t==="string"?t:""};
  if(typeof t.text!=="string") t.text="";
  t.style=fixStyle(t.style,colors);
  return t;
}
S.name=fixText(S.name,PALETTES[0].colors); S.sponsor=fixText(S.sponsor,PALETTES[0].colors); S.nickT=fixText(S.nickT,["AAAAAA","FFFFFF"]);
S.lore=S.lore.map(ln=>fixText(ln,PALETTES[5].colors));
let active="name"; // "name", "sponsor" или индекс строки описания
let mine=[];
try{ mine=JSON.parse(store.get(TPL_KEY,"[]")); if(!Array.isArray(mine)) mine=[]; }catch(e){ mine=[]; }
// Палитры старого генератора могли сохраниться со строчными буквами HEX («ff0000») — приводим к заглавным, а не выбрасываем.
mine.forEach(p=>{ if(p&&Array.isArray(p.c)&&!p.c.every(valid)&&p.c.every(c=>typeof c==="string"&&valid(c.toUpperCase()))) p.c=p.c.map(c=>c.toUpperCase()); });
mine=mine.filter(p=>p&&typeof p.n==="string"&&Array.isArray(p.c)&&p.c.every(valid));
const allPalettes = () => PALETTES.concat(mine.map(p=>({name:p.n,colors:p.c,own:true})));
const targetObj = t => t==="name" ? S.name : t==="sponsor" ? S.sponsor : t==="nick" ? S.nickT : S.lore[t];
const cur = () => targetObj(active).style;
function paletteName(style){
  if(style.mode==="solid") return "Один цвет #"+style.stops[0];
  const p=allPalettes().find(p=>p.colors.length===style.stops.length&&p.colors.every((c,i)=>c===style.stops[i]));
  return p ? p.name : "Свой градиент";
}
const save = () => store.set(STATE_KEY,JSON.stringify(S));

/* ================= Карточки палитр ================= */
function card(p,onPick,isActive){
  const b=document.createElement("button"); b.type="button"; b.className="pcard"+(isActive?" active":"");
  const bar=document.createElement("span"); bar.className="bar"; bar.style.background=p.codes?cssHard(p.codes):css(p.colors);
  const nm=document.createElement("span"); nm.className="nm"; nm.textContent=p.name;
  b.append(bar,nm); b.title=p.name+" — "+(p.codes?p.codes.map(c=>"&"+c).join(" "):p.colors.map(c=>"#"+c).join(" → "));
  b.setAttribute("aria-pressed",isActive?"true":"false");
  b.addEventListener("click",()=>onPick(p));
  return b;
}
const isOn = (style,p) => p.codes ? legOf(style).join("")===p.codes.join("")
  : style.mode==="gradient"&&p.colors.length===style.stops.length&&p.colors.every((c,i)=>c===style.stops[i]);
function applyPalette(t,p){ track("gen.palette",{name:p.name||"",own:p.own?1:0,classic:p.codes?1:0}); const st=targetObj(t).style; if(p.codes){ st.legacy=[...p.codes]; return; }
  if(!p.colors.length) return;                                   // пустая сохранённая палитра (c:[]) — применять нечего
  if(p.colors.length===1&&!isSponsor()){ st.mode="solid"; st.stops=[p.colors[0],st.stops[1]||p.colors[0]]; return; }   // один цвет вне /sponsor — как «Один цвет»
  st.mode="gradient"; st.stops=[...p.colors]; }

let palCat="all", palQuery="", palOpen=false;
function renderCats(){
  const box=$("cats"); box.innerHTML="";
  const q=palQuery.trim().toLowerCase();
  const count=c=>c==="mine"?mine.length:PALETTES.filter(p=>c==="all"||PAL_CAT[p.name]===c).length;
  CATS.concat([["mine","★ Мои"]]).forEach(([id,label])=>{
    const b=document.createElement("button"); b.type="button"; b.className="chip"; b.setAttribute("role","tab");
    b.setAttribute("aria-selected",palCat===id); b.textContent=label;
    const n=document.createElement("span"); n.className="cnt"; n.textContent=count(id); b.appendChild(n);
    b.addEventListener("click",()=>{ setTab(id); });
    box.appendChild(b);
  });
}
function renderPalettes(){
  const st=cur(), q=palQuery.trim().toLowerCase(), box=$("palettes"); box.innerHTML="";
  const cl=isClassic();
  $("palBar").hidden=cl; $("classicPalNote").hidden=!cl;
  if(cl){
    $("palCount").textContent=CLASSIC.length+" палитр /colors";
    $("paneReady").hidden=false; $("paneMine").hidden=true; $("palMore").hidden=true; $("palNone").hidden=true;
    CLASSIC.forEach(p=>box.appendChild(card(p,p=>{applyPalette(active,p);refresh();},isOn(st,p))));
    return;
  }
  renderCats();
  $("palCount").textContent=(PALETTES.length+mine.length)+" палитр";
  const isMine=palCat==="mine";
  $("paneReady").hidden=isMine; $("paneMine").hidden=!isMine;
  let list=PALETTES.filter(p=>(palCat==="all"||PAL_CAT[p.name]===palCat)&&(!q||p.name.toLowerCase().includes(q)));
  // Свёрнут только полный список без поиска; выбранная палитра видна всегда.
  const collapsible=palCat==="all"&&!q&&list.length>COLLAPSED;
  let shown=list;
  if(collapsible&&!palOpen){ shown=list.slice(0,COLLAPSED); const act=list.find(p=>isOn(st,p)); if(act&&!shown.includes(act)) shown=shown.concat(act); }
  shown.forEach(p=>box.appendChild(card(p,p=>{applyPalette(active,p);refresh();},isOn(st,p))));
  $("palMore").hidden=!collapsible;
  $("palMore").textContent=palOpen?"Свернуть ▴":"Ещё "+(list.length-shown.length)+" ▾";
  $("palNone").hidden=list.length>0;
  const m=$("mine"); m.innerHTML=""; $("mineEmpty").hidden=mine.length>0;
  mine.filter(pr=>!q||pr.n.toLowerCase().includes(q)).forEach(pr=>{
    const idx=mine.indexOf(pr);
    const w=document.createElement("div"); w.className="pwrap";
    const p={name:pr.n,colors:pr.c,own:true};
    w.appendChild(card(p,p=>{applyPalette(active,p);refresh();
      if(p.colors.length) mineMsg("Применена палитра «"+p.name+"»"); else mineMsg("Палитра «"+p.name+"» пустая — в ней нет цветов",true);},isOn(st,p)));
    const d=document.createElement("button"); d.type="button"; d.className="del"; d.textContent="✕";
    d.setAttribute("aria-label","Удалить палитру "+pr.n);
    let armed=false,timer=null;
    d.addEventListener("click",e=>{
      e.stopPropagation();
      if(!armed){ armed=true; d.classList.add("armed"); d.textContent="Удалить?";
        timer=setTimeout(()=>{armed=false;d.classList.remove("armed");d.textContent="✕";},3000); return; }
      clearTimeout(timer); mine.splice(idx,1); saveMine(); mineMsg("Палитра «"+pr.n+"» удалена"); refresh();
    });
    w.appendChild(d); m.appendChild(w);
  });
}
$("palMore").addEventListener("click",()=>{ palOpen=!palOpen; renderPalettes(); });
$("palSearch").addEventListener("input",()=>{ palQuery=$("palSearch").value; renderPalettes(); });
function saveMine(){ return store.set(TPL_KEY,JSON.stringify(mine)); }
function mineMsg(t,err){ const m=$("mineMsg"); m.textContent=t; m.classList.toggle("err",!!err); }
function setTab(which){ palCat = which==="ready" ? "all" : which; renderPalettes(); }

/* ================= Кнопка палитры у названия и у каждой строки + компактный popup ================= */
let openPop=null;
function closePop(){ if(!openPop) return; openPop.pop.remove(); openPop.btn.setAttribute("aria-expanded","false"); openPop=null; }
function palButton(t){
  const wrap=document.createElement("div"); wrap.className="pal-wrap";
  const st=targetObj(t).style;
  const b=document.createElement("button"); b.type="button"; b.className="pal-btn small"; b.setAttribute("aria-expanded","false");
  b.setAttribute("aria-haspopup","true"); b.title="Палитра строки";
  b.innerHTML='<span class="sw"></span><span class="nm"></span><span class="ar">▼</span>';
  if(isClassic()){ const cs=legOf(st); b.querySelector(".sw").style.background=cs.length?cssHard(cs):"transparent"; b.querySelector(".nm").textContent=legacyName(st); }
  else { b.querySelector(".sw").style.background=css(st.mode==="solid"?[st.stops[0]]:st.stops);
    b.querySelector(".nm").textContent=st.mode==="solid"?"#"+st.stops[0]:paletteName(st); }
  b.addEventListener("click",e=>{
    e.stopPropagation();
    const was=openPop&&openPop.btn===b; closePop(); if(was) return;
    if(active!==t) setActive(t);
    const pop=document.createElement("div"); pop.className="pal-pop"; pop.setAttribute("role","dialog");
    // Фильтр по категориям прямо в popup — чтобы не листать весь список.
    let popCat="all";
    const chips=document.createElement("div"); chips.className="chips pop-chips"; pop.appendChild(chips);
    const body=document.createElement("div"); pop.appendChild(body);
    const fill=()=>{
      chips.innerHTML=""; body.innerHTML="";
      if(isClassic()){
        const g=document.createElement("div"); g.className="mini-grid";
        CLASSIC.forEach(p=>g.appendChild(card(p,p=>{applyPalette(t,p);closePop();refresh();},isOn(targetObj(t).style,p))));
        body.appendChild(g); return;
      }
      CATS.concat(mine.length?[["mine","★ Мои"]]:[]).forEach(([id,label])=>{
        const c=document.createElement("button"); c.type="button"; c.className="chip"; c.textContent=label;
        c.setAttribute("aria-selected",popCat===id); c.addEventListener("click",()=>{ popCat=id; fill(); });
        chips.appendChild(c); });
      const list = popCat==="mine" ? mine.map(p=>({name:p.n,colors:p.c,own:true}))
        : PALETTES.filter(p=>popCat==="all"||PAL_CAT[p.name]===popCat);
      const g=document.createElement("div"); g.className="mini-grid";
      list.forEach(p=>g.appendChild(card(p,p=>{applyPalette(t,p);closePop();refresh();},isOn(targetObj(t).style,p))));
      body.appendChild(g);
    };
    fill();
    const foot=document.createElement("div"); foot.className="pop-foot";
    const solid=document.createElement("button"); solid.type="button"; solid.className="small"; solid.textContent="Один цвет";
    solid.addEventListener("click",()=>{ const s=targetObj(t).style; s.mode="solid"; closePop(); refresh(); $("stops").querySelector("input[type=text]")?.focus(); });
    const more=document.createElement("button"); more.type="button"; more.className="small"; more.textContent="Настроить цвета…";
    more.addEventListener("click",()=>{ closePop(); $("modeGrad").scrollIntoView({behavior:"smooth",block:"center"}); });
    if(isClassic()){ solid.hidden=true; more.onclick=null; more.addEventListener("click",()=>{ closePop(); $("legGrid").scrollIntoView({behavior:"smooth",block:"center"}); }); }
    foot.append(solid,more); pop.appendChild(foot);
    pop.addEventListener("click",e=>e.stopPropagation());
    wrap.appendChild(pop); b.setAttribute("aria-expanded","true"); openPop={pop,btn:b};
  });
  wrap.appendChild(b);
  return wrap;
}
/** Микро-кнопка «Aa» у строки описания: свой шрифт для строки. */
function fontButton(t){
  const wrap=document.createElement("div"); wrap.className="pal-wrap font-wrap";
  const st=targetObj(t).style, own=!!st.font;
  const b=document.createElement("button"); b.type="button"; b.className="font-btn small"+(own?" own":"");
  b.setAttribute("aria-haspopup","true"); b.setAttribute("aria-expanded","false");
  const label=v=>[...$("font").options].find(o=>o.value===v)?.textContent||v;
  b.textContent=convert("Aa",fontOf(st));
  b.title="Шрифт строки: "+(own?label(st.font):"как в общем списке ("+label(S.font)+")");
  b.addEventListener("click",e=>{
    e.stopPropagation();
    const was=openPop&&openPop.btn===b; closePop(); if(was) return;
    if(active!==t) setActive(t);
    const pop=document.createElement("div"); pop.className="pal-pop font-pop"; pop.setAttribute("role","dialog");
    const h=document.createElement("h4"); h.textContent="Шрифт строки "+(t+1); pop.appendChild(h);
    const opt=(value,text,sample)=>{
      const o=document.createElement("button"); o.type="button"; o.className="font-opt";
      o.setAttribute("aria-pressed",(st.font||"")===value);
      o.innerHTML='<span class="smp"></span><span class="lbl"></span>';
      o.children[0].textContent=sample; o.children[1].textContent=text;
      o.addEventListener("click",()=>{ if(value) st.font=value; else delete st.font; closePop(); refresh(); });
      pop.appendChild(o);
    };
    opt("","Как в общем списке — "+label(S.font),convert("Aa",S.font));
    [...$("font").options].forEach(o=>opt(o.value,o.textContent,convert("Aa",o.value)));
    pop.addEventListener("click",e=>e.stopPropagation());
    wrap.appendChild(pop); b.setAttribute("aria-expanded","true"); openPop={pop,btn:b};
  });
  wrap.appendChild(b);
  return wrap;
}
document.addEventListener("click",closePop);
document.addEventListener("keydown",e=>{ if(e.key==="Escape"){ if(openPop){ const b=openPop.btn; closePop(); b.focus(); } else if(!$("dlg").hidden) closeDlg(); } });

/* ================= Название и строки описания ================= */
/** Смена цели: перерисовываем редактор цвета, но не поля текста — чтобы не терять фокус и открытый popup. */
function setActive(t){ closePop(); active=t; markActive(); renderStops(); renderPalettes(); update(); }
function markActive(){
  $("nameTarget").classList.toggle("active",active==="name");
  $("nickTarget").classList.toggle("active",active==="nick");
  document.querySelectorAll(".lore-row").forEach((r,i)=>r.classList.toggle("active",active===i));
  $("targetLabel").textContent = active==="sponsor" ? "Префикс sponsor" : active==="nick" ? "Цвет ника"
    : active==="name" ? $("nameLabel").textContent : "Строка "+(active+1);
}
function renderText(){
  const np=$("namePal"); np.replaceWith(Object.assign(palButton("name"),{id:"namePal"}));
  const kp=$("nickPal"); kp.replaceWith(Object.assign(palButton("nick"),{id:"nickPal"}));
  const box=$("lore"); box.innerHTML="";
  $("loreEmpty").hidden=S.lore.length>0;
  S.lore.forEach((ln,i)=>{
    const r=document.createElement("div"); r.className="lore-row target";
    const num=document.createElement("span"); num.className="num"; num.textContent=i+1;
    const inp=document.createElement("input"); inp.type="text"; inp.value=ln.text; inp.maxLength=80; inp.setAttribute("aria-label","Строка описания "+(i+1));
    inp.addEventListener("input",()=>{ ln.text=inp.value; update(); });
    inp.addEventListener("focus",()=>{ if(active!==i) setActive(i); });
    const mv=document.createElement("div"); mv.className="mv";
    const up=document.createElement("button"); up.type="button"; up.textContent="▲"; up.disabled=i===0; up.setAttribute("aria-label","Выше");
    const dn=document.createElement("button"); dn.type="button"; dn.textContent="▼"; dn.disabled=i===S.lore.length-1; dn.setAttribute("aria-label","Ниже");
    const move=d=>{ const j=i+d; [S.lore[i],S.lore[j]]=[S.lore[j],S.lore[i]]; if(active===i) active=j; else if(active===j) active=i; refresh(); };
    up.addEventListener("click",()=>move(-1)); dn.addEventListener("click",()=>move(1));
    mv.append(up,dn);
    const del=document.createElement("button"); del.type="button"; del.className="icon-btn"; del.textContent="✕"; del.setAttribute("aria-label","Удалить строку "+(i+1));
    del.addEventListener("click",()=>{ S.lore.splice(i,1);
      if(active===i) active=S.lore.length?Math.min(i,S.lore.length-1):"name"; else if(typeof active==="number"&&active>i) active--; refresh(); });
    r.addEventListener("mousedown",e=>{ if(!e.target.closest("button")&&active!==i) setActive(i); });
    r.append(num,inp,fontButton(i),palButton(i),mv,del);
    box.appendChild(r);
  });
  $("addLore").disabled=S.lore.length>=MAX_LORE;
  markActive();
}
$("nickTarget").addEventListener("mousedown",e=>{ if(!e.target.closest("button")&&active!=="nick") setActive("nick"); });
$("nickName").addEventListener("focus",()=>{ if(isSponsorEdit()&&active!=="nick") setActive("nick"); });
$("name").addEventListener("input",()=>{ S.name.text=$("name").value; update(); });
$("name").addEventListener("focus",()=>{ if(active!=="name") setActive("name"); });
$("nameTarget").addEventListener("mousedown",e=>{ if(!e.target.closest("button")&&active!=="name") setActive("name"); });
$("addLore").addEventListener("click",()=>{
  if(S.lore.length>=MAX_LORE) return;
  // Новая строка берёт палитру предыдущей — удобно писать описание в одном стиле.
  const prev=S.lore.length?S.lore[S.lore.length-1].style:null;
  S.lore.push({text:"",style:prev?JSON.parse(JSON.stringify(prev)):newStyle(PALETTES[5].colors)});
  active=S.lore.length-1; refresh();
  const rows=document.querySelectorAll(".lore-row input[type=text]"); rows[rows.length-1]?.focus();
});

/* ================= Редактор цветов активной цели ================= */
function roleOf(i,n){ return i===0?"Начало":i===n-1?"Конец":"Середина"; }
function renderClassic(){
  const st=cur(), cs=legOf(st), seq=$("legSeq"); seq.innerHTML="";
  if(!cs.length){ const e=document.createElement("span"); e.className="empty-seq"; e.textContent="Нажмите на цвет ниже, чтобы добавить"; seq.appendChild(e); }
  cs.forEach((c,i)=>{
    const ch=document.createElement("span"); ch.className="leg-chip"; ch.title=LEG_NAME[c];
    ch.innerHTML=`<span class="sq"></span><span></span><button type="button" aria-label="Убрать цвет">✕</button>`;
    ch.children[0].style.background="#"+LEG[c]; ch.children[1].textContent=(i+1)+". &"+c;
    ch.children[2].addEventListener("click",()=>{ cs.splice(i,1); refresh(); });
    seq.appendChild(ch);
  });
  const g=$("legGrid"); if(!g.children.length) Object.keys(LEG).forEach(c=>{
    const b=document.createElement("button"); b.type="button"; b.className="leg-sw"; b.style.background="#"+LEG[c];
    b.title=LEG_NAME[c]+" — &"+c; b.innerHTML=`<span>&amp;${c}</span>`;
    b.addEventListener("click",()=>{ const s=legOf(cur()); if(s.length>=LEG_MAX) return; s.push(c); refresh(); });
    g.appendChild(b);
  });
  [...g.children].forEach(b=>b.disabled=cs.length>=LEG_MAX);
  $("legRev").disabled=cs.length<2; $("legClear").disabled=!cs.length;
  document.querySelectorAll("#fmt button").forEach(b=>b.setAttribute("aria-pressed",!!st.fmt[b.dataset.c]));
}
$("legRev").addEventListener("click",()=>{ legOf(cur()).reverse(); refresh(); });
$("legClear").addEventListener("click",()=>{ cur().legacy=[]; refresh(); });
$("legRnd").addEventListener("click",()=>{ track("gen.random",{classic:1});
  const all=Object.keys(LEG).filter(c=>c!=="0"), n=2+Math.floor(Math.random()*3), out=[];
  while(out.length<n){ const c=all[Math.floor(Math.random()*all.length)]; if(c!==out[out.length-1]) out.push(c); }
  cur().legacy=out; refresh();
});
function renderStops(){
  const cl=isClassic();
  ["seg","chain","chainName","stopsLabel","stops","stopTools"].forEach(id=>$(id).hidden=cl||(id==="seg"&&isSponsor()));
  $("classicBlock").hidden=!cl; $("fmtK").hidden=!cl;
  if(cl) return renderClassic();
  const st=cur(), solid=st.mode==="solid";
  $("modeSolid").setAttribute("aria-pressed",solid); $("modeGrad").setAttribute("aria-pressed",!solid);
  $("stopsLabel").textContent = solid ? "Цвет" : isSponsor() ? "Цвета префикса (1–7)" : "Цвета ("+minStops()+"–"+maxStops()+")";
  const list = solid ? [st.stops[0]] : st.stops;
  const box=$("stops"); box.innerHTML="";
  list.forEach((h,i)=>{
    const d=document.createElement("div"); d.className="stop";
    d.innerHTML=`<span class="pos">${i+1}</span><input type="color" aria-label="Цвет ${i+1}">
      <input type="text" maxlength="7" aria-label="HEX ${i+1}" spellcheck="false"><span class="role"></span>
      <button type="button" class="icon-btn" aria-label="Удалить цвет ${i+1}">✕</button>`;
    const [,pick,hex,role,del]=d.children;
    pick.value="#"+(valid(h)?h:"FFFFFF"); hex.value=h; hex.classList.toggle("bad",!valid(h));
    role.textContent = solid ? "Весь текст" : roleOf(i,list.length);
    del.hidden=solid; del.disabled=st.stops.length<=minStops();
    pick.addEventListener("input",()=>{ st.stops[i]=pick.value.slice(1).toUpperCase(); hex.value=st.stops[i]; hex.classList.remove("bad"); colorsChanged(); });
    hex.addEventListener("input",()=>{ const v=clean(hex.value); st.stops[i]=v; hex.classList.toggle("bad",!valid(v)); if(valid(v)) pick.value="#"+v; colorsChanged(); });
    del.addEventListener("click",()=>{ st.stops.splice(i,1); refresh(); });
    box.appendChild(d);
  });
  $("addStop").disabled = solid || st.stops.length>=maxStops();
  $("revStops").disabled = solid; $("rndStops").disabled=false;
  document.querySelectorAll("#fmt button").forEach(b=>b.setAttribute("aria-pressed",!!st.fmt[b.dataset.c]));
  renderChain();
}
function renderChain(){
  const st=cur(), list=st.mode==="solid"?[st.stops[0]]:st.stops, ch=$("chain"); ch.innerHTML="";
  if(!list.every(valid)){ $("chainName").textContent=""; return; }
  list.forEach((h,i)=>{
    if(i>0){ const l=document.createElement("span"); l.className="link"; l.style.background=css([list[i-1],h]); ch.appendChild(l); }
    const n=document.createElement("span"); n.className="node"; n.textContent="#"+h; n.style.background="#"+h;
    n.style.color = toRgb(h).reduce((a,v,k)=>a+v*[.299,.587,.114][k],0)>140 ? "#071006" : "#f2f2f2";
    ch.appendChild(n);
  });
  $("chainName").innerHTML=""; const b=document.createElement("b"); b.textContent=paletteName(st); $("chainName").appendChild(b);
}
// При вводе HEX не перерисовываем поля (чтобы не терять фокус) — только всё остальное.
function colorsChanged(){ renderChain(); renderPalettes(); refreshButtons(); update(); }
function refreshButtons(){ renderText(); }
$("modeSolid").addEventListener("click",()=>{ cur().mode="solid"; refresh(); });
$("modeGrad").addEventListener("click",()=>{ const st=cur(); st.mode="gradient";
  if(st.stops.length<2) st.stops.push(st.stops[0]); refresh(); });
$("addStop").addEventListener("click",()=>{
  const st=cur(); if(st.stops.length>=maxStops()) return;
  if(st.stops.length===1){ st.stops.push(st.stops[0]); refresh(); return; }
  const a=st.stops[st.stops.length-2], b=st.stops[st.stops.length-1];
  st.stops.splice(st.stops.length-1,0, valid(a)&&valid(b)?mix(a,b,.5):"FFFFFF"); refresh();
});
$("revStops").addEventListener("click",()=>{ cur().stops.reverse(); refresh(); });
function hsl2hex(h,s,l){
  s/=100;l/=100;const k=n=>(n+h/30)%12,a=s*Math.min(l,1-l);
  const f=n=>l-a*Math.max(-1,Math.min(k(n)-3,Math.min(9-k(n),1)));
  return [f(0),f(8),f(4)].map(x=>Math.round(x*255).toString(16).padStart(2,"0")).join("").toUpperCase();
}
$("rndStops").addEventListener("click",()=>{ track("gen.random");
  const st=cur(), n=st.mode==="solid"?1:(Math.random()<.65?2:3), h=Math.random()*360, step=35+Math.random()*70, dir=Math.random()<.5?1:-1;
  const cols=Array.from({length:n},(_,i)=>hsl2hex((h+dir*step*i+360)%360,75+Math.random()*25,55+Math.random()*20));
  if(st.mode==="solid") st.stops[0]=cols[0]; else st.stops=cols;
  refresh();
});
document.querySelectorAll("#fmt button").forEach(b=>b.addEventListener("click",()=>{
  const f=cur().fmt, c=b.dataset.c; f[c]=!f[c]; b.setAttribute("aria-pressed",f[c]); update();
}));

/* ================= Сохранение своей палитры ================= */
$("savePal").addEventListener("click",()=>{
  const st=cur();
  if(!st.stops.every(valid)){ $("warn").textContent="Сначала исправьте цвета: каждый должен быть из 6 символов HEX."; return; }
  const cols = st.mode==="solid" ? [st.stops[0],st.stops[0]] : st.stops;
  $("dlgBar").style.background=css(cols);
  $("dlgName").value="Моя палитра "+(mine.length+1); $("dlgErr").textContent="";
  $("dlg").hidden=false; setTimeout(()=>{ $("dlgName").focus(); $("dlgName").select(); },30);
});
function closeDlg(){ $("dlg").hidden=true; }
function confirmDlg(){
  const name=$("dlgName").value.trim().slice(0,24);
  if(!name){ $("dlgErr").textContent="Введите название."; return; }
  const st=cur(), cols = st.mode==="solid" ? [st.stops[0],st.stops[0]] : [...st.stops];
  const i=mine.findIndex(p=>p.n.toLowerCase()===name.toLowerCase());
  if(i>=0) mine[i]={n:name,c:cols}; else mine.push({n:name,c:cols});
  if(!saveMine()){ $("dlgErr").textContent="Браузер запретил сохранение данных."; return; }
  track("gen.pal_save",{colors:cols.length}); closeDlg(); setTab("mine"); mineMsg(i>=0?"Палитра «"+name+"» обновлена":"Палитра «"+name+"» сохранена");
  const t=$("savePal").querySelector("span"); t.textContent="Сохранено ✓"; setTimeout(()=>t.textContent="Сохранить палитру",1500);
  refresh();
}
$("dlgOk").addEventListener("click",confirmDlg);
$("dlgCancel").addEventListener("click",closeDlg);
$("dlg").addEventListener("click",e=>{ if(e.target===$("dlg")) closeDlg(); });
$("dlgName").addEventListener("keydown",e=>{ if(e.key==="Enter") confirmDlg(); });

/* ================= Шрифт Small Caps (из HEX-генератора) ================= */
const SMALL = {
  a:"ᴀ",b:"ʙ",c:"ᴄ",d:"ᴅ",e:"ᴇ",f:"ꜰ",g:"ɢ",h:"ʜ",i:"ɪ",j:"ᴊ",k:"ᴋ",l:"ʟ",m:"ᴍ",
  n:"ɴ",o:"ᴏ",p:"ᴘ",q:"ǫ",r:"ʀ",s:"ꜱ",t:"ᴛ",u:"ᴜ",v:"ᴠ",w:"ᴡ",x:"x",y:"ʏ",z:"ᴢ",
  "а":"ᴀ","в":"ʙ","е":"ᴇ","ё":"ᴇ","к":"ᴋ","м":"ᴍ","н":"ʜ",
  "о":"ᴏ","р":"ᴘ","с":"ᴄ","ф":"ȹ","х":"x"
};
/* Шрифты-«стили» из Unicode. Математические буквы: A — код заглавной A, a — строчной, d — цифры 0,
   ex — буквы, которых нет в блоке (берутся из «Буквоподобных символов»). Меняется только латиница. */
const MATH_FONTS={
  fraktur:    {A:0x1D504,a:0x1D51E,ex:{C:"ℭ",H:"ℌ",I:"ℑ",R:"ℜ",Z:"ℨ"}},
  frakturBold:{A:0x1D56C,a:0x1D586},
  scriptBold: {A:0x1D4D0,a:0x1D4EA},
  script:     {A:0x1D49C,a:0x1D4B6,ex:{B:"ℬ",E:"ℰ",F:"ℱ",H:"ℋ",I:"ℐ",L:"ℒ",M:"ℳ",R:"ℛ",e:"𝑒",g:"𝑔",o:"𝑜"}},
  double:     {A:0x1D538,a:0x1D552,d:0x1D7D8,ex:{C:"ℂ",H:"ℍ",N:"ℕ",P:"ℙ",Q:"ℚ",R:"ℝ",Z:"ℤ"}},
  sansItalic: {A:0x1D608,a:0x1D622},
  mono:       {A:0x1D670,a:0x1D68A,d:0x1D7F6},
  sansBold:   {A:0x1D5D4,a:0x1D5EE,d:0x1D7EC},
};
/* Шрифты-замены: одна таблица на заглавные и строчные. */
const MAP_FONTS={
  currency:{a:"₳",b:"฿",c:"₵",d:"Đ",e:"Ɇ",f:"₣",g:"₲",h:"Ⱨ",i:"ł",j:"J",k:"₭",l:"Ⱡ",m:"₥",n:"₦",o:"Ø",p:"₱",q:"Q",r:"Ɽ",s:"₴",t:"₮",u:"Ʉ",v:"V",w:"₩",x:"Ӿ",y:"Ɏ",z:"Ⱬ"},
  asian:   {a:"卂",b:"乃",c:"匚",d:"ᗪ",e:"乇",f:"千",g:"Ꮆ",h:"卄",i:"丨",j:"ﾌ",k:"Ҝ",l:"ㄥ",m:"爪",n:"几",o:"ㄖ",p:"卩",q:"Ɋ",r:"尺",s:"丂",t:"ㄒ",u:"ㄩ",v:"ᐯ",w:"山",x:"乂",y:"ㄚ",z:"乙"},
};
function mathFont(t,f){
  return [...t].map(c=>{
    if(f.ex&&f.ex[c]) return f.ex[c];
    const o=c.charCodeAt(0);
    if(c>="A"&&c<="Z") return String.fromCodePoint(f.A+o-65);
    if(c>="a"&&c<="z") return String.fromCodePoint(f.a+o-97);
    if(f.d&&c>="0"&&c<="9") return String.fromCodePoint(f.d+o-48);
    return c;
  }).join("");
}
/** Шрифт строки: свой (style.font) или общий из списка «Шрифт». */
const fontOf = style => (style&&style.font) || S.font;
const convert = (t,font=S.font) => font==="small" ? [...t.toLowerCase()].map(c=>SMALL[c]||c).join("")
  : MATH_FONTS[font] ? mathFont(t,MATH_FONTS[font])
  : MAP_FONTS[font] ? [...t].map(c=>MAP_FONTS[font][c.toLowerCase()]||c).join("")
  : t;
// Таких букв нет в шрифте Minecraft — в предпросмотре рисуем их пикселями.
const SC_PIX={
  "ᴀ":[".###.","#...#","#####","#...#","#...#"], "ʙ":["####.","#...#","####.","#...#","####."],
  "ᴄ":[".####","#....","#....","#....",".####"], "ᴅ":["####.","#...#","#...#","#...#","####."],
  "ᴇ":["#####","#....","####.","#....","#####"], "ꜰ":["#####","#....","####.","#....","#...."],
  "ɢ":[".####","#....","#..##","#...#",".####"], "ʜ":["#...#","#...#","#####","#...#","#...#"],
  "ɪ":["###",".#.",".#.",".#.","###"],           "ᴊ":["..###","...#.","...#.","#..#.",".##.."],
  "ᴋ":["#...#","#..#.","###..","#..#.","#...#"], "ʟ":["#....","#....","#....","#....","#####"],
  "ᴍ":["#...#","##.##","#.#.#","#...#","#...#"], "ɴ":["#...#","##..#","#.#.#","#..##","#...#"],
  "ᴏ":[".###.","#...#","#...#","#...#",".###."], "ᴘ":["####.","#...#","####.","#....","#...."],
  "ǫ":[".###.","#...#","#...#","#..#.",".##.#"], "ʀ":["####.","#...#","####.","#..#.","#...#"],
  "ꜱ":[".####","#....",".###.","....#","####."], "ᴛ":["#####","..#..","..#..","..#..","..#.."],
  "ᴜ":["#...#","#...#","#...#","#...#",".###."], "ᴠ":["#...#","#...#","#...#",".#.#.","..#.."],
  "ᴡ":["#...#","#...#","#.#.#","##.##","#...#"], "ʏ":["#...#",".#.#.","..#..","..#..","..#.."],
  "ᴢ":["#####","...#.","..#..",".#...","#####"], "ȹ":[".###.","#.#.#","#.#.#",".###.","..#.."]
};
const U = () => matchMedia("(max-width:560px)").matches?2:3;
function glyphSvg(rows){
  let r=""; const w=rows[0].length;
  rows.forEach((row,y)=>[...row].forEach((ch,x)=>{ if(ch==="#") r+=`<rect x='${x}' y='${y}' width='1' height='1'/>`; }));
  return "data:image/svg+xml;utf8,"+encodeURIComponent(`<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 ${w} 5' preserveAspectRatio='none' shape-rendering='crispEdges'>${r}</svg>`);
}
/** hex — без «#». */
function makeGlyph(ch,hex,f){
  const rows=SC_PIX[ch]; if(!rows) return null;
  f=f||{}; const u=U(), w=rows[0].length, col="#"+hex;
  const outer=document.createElement("span"); outer.className="sc";
  const fl=[]; if(f.l) fl.push(`drop-shadow(${u}px 0 0 ${col})`);
  fl.push(`drop-shadow(${u}px ${u}px 0 ${shade(hex)})`);
  outer.style.filter=fl.join(" "); outer.style.marginRight=(f.l?2*u:u)+"px";
  if(f.o) outer.style.transform="skewX(-12deg)";
  const inner=document.createElement("span"); inner.className="sc-in";
  inner.style.width=(w*u)+"px"; inner.style.height=(5*u)+"px"; inner.style.background=col;
  inner.style.webkitMaskImage=inner.style.maskImage=`url("${glyphSvg(rows)}")`;
  outer.appendChild(inner);
  [[f.n,`bottom:${-2*u}px`],[f.m,`top:${2*u}px`]].forEach(([on,pos])=>{
    if(!on) return; const l=document.createElement("span"); l.className="sc-line";
    l.style.cssText=`${pos};height:${u}px;right:${-u}px;background:${col}`; outer.appendChild(l); });
  return outer;
}

/* ================= Предпросмотр ================= */
/** Один символ цветом hex (без «#») с форматированием f. */
const OBF="ABCDEFGHJKLMNOPQRSTUVWXYZabcdefghkmnopqrstuvwxyz0123456789#$%&?@";
setInterval(()=>document.querySelectorAll(".obf").forEach(e=>{ e.textContent=OBF[Math.floor(Math.random()*OBF.length)]; }),70);
function glyphEl(c,hex,f){
  if(f.k&&isClassic()&&c!==" "){ const sp=document.createElement("span"); sp.className="obf"; sp.textContent=OBF[0];
    sp.style.color="#"+hex; sp.style.textShadow=`${U()}px ${U()}px 0 ${shade(hex)}`; if(f.l) sp.style.fontWeight="700"; return sp; }
  const g=makeGlyph(c,hex,f); if(g) return g;
  const u=U(), col="#"+hex, sh=shade(hex), sp=document.createElement("span");
  sp.textContent=c; sp.style.color=col;
  if(f.l){ sp.style.textShadow=`${u}px 0 0 ${col}, ${u}px ${u}px 0 ${sh}, ${2*u}px ${u}px 0 ${sh}`; sp.style.marginRight=u+"px"; }
  else sp.style.textShadow=`${u}px ${u}px 0 ${sh}`;
  if(f.o) sp.style.fontStyle="italic";
  const deco=[f.n?"underline":"",f.m?"line-through":""].filter(Boolean).join(" ");
  if(deco) sp.style.textDecoration=deco;
  return sp;
}
function renderLine(el,text,style){
  glyphColors(convert(text,fontOf(style)),style).forEach(g=>el.appendChild(glyphEl(g.c,g.hex,style.fmt)));
}
/** Цвет в точке t∈[0,1] сплошного градиента (как colorAt в генераторе — для sponsor). */
function colorAt(stops,t){
  if(stops.length===1) return stops[0];
  const seg=t*(stops.length-1), k=Math.min(Math.floor(seg),stops.length-2);
  return mix(stops[k],stops[k+1],seg-k);
}
const nickColor = () => clean(S.nickHex);
function sponsorColors(){ const n=SP_PREFIX.length; return [...SP_PREFIX].map((_,i)=>colorAt(S.sponsor.style.stops,n>1?i/(n-1):0)); }
function renderPreview(){
  const tt=$("tooltip"); tt.innerHTML="";
  const line=()=>{ const d=document.createElement("div"); d.className="line"; tt.appendChild(d); return d; };
  const plain=(el,t,col)=>{ el.textContent=t; el.style.color=col; el.style.textShadow=`${U()}px ${U()}px 0 ${col==="#fff"?"#3f3f3f":shade(col.slice(1))}`; };
  if(isSponsor()){
    const d=line(); if(!S.sponsor.style.stops.every(valid)||!valid(nickColor())) return;
    const word=document.createElement("span"); word.className="sc-word";
    sponsorColors().forEach((hex,i)=>{ const g=makeGlyph(SMALL[SP_PREFIX[i]],hex); if(g) word.appendChild(g); });
    d.appendChild(word); d.appendChild(document.createTextNode(" "));
    const nk=document.createElement("span"); plain(nk,S.nick||"nickname","#"+nickColor()); d.appendChild(nk);
    return;
  }
  if(isSponsorEdit()){
    const d=line(), st=S.name.style, ns=S.nickT.style;
    if(!st.stops.every(valid)||!ns.stops.every(valid)) return;
    // Префикс и ник — каждый со своим градиентом, как в команде.
    const paint=(text,style)=>{ const ch=[...text], n=ch.length;
      ch.forEach((c,i)=>d.appendChild(glyphEl(c, style.mode==="solid"?style.stops[0]:colorAt(style.stops,n>1?i/(n-1):0), style.fmt))); };
    paint(convert(S.name.text,fontOf(st)),st);
    d.appendChild(document.createTextNode(" "));
    paint(S.nick||"nickname",ns);
    return;
  }
  const nm=line();
  if(showName()){ if(S.name.text && S.name.style.stops.every(valid)) renderLine(nm,S.name.text,S.name.style); }
  else plain(nm,"Paper","#fff");
  if(showLore()) S.lore.forEach(ln=>{ const d=line(); if(ln.style.stops.every(valid)) renderLine(d,ln.text,ln.style); });
  const sub=line(); sub.className="line sub"; sub.textContent="minecraft:paper";
}

/* ================= Команды для сервера — те же форматы, что в HEX-генераторе ================= */
const LIMITS={"/itemname":64,"/itemlore":65};   // AgeMagic: лимит на текст после команды
const BB={l:"B",o:"I",n:"U",m:"S"}, MM={l:"b",o:"i",n:"u",m:"st"};
const mmEscape=c=>c.replace(/\\/g,"\\\\").replace(/</g,"\\<");
const onFmt = style => Object.keys(style.fmt).filter(k=>style.fmt[k]&&(k!=="k"||isClassic()));
/** AgeMagic: {#HEX>}текст{#HEX<>}текст{#HEX<} — как в генераторе. */
function ageMagic(text,style){
  const codes=onFmt(style).map(k=>"&"+k).join("");
  const st = style.mode==="solid" ? [style.stops[0],style.stops[0]] : style.stops;
  let s=""; segments([...text],st.length).forEach((seg,k)=>{ s+=`{#${st[k]}${k===0?">":"<>"}}`+codes+seg.join(""); });
  return s+`{#${st[st.length-1]}<}`;
}
/** Другие форматы: цвет каждой буквы — тот же, что в предпросмотре. */
function formatOther(kind,text,style){
  const on=onFmt(style), f=style.fmt, glyphs=glyphColors(text,style);
  const amp=on.map(k=>"&"+k).join(""), sect=on.map(k=>"§"+k).join("");
  const plainSpace=c=>c===" "&&!f.n&&!f.m;
  if(kind==="minimessage"){
    const open = style.mode==="solid" ? "<#"+style.stops[0]+">" : "<gradient:"+style.stops.map(h=>"#"+h).join(":")+">";
    const close = style.mode==="solid" ? "" : "</gradient>";
    return open+on.map(k=>"<"+MM[k]+">").join("")+[...text].map(mmEscape).join("")+on.slice().reverse().map(k=>"</"+MM[k]+">").join("")+close;
  }
  if(kind==="bbcode"){
    return on.map(k=>"["+BB[k]+"]").join("")
      +glyphs.map(g=>plainSpace(g.c)?" ":"[COLOR=#"+g.hex+"]"+g.c+"[/COLOR]").join("")
      +on.slice().reverse().map(k=>"[/"+BB[k]+"]").join("");
  }
  if(kind==="birdflop"){
    const tpl=S.tpl||DEFAULT_TPL;
    return glyphs.map(g=>{ let part=tpl; for(let i=0;i<6;i++) part=part.split("$"+(i+1)).join(g.hex[i]);
      return part.split("$f").join(amp).split("$c").join(g.c); }).join("");
  }
  const color=h=>kind==="nickname"?"&#"+h : kind==="chat"?"<#"+h+">"
    : kind==="legacy"?"&x"+[...h].map(d=>"&"+d).join("") : "§x"+[...h].map(d=>"§"+d).join("");
  const codes=kind==="console"?sect:amp;
  let out="", started=false;
  glyphs.forEach(g=>{ if(started&&plainSpace(g.c)){ out+=" "; return; } out+=color(g.hex)+codes+g.c; started=true; });
  return out;
}
const withCmd = (cmd,body) => cmd ? cmd.trim()+" "+body : body;
/** Классические цвета: &c текст &6 текст … — после каждого цвета повторяются коды формата (цвет их сбрасывает). */
function classicBody(text,style){
  const cs=legOf(style), codes=onFmt(style).map(k=>"&"+k).join(""), chars=[...text];
  if(!cs.length) return codes+text;
  const segs = cs.length===1 ? [chars] : segments(chars,cs.length+1);
  return segs.map((seg,i)=>seg.length?"&"+cs[i]+codes+seg.join(""):"").join("");
}
/** Команда для названия или строки описания. Пустая строка описания — пропуск строки: &k. */
function buildLine(isName,t){
  const other=otherKind(), text=convert(t.text,fontOf(t.style)), empty=!t.text.trim(), b=base();
  let cmd, limit=256;
  if(b==="item"||b==="/itemname"||b==="/itemlore"){ cmd=isName?"/itemname":"/itemlore"; limit=LIMITS[cmd]; }
  else if(other) cmd=(isName?S.cmdName:S.cmdLore)||"";
  else cmd="";
  const body = empty ? (other==="console"?"§k":"&k") : isClassic() ? classicBody(text,t.style)
    : other ? formatOther(other,text,t.style) : ageMagic(text,t.style);
  const full=withCmd(cmd,body);
  // Для /itemname и /itemlore лимит считается по тексту после команды, как в генераторе.
  return {full,len:cmd in LIMITS?body.length:full.length,limit};
}
function sponsorLine(){
  return {full:"/sponsor prefix "+sponsorColors().map(c=>"{#"+c+"}").join("")+" {#"+nickColor()+"}",limit:256};
}
/** <gradient:…>префикс<gradient:…> — второй градиент сервер применяет к нику. */
function sponsorEditLine(){
  const st=S.name.style, ns=S.nickT.style, on=onFmt(st), non=onFmt(ns);
  const tag=x => x.mode==="solid" ? "<#"+x.stops[0]+">" : "<gradient:"+x.stops.map(h=>"#"+h).join(":")+">";
  const prefix=tag(st)+on.map(k=>"<"+MM[k]+">").join("")+[...convert(S.name.text,fontOf(st))].map(mmEscape).join("")
    +on.slice().reverse().map(k=>"</"+MM[k]+">").join("");          // формат префикса не переходит на ник
  return {full:"/sponsor editprefix "+prefix+tag(ns)+non.map(k=>"<"+MM[k]+">").join(""),limit:256};
}
function cmdRow(num,line){
  if(line.len===undefined) line.len=line.full.length;
  const r=document.createElement("div"); r.className="cmd";
  const n=document.createElement("span"); n.className="n"; n.textContent=num;
  const c=document.createElement("code"); c.textContent=line.full;
  const l=document.createElement("span"); l.className="len"; l.textContent=line.len+" / "+line.limit;
  l.classList.toggle("over",line.len>line.limit); l.classList.toggle("warn-len",line.len<=line.limit&&line.len>line.limit*.85);
  const why=document.createElement("span"); why.className="len-why"+(line.len>line.limit?" is-over":line.len>line.limit*.85?" is-near":"");
  why.textContent = line.len>line.limit ? "длиннее лимита на "+(line.len-line.limit) : line.len>line.limit*.85 ? "почти предел" : "";
  why.hidden=!why.textContent;
  const b=document.createElement("button"); b.type="button"; b.className="small"; b.textContent="Копировать";
  b.addEventListener("click",()=>copyText(line.full,b,"Копировать"));
  r.append(n,c,l,why,b); return r;
}
let lastAll="";
async function copyText(t,btn,label){
  track("gen.copy",{fmt:S.cmd,len:t.length,all:btn.id==="copyAll"?1:0});
  let ok=false;
  try{ await navigator.clipboard.writeText(t); ok=true; }
  catch(e){ const ta=document.createElement("textarea"); ta.value=t; document.body.appendChild(ta);
    try{ ta.select(); ok=document.execCommand("copy")===true; }catch(_){ ok=false; } finally{ ta.remove(); } }
  // «Скопировано» — только если браузер подтвердил копирование; иначе честно говорим, и команда строки выделяется для ручного копирования.
  if(!ok){ const code=btn.closest(".cmd")?.querySelector("code");
    if(code){ const r=document.createRange(), s=getSelection(); r.selectNodeContents(code); s.removeAllRanges(); s.addRange(r); } }
  btn.textContent=ok?"Скопировано":"Не удалось скопировать"; setTimeout(()=>btn.textContent=label,ok?1400:3000);
}
function renderCommands(){
  const box=$("cmds"); box.innerHTML="";
  const h=t=>{ const e=document.createElement("h4"); e.textContent=t; box.appendChild(e); };
  const none=t=>{ const p=document.createElement("p"); p.className="none"; p.textContent=t; box.appendChild(p); };
  const lines=[];
  const add=(num,l)=>{ lines.push(l.full); box.appendChild(cmdRow(num,l)); };
  if(isSponsor()){ h("Префикс:"); add("",sponsorLine()); }
  else if(isSponsorEdit()){ h("Префикс:"); if(S.name.text.trim()) add("",sponsorEditLine()); else none("Введите текст префикса."); }
  else {
    if(showName()){ h(base()==="item"||base()==="/itemname"?"Название:":"Текст:");
      if(S.name.text.trim()) add("",buildLine(true,S.name)); else none("Текст не задан."); }
    if(showLore()){ h("Описание:");
      if(!S.lore.length) none("Строк нет.");
      S.lore.forEach((ln,i)=>add((i+1)+" -",buildLine(false,ln))); }
  }
  lastAll=lines.join("\n");
  $("copyAll").disabled=!lines.length;
  const other=otherKind();
  $("outNote").textContent =
      isSponsor() ? "Префикс sponsor из 7 букв: каждая буква получает свой цвет из градиента, в конце через пробел идёт цвет ника."
    : isSponsorEdit() ? "Первый градиент — цвет префикса, второй (в конце команды) — цвет ника. Цвет ника меняется кнопкой «Цвет ника» выше."
    : isClassic() ? "Только цвета из /colors (&0–&f) и коды формата — доступно обычным игрокам. "+(base()==="am-raw"?"Пустая строка описания — &k.":"Вводите по очереди, держа предмет в руке; пустая строка описания — /itemlore &k.")
    : S.cmd==="item" ? "Вводите по очереди, держа предмет в руке: сначала /itemname, потом строки описания по порядку. Пустая строка описания — /itemlore &k (пропуск строки)."
    : S.cmd==="/itemlore" ? "Каждая строка — отдельная команда /itemlore, по порядку. Пустая строка — /itemlore &k (пропуск строки)."
    : other==="minimessage" ? "MiniMessage сам распределяет градиент по тексту — результат получается короче."
    : other==="console" ? "Формат с § — для консоли и конфигов: в чат игры такой текст отправить нельзя."
    : other ? "Каждая буква получает свой цвет в выбранном формате. Команды перед текстом можно поменять в полях выше."
    : S.cmd==="am-raw" ? "Только текст, без команды. Пустая строка описания — &k."
    : "Промежуточные цвета делят текст на равные части: каждая часть переливается к следующему цвету.";
}
$("copyAll").addEventListener("click",()=>{ if(lastAll) copyText(lastAll,$("copyAll"),"Скопировать всё"); });

/* ================= Смена формата ================= */
let lastEndSynced=null;
let modeAtStartup=true;   // при загрузке страницы сохранённый цвет ника не перезаписывается (раньше сбрасывался на конечный цвет)
function syncNickToEnd(force){
  const st=S.sponsor.style.stops, end=st[st.length-1];
  if(!valid(end)) return;
  if(force||end!==lastEndSynced){ S.nickHex=end; $("nickHex").value=end; $("nickPick").value="#"+end; lastEndSynced=end; }
}
function applyMode(){
  const other=otherKind(), sp=isSponsor(), spe=isSponsorEdit();
  $("otherBlock").hidden=!other; $("tplBlock").hidden=other!=="birdflop";
  $("sponsorBlock").hidden=!(sp||spe); $("nickColorBox").hidden=!sp; $("nickGradBox").hidden=!spe;
  if(active==="nick"&&!spe) active="name";
  $("sponsorNote").hidden=!sp;
  $("nameBlock").hidden=!showName(); $("loreBlock").hidden=!showLore();
  $("fontBlock").hidden=sp; $("symWrap").hidden=sp; $("fmtBlock").hidden=sp; $("seg").hidden=sp;
  $("nameLabel").textContent = spe ? "Префикс" : (base()==="item"||base()==="/itemname") ? "Название" : "Текст";
  $("loreLabel").textContent = base()==="/itemlore" ? "Строки описания (/itemlore)" : "Описание (lore)";
  $("cmdMeta").textContent = sp||spe ? "/sponsor" : isClassic() ? "цвета /colors" : other ? "формат "+other : "AgeMagic";
  // Выбор цели под формат.
  if(sp) active="sponsor";
  else {
    if(active==="sponsor") active="name";
    if(base()==="/itemlore"){
      if(!S.lore.length) S.lore.push({text:"",style:JSON.parse(JSON.stringify(S.name.style))});
      if(active==="name") active=0;
    }
    if(typeof active==="number"&&(!showLore()||active>=S.lore.length)) active="name";
  }
  if(sp){ S.sponsor.style.mode="gradient"; syncNickToEnd(!modeAtStartup); }
  else { const st=cur(); if(st.mode==="gradient"&&st.stops.length<2) st.stops.push(st.stops[0]);
    if(st.stops.length>6) st.stops=st.stops.slice(0,5).concat(st.stops[st.stops.length-1]); }
  refresh();
}
$("cmd").addEventListener("change",()=>{ S.cmd=$("cmd").value; applyMode(); track("gen.format",{fmt:S.cmd}); });
$("font").addEventListener("change",()=>{ S.font=$("font").value; update(); track("gen.font",{font:S.font}); });
$("cmdName").addEventListener("input",()=>{ S.cmdName=$("cmdName").value; update(); });
$("cmdLore").addEventListener("input",()=>{ S.cmdLore=$("cmdLore").value; update(); });
$("tpl").addEventListener("input",()=>{ S.tpl=$("tpl").value; update(); });
$("nickName").addEventListener("input",()=>{ S.nick=$("nickName").value; update(); });
$("nickPick").addEventListener("input",()=>{ S.nickHex=$("nickPick").value.slice(1).toUpperCase(); $("nickHex").value=S.nickHex; update(); });
$("nickHex").addEventListener("input",()=>{ const v=clean($("nickHex").value); S.nickHex=v; if(valid(v)) $("nickPick").value="#"+v; update(); });

/* ================= Символы ================= */
let lastInput=null, lastSel=null;
document.addEventListener("focusin",e=>{ const t=e.target; if(t.matches&&t.matches("#name, .lore-row input[type=text]")) lastInput=t; });
document.addEventListener("selectionchange",()=>{ const a=document.activeElement; if(a&&a===lastInput) lastSel=[a.selectionStart,a.selectionEnd]; });
// Категории символов (только те, что Minecraft показывает своим шрифтом). Первая вкладка — недавние.
const SYM_CATS = [["Звёзды и снежинки", "⋆✢✣✤✥✦✧✩✪✫✬✭✮✯✰✱✲✳✴✵✶✷✸✹✺✻✼✽✾✿❀❁❂❃❄❅❆❇❈❉❊❋❖★☆⁂⁎⁑"], ["Сердца и смайлы", "❣❤❥❦♠♥♡❧♤☃☻☺☹ツ✌✍☚☛☜☝☞☟ღ"], ["Стрелки", "➱➲➳➴➵➶➷➸➘➙➚➛➜➝➞➟➠➡➢➣➤➥➦➧➨➩➪➫➬➭➮➯➔➹➺➻➼➽➾←↑→↓↔↕↖↗↘↙↚↛↜↝↞↟↠↡↢↣↤↥↦↧↨↩↪↫↬↭↮↯↰↱↲↳↴↵↶↷↸↹↼↽↾↿⇀⇁⇂⇃⇄⇅⇆⇇⇈⇉⇊⇋⇌⇍⇎⇏⇐⇑⇒⇓⇔⇕⇖⇗⇘⇙⇚⇛⇜⇝⇞⇟⇠⇡⇢⇣⇤⇥⇦⇧⇨⇩⇪☇↺↻⇵⏏⏩⏪⏭⏮⏯⊲⊳⊴⊵⤴⤵⟵⟶⟷«»‹›"], ["Предметы", "🗡🏹🔱🧪🪓⛏🛡🎣✂🍖🔔☂☔⛄☃⌛⌚☎☏✁✃✄✆✎✏✐✑✒✈⚓⚡🌊⭐🔥♔♕♚♛♨⚔☠⚠۞⛀⛁⛃⛂⚒⚑⚐⛨⚖⚗⚙⚛⚜⛓✉⏳⚱⚕⚚☤⚘☕⛺⛵⌂⛰⛩"], ["Погода и природа", "☁☀☄🌧⛈❄☼☂☔☃⛄⛅☾☽❅❆⚡☘♣❀✿❁⚘❖"], ["Игры и музыка", "⚀⚁⚂⚃⚄⚅♤♧♡♢♪♩♫♬♔♕♖♗♘♙♚♛♜♝♞♟♠♣♥♦♭♮♯"], ["Галочки и кресты", "╳✕✖✗✘☓❌♱♰✞✟†‡☨☒✓✔☑✙✚✛✜"], ["Ромбы и треугольники", "◆◇◈◊⋄♦♢▲△▴▵▼▽▾▿▶▷▸▹►▻◀◁◂◃◄◅◭◮"], ["Кружки и точки", "◍◎●◐◑◒◓◔◕◖◗◯✇☮☯❍⊕⊖⊗⊘⊙⊚⊛⊜⊝○｡☣☢㊣⭘◦☪⏺∅⋅㊚㊛･∙⋮⋯⋰⋱°∵∴ﾟ❂∶∷•᛫᛬☉✚✪✣✤✥✦❉❃❁❀☖☗☀·‣⁃⦁⦾⦿⁌⁍❏❐❑❒❖➧⁕⁜"], ["Квадраты и блоки", "▁▂▃▄▅▆▇█▉▊▋▌▍■▬▏▕▐░▒▓▔▀⧈□▣▤▥▦▧▨▩▪▫▭▮▯▰▱◘◙◚◛◧◨◩◪◫☐❏❐❑❒回♏♒▢◉◌◢◣◤◥"], ["Линии и скобки", "─━│┃┄┅┆┇┈┉┊┋┌┍–—┱┲‑‒―﹏﹋﹌﹉﹊﹍﹎☰☱☳☴☶☷☲☵‾‗⁃Ξ┎┏┐┑┒┓└┕┖┗┘┙┚┛├┝┞┟┠┡┢┣┤┥┦┧┨┩┪┫┬┭┮┯┰┳┴┵┶┷┸┹┺┻┼┽┾┿╀╁╂╃╄╅╆╇╈╉╊╋╌╍╎╏═║╒╓╔╕╖╗╘╙╚╛╜╝╞╟╠╡╢╣╤╥╦╧╨╩╪╫╬╭╮╯╰╱╲╴╵╶╷╸╹╺╻╼╽╾╿☽☾⌃⌄⌅⌆⌇⌈⌉⌊⌋﹃﹄「」‖︴⌠⌡ΣΠ«»๑⁅⁆⁐ℵℶℷℸ◠◡ぃ【】⁽⁾ʔʕ‸‹›╳▀▄█▌▐░▒▓¦⎯〖〗『』〔〕《》〈〉⟦⟧⟨⟩❨❩❪❫❬❭❮❯❰❱❲❳❴❵"], ["Кавычки", "‘’‚‛“”„‟′″‴‵‶‷՚՛՜՝՞❛❜❝❞"], ["Особые", "⚗⚑✉⚐☠☣☤☩☫☬☭♂♀☿⛨۩☊☋☌☍⁈⁉☥∡∢∱∲∳∸∞Δʊღ₪∀®©℠℡™✓✔✕✖✗✘☑☒⚠☢☮☯☪☸✚✛✜✝✞✟✠⚤⚥⚦⚧℗№§¶†‡※☺☻☹"], ["Буквы", "ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺⒶⒷⒸⒹⒺⒻⒼⒽⒾⒿⓀⓁⓂⓃⓄⓅⓆⓇⓈⓉⓊⓋⓌⓍⓎⓏᴀʙᴄᴅᴇғɢʜɪᴊᴋʟᴍɴᴏᴘǫʀᴛᴜᴠᴡʏᴢａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ⒜⒝⒞⒟⒠⒡⒢⒣⒤⒥⒦⒧⒨⒩⒪⒫⒬⒭⒮⒯⒰⒱⒲⒳⒴⒵ⓐⓑⓒⓓⓔⓕⓖⓗⓘⓙⓚⓛⓜⓝⓞⓟⓠⓡⓢⓣⓤⓥⓦⓧⓨⓩꜰꜱ"], ["Цифры", "⓪①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳❶❷❸❹❺❻❼❽❾❿⓫⓬⓭⓮⓯⓰⓱⓲⓳⓴➊➋➌➍➎➏➐➑➒➓⑴⑵⑶⑷⑸⑹⑺⑻⑼⑽⑾⑿⒀⒁⒂⒃⒄⒅⒆⒇⒈⒉⒊⒋⒌⒍⒎⒏⒐⒑⒒⒓⒔⒕⒖⒗⒘⒙⒚⒛ⅠⅡⅢⅣⅤⅥⅦⅧⅨⅩⅪⅫⅬ⁰⁴⁵⁶⁷⁸⁹₀₁₂₃₄₅₆₇₈₉➀➁➂➃➄➅➆➇➈➉¹²³½⅓⅔¼¾"], ["Математика", "½⅓¼⅕⅙⅐⅛⅑⅒⅔⅖¾⅗⅜⅘⅚⅝⅞℅⅟‱⁺⁻⁼⁽⁾ⁿⁱ₊₋₌₍₎ₐₑₒₓₔ≤≥≦≧≨≩≮≯≰≱≲≳≴≵≡±−≣≪≫∹∺∻∼∽∾∿≀≁≂≃≄≅≆≇≈≉≊≋≌≍⊢⊣⊤⊥⊦⊧⊨⊩⊪⊫⊬⊭⊮⊯≎≏≐≑≒≓≔≕≖≗≘≙≚≛≜≝≞≟≠≢≬≭≶≷≸≹≺≻≼≽≾≿⊀⊁⊂⊃⊄⊅⊆⊇⊈⊉⊊⊋⊌⊍⊎⊏⊐⊑⊒⊓⊔⊡⊰⊱⊲⊳⊴⊵⊶⊷⊸⊹⊺⊻⊼⊽⊾⊿⋀⋁⋂⋃⋇⋈⋉⋊⋋⋌⋍⋎⋏⋐⋑⋒⋓⋔⋕⋖⋗⋘⋙⋚⋛⋜⋝⋞⋟∟∃∄∉∋∌∧∨∥⋠⋡⋢⋣⋤⋥⋦⋧⋨⋩⋪⋫⋬⋭⋲⋳⋴⋵⋶⋷⋸⋹⋺⋻⋼⋽⋾⋿∣∤∦∩∪∫∬∭∮∯∰∊∍∈×÷∑∏√∛∆∇∂¬∀∅πΩµ°‰′″€£¥₽₴₿¢"], ["Латиница с акцентами", "ʂʐɶǍǎǞǟǺǻȂȃȦȧǠǡḀḁȀȁḆḇḄḅᵬḈḉḐḑḒḓḎḏḌᵭḔḕḖḗḘḙḜḝȨȩḚḛȄȅȆᵮǴǵǦḦḧḨḩḪḫȞȟḤẖḮḯȊȋǏǐȈȉḬḭǰȷǨǩḲḳḴḵḺḻḼḽḶḷḸḹⱢḾḿṂṃᵯṄṅṆṇṊṋǸǹṈṉᵰǬǭȬȭṌṍṎṏṐṑṒṓȎȏȪȫǑǒȮȯȰȱȌȍǪṔṕᵱȒȓṘṙṜṝṞṟȐȑṚᵳᵲṤṥṦṧṢṣṨṩᵴṰṱṮṯṬẗᵵṲṳṶṷṸṹṺṻǓǔǕǖǗǘǙǚǛǜṴṵȔȕȖṾṿṼṽẆẇẈẉẘẌẍẊẋȲȳẎẏẙẔẕẐẑẓᵶǮǯẛꜾꜿǢǣᵺỻᴂᴔȸʣʥʤʩʪʫȹʨʦʧỺƀƂƃƇƈƊƋƌƓǤǥƗƖɩƘƙƝƤƥɽƦƬƭƫƮȗƱƜƳƴƵƶƢƣȢȣʭʮʯƍƺⱾȿⱿɀᶀꟄꞔᶁᶂᶃꞕᶄᶅᶆᶇᶈᶉᶊᶋᶌᶍᶎᶏᶐᶒᶓᶔᶕᶖᶗᶘᶙᶚẚÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖÙÚÛÜÝàáâãäåæçìíîïñòóôõöùúûüýÿĀāĂăĄąĆćĈĉĊċČčĎďĐđĒēĔĕĖėĘęĚěĜĝḠḡĞğĠġĢģĤĥĦħĨĩĪīĬĭĮįİıĴĵĶķĹĺĻļĽľĿŀŁłŃńŅņŇňŊŋŌōŎŏŐőŒœŔŕŖŗŘřŚśŜŝŞşŠšŢţŤťŦŧŨũŪūŬŭŮůŰűŲųŴŵŶŷŸŹźŻżŽžǼǽǾǿȘșȚțḂḃḊḋḞḟḢḣḰḱṀṁṖṗṠṡṪṫẀẁẂẃẄẅỲỳèéêëŉǧǫḍḥṛṭẒỊịỌọỤụȇƔɣʃẴẵẼẽỄễỒỠỡỮỸỹǱǲǳǄǅǆǇǈǊǋǌᵫꜲꜳꜴꜵꜶꜷꜸꜺꜼꜽꝎꝏꝠꝡƠơƯưẮắẤấẾếốỚớỨứẰằẦầỀềồỜờỪừẢảẲẳẨẩẺẻổỞỂểỈỉỎỏỔởỦủỬửỶỷẠạẶặẬậẸẹỆệỘộỢợỰựỴỵỐƕẪẫỖỗữɼƄƅẟȽƚƛȠƞƟƧƨƪƸƹƻƼƽƾȡȴȵȶȺⱥȻȼɆɇȾⱦɁɂɃɄɈɉɊɋɌɍɎɏẜẝỼỽỾỿꞨꞩ§ɱɳɲʈɖɡʡɕʑɸʝʢɻʁɦʋɰɬɮʘǀǃǂǁɓɗᶑʄɠʛɧɫɨʉʊɘɵɤɜɞɑɒɚɝƁƉƑƩƲꜧꜦɺⱱʠʗʖɭɷɿʅʆʓʚ¿×ØÞðøþⱯƆƎꞰꞀꝹᴚɅɐɔǝɟᵷɥᴉɾʞꞁɯɹʇʌʍʎə"], ["Греческий и кириллица", "ԂԪԬԄԐԆԞԚԮԒԠԈԔԨԢԊԤԖԌԎԦԘԜԃԫԭԅԑԇԟԛԯԓԡԉԕԩԣԋԥԗԍԏԧԙԝϓϔΆΈΉΊΌΎΏΐΪΫάέήίΰϊϋόύώһΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩαβγδεζηθικλμνξοπρςστυφχψωЂЅІЈЉЊЋѕіјљњҚқҒғҰұӘәҖҗҢңҺԀ"], ["Другие алфавиты", "ꭣꭐꭑ₧אַאָﬔﬕﬗﬖﬓꚂꚀꚈꚄꚐꚊꚌꚔꚎꚒꚖꚆꚃꚁꚉꚅꚑꚋꚍꚕꚏꚓꚗꚇἈἀἉἁἊἂἋἃἌἄἍἅἎἆἏἇᾺὰᾸᾰᾹᾱΆάᾈᾀᾉᾁᾊᾂᾋᾃᾌᾄᾍᾅᾎᾆᾏᾇᾼᾴᾶᾷᾲᾳἘἐἙἑἚἒἛἓἜἔἝἕῈΈὲέἨἠῊὴἩἡἪἢἫἣἬἤἭἥἮἦἯἧᾘᾐᾙᾑᾚᾒᾛᾓᾜᾔᾝᾕᾞᾖᾟᾗΉήῌῃῂῄῆῇῚὶΊίἸἰἹἱἺἲἻἳἼἴἽἵἾἶἿἷῘῐῙῑῒΐῖῗῸὸΌόὈὀὉὁὊὂὋὃὌὄὍὅῬῤῥῪὺΎύὙὑὛὓὝὕὟὗῨῠῩῡῢΰῧὐὒὔῦὖῺὼΏώὨὠὩὡὪὢὫὣὬὤὭὥὮὦὯὧᾨᾠᾩᾡᾪᾢᾫᾣᾬᾤᾭᾥᾮᾦᾯᾧῼῳῲῴῶῷ₿№ℹﬄﬆᚡᚵ𐌰𐌱𐌲𐌳𐌴𐌵𐌶𐌷𐌸𐌹𐌺𐌻𐌼𐌽𐌾𐌿𐍀𐍁𐍂𐍃𐍄𐍅𐍆𐍇𐍈ႠႡႢႣႤႥႦႧႨႩႪႫႬႭႮႯႰႱႲႳႴႵႶႷႸႹႺႻႼႽႾႿჀჁჂჃჄჅჇჍაბგდევზთიკლმნოპჟრსტუფქღყშჩცძწჭხჯჰჱჲჳჴჵჶჷჸჹჺ჻ჼჽჾჿתּשׂפֿפּכּײַיִוֹוּבֿבּ₪₾֊ⴀⴁⴂⴃⴄⴅⴆⴡⴇⴈⴉⴊⴋⴌⴢⴍⴎⴏⴐⴑⴒⴣⴓⴔⴕⴖⴗⴘⴙⴚⴛⴜⴝⴞⴤⴟⴠⴥ₴≠אבגדהוזחטיכלמםנןסעפףצץקרᗺᗡℲ⅁⟘∩⅄ԱԲԳԴԶԷԹԺԻԼԽԾԿՀՁՂՃՄՅՆՇՈՉՋՌՍՎՏՐՑՒՓՔՕՖՙաբգդեզէըթժիլխծկհձղճմյնշոչպջռսվտրցւփքօֆևשתԸ∞"]];
const RECENT_KEY="fspirat_recent_symbols";
let recentSyms=[]; try{ recentSyms=JSON.parse(store.get(RECENT_KEY,"[]")); if(!Array.isArray(recentSyms)) recentSyms=[]; }catch(e){ recentSyms=[]; }
const FAV_KEY="fspirat_fav_symbols";
let favSyms=[]; try{ favSyms=JSON.parse(store.get(FAV_KEY,"[]")); if(!Array.isArray(favSyms)) favSyms=[]; }catch(e){ favSyms=[]; }
let symCat = favSyms.length ? "fav" : recentSyms.length ? "recent" : 0;
function toggleFav(sym){
  favSyms = favSyms.includes(sym) ? favSyms.filter(x=>x!==sym) : favSyms.concat(sym);
  store.set(FAV_KEY,JSON.stringify(favSyms));
}
function renderSymTabs(){
  const box=$("symTabs"); box.innerHTML="";
  const tabs=[["fav","★ Избранные"+(favSyms.length?" "+favSyms.length:"")],["recent","⟲ Недавние"]].concat(SYM_CATS.map((c,i)=>[i,c[0]]));
  tabs.forEach(([id,label])=>{
    const b=document.createElement("button"); b.type="button"; b.className="chip"; b.setAttribute("role","tab");
    b.setAttribute("aria-selected",symCat===id); b.textContent=label;
    b.addEventListener("click",()=>{ symCat=id; renderSymTabs(); renderSyms(); });
    box.appendChild(b);
  });
}
function renderSyms(){
  const box=$("syms"); box.innerHTML=""; box.scrollTop=0;
  const list = symCat==="fav" ? favSyms : symCat==="recent" ? recentSyms : [...SYM_CATS[symCat][1]];
  if((symCat==="fav"||symCat==="recent")&&!list.length){
    const e=document.createElement("p"); e.className="sym-empty";
    e.textContent= symCat==="fav" ? "Здесь пока пусто. Наведите на символ в любой вкладке и нажмите ☆ в его углу — он появится здесь."
      : "Здесь появятся символы, которые вы недавно вставляли.";
    box.appendChild(e); return;
  }
  list.forEach(addSym);
}
function rememberSym(sym){
  recentSyms=[sym,...recentSyms.filter(x=>x!==sym)].slice(0,30);
  store.set(RECENT_KEY,JSON.stringify(recentSyms));
}
let symHintTimer=null;
function symFull(max){
  const h=document.querySelector("#symPop .sym-hint"); if(!h) return;
  if(!h.dataset.idle) h.dataset.idle=h.textContent;
  h.textContent="Символ не помещается: в поле не больше "+max+" знаков. Удалите что-нибудь или выберите другое поле."; h.classList.add("err");
  clearTimeout(symHintTimer); symHintTimer=setTimeout(()=>{ h.textContent=h.dataset.idle; h.classList.remove("err"); },4000);
}
function addSym(sym){
  const cell=document.createElement("div"); cell.className="sym-cell";
  const on=favSyms.includes(sym);
  const f=document.createElement("button"); f.type="button"; f.className="fav"+(on?" on":"");
  f.textContent=on?"★":"☆"; f.title=on?"Убрать из избранного":"В избранное";
  f.setAttribute("aria-label",(on?"Убрать из избранного ":"Добавить в избранное ")+sym); f.setAttribute("aria-pressed",on);
  f.addEventListener("click",e=>{
    e.stopPropagation(); toggleFav(sym); renderSymTabs();
    if(symCat==="fav") renderSyms();               // во вкладке «Избранные» символ сразу исчезает
    else { const now=favSyms.includes(sym); f.classList.toggle("on",now); f.textContent=now?"★":"☆";
      f.title=now?"Убрать из избранного":"В избранное"; f.setAttribute("aria-pressed",now); }
  });
  const b=document.createElement("button"); b.type="button"; b.textContent=sym; b.title=sym+"  U+"+sym.codePointAt(0).toString(16).toUpperCase().padStart(4,"0");
  b.setAttribute("aria-label","Вставить символ "+sym);
  b.addEventListener("click",()=>{
    let t=lastInput&&document.body.contains(lastInput)&&!lastInput.closest("[hidden]") ? lastInput : null;
    if(!t) t = showName() ? $("name") : document.querySelector(".lore-row input[type=text]");
    if(!t) return;
    const same=t===lastInput&&lastSel, a=same?lastSel[0]:t.value.length, e=same?lastSel[1]:a;
    // maxlength поля (80) действует и на символы: если символ не помещается, поле не меняется, а подсказка говорит почему.
    if(t.maxLength>0&&t.value.length-(e-a)+sym.length>t.maxLength){ symFull(t.maxLength); return; }
    t.value=t.value.slice(0,a)+sym+t.value.slice(e);
    t.focus(); t.setSelectionRange(a+sym.length,a+sym.length); lastSel=[a+sym.length,a+sym.length];
    t.dispatchEvent(new Event("input"));
    rememberSym(sym);
  });
  cell.append(b,f);
  $("syms").appendChild(cell);
}
renderSymTabs(); renderSyms();
function toggleSyms(open){
  const o = open===undefined ? $("symPop").hidden : open;
  $("symPop").hidden=!o; $("symBtn").setAttribute("aria-expanded",o);
  if(o){ renderSymTabs(); renderSyms(); }
}
$("symBtn").addEventListener("mousedown",e=>e.preventDefault()); // не забираем фокус у поля ввода
$("symBtn").addEventListener("click",()=>toggleSyms());
$("symPop").addEventListener("mousedown",e=>e.preventDefault());
document.addEventListener("mousedown",e=>{ if(!$("symPop").hidden&&!e.target.closest(".sym-wrap")) toggleSyms(false); });
document.addEventListener("keydown",e=>{ if(e.key==="Escape"&&!$("symPop").hidden) toggleSyms(false); });

function update(){
  const all = isSponsor() ? [S.sponsor] : isSponsorEdit() ? [S.name,S.nickT] : [...(showName()?[S.name]:[]),...(showLore()?S.lore:[])];
  const allOk=isClassic() ? true : all.every(t=>t.style.stops.every(valid));
  const nickOk=!isSponsor()||valid(nickColor());
  if(isSponsor()) syncNickToEnd(false);
  $("warn").textContent = isClassic() ? (legOf(cur()).length ? "" : "Выберите хотя бы один цвет.")
    : !cur().stops.every(valid) ? "Каждый цвет должен состоять из 6 символов HEX, например FF8F5A."
    : !allOk ? badTargets(all)+": каждый цвет должен состоять из 6 символов HEX, например FF8F5A."
    : !nickOk ? "Цвет ника должен состоять из 6 символов HEX, например FD2177." : "";
  renderPreview();
  if(!allOk||!nickOk){ $("cmds").innerHTML='<p class="none">Исправьте цвета выше — команды появятся здесь.</p>'; lastAll=""; $("copyAll").disabled=true; $("outNote").textContent=""; }
  else renderCommands();
  save();
}
/** Названия целей с неверным HEX — для подсказки, когда ошибка не в редактируемой строке. */
function badTargets(all){
  return all.filter(t=>!t.style.stops.every(valid)).map(t=>t===S.name?$("nameLabel").textContent:t===S.nickT?"Цвет ника"
    :t===S.sponsor?"Префикс sponsor":"Строка "+(S.lore.indexOf(t)+1)).join(", ");
}
function refresh(){ closePop(); renderText(); renderStops(); renderPalettes(); update(); }
addEventListener("resize",()=>renderPreview());

/* ================= Градиент со скриншота =================
 * Всё считается в браузере, картинка никуда не отправляется.
 * 1) пиксели текста — те, что ярче порога (тень Minecraft в 4 раза темнее и отсекается);
 * 2) связные области → буквы, буквы группируются в строки, большие промежутки — пробелы;
 * 3) цвет буквы — медиана её пикселей;
 * 4) цвета градиента подбираются методом наименьших квадратов по той же формуле, что и у генератора,
 *    берётся наименьшее число цветов, которое даёт такой же результат. */
const shot={W:0,H:0,data:null,sel:null,lines:[],thr:null,drag:null};
const shotMsg=(t,cls)=>{ const m=$("shotMsg"); m.textContent=t||""; m.className="shot-msg"+(cls?" "+cls:""); };
function openShot(){ $("shotDlg").hidden=false; shotMsg(""); if(shot.data) renderShotLines(); setTimeout(()=>$("shotClose").focus(),30); }
function closeShot(){ $("shotDlg").hidden=true; }
function loadShot(file){
  if(!file||!/^image\//.test(file.type)){ shotMsg("Это не картинка. Нужен PNG, JPG или WEBP.","err"); return; }
  const url=URL.createObjectURL(file), img=new Image();
  img.onload=()=>{
    let W=img.naturalWidth,H=img.naturalHeight; const k=Math.min(1,Math.sqrt(16e6/(W*H)));
    W=Math.max(1,Math.round(W*k)); H=Math.max(1,Math.round(H*k));
    const c=document.createElement("canvas"); c.width=W; c.height=H;
    const g=c.getContext("2d",{willReadFrequently:true}); g.drawImage(img,0,0,W,H); URL.revokeObjectURL(url);
    Object.assign(shot,{W,H,data:g.getImageData(0,0,W,H).data,sel:null,thr:null,src:c,fresh:true});
    const v=$("shotCanvas"); v.width=W; v.height=H;
    $("shotWork").hidden=false; $("shotDrop").classList.add("mini");
    $("shotDropText").innerHTML="<b>Ctrl+V</b> или <b>нажмите</b> — другая картинка";
    analyzeShot();
  };
  img.onerror=()=>{ URL.revokeObjectURL(url); shotMsg("Не удалось открыть картинку.","err"); };
  img.src=url;
}
/** Порог Оцу по яркости (максимум из R,G,B). */
function otsu(hist,total){
  let sum=0; for(let i=0;i<256;i++) sum+=i*hist[i];
  let sB=0,wB=0,best=0,thr=128;
  for(let t=0;t<256;t++){ wB+=hist[t]; if(!wB) continue; const wF=total-wB; if(!wF) break;
    sB+=t*hist[t]; const mB=sB/wB, mF=(sum-sB)/wF, v=wB*wF*(mB-mF)*(mB-mF); if(v>best){best=v;thr=t+1;} }
  return thr;
}
const median = a => { if(!a.length) return 0; const s=Float64Array.from(a).sort(); return s[s.length>>1]; };
/** Пиксель текста Minecraft: под ним (со сдвигом на 1 пиксель шрифта вниз-вправо) лежит тень ровно в 4 раза темнее. */
function shadowOk(d,W,i,j,strict){
  const v=Math.max(d[i],d[i+1],d[i+2]); if(v<40) return false;
  const tol=strict?1:4+(v>>5);
  for(let c=0;c<3;c++) if(Math.abs(d[j+c]-(d[i+c]>>2))>tol) return false;
  return v-Math.max(d[j],d[j+1],d[j+2])>=24;
}
function analyzeShot(){
  const {W,H,data}=shot, r=shot.sel||{x:0,y:0,w:W,h:H}, w=r.w, h=r.h, N=w*h;
  const V=new Uint8Array(N), hist=new Uint32Array(256);
  for(let y=0;y<h;y++) for(let x=0;x<w;x++){ const i=((r.y+y)*W+r.x+x)*4, v=Math.max(data[i],data[i+1],data[i+2]); V[y*w+x]=v; hist[v]++; }
  // размер «пикселя шрифта» (масштаб интерфейса) — тот сдвиг, при котором у ярких пикселей чаще всего находится тень
  let u=0,bestCnt=0; const step=N>2e6?2:1;
  for(let s=1;s<=12;s++){ let cnt=0;
    for(let y=0;y+s<h;y+=step) for(let x=0;x+s<w;x+=step){ const p=y*w+x; if(V[p]<40) continue;
      if(shadowOk(data,W,((r.y+y)*W+r.x+x)*4,((r.y+y+s)*W+r.x+x+s)*4)) cnt++; }
    if(cnt>bestCnt*1.15){ bestCnt=cnt; u=s; } }
  const useShadow=bestCnt*step*step>=25;
  // точный скриншот (PNG без масштабирования): тень совпадает до единицы
  let strict=0; if(useShadow) for(let y=0;y+u<h;y+=step) for(let x=0;x+u<w;x+=step){ if(V[y*w+x]<40) continue;
    if(shadowOk(data,W,((r.y+y)*W+r.x+x)*4,((r.y+y+u)*W+r.x+x+u)*4,true)) strict++; }
  shot.exact=useShadow&&strict>=bestCnt*.6;
  const auto=useShadow?40:Math.max(40,otsu(hist,N)), thr=shot.thr ?? auto;
  $("shotThr").value=thr; $("shotThrVal").textContent=shot.thr==null?"авто ("+thr+")":thr;
  shot.mode=useShadow?"shadow":"bright"; shot.u=u;
  // маска текста
  const M=new Uint8Array(N);
  if(useShadow){
    const q=[]; for(let y=0;y+u<h;y++) for(let x=0;x+u<w;x++){ const p=y*w+x; if(V[p]<thr) continue;
      const i=((r.y+y)*W+r.x+x)*4;
      // сам пиксель — тень другой буквы (у неё тоже может найтись «тень» на тёмном фоне)
      if(x>=u&&y>=u&&shadowOk(data,W,((r.y+y-u)*W+r.x+x-u)*4,i)) continue;
      if(shadowOk(data,W,i,((r.y+y+u)*W+r.x+x+u)*4)){ M[p]=2; q.push(p); } }
    // пиксели внутри буквы, чья тень закрыта соседними пикселями той же буквы: тот же цвет рядом с найденными
    while(q.length){ const p=q.pop(), px=p%w, py=(p-px)/w, i=((r.y+py)*W+r.x+px)*4;
      for(let dy=-1;dy<=1;dy++){ const yy=py+dy; if(yy<0||yy>=h) continue;
        for(let dx=-1;dx<=1;dx++){ const xx=px+dx; if(xx<0||xx>=w) continue; const o=yy*w+xx; if(M[o]||V[o]<thr) continue;
          const j=((r.y+yy)*W+r.x+xx)*4;
          if(Math.abs(data[j]-data[i])<=10&&Math.abs(data[j+1]-data[i+1])<=10&&Math.abs(data[j+2]-data[i+2])<=10){ M[o]=1; q.push(o); } } } }
  } else for(let p=0;p<N;p++) M[p]=V[p]>=thr?1:0;
  // связные области (8 соседей)
  const lab=new Int32Array(N).fill(-1), comps=[], stack=new Int32Array(N);
  for(let p=0;p<N;p++){
    if(!M[p]||lab[p]>=0) continue;
    const id=comps.length, c={x0:w,y0:h,x1:-1,y1:-1,n:0,seed:0,id}; let sp=0; stack[sp++]=p; lab[p]=id;
    while(sp){ const q=stack[--sp], qx=q%w, qy=(q-qx)/w; c.n++; if(M[q]===2) c.seed++;
      if(qx<c.x0)c.x0=qx; if(qx>c.x1)c.x1=qx; if(qy<c.y0)c.y0=qy; if(qy>c.y1)c.y1=qy;
      for(let dy=-1;dy<=1;dy++){ const yy=qy+dy; if(yy<0||yy>=h) continue;
        for(let dx=-1;dx<=1;dx++){ const xx=qx+dx; if(xx<0||xx>=w) continue; const o=yy*w+xx;
          if(lab[o]<0&&M[o]){ lab[o]=id; stack[sp++]=o; } } } }
    comps.push(c);
  }
  // шум, рамки подсказки, подчёркивания
  const big=comps.filter(c=>c.n>=3), mh=median(big.map(c=>c.y1-c.y0+1))||1;
  const keep=comps.filter(c=>{ const cw=c.x1-c.x0+1, ch=c.y1-c.y0+1;
    if(c.n<Math.max(2,useShadow?u*u:mh*mh*.02)) return false;
    if(useShadow&&c.seed<c.n*.2) return false;  // у настоящей буквы тень есть у заметной части пикселей
    if(cw>=8*ch&&cw>mh*3) return false;        // длинная горизонтальная линия
    if(ch>mh*2.6) return false;                 // вертикальная рамка, картинка
    return true; });
  // строки: полосы по вертикали
  const rows=new Uint16Array(h); keep.forEach(c=>{ for(let y=c.y0;y<=c.y1;y++) rows[y]++; });
  const bands=[]; for(let y=0;y<h;y++){ if(!rows[y]) continue; let y2=y; while(y2+1<h&&rows[y2+1]) y2++; bands.push([y,y2]); y=y2; }
  const lines=[];
  for(const [b0,b1] of bands){
    const cs=keep.filter(c=>c.y0>=b0&&c.y1<=b1).sort((a,b)=>a.x0-b.x0);
    if(!cs.length) continue;
    const gl=[]; for(const c of cs){ const g=gl[gl.length-1]; if(g&&c.x0<=g.x1){ g.x1=Math.max(g.x1,c.x1); g.y0=Math.min(g.y0,c.y0); g.y1=Math.max(g.y1,c.y1); g.ids.push(c.id); } else gl.push({x0:c.x0,x1:c.x1,y0:c.y0,y1:c.y1,ids:[c.id]}); }
    const bh=b1-b0+1; if(bh<4) continue;
    // цвет участка буквы: медиана самых ярких пикселей (края при масштабировании размыты)
    const colorOf=(g,xa,xb)=>{ const ids=new Set(g.ids), R=[],G=[],B=[],Vs=[];
      for(let y=g.y0;y<=g.y1;y++) for(let x=xa;x<=xb;x++){ const p=y*w+x; if(!ids.has(lab[p])) continue;
        const i=((r.y+y)*W+r.x+x)*4; R.push(data[i]); G.push(data[i+1]); B.push(data[i+2]); Vs.push(V[p]); }
      if(!Vs.length) return null;
      const cut=median(Vs)*.85, idx=Vs.map((v,i)=>v>=cut?i:-1).filter(i=>i>=0);
      return [R,G,B].map(ch=>Math.round(median(idx.map(i=>ch[i])))); };
    const diff=(a,b)=>Math.max(...a.map((v,i)=>Math.abs(v-b[i])));
    gl.forEach(g=>g.rgb=colorOf(g,g.x0,g.x1));
    const mw=median(gl.map(g=>g.x1-g.x0+1))||1;
    let gs=[];
    for(const g of gl){
      // слипшиеся буквы (на точном скриншоте у соседних букв градиента разный цвет) — делим по смене цвета
      if(shot.exact&&g.x1-g.x0+1>mw*1.4){
        let start=g.x0, prev=null;
        for(let x=g.x0;x<=g.x1;x++){ const c=colorOf(g,x,x); if(!c) continue;
          if(prev&&diff(c,prev)>=2&&x-start>=u*2){ gs.push({...g,x0:start,x1:x-1,rgb:colorOf(g,start,x-1)}); start=x; }
          prev=c; }
        gs.push({...g,x0:start,rgb:colorOf(g,start,g.x1)});
      } else gs.push(g);
    }
    // части одной буквы («ы», «"»): узкий кусок того же цвета вплотную к соседу
    const merged=[]; for(const g of gs){ const m=merged[merged.length-1];
      if(m&&diff(m.rgb,g.rgb)<=1&&g.x0-m.x1-1<=(shot.mode==="shadow"?u:mw*.2)&&Math.min(g.x1-g.x0,m.x1-m.x0)+1<=mw*.4){ m.x1=g.x1; m.ids=m.ids.concat(g.ids); }
      else merged.push({...g}); }
    gs=merged;
    // пробелы: в Minecraft между буквами 1 пиксель шрифта, пробел добавляет ещё ~4
    const gaps=gs.slice(1).map((g,i)=>g.x0-gs[i].x1-1), g0=shot.mode==="shadow"?u:Math.max(median(gaps.filter(v=>v>0)),bh/16,1);
    // очень большой разрыв — это уже не та же строка (например, мусор с фона справа)
    let part=[gs[0]], parts=[part];
    gs.forEach((g,i)=>{ if(!i) return; const gap=gaps[i-1];
      if(gap>g0*14){ part=[g]; parts.push(part); return; }
      if(gap>g0*2.6) for(let s=Math.max(1,Math.round((gap-g0)/(4*g0)));s>0;s--) part.push(null);
      part.push(g); });
    for(const chars of parts){
      const real=chars.filter(Boolean); if(real.length<2||chars.length-real.length>real.length) continue;
      const box={x0:real[0].x0,x1:real[real.length-1].x1,y0:Math.min(...real.map(g=>g.y0)),y1:Math.max(...real.map(g=>g.y1))};
      if(shot.mode==="shadow"&&box.y1-box.y0+1<u*3) continue;   // ниже 3 пикселей шрифта — не текст
      const fit=fitShot(chars);
      if(shot.mode==="shadow"&&fit.err>6&&median(real.map(g=>Math.max(...g.rgb)))<110) continue;   // тёмный шум с фона
      lines.push({box,chars,...fit});
    }
  }
  shot.lines=lines; drawShot(); renderShotLines();
  if(shot.fresh){ shot.fresh=false; track("gen.shot_load",{exact:shot.exact?1:0,lines:lines.length,src:shot.from||"file"}); }
}
/** Вес каждого цвета градиента для символа j из n — та же формула, что в glyphColors / sponsorColors. */
function shotBasis(n,k,j){
  const w=new Float64Array(k); if(k===1){ w[0]=1; return w; }
  if(isSponsor()){ const s=(n>1?j/(n-1):0)*(k-1), a=Math.min(Math.floor(s),k-2); w[a]=1-(s-a); w[a+1]+=s-a; return w; }
  for(let p=0;p<k-1;p++){ const a=Math.round(n*p/(k-1)), b=Math.round(n*(p+1)/(k-1));
    if(j>=a&&j<b){ const m=b-a, t=m>1?(j-a)/(m-1):0; w[p]=1-t; w[p+1]+=t; return w; } }
  w[k-1]=1; return w;
}
function solveLS(rows,ys,k){
  const A=Array.from({length:k},()=>new Float64Array(k+3));
  rows.forEach((w,r)=>{ for(let i=0;i<k;i++){ if(!w[i]) continue; for(let j=0;j<k;j++) A[i][j]+=w[i]*w[j]; for(let c=0;c<3;c++) A[i][k+c]+=w[i]*ys[r][c]; } });
  for(let i=0;i<k;i++) A[i][i]+=1e-6;
  for(let i=0;i<k;i++){ let p=i; for(let r=i+1;r<k;r++) if(Math.abs(A[r][i])>Math.abs(A[p][i])) p=r; [A[i],A[p]]=[A[p],A[i]];
    for(let r=0;r<k;r++){ if(r===i) continue; const f=A[r][i]/A[i][i]; if(f) for(let c=i;c<k+3;c++) A[r][c]-=f*A[i][c]; } }
  return A.map((row,i)=>[0,1,2].map(c=>Math.max(0,Math.min(255,Math.round(row[k+c]/row[i])))));
}
const rgbHex = c => c.map(v=>v.toString(16).padStart(2,"0")).join("").toUpperCase();
function fitShot(chars){
  const n=chars.length, known=chars.map((c,j)=>c?j:-1).filter(j=>j>=0), ys=known.map(j=>chars[j].rgb);
  const codes=[]; ys.forEach(c=>{ const l=nearestLeg(rgbHex(c)); if(codes[codes.length-1]!==l) codes.push(l); });
  const K=Math.max(1,Math.min(isSponsor()?7:6, isSponsor()?known.length:Math.floor(n/2)+1));
  const fits=[];
  for(let k=1;k<=K;k++){
    const rows=known.map(j=>shotBasis(n,k,j)), st=solveLS(rows,ys,k);
    let e=0; rows.forEach((w,r)=>{ for(let c=0;c<3;c++){ let v=0; for(let i=0;i<k;i++) v+=w[i]*st[i][c]; e+=(v-ys[r][c])**2; } });
    fits.push({k,stops:st.map(rgbHex),err:Math.sqrt(e/Math.max(1,ys.length*3))});
  }
  const best=Math.min(...fits.map(f=>f.err)), pick=fits.find(f=>f.err<=best+Math.max(2.5,best*.35))||fits[fits.length-1];
  // одинаковые цвета подряд: часть текста на скриншоте одного цвета, а генератор делит текст на равные части —
  // точная копия требует повторов. Предлагаем и упрощённый вариант без повторов.
  const uniq=pick.stops.filter((c,i,a)=>!i||Math.max(...toRgb(c).map((v,j)=>Math.abs(v-toRgb(a[i-1])[j])))>4);
  const alt=uniq.length<pick.stops.length&&uniq.length>=(isSponsor()?1:2)?fits[uniq.length-1]:null;
  return {stops:pick.stops,err:pick.err,codes:codes.slice(0,LEG_MAX),alt};
}
function drawShot(){
  const v=$("shotCanvas"), g=v.getContext("2d"); g.drawImage(shot.src,0,0);
  const sc=Math.max(1,shot.W/(v.clientWidth||shot.W)), r=shot.sel||{x:0,y:0};
  g.lineWidth=Math.max(1,sc*1.5);
  if(shot.sel){ g.fillStyle="rgba(0,0,0,.55)"; const s=shot.sel;
    g.fillRect(0,0,shot.W,s.y); g.fillRect(0,s.y+s.h,shot.W,shot.H-s.y-s.h); g.fillRect(0,s.y,s.x,s.h); g.fillRect(s.x+s.w,s.y,shot.W-s.x-s.w,s.h);
    g.strokeStyle="#9be052"; g.setLineDash([6*sc,4*sc]); g.strokeRect(s.x+.5,s.y+.5,s.w-1,s.h-1); g.setLineDash([]); }
  const pad=Math.max(2,sc*2);
  shot.lines.forEach((l,i)=>{ const b=l.box, hi=i===shot.hi;
    g.strokeStyle=hi?"#d4ff9a":"rgba(127,191,58,.85)"; g.lineWidth=Math.max(1,sc*(hi?2.5:1.5));
    g.strokeRect(r.x+b.x0-pad,r.y+b.y0-pad,b.x1-b.x0+1+pad*2,b.y1-b.y0+1+pad*2);
    const fs=Math.round(Math.max(10,12*sc)); g.font=fs+"px monospace"; g.fillStyle="#9be052"; g.textBaseline="bottom";
    const label=String(i+1), lw=g.measureText(label).width, lx=r.x+b.x0-pad-lw-3*sc;
    if(lx>=0){ g.textBaseline="middle"; g.fillText(label,lx,r.y+(b.y0+b.y1)/2); } else g.fillText(label,r.x+b.x0-pad,r.y+b.y0-pad-1); });
}
function renderShotLines(){
  const box=$("shotLines"); box.innerHTML="";
  const target=$("targetLabel").textContent, cl=isClassic();
  if(!shot.lines.length){ shotMsg("Текст не найден. Выделите строку с текстом или сдвиньте «Порог».","err"); return; }
  shotMsg("Найдено строк: "+shot.lines.length+". Выберите нужную и нажмите «Применить» — цвета встанут в «"+target+"»."
    +(shot.exact?"":" Картинка сжата, уменьшена или это фото — цвета примерные."));
  shot.lines.forEach((l,i)=>{
    const d=document.createElement("div"); d.className="shot-line";
    const cols=cl?l.codes.map(c=>LEG[c]):l.stops, glyphs=l.chars.filter(Boolean).length;
    const acc=Math.max(0,Math.round(100-l.err/2.55));
    d.innerHTML=`<span class="n">${i+1}</span><div><div class="shot-chars"></div><div class="bar"></div>
      <div class="shot-hex"></div></div><button type="button" class="accent">Применить</button>`;
    const ch=d.querySelector(".shot-chars");
    l.chars.forEach(c=>{ const s=document.createElement("i"); if(c) s.style.background="rgb("+c.rgb.join(",")+")"; else s.className="sp"; ch.appendChild(s); });
    d.querySelector(".bar").style.background=cl?cssHard(l.codes):css(cols.length===1?[cols[0],cols[0]]:cols);
    d.querySelector(".shot-hex").innerHTML = (cl ? l.codes.map(c=>"&amp;"+c).join(" ") : cols.map(c=>"#"+c).join(" → "))
      + ` <span><b>· ${glyphs} букв</b> <b>· ${cl?l.codes.length:cols.length} ${cl?"цв. /colors":"цв."}</b>${cl?"":" <b>· совпадение "+acc+"%</b>"}</span>`;
    if(l.alt&&!cl){ const a=document.createElement("div"); a.className="shot-alt";
      a.innerHTML=`<span>В конце повторяется один цвет: значит, часть текста на скриншоте одного цвета. Без повторов (примерно): </span><b></b> <span>· совпадение ${Math.max(0,Math.round(100-l.alt.err/2.55))}%</span> <button type="button" class="small">Без повторов</button>`;
      a.querySelector("b").textContent=l.alt.stops.map(c=>"#"+c).join(" → ");
      a.querySelector("button").addEventListener("click",()=>applyShot({...l,stops:l.alt.stops}));
      d.children[1].appendChild(a); }
    d.addEventListener("mouseenter",()=>{ shot.hi=i; drawShot(); });
    d.addEventListener("mouseleave",()=>{ shot.hi=null; drawShot(); });
    d.querySelector("button.accent").addEventListener("click",()=>applyShot(l));
    box.appendChild(d);
  });
}
function applyShot(l){
  track("gen.shot_apply",{colors:isClassic()?l.codes.length:l.stops.length,simple:l.alt&&l.stops===l.alt.stops?1:0,classic:isClassic()?1:0});
  const st=cur(), target=$("targetLabel").textContent;
  if(isClassic()) st.legacy=[...l.codes];
  else if(l.stops.length===1&&!isSponsor()){ st.mode="solid"; st.stops=[l.stops[0],st.stops[1]||l.stops[0]]; }
  else { st.mode="gradient"; st.stops=l.stops.length===1&&!isSponsor()?[l.stops[0],l.stops[0]]:[...l.stops]; }
  refresh(); closeShot();
  $("warn").textContent=""; const b=$("shotBtn").querySelector("span"), t=b.textContent;
  b.textContent="Цвета со скриншота → «"+target+"» ✓"; setTimeout(()=>b.textContent=t,1800);
}
function shotPoint(e){ const v=$("shotCanvas"), b=v.getBoundingClientRect();
  return [Math.max(0,Math.min(shot.W,Math.round((e.clientX-b.left)*shot.W/b.width))),Math.max(0,Math.min(shot.H,Math.round((e.clientY-b.top)*shot.H/b.height)))]; }
$("shotCanvas").addEventListener("pointerdown",e=>{ if(!shot.data) return; e.preventDefault(); $("shotCanvas").setPointerCapture(e.pointerId); shot.drag=shotPoint(e); });
$("shotCanvas").addEventListener("pointermove",e=>{ if(!shot.drag) return; const [x,y]=shotPoint(e), [a,b]=shot.drag;
  shot.sel={x:Math.min(a,x),y:Math.min(b,y),w:Math.abs(x-a),h:Math.abs(y-b)}; shot.lines=[]; drawShot(); });
$("shotCanvas").addEventListener("pointerup",()=>{ if(!shot.drag) return; shot.drag=null;
  if(!shot.sel||shot.sel.w<4||shot.sel.h<4) shot.sel=null; analyzeShot(); });
$("shotThr").addEventListener("input",()=>{ if(!shot.data) return; shot.thr=+$("shotThr").value; analyzeShot(); });
$("shotReset").addEventListener("click",()=>{ shot.sel=null; shot.thr=null; analyzeShot(); });
$("shotBtn").addEventListener("click",openShot);
$("shotClose").addEventListener("click",closeShot);
$("shotDlg").addEventListener("click",e=>{ if(e.target===$("shotDlg")) closeShot(); });
$("shotFile").addEventListener("change",e=>{ shot.from="file"; loadShot(e.target.files[0]); e.target.value=""; });
["dragenter","dragover"].forEach(t=>$("shotDlg").addEventListener(t,e=>{ e.preventDefault(); $("shotDrop").classList.add("over"); }));
["dragleave","drop"].forEach(t=>$("shotDlg").addEventListener(t,()=>$("shotDrop").classList.remove("over")));
$("shotDlg").addEventListener("drop",e=>{ e.preventDefault(); shot.from="drop"; loadShot(e.dataTransfer.files[0]); });
document.addEventListener("keydown",e=>{ if(e.key==="Escape"&&!$("shotDlg").hidden) closeShot(); });
// Ctrl+V с картинкой в любом месте страницы открывает окно; обычная вставка текста не трогается
document.addEventListener("paste",e=>{
  const f=[...(e.clipboardData?.items||[])].find(i=>i.kind==="file"&&/^image\//.test(i.type));
  if(!f) return; e.preventDefault(); openShot(); shot.from="paste"; loadShot(f.getAsFile());
});

/* ================= Старт ================= */
if(S.cmd==="am") S.cmd="item";
if(![...$("cmd").options].some(o=>o.value===S.cmd)) S.cmd="item";
$("cmd").value=S.cmd; if(![...$("font").options].some(o=>o.value===S.font)) S.font="normal"; $("font").value=S.font;
$("cmdName").value=S.cmdName; $("cmdLore").value=S.cmdLore; $("tpl").value=S.tpl||DEFAULT_TPL;
$("nickName").value=S.nick; $("nickHex").value=S.nickHex; if(valid(S.nickHex)) $("nickPick").value="#"+S.nickHex;
lastEndSynced=S.sponsor.style.stops[S.sponsor.style.stops.length-1];
$("name").value=S.name.text;
applyMode(); modeAtStartup=false;
