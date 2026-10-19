// Traditional Chen ER notation: editable rectangles, ellipses and diamonds.
const fs = require('node:fs');
const path = require('node:path');
const base = fs.readFileSync(path.join(__dirname,'generate-er-drawio.cjs'),'utf8').split('const esc=')[0];
const {tables,groups,names,root}=new Function('require','__dirname',base+';return {tables,groups,names,root};')(require,__dirname);
const cn={id:'编号',username:'用户名',password:'密码',real_name:'姓名',phone:'手机号',email:'邮箱',department:'部门',status:'状态',permissions:'权限',created_at:'创建时间',updated_at:'更新时间',avatar_url:'头像',role_code:'角色编码',role_name:'角色名称',remark:'备注',user_id:'用户编号',role_id:'角色编号',lab_id:'实验室编号',lab_code:'实验室编码',lab_name:'实验室名称',building:'楼栋',floor:'楼层',room_no:'房间号',lab_type:'实验室类型',capacity:'容纳人数',manager_user_id:'管理员编号',open_status:'开放状态',device_code:'设备编码',device_name:'设备名称',category:'类别',location:'位置',owner_user_id:'责任人编号',health:'健康状态',online:'在线状态',specs:'规格',purchase_date:'购买日期',inventory_date:'盘点日期',warranty_date:'保修日期',usage_hours:'使用时长',maintenance:'维护说明',quantity:'数量',unit:'单位',deleted:'删除标记',source_type:'来源类型',device_id:'设备编号',event_type:'事件类型',occurred_at:'发生时间',operator_user_id:'操作用户编号',operator_name:'操作人',result_status:'结果状态',before_state:'变更前状态',after_state:'变更后状态',order_no:'工单编号',reporter_user_id:'报修人编号',reporter_name:'报修人姓名',contact:'联系方式',fault_type:'故障类型',priority_level:'优先级',assignee_name:'处理人',deadline:'截止时间',progress:'处理进度',description:'描述',result_desc:'处理结果',record_kind:'记录类型',person:'使用人',resource:'使用资源',scene:'场景',use_time:'使用时间',source_id:'来源编号',semester:'学期',row_class_name:'原始班级',class_name:'班级名称',weekday:'星期',section_code:'节次编码',section_text:'节次描述',start_section:'开始节次',end_section:'结束节次',course_name:'课程名称',teacher:'教师',week_raw:'原始周次',week_text:'周次描述',week_expanded:'展开周次',classroom:'教室',create_time:'创建时间',applicant_user_id:'申请人编号',applicant_name:'申请人姓名',reservation_date:'预约日期',time_range:'预约时段',attendee_count:'参与人数',reason:'原因',conflict_flag:'冲突标记',reviewer_name:'审核人',review_remark:'审核意见',teacher_user_id:'教师编号',assigned_lab_id:'分配实验室编号',name:'名称',course:'课程',os:'操作系统',software_list:'软件列表',labs:'适用实验室',tag_type:'标签类型',stock:'库存',warn_threshold:'预警阈值',consumable_id:'耗材编号',consumable_name:'耗材名称',movement_type:'出入库类型',before_stock:'变动前库存',after_stock:'变动后库存',operated_at:'操作时间',source_target:'来源或去向',borrower:'借用人',returned_quantity:'已归还数量',purpose:'用途',due_date:'应还日期',borrowed_at:'借用时间',last_returned_at:'最近归还时间',title:'标题',content:'内容',notice_type:'通知类型',target_role:'目标角色',publish_status:'发布状态',publish_time:'发布时间',created_by:'创建人编号',priority:'优先级',notice_id:'通知编号',read_status:'阅读状态',read_time:'阅读时间',archived:'归档标记',archived_at:'归档时间',deleted_at:'删除时间',aggregate_type:'业务类型',aggregate_id:'业务编号',action:'操作',actor_user_id:'操作人编号',actor_name:'操作人姓名',detail_json:'详情',module_name:'模块名称',record_key:'记录键',record_json:'记录内容',config_id:'配置编号',config_key:'配置键',schedule_enabled:'定时抓取开关',cron_expression:'定时表达式',semester_start_date:'学期开始日期',credential_username_cipher:'加密账号',credential_password_cipher:'加密密码',trigger_type:'触发方式',scope:'抓取范围',source:'来源',attempts:'尝试次数',updated_count:'更新数量',started_at:'开始时间',finished_at:'结束时间',error_message:'错误信息',output:'任务输出',log_path:'日志路径',conversation_id:'会话编号',preview:'预览',messages_json:'消息内容',model:'文本模型',vision_model:'视觉模型',enabled:'启用状态',api_key:'API密钥',base_url:'接口地址',system_prompt:'系统提示词'};
const esc=s=>String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
const labels={manager_user_id:'管理',owner_user_id:'负责',applicant_user_id:'申请',reporter_user_id:'报修',created_by:'发布',actor_user_id:'执行',operator_user_id:'操作',role_id:'分配角色',user_id:'关联用户',lab_id:'归属',device_id:'关联设备',notice_id:'接收',config_id:'产生',iot_device_id:'关联物联设备',gateway_id:'通过网关',activation_code_id:'使用接入码',terminal_record_id:'关联终端',runtime_status_id:'记录状态',asset_device_id:'对应资产',consumable_id:'关联耗材'};
let counter=0;
function diagram(title,list,mode){
 const cells=['<mxCell id="0"/><mxCell id="1" parent="0"/>'],positions=new Map();
 const style='html=0;fillColor=#ffffff;strokeColor=#000000;strokeWidth=1.5;fontColor=#000000;fontFamily=SimSun;fontSize=16;whiteSpace=wrap;';
 function node(id,label,shape,x,y,w,h,extra=''){cells.push(`<mxCell id="${id}" value="${esc(label)}" style="${style}${shape};${extra}" vertex="1" parent="1"><mxGeometry x="${x}" y="${y}" width="${w}" height="${h}" as="geometry"/></mxCell>`);}
 function edge(a,b,label='',extra=''){cells.push(`<mxCell id="edge${counter++}" value="${esc(label)}" style="html=0;endArrow=none;startArrow=none;strokeColor=#000000;strokeWidth=1.2;fontFamily=Times New Roman;fontSize=16;labelBackgroundColor=#ffffff;${extra}" edge="1" parent="1" source="${a}" target="${b}"><mxGeometry relative="1" as="geometry"/></mxCell>`);}
 node('title',`智云实验室 ${title}`,'text;strokeColor=none',30,20,1900,45,'fontSize=26;fontStyle=1;align=left;');
 node('legend',mode==='entity'?'矩形：实体；椭圆：属性；下划线：主键。悬停属性可查看物理字段（见标签括号）。':'矩形：实体；椭圆：主要属性；菱形：关系；1/N：基数。0..1 表示可选关联；无外键的表独立展示。','text;strokeColor=none',30,75,2100,35,'fontSize=15;align=left;');
 const columnCount=mode==='entity'?1:mode==='all'?5:3;
 const dx=mode==='entity'?1300:850,dy=mode==='entity'?1100:720;
 list.forEach((name,i)=>{
   const t=tables.get(name), x=430+(i%columnCount)*dx,y=450+Math.floor(i/columnCount)*dy;
   positions.set(name,{x,y});
   node(name,names[name],'rounded=0',x-85,y-32,170,64,'fontSize=19;');
   let attrs=mode==='entity'?t.cols:t.cols.filter(c=>!t.fks.some(f=>f.col===c.name)&&!['updated_at','created_at','deleted'].includes(c.name)).slice(0,mode==='all'?3:6);
   if(mode!=='entity'&&!attrs.some(c=>t.pk.includes(c.name)))attrs=[t.cols.find(c=>t.pk.includes(c.name)),...attrs].filter(Boolean).slice(0,6);
   const rx=mode==='entity'?440:260,ry=mode==='entity'?320:200;
   attrs.forEach((c,j)=>{
     const a=-Math.PI/2+2*Math.PI*j/attrs.length, ax=x+rx*Math.cos(a),ay=y+ry*Math.sin(a),id=`${name}_${c.name}`;
     const label=cn[c.name]||c.comment.split(/[：:，,；;]/)[0].slice(0,15)||c.name;
     node(id,mode==='entity'?`${label}\n(${c.name})`:label,'ellipse',ax-(mode==='entity'?100:72),ay-(mode==='entity'?27:22),mode==='entity'?200:144,mode==='entity'?54:44,t.pk.includes(c.name)?'fontStyle=4;':'');
     edge(name,id);
   });
 });
 if(mode!=='entity')for(const name of list){const t=tables.get(name);for(const fk of t.fks){if(!positions.has(fk.parent))continue;const p=positions.get(fk.parent),q=positions.get(name);const index=t.fks.indexOf(fk);const x=(p.x+q.x)/2+(index%3-1)*75,y=(p.y+q.y)/2+75+(index%2)*65;const id='rel'+counter++;node(id,labels[fk.col]||'关联','rhombus',x-58,y-35,116,70,'fontSize=15;');const optional=t.cols.find(c=>c.name===fk.col).nullable;const unique=t.unique.some(u=>u.length===1&&u[0]===fk.col);edge(fk.parent,id,optional?'0..1':'1');edge(id,name,unique?'0..1':'N');}}
 return `<diagram id="d${counter++}" name="${esc(title)}"><mxGraphModel grid="1" gridSize="10" page="0" pageScale="1" background="#ffffff"><root>${cells.join('')}</root></mxGraphModel></diagram>`;
}
const all=groups.flatMap(g=>g[2]);
const pages=[diagram('全库总览（39表）',all,'all')];
for(const [label,,list] of groups){const related=new Set(list);for(const n of list)for(const f of tables.get(n).fks)related.add(f.parent);pages.push(diagram(label,[...related],'module'));}
// Entity attribute sheets retain every DDL column without overcrowding the system diagrams.
for(const name of all)pages.push(diagram(`${names[name]}—完整属性`,[name],'entity'));
const out=path.join(root,'docs/智云实验室-陈氏ER图.drawio');
fs.writeFileSync(out,`<?xml version="1.0" encoding="UTF-8"?><mxfile host="app.diagrams.net" type="device">${pages.join('')}</mxfile>`);
console.log(JSON.stringify({file:out,tables:tables.size,pages:pages.length,relationships:[...tables.values()].reduce((n,t)=>n+t.fks.length,0)},null,2));
