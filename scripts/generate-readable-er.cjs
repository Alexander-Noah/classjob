const fs=require('node:fs'),path=require('node:path');
const root=path.resolve(__dirname,'..');
const esc=s=>String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
let serial=0;
function makePage(title,entities,relations,note){
 const nodes=[],edges=[];
 const node=(id,label,shape,x,y,w,h,pk=false)=>nodes.push({id,label,shape,x,y,w,h,pk});
 const edge=(a,b,label='')=>edges.push({a,b,label});
 entities.forEach(([id,label,x,y,attrs,side='top'])=>{
  node(id,label,'rectangle',x,y,150,62);
  const offsets=side==='top'?[[-145,-125],[0,-165],[145,-125]]:side==='bottom'?[[-145,125],[0,165],[145,125]]:side==='left'?[[-170,-115],[-210,0],[-170,115]]:side==='right'?[[170,-115],[210,0],[170,115]]:[[155,-140],[225,-70],[155,140]];
  attrs.forEach((a,i)=>{const [dx,dy]=offsets[i];node(id+'a'+i,a,'ellipse',x+dx,y+dy,140,46,i===0);edge(id,id+'a'+i);});
 });
 relations.forEach(([a,b,label,x,y,left='1',right='N'])=>{const id='r'+serial++;node(id,label,'rhombus',x,y,110,68);edge(a,id,left);edge(id,b,right);});
 const cells=['<mxCell id="0"/><mxCell id="1" parent="0"/>'];
 const text=(id,label,x,y,w,h,size)=>cells.push(`<mxCell id="${id}" value="${esc(label)}" style="text;html=0;align=center;fontFamily=SimSun;fontSize=${size};fontColor=#111111;" vertex="1" parent="1"><mxGeometry x="${x}" y="${y}" width="${w}" height="${h}" as="geometry"/></mxCell>`);
 text('title',title,60,25,1580,55,28);text('note',note,100,87,1500,55,15);
 for(const e of edges)cells.push(`<mxCell id="e${serial++}" value="${esc(e.label)}" style="endArrow=none;startArrow=none;html=0;strokeColor=#222222;strokeWidth=1.5;fontFamily=Times New Roman;fontSize=19;labelBackgroundColor=#ffffff;" edge="1" parent="1" source="${e.a}" target="${e.b}"><mxGeometry relative="1" as="geometry"/></mxCell>`);
 for(const n of nodes)cells.push(`<mxCell id="${n.id}" value="${esc(n.label)}" style="shape=${n.shape};whiteSpace=wrap;html=0;fillColor=#ffffff;strokeColor=#222222;strokeWidth=1.7;fontFamily=SimSun;fontSize=${n.shape==='rectangle'?22:18};fontStyle=${n.pk?4:0};" vertex="1" parent="1"><mxGeometry x="${n.x-n.w/2}" y="${n.y-n.h/2}" width="${n.w}" height="${n.h}" as="geometry"/></mxCell>`);
 return {xml:`<diagram id="p${serial++}" name="${esc(title)}"><mxGraphModel grid="0" page="1" pageWidth="1700" pageHeight="1500" background="#ffffff"><root>${cells.join('')}</root></mxGraphModel></diagram>`,nodes,edges,title,note};
}
const main=makePage('智云实验室核心业务 E-R 图',[
 ['device','设备资产',330,340,['设备编号','设备名称','资产状态']],
 ['repair','报修维护',800,340,['工单编号','故障描述','处理状态']],
 ['role','角色',1270,340,['角色编号','角色名称','权限']],
 ['lab','实验室',330,790,['实验室编号','实验室名称','容纳人数'],'left'],
 ['user','用户',800,790,['用户编号','姓名','部门'],'inner'],
 ['notice','通知',1270,790,['通知编号','通知标题','发布状态'],'right'],
 ['reservation','实验室预约',330,1240,['预约编号','预约日期','预约时段'],'bottom'],
 ['usage','使用记录',800,1240,['记录编号','使用时间','使用状态'],'bottom'],
],[
 ['lab','device','配置',330,565],['device','repair','产生',565,340],
 ['user','repair','报修',800,565],['user','role','具有',1035,565,'M','N'],
 ['user','lab','管理',565,790],['user','notice','发布',1035,790],
 ['lab','reservation','接受',330,1015],['user','reservation','申请',565,1015],
 ['user','usage','使用',800,1015],
],'矩形表示实体，椭圆表示主要属性，菱形表示关系；下划线表示主键。\n本页展示核心业务概念，不等同于全部物理表；可空外键的可选性省略，用户与角色通过关联表实现。');
const pages=[main];
// Supporting modules are intentionally shown on small pages, with no cross-module wires.
const modules=[
 ['耗材业务',[['stock','耗材库存',['耗材编号','耗材名称','当前库存']],['movement','库存流水',['流水编号','变动数量','操作时间']],['loan','借还台账',['借用编号','借用数量','归还数量']]],[['stock','movement','发生'],['stock','loan','借出']]],
 ['通知接收',[['user','用户',['用户编号','姓名','部门']],['recipient','通知接收记录',['记录编号','阅读状态','归档状态']],['notice','通知',['通知编号','标题','发布时间']]],[['user','recipient','接收'],['notice','recipient','分发']]],
 ['课表接入',[['config','课表接入配置',['配置编号','学期','定时规则']],['job','课表导入任务',['任务编号','执行状态','更新条数']]],[['config','job','触发']]],
 ['AI助手',[['user','用户',['用户编号','姓名','部门']],['conversation','AI会话',['会话编号','标题','消息内容']],['config','AI配置',['配置编号','模型','接口地址']]],[['user','conversation','拥有'],['user','config','配置','1','1']]],
 ['设备历史',[['device','设备资产',['设备编号','设备名称','资产状态']],['event','盘点调拨事件',['事件编号','事件类型','发生时间']],['state','设备运行状态',['状态编号','在线状态','健康状态']]],[['device','event','记录'],['device','state','关联']]],
 ['物联设备',[['iot','物联设备',['设备编号','设备名称','设备类型']],['capability','设备能力',['能力编号','能力名称','能力类型']],['telemetry','最新遥测',['遥测编号','指标名称','指标值']]],[['iot','capability','具备'],['iot','telemetry','上报']]],
 ['网关控制',[['gateway','网关配置',['网关编号','网关名称','协议']],['point','网关点位',['点位编号','点位编码','命令类型']],['iot','物联设备',['设备编号','设备名称','设备类型']]],[['gateway','point','配置'],['iot','point','映射']]],
 ['终端接入',[['lab','实验室',['实验室编号','实验室名称','容纳人数']],['code','实验室接入码',['接入码编号','有效期','终端额度']],['terminal','终端绑定',['终端编号','终端类型','绑定状态']]],[['lab','code','签发'],['code','terminal','绑定']]],
];
for(const [title,es,rs] of modules){const coords=[[420,430],[1100,430],[760,1050]];const entities=es.map((e,i)=>[e[0],e[1],...coords[i],e[2],i===2?'bottom':'top']);const relations=rs.map((r,i)=>{const a=entities.find(e=>e[0]===r[0]),b=entities.find(e=>e[0]===r[1]);return [r[0],r[1],r[2],(a[2]+b[2])/2,(a[3]+b[3])/2,r[3]||'1',r[4]||'N'];});pages.push(makePage(title+' E-R 图',entities,relations,'模块局部示意：展示主要实体及属性；各页重复实体用于阅读定位，不代表新增表。\n完整字段、其余实体和全部外键请参阅另附的全库结构图。'));}
const out=path.join(root,'docs/智云实验室-论文展示版ER图.drawio');fs.writeFileSync(out,`<?xml version="1.0" encoding="UTF-8"?><mxfile host="app.diagrams.net" type="device">${pages.map(p=>p.xml).join('')}</mxfile>`);
// Matching SVG preview of the first page.
const svg=['<svg xmlns="http://www.w3.org/2000/svg" width="1700" height="1500" viewBox="0 0 1700 1500"><rect width="1700" height="1500" fill="white"/>'];
function txt(label,x,y,size=18,underline=false){const lines=label.split('\n');return `<text x="${x}" y="${y}" text-anchor="middle" font-family="SimSun,Microsoft YaHei,serif" font-size="${size}" ${underline?'text-decoration="underline"':''}>${lines.map((l,i)=>`<tspan x="${x}" dy="${i?24:0}">${esc(l)}</tspan>`).join('')}</text>`;}
svg.push(txt(main.title,850,57,28),txt(main.note,850,104,15));
function boundary(n,other){const dx=other.x-n.x,dy=other.y-n.y;const scale=n.shape==='ellipse'?1/Math.sqrt((dx/(n.w/2))**2+(dy/(n.h/2))**2):n.shape==='rhombus'?1/(Math.abs(dx)/(n.w/2)+Math.abs(dy)/(n.h/2)):Math.min(dx?Math.abs(n.w/2/dx):Infinity,dy?Math.abs(n.h/2/dy):Infinity);return {x:n.x+dx*scale,y:n.y+dy*scale};}
for(const e of main.edges){const a=main.nodes.find(n=>n.id===e.a),b=main.nodes.find(n=>n.id===e.b),p=boundary(a,b),q=boundary(b,a);svg.push(`<path d="M ${p.x} ${p.y} L ${q.x} ${q.y}" stroke="#222" stroke-width="1.5" fill="none"/>`);if(e.label)svg.push(txt(e.label,(p.x+q.x)/2+13,(p.y+q.y)/2-9,19));}
for(const n of main.nodes){const common='fill="white" stroke="#222" stroke-width="1.7"';svg.push(n.shape==='ellipse'?`<ellipse cx="${n.x}" cy="${n.y}" rx="${n.w/2}" ry="${n.h/2}" ${common}/>`:n.shape==='rhombus'?`<polygon points="${n.x},${n.y-n.h/2} ${n.x+n.w/2},${n.y} ${n.x},${n.y+n.h/2} ${n.x-n.w/2},${n.y}" ${common}/>`:`<rect x="${n.x-n.w/2}" y="${n.y-n.h/2}" width="${n.w}" height="${n.h}" ${common}/>`);svg.push(txt(n.label,n.x,n.y+6,n.shape==='rectangle'?22:18,n.pk));}
svg.push('</svg>');fs.writeFileSync(path.join(root,'docs/智云实验室-核心ER图预览.svg'),svg.join(''));
console.log(JSON.stringify({file:out,pages:pages.length,coreEntities:main.nodes.filter(n=>n.shape==='rectangle').length}));
