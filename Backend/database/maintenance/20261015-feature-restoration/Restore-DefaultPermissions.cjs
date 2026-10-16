'use strict';
const fs=require('node:fs'),path=require('node:path'),cp=require('node:child_process');
if(!process.env.MYSQL_PWD)throw new Error('MYSQL_PWD is required');
const defaults=JSON.parse(fs.readFileSync(path.join(__dirname,'default-permissions.json'),'utf8'));
const legacy={
 systemAdmin:[['用户管理','平台统计','通知公告']],
 labAdmin:[['实验室管理','课表数据','预约管理','设备资产','维护记录填写','维护记录查看','使用记录','统计分析','通知公告'],['实验室管理','课表数据','预约管理','设备资产','耗材库存','设备状态','环境管理','故障报修','物联网扩展','使用记录','统计分析','AI智能助手','通知公告']],
 teacher:[['教师课表'],['教师课表','课程环境']]
};
const labels=groups=>groups.flatMap(group=>typeof group==='string'?[group]:(group.items||[]));
const canonical=items=>JSON.stringify([...new Set(items)].sort());
const hex=text=>"convert(0x"+Buffer.from(text,'utf8').toString('hex')+" using utf8mb4) collate utf8mb4_general_ci";
function query(input){const r=cp.spawnSync(process.env.RECOVERY_MYSQL_EXE||'C:/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe',['--host=127.0.0.1','--user=root','--default-character-set=utf8mb4','--batch','--skip-column-names','--raw','--database=smart_lab_basic'],{input,encoding:'utf8',windowsHide:true});if(r.error||r.status!==0)throw new Error('Default permission restoration failed');return r.stdout.trim()}
const rows=query("select json_object('id',u.id,'role',r.role_code,'permissions',u.permissions) from sys_user u join user_role ur on ur.user_id=u.id join role r on r.id=ur.role_id;").split(/\r?\n/).filter(Boolean).map(line=>JSON.parse(line));
const updatedIds=[],preservedCustomIds=[];
for(const row of rows){
 if(!defaults[row.role]||!row.permissions)continue;
 let parsed;try{parsed=JSON.parse(row.permissions)}catch{preservedCustomIds.push(row.id);continue}
 if(!Array.isArray(parsed)){preservedCustomIds.push(row.id);continue}
 const current=canonical(labels(parsed));
 if(current===canonical(labels(defaults[row.role])))continue;
 if(!(legacy[row.role]||[]).some(items=>canonical(items)===current)){preservedCustomIds.push(row.id);continue}
 if(!/^\d+$/.test(String(row.id)))throw new Error('Invalid user identifier');
 const result=query('update sys_user set permissions='+hex(JSON.stringify(defaults[row.role]))+' where id='+row.id+' and permissions='+hex(row.permissions)+'; select row_count();');
 if(result==='1')updatedIds.push(row.id);
}
fs.writeFileSync(path.join(__dirname,'permission-restoration-report.json'),JSON.stringify({database:'smart_lab_basic',checkedAt:new Date().toISOString(),updatedIds,preservedCustomIds},null,2));
console.log('Default role permissions restored for '+updatedIds.length+' account(s); custom permissions preserved.');
