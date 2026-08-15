/**
 * Generates app/src/main/java/com/stampbook/app/data/country/CountryDetails.kt —
 * the per-country traits that make a stamp feel like it belongs to its country.
 *
 * Inputs (not runtime dependencies; fetch them by hand when regenerating):
 *   npm pack flag-icons@7.2.3 world-countries@5.0.0
 *
 *   node tools/build_country_details.mjs <dir-with-extracted-packages>
 *
 * Three traits come out of it:
 *   ink    the flag's dominant hue, muted into something that reads as stamp ink
 *   entry  the word a border post would actually print, in the country's language
 *   region which design family the country's stamp belongs to
 *
 * Rasterising the flags needs a browser, so this runs under Playwright rather
 * than as a plain script.
 */
import { chromium } from 'playwright';
import { readFileSync, writeFileSync, existsSync } from 'fs';
import { join } from 'path';

const root = process.argv[2] || '.';
const KT_OUT = 'app/src/main/java/com/stampbook/app/data/country/CountryDetails.kt';

/** What a border post prints, by primary language. */
const ENTRY_WORD = {
  eng: 'ENTRY', fra: 'ENTRÉE', spa: 'ENTRADA', por: 'ENTRADA', cat: 'ENTRADA',
  deu: 'EINREISE', nld: 'INREIS', afr: 'INGANG', ita: 'INGRESSO', ron: 'INTRARE',
  dan: 'INDREJSE', kal: 'INDREJSE', swe: 'INRESA', nno: 'INNREISE', nob: 'INNREISE',
  isl: 'KOMA', fin: 'MAAHANTULO', est: 'SISSESÕIT', lav: 'IEBRAUKŠANA',
  lit: 'ĮVAŽIAVIMAS', pol: 'WJAZD', ces: 'VSTUP', slk: 'VSTUP', slv: 'VSTOP',
  hun: 'BELÉPÉS', sqi: 'HYRJE', bos: 'ULAZAK', hrv: 'ULAZAK', srp: 'ULAZAK',
  cnr: 'ULAZAK', tur: 'GİRİŞ', aze: 'GİRİŞ', msa: 'MASUK', ind: 'MASUK',
  vie: 'NHẬP CẢNH', crs: 'ENTRÉE',
  ara: 'دخول', fas: 'ورود', prs: 'ورود',
  rus: 'ВЪЕЗД', bel: 'УЕЗД', ukr: 'В’ЇЗД', bul: 'ВХОД', mkd: 'ВЛЕЗ',
  kaz: 'КІРУ', kir: 'КИРҮҮ', mon: 'ОРЛОО',
  ell: 'ΕΙΣΟΔΟΣ', hye: 'ՄՈՒՏՔ', kat: 'შესვლა', amh: 'መግቢያ',
  zho: '入境', jpn: '上陸許可', kor: '입국', tha: 'ขาเข้า', khm: 'ចូល', lao: 'ເຂົ້າ',
  ben: 'প্রবেশ', nep: 'प्रवेश', hin: 'प्रवेश', swa: 'KUINGIA',
};

/**
 * Which of a country's official languages a border post would print in. English
 * ranks last on purpose: it is already the fallback, so preferring a local
 * language where one exists is what makes the stamps worth looking at. Every
 * language here is official in the country that gets it.
 */
const LANGUAGE_RANK = [
  'spa', 'fra', 'por', 'ara', 'rus', 'zho', 'deu', 'jpn', 'kor', 'ita', 'nld',
  'tur', 'tha', 'vie', 'ind', 'msa', 'pol', 'ron', 'ell', 'ces', 'hun', 'swe',
  'dan', 'nno', 'nob', 'fin', 'isl', 'est', 'lav', 'lit', 'slk', 'slv', 'hrv',
  'srp', 'bos', 'cnr', 'sqi', 'mkd', 'bul', 'ukr', 'bel', 'kaz', 'kir', 'mon',
  'hye', 'kat', 'fas', 'prs', 'aze', 'amh', 'khm', 'lao', 'ben', 'nep', 'hin',
  'afr', 'swa', 'crs', 'cat', 'kal', 'eng',
];

/** Design families. Broadly follows what each region's real stamps look like. */
const REGION = {
  'Western Europe': 'EUROPE', 'Central Europe': 'EUROPE', 'Northern Europe': 'EUROPE',
  'Southern Europe': 'EUROPE', 'Southeast Europe': 'EUROPE', 'Eastern Europe': 'EUROPE',
  'Western Asia': 'LEVANT', 'Northern Africa': 'LEVANT', 'Central Asia': 'LEVANT',
  'Southern Asia': 'MONSOON', 'South-Eastern Asia': 'MONSOON',
  'Eastern Asia': 'EAST_ASIA',
  'Eastern Africa': 'AFRICA', 'Western Africa': 'AFRICA', 'Middle Africa': 'AFRICA',
  'Southern Africa': 'AFRICA',
  'North America': 'AMERICAS', 'Central America': 'AMERICAS', 'Caribbean': 'AMERICAS',
  'South America': 'AMERICAS',
  'Australia and New Zealand': 'PACIFIC', 'Melanesia': 'PACIFIC',
  'Micronesia': 'PACIFIC', 'Polynesia': 'PACIFIC', 'Antarctic': 'PACIFIC',
};

const countries = JSON.parse(readFileSync(join(root, 'package/world-countries.json'), 'utf8'));
const byCode = Object.fromEntries(countries.map(c => [c.cca2, c]));

const source = readFileSync('app/src/main/java/com/stampbook/app/data/country/Countries.kt', 'utf8');
const codes = [...source.matchAll(/c\("([A-Z]{2})"/g)].map(m => m[1]);

const flags = codes.map(code => {
  const path = join(root, `flag-icons/flags/4x3/${code.toLowerCase()}.svg`);
  return {
    code,
    uri: existsSync(path)
      ? 'data:image/svg+xml;base64,' + readFileSync(path).toString('base64')
      : null,
  };
});

const browser = await chromium.launch();
const page = await browser.newPage();
const { ink, missing } = await page.evaluate(async (flags) => {
  const canvas = document.createElement('canvas');
  canvas.width = 64; canvas.height = 48;
  const ctx = canvas.getContext('2d', { willReadFrequently: true });

  const rgbToHsl = (r, g, b) => {
    r/=255; g/=255; b/=255;
    const mx = Math.max(r,g,b), mn = Math.min(r,g,b), l = (mx+mn)/2;
    let h = 0, s = 0;
    if (mx !== mn){
      const d = mx - mn;
      s = l > 0.5 ? d/(2-mx-mn) : d/(mx+mn);
      if (mx === r) h = ((g-b)/d + (g < b ? 6 : 0));
      else if (mx === g) h = (b-r)/d + 2;
      else h = (r-g)/d + 4;
      h *= 60;
    }
    return [h, s, l];
  };
  const hslToRgb = (h, s, l) => {
    h = ((h % 360) + 360) % 360 / 360;
    const f = (p, q, t) => {
      if (t < 0) t += 1; if (t > 1) t -= 1;
      if (t < 1/6) return p + (q-p)*6*t;
      if (t < 1/2) return q;
      if (t < 2/3) return p + (q-p)*(2/3-t)*6;
      return p;
    };
    if (s === 0){ const v = Math.round(l*255); return [v,v,v]; }
    const q = l < 0.5 ? l*(1+s) : l+s-l*s, p = 2*l-q;
    return [f(p,q,h+1/3), f(p,q,h), f(p,q,h-1/3)].map(v => Math.round(v*255));
  };

  const ink = {}, missing = [];
  for (const flag of flags){
    if (!flag.uri){ missing.push(flag.code); continue; }
    const img = new Image();
    const loaded = await new Promise(res => {
      img.onload = () => res(true);
      img.onerror = () => res(false);
      img.src = flag.uri;
    });
    if (!loaded){ missing.push(flag.code); continue; }
    ctx.clearRect(0, 0, 64, 48);
    ctx.drawImage(img, 0, 0, 64, 48);
    const data = ctx.getImageData(0, 0, 64, 48).data;
    // Bucket by hue: a flag's white and black fields say nothing about its identity.
    const buckets = new Map();
    for (let i = 0; i < data.length; i += 4){
      if (data[i+3] < 200) continue;
      const [h, s, l] = rgbToHsl(data[i], data[i+1], data[i+2]);
      if (s < 0.18 || l > 0.92 || l < 0.06) continue;
      const key = Math.round(h / 12);
      const b = buckets.get(key) || { n: 0, h: 0, s: 0, l: 0 };
      b.n++; b.h += h; b.s += s; b.l += l;
      buckets.set(key, b);
    }
    let best = null;
    buckets.forEach(b => { if (!best || b.n > best.n) best = b; });
    // Mute it: flag vinyl is far brighter than anything a rubber stamp leaves.
    const rgb = best
      ? hslToRgb(best.h/best.n,
          Math.min(Math.max(best.s/best.n, 0.42), 0.68),
          Math.min(Math.max(best.l/best.n * 0.62, 0.20), 0.32))
      : [43, 47, 54];
    ink[flag.code] = rgb.map(v => v.toString(16).padStart(2, '0')).join('').toUpperCase();
  }
  return { ink, missing };
}, flags);
await browser.close();

const rows = codes.map(code => {
  const meta = byCode[code];
  // world-countries lists languages alphabetically, which is meaningless: Peru
  // leads with Aymara and Belgium with German. Rank them instead.
  const spoken = Object.keys((meta && meta.languages) || {})
    .filter(code => ENTRY_WORD[code])
    .sort((a, b) => LANGUAGE_RANK.indexOf(a) - LANGUAGE_RANK.indexOf(b));
  const entry = ENTRY_WORD[spoken[0]] || 'ENTRY';
  const region = (meta && REGION[meta.subregion || meta.region]) || 'AMERICAS';
  const alpha3 = (meta && meta.cca3) || code;
  return `        d("${code}", "${alpha3}", 0xFF${ink[code] || '2B2F36'}, "${entry}", ${region}),`;
});

writeFileSync(KT_OUT, `package com.stampbook.app.data.country

import com.stampbook.app.core.CountryTraits
import com.stampbook.app.core.DesignRegion
import com.stampbook.app.core.DesignRegion.AFRICA
import com.stampbook.app.core.DesignRegion.AMERICAS
import com.stampbook.app.core.DesignRegion.EAST_ASIA
import com.stampbook.app.core.DesignRegion.EUROPE
import com.stampbook.app.core.DesignRegion.LEVANT
import com.stampbook.app.core.DesignRegion.MONSOON
import com.stampbook.app.core.DesignRegion.PACIFIC

/**
 * GENERATED by tools/build_country_details.mjs — do not edit by hand.
 *
 * What makes a country's stamp its own: the ink is its flag's dominant hue muted
 * to something a rubber stamp could leave, the wording is what a border post in
 * that country would actually print, and the region picks the design family.
 */
object CountryDetails {

    private fun d(code: String, alpha3: String, ink: Long, entry: String, region: DesignRegion) =
        code to CountryTraits(code, alpha3, ink.toInt(), entry, region)

    private val byCode: Map<String, CountryTraits> = mapOf(
${rows.join('\n')}
    )

    /** Falls back to a neutral design rather than failing on an unknown code. */
    operator fun get(code: String): CountryTraits =
        byCode[code.uppercase()] ?: CountryTraits.unknown(code.uppercase())

    val all: Collection<CountryTraits> get() = byCode.values
}
`);

console.log(`${rows.length} countries -> ${KT_OUT}`);
if (missing.length) console.log('no flag artwork:', missing.join(', '));
