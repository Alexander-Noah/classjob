const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '../..');
const tables = JSON.parse(fs.readFileSync(path.join(__dirname,'schema-manifest.json'),'utf8'));
const columns = Object.fromEntries(Object.entries(tables).map(([t,s])=>[t,new Set([...s.matchAll(/(?:\(|,)\s*`?(\w+)`?\s+(?:bigint|int|tinyint|varchar|text|longtext|mediumtext|json|datetime|date|decimal|double|float|timestamp|char|boolean)\b/gi)].map(m=>m[1].toLowerCase()))]));
function files(dir) {return fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>e.isDirectory()?files(path.join(dir,e.name)):[path.join(dir,e.name)]);}
const errors = [], refs = new Set();
let insertColumns = 0, qualifiedColumns = 0;
for(const file of files(path.join(root,'src/main')).filter(f=>/\.(java|xml)$/.test(f))) {
 let text = fs.readFileSync(file,'utf8');
 // Ignore Java/MyBatis placeholders and quoted strings when checking qualified SQL identifiers.
 text = text.replace(/#\{[^}]*\}|\$\{[^}]*\}/g,'?');
 for(const m of text.matchAll(/insert\s+into\s+(\w+)\s*\(([^)]+)\)/gi)) {
  const table = m[1]; if(!columns[table]) {errors.push(`${file}: missing INSERT table ${table}`);continue;}
  refs.add(table);
  for(const col of m[2].split(',').map(x=>x.trim()).filter(x=>/^\w+$/.test(x))) {insertColumns++;if(!columns[table].has(col.toLowerCase())) errors.push(`${table}.${col} (INSERT)`);}
 }
 const aliases = new Map();
 for(const m of text.matchAll(/\b(?:from|join)\s+(\w+)(?:\s+(\w+))?/gi)) {
  if(!columns[m[1]]) continue;
  refs.add(m[1]); aliases.set(m[1],new Set([m[1]]));
  if(m[2]&&!/^(where|on|set|left|right|inner|join|group|order|limit|union|having)$/i.test(m[2])) {
   if(!aliases.has(m[2]))aliases.set(m[2],new Set());aliases.get(m[2]).add(m[1]);
  }
 }
 const clean = text.replace(/'(?:''|[^'])*'/g,"''");
 for(const m of clean.matchAll(/\b(\w+)\.(\w+)\b/g)) {
  if(!aliases.has(m[1]))continue;
  qualifiedColumns++;
  if(![...aliases.get(m[1])].some(t=>columns[t].has(m[2].toLowerCase())))errors.push(`${path.basename(file)}: ${m[1]}.${m[2]}`);
 }
}
console.log(JSON.stringify({tables:Object.keys(tables).length,referencedTables:refs.size,insertColumns,qualifiedColumns,errors:[...new Set(errors)]},null,2));
if(errors.length)process.exitCode=1;
