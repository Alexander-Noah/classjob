/* Generate an editable draw.io ER diagram from the exported DDL; never reads INSERT values. */
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const source = path.join(root, 'Backend/database/smart_lab_basic.sql');
const sql = fs.readFileSync(source, 'utf8');
const groups = [
  ['用户与权限', '#dae8fc', ['sys_user', 'role', 'user_role']],
  ['实验室与资产维修', '#d5e8d4', ['lab', 'device', 'device_event', 'repair_order', 'usage_record']],
  ['教学预约与环境', '#fff2cc', ['class_timetable', 'lab_reservation', 'course_environment_request', 'environment_template', 'lab_software', 'schedule_adjustment']],
  ['耗材管理', '#ffe6cc', ['consumable_inventory', 'consumable_stock_record', 'consumable_loan']],
  ['物联网', '#e1d5e7', ['iot_device', 'iot_device_capability', 'iot_gateway_config', 'iot_gateway_point', 'iot_command_log', 'iot_telemetry_latest']],
  ['终端与运行状态', '#d0cee2', ['lab_activation_code', 'lab_terminal', 'device_runtime_status', 'device_status_event', 'teacher_host', 'student_client']],
  ['通知审批与审计', '#f8cecc', ['notice', 'notice_recipient', 'approval_countersign', 'audit_event', 'module_record']],
  ['AI与课表接入', '#cce5ff', ['ai_assistant_conversation', 'ai_assistant_user_config', 'timetable_integration_config', 'timetable_import_job', 'flyway_schema_history']],
];
const names = {sys_user:'用户',role:'角色',user_role:'用户角色',lab:'实验室',device:'设备资产',device_event:'设备盘点调拨事件',repair_order:'报修与维护',usage_record:'使用记录',class_timetable:'课程课表',lab_reservation:'实验室预约',course_environment_request:'课程环境申请',environment_template:'环境模板',lab_software:'实验室软件',schedule_adjustment:'调课申请',consumable_inventory:'耗材库存',consumable_stock_record:'库存流水',consumable_loan:'耗材借还',iot_device:'物联设备',iot_device_capability:'设备能力',iot_gateway_config:'网关配置',iot_gateway_point:'网关点位',iot_command_log:'控制命令日志',iot_telemetry_latest:'最新遥测',lab_activation_code:'实验室接入码',lab_terminal:'终端绑定',device_runtime_status:'设备运行状态',device_status_event:'设备状态事件',teacher_host:'教师端主机',student_client:'学生端',notice:'通知',notice_recipient:'通知接收人',approval_countersign:'审批加签',audit_event:'审计事件',module_record:'通用模块记录',ai_assistant_conversation:'AI会话',ai_assistant_user_config:'AI用户配置',timetable_integration_config:'课表接入配置',timetable_import_job:'课表导入任务',flyway_schema_history:'迁移历史（技术表）'};
const tables = new Map();
for (const m of sql.matchAll(/CREATE TABLE `([^`]+)`\s*\(([\s\S]*?)\) ENGINE[^;]*;/g)) {
  const body = m[2];
  const cols = [...body.matchAll(/^\s*`([^`]+)`\s+([a-z]+(?:\([^\r\n]*?\))?)([^\r\n]*)/gm)].map(c => ({name:c[1],type:c[2],nullable:!c[3].includes('NOT NULL'),comment:c[3].match(/COMMENT '((?:[^'\\]|\\.)*)'/)?.[1] || ''}));
  const pk = [...(body.match(/PRIMARY KEY \(([^)]+)\)/)?.[1] || '').matchAll(/`([^`]+)`/g)].map(x=>x[1]);
  const unique = [...body.matchAll(/UNIQUE (?:INDEX|KEY) `[^`]+`\(([^)]+)\)/g)].map(x=>[...x[1].matchAll(/`([^`]+)`/g)].map(y=>y[1]));
  const fks = [...body.matchAll(/CONSTRAINT `([^`]+)` FOREIGN KEY \(`([^`]+)`\) REFERENCES `([^`]+)` \(`([^`]+)`\)([^\r\n]*)/g)].map(x=>({name:x[1],col:x[2],parent:x[3],target:x[4],rule:x[5].replace(/,$/,'').trim()}));
  tables.set(m[1], {name:m[1],cols,pk,unique,fks});
}
if(tables.size!==39) throw Error(`Expected 39 tables, found ${tables.size}`);
const assigned=groups.flatMap(g=>g[2]);
if(new Set(assigned).size!==39 || assigned.some(n=>!tables.has(n))) throw Error('Incomplete module mapping');
const esc=s=>String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
let serial=0;
function page(title, entries, overview=false){
  const cells=['<mxCell id="0"/><mxCell id="1" parent="0"/>'];
  const positions=new Map();
  const add=(id,value,style,x,y,w,h,parent='1')=>cells.push(`<mxCell id="${id}" value="${esc(value)}" style="${style}" vertex="1" parent="${parent}"><mxGeometry x="${x}" y="${y}" width="${w}" height="${h}" as="geometry"/></mxCell>`);
  add('title',`智云实验室 · ${title}`,'text;html=0;align=left;fontSize=26;fontStyle=1;',35,20,1800,44);
  add('legend','来源：smart_lab_basic.sql｜39张表（含Flyway技术表）｜PK 主键 · FK 外键 · UK 唯一键 · UQ组 联合唯一 · ? 可空\n仅绘制DDL明确声明的外键；0..1/1 表示每条子记录可关联的父记录数。未声明外键的业务联系不自动推断。','text;html=0;align=left;whiteSpace=wrap;fontSize=13;',35,70,1850,52);
  let bottom=0;
  if(overview){
    const columns=4, gap=38, width=450;
    let y=145;
    for(let row=0;row<2;row++){
      const gs=groups.slice(row*columns,row*columns+columns);
      let rowHeight=0;
      for(let j=0;j<gs.length;j++){
        const [label,color,list]=gs[j], x=35+j*(width+gap);
        let cy=y+42;
        for(const name of list){
          const t=tables.get(name), selected=t.cols.filter(c=>t.pk.includes(c.name)||t.fks.some(f=>f.col===c.name));
          const h=drawTable(t,x+12,cy,width-24,color,selected);
          cy+=h+22;
        }
        add(`group${row}-${j}`,label,`rounded=1;fillColor=${color};strokeColor=#888888;fontStyle=1;fontSize=17;align=left;spacingLeft=12;verticalAlign=top;spacingTop=8;fillOpacity=12;`,x,y,width,cy-y);
        // Place module backgrounds behind the tables.
        const bg=cells.pop(); cells.splice(3,0,bg);
        rowHeight=Math.max(rowHeight,cy-y);
      }
      y+=rowHeight+50;
    }
    bottom=y;
  }else{
    const widths=620, colCount=3, ys=[150,150,150];
    for(const name of entries){
      const t=tables.get(name), color=groups.find(g=>g[2].includes(name))[1];
      const col=ys.indexOf(Math.min(...ys));
      const h=drawTable(t,35+col*690,ys[col],widths,color,t.cols);
      ys[col]+=h+65;
    }
    bottom=Math.max(...ys);
  }
  function drawTable(t,x,y,w,color,cols){
    const id=`t_${t.name}`, rowH=27, h=62+rowH*cols.length;
    add(id,`${names[t.name]}\n${t.name}`,`swimlane;html=0;startSize=58;horizontal=1;fillColor=${color};swimlaneFillColor=#ffffff;strokeColor=#65758b;fontSize=15;fontStyle=1;rounded=0;`,x,y,w,h);
    positions.set(t.name,{id});
    cols.forEach((c,i)=>{
      const tags=[];
      if(t.pk.includes(c.name))tags.push('PK');
      if(t.fks.some(f=>f.col===c.name))tags.push('FK');
      if(t.unique.some(u=>u.length===1&&u[0]===c.name))tags.push('UK');
      if(t.unique.some(u=>u.length>1&&u.includes(c.name)))tags.push('UQ组');
      let value=`${tags.length?'['+tags.join(',')+'] ':''}${c.name} : ${c.type}${c.nullable?' ?':''}`;
      if(!overview&&c.comment)value+=`  ${c.comment}`;
      add(`${id}_${c.name}`,value,'text;html=0;align=left;verticalAlign=middle;spacingLeft=9;fontSize=12;overflow=hidden;whiteSpace=nowrap;',0,59+i*rowH,w,rowH,id);
    });
    return h;
  }
  let edgeCount=0;
  for(const t of tables.values())for(const fk of t.fks){
    if(!positions.has(t.name)||!positions.has(fk.parent))continue;
    const c=t.cols.find(c=>c.name===fk.col), one=t.unique.some(u=>u.length===1&&u[0]===fk.col);
    const from=`t_${fk.parent}_${fk.target}`,to=`t_${t.name}_${fk.col}`;
    const label=`${fk.col} → ${fk.parent}.${fk.target}\n父 ${c.nullable?'0..1':'1'} / 子 ${one?'0..1':'0..N'}`;
    const style=`edgeStyle=orthogonalEdgeStyle;rounded=1;html=0;strokeColor=#70849d;strokeWidth=1.3;fontSize=10;labelBackgroundColor=#ffffff;startArrow=${c.nullable?'ERzeroToOne':'ERone'};startFill=0;endArrow=${one?'ERzeroToOne':'ERzeroToMany'};endFill=0;`;
    cells.push(`<mxCell id="e${serial++}" value="${esc(label)}" style="${style}" edge="1" parent="1" source="${from}" target="${to}"><mxGeometry relative="1" as="geometry"/></mxCell>`);
    edgeCount++;
  }
  return {xml:`<diagram id="page${serial++}" name="${esc(title)}"><mxGraphModel dx="2000" dy="1200" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="0" pageScale="1" pageWidth="2200" pageHeight="${Math.ceil(bottom+100)}" math="0" shadow="0"><root>${cells.join('')}</root></mxGraphModel></diagram>`,edgeCount};
}
const pages=[page('全库总览（39表·主外键）',assigned,true)];
for(const [title,,list] of groups){
  const related=new Set(list);
  for(const n of list)for(const f of tables.get(n).fks)related.add(f.parent);
  pages.push(page(title+'（完整字段）',[...related]));
}
const fkCount=[...tables.values()].reduce((n,t)=>n+t.fks.length,0);
if(pages[0].edgeCount!==fkCount)throw Error('Missing overview relationship');
const xml=`<?xml version="1.0" encoding="UTF-8"?>\n<mxfile host="app.diagrams.net" type="device" version="24.7.17">${pages.map(p=>p.xml).join('')}</mxfile>`;
const out=path.join(root,'docs','智云实验室-数据库ER图.drawio');
fs.writeFileSync(out,xml);
console.log(JSON.stringify({file:out,tables:tables.size,foreignKeys:fkCount,pages:pages.length,bytes:Buffer.byteLength(xml)},null,2));
