'use strict';
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),cp=require('node:child_process'),vm=require('node:vm');
const backend=path.resolve(__dirname,'../../..'),workspace=path.dirname(backend);
const parser=require(path.join(workspace,'web/node_modules/@babel/parser'));
const {run:coreSmoke}=require('../20261015-recovered-consolidation/Smoke-CoreApi.cjs');
const schema='smart_lab_feature_ui_it_'+crypto.randomUUID().replace(/-/g,'');
const archive=path.join(__dirname,'.local-archives','verify-'+new Date().toISOString().replace(/[:.]/g,'-'));
const mysql=process.env.RECOVERY_MYSQL_EXE || 'C:/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe';
const dump=process.env.RECOVERY_MYSQLDUMP_EXE || 'C:/Program Files/MySQL/MySQL Server 8.4/bin/mysqldump.exe';
const common=['--host=127.0.0.1','--port=3306','--user=root','--default-character-set=utf8mb4'];
const report={schema,startedAt:new Date().toISOString(),success:false,checks:[]};
let child,created=false;
function check(ok,message){if(!ok)throw new Error(message)}
function native(exe,args,options={}){const r=cp.spawnSync(exe,args,{encoding:'utf8',windowsHide:true,maxBuffer:20*1024*1024,...options});check(!r.error&&r.status===0,'Local database command failed');return r.stdout||''}
function query(sql,db){return native(mysql,[...common,'--batch','--skip-column-names','--raw',...(db?['--database='+db]:[])],{input:sql}).trim()}
function note(message){report.checks.push(message);console.log('PASS '+message)}
function frontendMenus(role,user){
 const context=vm.createContext({DEFAULT_AUTH_ROLE:'labAdmin',markRaw:x=>x});
 for(const name of ['routeModules','permissions','navigation']){
  const source=fs.readFileSync(path.join(workspace,'web/src/constants/'+name+'.js'),'utf8');
  const imports=parser.parse(source,{sourceType:'module'}).program.body.filter(n=>n.type==='ImportDeclaration');
  for(const imported of imports)for(const spec of imported.specifiers){if(!(spec.local.name in context))context[spec.local.name]={}}
  let code=source;for(const imported of imports.reverse())code=code.slice(0,imported.start)+code.slice(imported.end);
  code=code.replace(/\bexport\s+/g,'');
  vm.runInContext(code,context,{timeout:1000});
 }
 return vm.runInContext('getRoleMenus',context)(role,user);
}
const base='http://127.0.0.1:18089';
async function request(endpoint,token,method='GET',body,expected=200){
 const response=await fetch(base+endpoint,{method,headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},...(body?{body:JSON.stringify(body)}:{}),signal:AbortSignal.timeout(15000)});
 check(response.status===expected,method+' '+endpoint+' HTTP '+response.status);
 if(expected!==200)return;
 const result=await response.json();check(result.code===200,method+' '+endpoint+' business response failed');return result.data;
}
async function main(){
 check(process.env.MYSQL_PWD,'MYSQL_PWD is required');
 fs.mkdirSync(archive,{recursive:true});
 const snapshot=path.join(archive,'verification-clone.sql');
 native(dump,[...common,'--single-transaction','--hex-blob','--no-tablespaces','--set-gtid-purged=OFF','--result-file='+snapshot,'smart_lab_basic']);
 query('create database `'+schema+'` character set utf8mb4 collate utf8mb4_general_ci;');created=true;
 const input=fs.openSync(snapshot,'r');try{native(mysql,[...common,'--database='+schema],{stdio:[input,'pipe','pipe']})}finally{fs.closeSync(input)}
 const checks=JSON.parse(fs.readFileSync(path.join(backend,'database/recovery/mapper-select-checks.json'),'utf8'));
 for(const[name,sql]of checks){try{query('explain '+sql+';',schema)}catch{throw new Error('SELECT verification failed: '+name)}}
 note(checks.length+' SELECT plans on a full isolated clone');
 const credentials=fs.readFileSync(path.join(backend,'database/recovery/.local-admin-credentials.txt'),'utf8');
 const username=credentials.match(/^系统登录用户名\s*[:：=]\s*(.+)$/m)?.[1]?.trim();
 const password=credentials.match(/^系统登录密码\s*[:：=]\s*(.+)$/m)?.[1]?.trim();check(username&&password,'Administrator credentials missing');
 const log=fs.openSync(path.join(archive,'backend.log'),'w');
 child=cp.spawn('java',['-jar',path.join(backend,'target/Backend-0.0.1-SNAPSHOT.jar'),'--server.port=18089'],{cwd:backend,windowsHide:true,stdio:['ignore',log,log],env:{...process.env,SMART_LAB_DB_URL:'jdbc:mysql://127.0.0.1:3306/'+schema+'?useSSL=false&allowPublicKeyRetrieval=true',SMART_LAB_DB_USERNAME:'root',SMART_LAB_DB_PASSWORD:process.env.MYSQL_PWD,SMART_LAB_FLYWAY_ENABLED:'false'}});fs.closeSync(log);child.on('error',()=>{});
 let ready=false;for(let i=0;i<60;i++){check(child.exitCode==null,'Test backend exited; inspect the local log');try{const r=await fetch(base+'/auth/profile',{signal:AbortSignal.timeout(1000)});if([200,401,403].includes(r.status)){ready=true;break}}catch{}await new Promise(r=>setTimeout(r,1000))}check(ready,'Test backend readiness timeout');
 await coreSmoke({BASE_URL:base,ADMIN_USERNAME:username,ADMIN_PASSWORD:password},globalThis.fetch,()=>{});note('20 original core API reads and administrator login');
 const admin=await request('/auth/login',null,'POST',{username,password});
 const endpoints=['/consumables','/consumables/stock-records','/iot/devices','/iot/hardware','/iot/command-logs','/device-status/summary','/device-status/events','/system-settings','/admin/activation-codes','/activation-codes/lab-bindings','/environment-templates','/course-environment-requests','/lab-software','/schedule-adjustments','/ai-assistant/user-config','/ai-assistant/conversations'];
 for(const endpoint of endpoints)await request(endpoint,admin.token);
 note(endpoints.length+' restored feature API reads');
 const tempPassword='Restore-'+crypto.randomUUID();
 for(const role of ['labAdmin','teacher'])await request('/users',admin.token,'POST',{username:'restore_'+role,password:tempPassword,realName:'Restoration verification',roleCode:role,status:1});
 const labAdmin=await request('/auth/login',null,'POST',{username:'restore_labAdmin',password:tempPassword});
 const teacher=await request('/auth/login',null,'POST',{username:'restore_teacher',password:tempPassword});
 const flatten=menus=>menus.flatMap(menu=>menu.children?flatten(menu.children):[menu.index]);
 const labMenus=flatten(frontendMenus('labAdmin',labAdmin.user));
 for(const feature of ['consumables','room-control','device-status','environments','ai-assistant'])check(labMenus.includes('/lab-admin/'+feature),'Restored menu hidden: '+feature);
 const adminMenus=flatten(frontendMenus('systemAdmin',admin.user));
 check(adminMenus.includes('/system-admin/system')&&adminMenus.includes('/system-admin/activation-codes'),'Administrator settings/activation menus hidden');
 const teacherMenus=flatten(frontendMenus('teacher',teacher.user));
 check(teacherMenus.includes('/teacher/course-environments')&&teacherMenus.includes('/teacher/adjustments'),'Teacher feature menus hidden');
 note('actual role profiles expose all restored frontend menu entries');
 for(const endpoint of ['/consumables','/iot/devices','/device-status/summary','/activation-codes/lab-bindings','/ai-assistant/user-config'])await request(endpoint,labAdmin.token);
 await request('/system-settings',labAdmin.token,'PUT',{},403);
 await request('/admin/activation-codes/generate',labAdmin.token,'POST',{},403);
 await request('/modules/system-settings/batch',labAdmin.token,'PUT',{records:[]},403);
 await request('/iot/devices',teacher.token,'GET',null,403);
 await request('/schedule-adjustments/1/approve',teacher.token,'POST',{},403);
 note('lab/teacher roles work without granting system administration or approval privileges');
 const consumable=await request('/consumables',labAdmin.token,'POST',{name:'RESTORE-SMOKE',stock:10,unit:'个',warnThreshold:1});
 await request('/consumables/'+consumable.id+'/movement',labAdmin.token,'POST',{type:'in',quantity:2,operator:'verification'});
 const inventory=await request('/consumables',labAdmin.token);check(inventory.some(row=>row.id===consumable.id&&row.stock===12),'Consumable stock movement did not persist');note('consumable creation and inbound stock movement persist');
 const lab=await request('/labs',labAdmin.token,'POST',{code:'RESTORE-SMOKE',name:'Restoration verification lab',capacity:20});
 const iot=await request('/iot/devices',labAdmin.token,'POST',{iotCode:'RESTORE-SMOKE',iotName:'Verification device',deviceType:'light',labId:lab.id,protocol:'mock',enabled:true});
 const iotList=await request('/iot/devices',labAdmin.token);check(iotList.devices.some(row=>row.id===iot.id),'IoT device did not persist');
 await request('/iot/labs/'+lab.id+'/devices',labAdmin.token);note('IoT device creation and lab listing persist; no hardware command sent');
 await request('/system-settings',admin.token,'PUT',{basicConfig:{platformName:'RESTORE-SMOKE'}});
 check((await request('/system-settings',admin.token)).basicConfig.platformName==='RESTORE-SMOKE','System settings did not persist');note('system settings save and reload persist');
 await request('/admin/activation-codes/generate',admin.token,'POST',{quantity:1,deviceLimit:1});
 const activation=await request('/admin/activation-codes',admin.token);check(activation.total>=1,'Activation code generation did not persist');note('activation code generation and listing persist');
 await request('/environment-templates',labAdmin.token,'POST',{name:'RESTORE-SMOKE',course:'Verification course',os:'Windows'});
 check((await request('/environment-templates',labAdmin.token)).some(row=>row.name==='RESTORE-SMOKE'),'Environment template did not persist');note('environment template create and list persist');
 report.success=true;report.completedAt=new Date().toISOString();
}
main().catch(error=>{report.error=error.message;process.exitCode=1;console.error(error.message)}).finally(async()=>{
 if(child&&child.exitCode==null&&child.pid){child.kill();await new Promise(resolve=>{child.once('exit',resolve);setTimeout(resolve,5000)})}
 if(created){check(/^smart_lab_feature_ui_it_[0-9a-f]{32}$/.test(schema),'Unsafe clone cleanup');query('drop database `'+schema+'`;')}
 report.isolatedCloneRemoved=created;fs.writeFileSync(path.join(__dirname,'verification-report.json'),JSON.stringify(report,null,2));
});
