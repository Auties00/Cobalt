import { chromium } from 'playwright';

const CDP = 'http://127.0.0.1:47832';
const TAP = `(() => {
  if (window.__tapInstalled) return 'already';
  window.__pcm = []; window.__pcmRate = 0; window.__t0 = 0;
  const oc = AudioNode.prototype.connect;
  AudioNode.prototype.connect = function(dest, ...r) {
    try {
      if (dest && dest.context && dest === dest.context.destination && !this.__t) {
        const ctx = dest.context; window.__pcmRate = ctx.sampleRate;
        const sp = ctx.createScriptProcessor(4096, 1, 1);
        sp.onaudioprocess = (e) => {
          if (!window.__t0) window.__t0 = performance.now();
          const ch = e.inputBuffer.getChannelData(0);
          let s = 0; for (let i=0;i<ch.length;i++) s += ch[i]*ch[i];
          window.__pcm.push(Math.sqrt(s/ch.length)); // per-buffer RMS (~85ms at 48k/4096)
        };
        this.__t = true; oc.call(this, sp); oc.call(sp, dest); return;
      }
    } catch(_){}
    return oc.call(this, dest, ...r);
  };
  window.__tapInstalled = true; return 'installed';
})()`;

const ANSWER = `(() => {
  const all = [...document.querySelectorAll('button,[role="button"],div[aria-label],span[aria-label]')];
  const a = all.find(e => /accept|answer|rispondi/i.test((e.getAttribute('aria-label')||'')+' '+(e.innerText||'')));
  if (a) { const r=a.getBoundingClientRect();
    ['pointerdown','mousedown','pointerup','mouseup','click'].forEach(t=>a.dispatchEvent(new MouseEvent(t,{bubbles:true,cancelable:true,clientX:r.x+r.width/2,clientY:r.y+r.height/2})));
    return 'answered: '+((a.getAttribute('aria-label')||a.innerText||'').trim()); }
  return 'no-answer-btn; labels='+all.map(e=>(e.getAttribute('aria-label')||e.innerText||'').trim()).filter(Boolean).slice(0,20).join(' | ');
})()`;

const browser = await chromium.connectOverCDP(CDP);
const ctx = browser.contexts()[0];
const knownUrls = new Set(ctx.pages().map(p => p.url()));
console.log('connected; existing pages=' + ctx.pages().length);

// Install tap on all existing pages too (in case the call reuses the main window's audio context)
for (const p of ctx.pages()) { try { console.log('tap@existing: ' + await p.evaluate(TAP)); } catch(_){} }

let callPage = null;
ctx.on('page', (p) => { console.log('NEW PAGE: ' + p.url()); if (!callPage) callPage = p; });

console.log('waiting for call window (up to 70s)... PLACE THE CALL NOW');
const t0 = Date.now();
while (!callPage && Date.now() - t0 < 70000) {
  await new Promise(r => setTimeout(r, 500));
  // also detect a newly-appeared page among contexts
  for (const p of ctx.pages()) { if (!knownUrls.has(p.url())) { callPage = p; break; } }
}
if (!callPage) { console.log('NO CALL WINDOW APPEARED'); process.exit(1); }

await callPage.waitForLoadState('domcontentloaded').catch(()=>{});
console.log('call window: ' + callPage.url());
console.log('tap@call: ' + await callPage.evaluate(TAP).catch(e=>'taperr:'+e));

// answer (retry a few times as UI renders)
for (let i=0;i<12;i++) {
  const res = await callPage.evaluate(ANSWER).catch(e=>'err:'+e);
  console.log('answer try '+i+': ' + res);
  if (res.startsWith('answered')) break;
  await new Promise(r => setTimeout(r, 700));
}

console.log('capturing 18s of audio...');
await new Promise(r => setTimeout(r, 18000));

const env = await callPage.evaluate(() => ({ rate: window.__pcmRate, rms: window.__pcm || [] })).catch(e=>({err:String(e)}));
const rms = env.rms || [];
// each entry ~ 4096 samples = 4096/rate sec
const bufSec = env.rate ? 4096/env.rate : 0;
const active = rms.filter(v => v > 0.01).length;
const silent = rms.filter(v => v < 0.001).length;
// count active->silent->active transitions (dropouts) ignoring leading/trailing silence
let gaps = 0; let seenAudio = false;
for (let i=1;i<rms.length;i++){ if (rms[i-1] > 0.01) seenAudio = true; if (seenAudio && rms[i-1] > 0.01 && rms[i] < 0.001) gaps++; }
console.log('=== AUDIO ANALYSIS ===');
console.log('rate='+env.rate+' buffers='+rms.length+' (~'+(rms.length*bufSec).toFixed(1)+'s) bufSec='+bufSec.toFixed(3));
console.log('active(>0.01)='+active+' silent(<0.001)='+silent+' dropouts(active->silent)='+gaps);
console.log('envelope(x100): ' + rms.slice(0, 120).map(v=>Math.min(99,Math.round(v*100))).join(','));
process.exit(0);
