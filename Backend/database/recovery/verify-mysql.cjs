const fs=require('node:fs'), path=require('node:path'), cp=require('node:child_process');
const mysql=process.env.RECOVERY_MYSQL_EXE || 'C:/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe';
const checks=JSON.parse(fs.readFileSync(path.join(__dirname,'mapper-select-checks.json'),'utf8'));
const failures=[];
for(const [name,sql] of checks){
 const r=cp.spawnSync(mysql,['-h','127.0.0.1','-u','root','--default-character-set=utf8mb4','--batch','smart_lab_recovered_20261015'],{input:'EXPLAIN '+sql+';',encoding:'utf8',windowsHide:true});
 if(r.status!==0)failures.push({name,error:r.stderr||String(r.error)});
}
const report={checked:checks.length,passed:checks.length-failures.length,failures};
fs.writeFileSync(path.join(__dirname,'mysql-query-verification.json'),JSON.stringify(report,null,2));
console.log(JSON.stringify(report,null,2));
if(failures.length)process.exitCode=1;
