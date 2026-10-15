/* Логика &k (obfuscated) и формата по символам — без DOM, та же, что в моде FSTWEAK (HexCore в hexgen-1.21.11).
 * Подключается перед generator.js (window.GenCore); в node — через require (module.exports).
 * Биты формата как в моде: l=1, o=2, n=4, m=8, k=16; коды пишутся в порядке «lonmk». */
(function(root,factory){
  const api=factory();
  if(typeof module==="object"&&module.exports) module.exports=api; else root.GenCore=api;
})(typeof self!=="undefined"?self:this,function(){
  const LETTERS="lonmk", BIT={l:1,o:2,n:4,m:8,k:16}, K=16, LINES=4|8;   // LINES — подчёркнутый и зачёркнутый
  const MM=["b","i","u","st","obf"], BB=["B","I","U","S",null];        // в BBCode тега для &k нет

  const fmtBits = f => f ? [...LETTERS].reduce((b,c,i)=>f[c]?b|(1<<i):b,0) : 0;
  const codes = (b,sign="&") => [...LETTERS].filter((c,i)=>b&(1<<i)).map(c=>sign+c).join("");

  /* ---------- коды прямо в поле: &k / &K / §k — «бегущие» символы, &r / &R / §r — сброс ---------- */
  function isCode(s,i){
    if(i+1>=s.length) return false;
    const a=s[i], b=s[i+1].toLowerCase();
    return (a==="&"||a==="§")&&(b==="k"||b==="r");
  }
  /** Текст поля, разобранный на видимые символы (кодовые точки): bits — K от кодов, rawAt — позиция символа в поле (UTF-16). */
  function inline(raw){
    const s=raw==null?"":String(raw), bits=[], at=[];
    let vis="", obf=false;
    for(let i=0;i<s.length;){
      if(isCode(s,i)){ obf=s[i+1].toLowerCase()==="k"; i+=2; continue; }
      const ch=String.fromCodePoint(s.codePointAt(i));
      vis+=ch; bits.push(obf?K:0); at.push(i); i+=ch.length;
    }
    at.push(s.length);
    return {visible:vis, bits, rawAt:at, length:bits.length,
      /** Сколько видимых символов стоит перед позицией курсора в поле. */
      visibleIndex(p){ let i=0; while(i<bits.length&&at[i]<p) i++; return i; },
      /** Позиция в поле перед видимым символом с этим номером. */
      rawOffset(v){ return at[Math.max(0,Math.min(v,bits.length))]; }};
  }
  function hasInlineCodes(raw){
    const s=raw==null?"":String(raw);
    for(let i=0;i+1<s.length;i++) if(isCode(s,i)) return true;
    return false;
  }

  /** Маска на изменённый текст: общее начало и конец сохраняются, вставленные символы берут формат соседа слева (HexCore.spliceMask). */
  function spliceMask(oldText,newText,oldMask,fallback){
    const o=[...oldText], n=[...newText];
    if(!Array.isArray(oldMask)||oldMask.length!==o.length) return n.map(()=>fallback);
    let p=0; while(p<o.length&&p<n.length&&o[p]===n[p]) p++;
    let sfx=0; while(sfx<o.length-p&&sfx<n.length-p&&o[o.length-1-sfx]===n[n.length-1-sfx]) sfx++;
    const inherit = p>0 ? oldMask[p-1] : o.length>0 ? oldMask[0] : fallback;
    const m=[];
    for(let i=0;i<n.length;i++) m.push(i<p ? oldMask[i] : i>=n.length-sfx ? oldMask[o.length-(n.length-i)] : inherit);
    return m;
  }
  /** Итоговый формат каждого видимого символа: своя маска (или формат строки) | &k от кодов в поле. */
  const effective = (mask,inlBits,fb) => inlBits.map((b,i)=>(Array.isArray(mask)&&i<mask.length?mask[i]:fb)|b);
  /** Общий формат всех символов или -1, если он меняется по ходу текста. */
  function uniform(bits,fb){
    if(!bits.length) return fb;
    return bits.every(b=>b===bits[0]) ? bits[0] : -1;
  }

  /* ---------- цвет: та же формула, что в генераторе и в моде ---------- */
  const toRgb = h => [0,2,4].map(i=>parseInt(h.slice(i,i+2),16));
  const mix = (a,b,t) => { const A=toRgb(a),B=toRgb(b);
    return A.map((v,i)=>Math.round(v+(B[i]-v)*t).toString(16).padStart(2,"0")).join("").toUpperCase(); };
  function segments(chars,n){
    const parts=n-1, out=[];
    for(let k=0;k<parts;k++){
      const a=Math.round(chars.length*k/parts), b=Math.round(chars.length*(k+1)/parts);
      out.push(chars.slice(a,b));
    }
    return out;
  }

  /* ---------- вывод. Строка: text — видимый текст в шрифте, chars — он же по символам, bits — формат символов, fb — формат строки ---------- */
  /** AgeMagic {#A>}…{#B<>}…{#C<}: формат один на всю строку — как раньше; меняется по ходу — метки на границах (runs). */
  function ageMagic(L,st){
    const u=uniform(L.bits,L.fb);
    if(u<0) return runs(L.chars,st,L.bits);
    const c=codes(u); let s="";
    segments([...L.text],st.length).forEach((seg,k)=>{ s+=`{#${st[k]}${k===0?">":"<>"}}`+c+seg.join(""); });
    return s+`{#${st[st.length-1]}<}`;
  }
  /**
   * Градиент, в котором формат меняется по ходу текста (HexCore.runsCommand). Сервер сбрасывает формат на каждой
   * метке {#…}, поэтому на границе формата ставится метка {#RRGGBB<>} цветом того же градиента посередине между
   * соседними буквами, а за ней — коды нового куска. Так строка короче, чем цвет на каждой букве.
   */
  function runs(chars,st,mask){
    const n=chars.length, parts=Math.max(1,st.length-1), segStart=[];
    for(let k=0;k<=parts;k++) segStart.push(Math.round(n*k/parts));
    let s="", seg=0;
    for(let i=0;i<n;i++){
      while(seg+1<parts&&i>=segStart[seg+1]) seg++;
      const segB=i===segStart[seg], fmtB=i>0&&mask[i]!==mask[i-1];
      if(i===0) s+=`{#${st[0]}>}`;
      else if(segB) s+=`{#${st[seg]}<>}`;
      else if(fmtB){
        const a=segStart[seg], len=segStart[seg+1]-a, t=len>1?(i-0.5-a)/(len-1):0;
        s+=`{#${mix(st[seg],st[Math.min(seg+1,st.length-1)],Math.max(0,Math.min(1,t)))}<>}`;
      }
      if(i===0||segB||fmtB) s+=codes(mask[i]);
      s+=chars[i];
    }
    return s+`{#${st[st.length-1]}<}`;
  }

  const mmEscape = c => c.replace(/\\/g,"\\\\").replace(/</g,"\\<");
  const mmOpen = b => MM.filter((t,i)=>b&(1<<i)).map(t=>"<"+t+">").join("");
  const mmClose = b => MM.filter((t,i)=>b&(1<<i)).reverse().map(t=>"</"+t+">").join("");
  /** Закрыть старые и открыть новые теги формата (BBCode, MiniMessage), как HexCore.switchTags. */
  function switchTags(from,to,bb){
    if(bb){ from&=~K; to&=~K; }
    if(from===to) return "";
    let s="";
    for(let i=4;i>=0;i--) if(from&(1<<i)) s+=bb?"[/"+BB[i]+"]":"</"+MM[i]+">";
    for(let i=0;i<5;i++) if(to&(1<<i)) s+=bb?"["+BB[i]+"]":"<"+MM[i]+">";
    return s;
  }
  /**
   * Другие форматы. glyphs — буквы с цветом ({c,hex}), bits — формат каждой из них, u — общий формат или -1,
   * mode/stops — цвета строки (для MiniMessage), tpl — шаблон BirdFlop.
   */
  function formatOther(kind,o){
    const {glyphs,bits,u}=o;
    if(kind==="minimessage"){
      if(u>=0){
        const open = o.mode==="solid" ? "<#"+o.stops[0]+">" : "<gradient:"+o.stops.map(h=>"#"+h).join(":")+">";
        const close = o.mode==="solid" ? "" : "</gradient>";
        return open+mmOpen(u)+[...o.text].map(mmEscape).join("")+mmClose(u)+close;
      }
    }
    if(kind==="minimessage"||kind==="bbcode"){
      const bb=kind==="bbcode"; let s="", open=0;
      glyphs.forEach((g,i)=>{
        s+=switchTags(open,bits[i],bb); open=bits[i];
        if(g.c===" "&&!(bits[i]&LINES)) s+=" ";
        else s+=bb ? "[COLOR=#"+g.hex+"]"+g.c+"[/COLOR]" : "<#"+g.hex+">"+mmEscape(g.c);
      });
      return s+switchTags(open,0,bb);
    }
    if(kind==="birdflop"){
      return glyphs.map((g,i)=>{ let part=o.tpl; for(let k=0;k<6;k++) part=part.split("$"+(k+1)).join(g.hex[k]);
        return part.split("$f").join(codes(bits[i])).split("$c").join(g.c); }).join("");
    }
    const color=h=>kind==="nickname"?"&#"+h : kind==="chat"?"<#"+h+">"
      : kind==="legacy"?"&x"+[...h].map(d=>"&"+d).join("") : "§x"+[...h].map(d=>"§"+d).join("");
    const sign=kind==="console"?"§":"&";
    let s="", prev=-1;
    glyphs.forEach((g,i)=>{
      if(g.c===" "&&bits[i]===prev&&!(bits[i]&LINES)){ s+=" "; return; }
      s+=color(g.hex)+codes(bits[i],sign)+g.c; prev=bits[i];
    });
    return s;
  }
  /** Цвета /colors: &c текст &6 текст… Коды формата — после каждого цвета; на границе формата цвет повторяется (он сбрасывает формат). */
  function classicBody(L,cs){
    const u=uniform(L.bits,L.fb);
    if(u>=0){
      const c=codes(u), chars=[...L.text];
      if(!cs.length) return c+L.text;
      const segs = cs.length===1 ? [chars] : segments(chars,cs.length+1);
      return segs.map((seg,i)=>seg.length?"&"+cs[i]+c+seg.join(""):"").join("");
    }
    const n=L.chars.length, idx=L.chars.map((_,i)=>i);
    const segs = cs.length<=1 ? [idx] : segments(idx,cs.length+1);
    let s="";
    segs.forEach((seg,k)=>seg.forEach((j,p)=>{
      if(p===0||L.bits[j]!==L.bits[j-1]) s+=(cs.length?"&"+cs[k]:j>0?"&r":"")+codes(L.bits[j]);
      s+=L.chars[j];
    }));
    return n?s:"";
  }
  /** /sponsor editprefix: префикс в теге цвета (градиента), формат по символам внутри, на ник не переходит. */
  function editPrefix(L,tag){
    const u=uniform(L.bits,L.fb);
    if(u>=0) return tag+mmOpen(u)+[...L.text].map(mmEscape).join("")+mmClose(u);
    let s=tag, open=0;
    L.chars.forEach((c,i)=>{ s+=switchTags(open,L.bits[i],false); open=L.bits[i]; s+=mmEscape(c); });
    return s+switchTags(open,0,false);
  }

  return {LETTERS,BIT,K,fmtBits,codes,inline,hasInlineCodes,spliceMask,effective,uniform,mix,segments,
    ageMagic,runs,formatOther,classicBody,editPrefix,mmOpen,mmEscape};
});
