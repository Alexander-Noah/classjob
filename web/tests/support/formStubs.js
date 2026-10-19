import { computed,defineComponent,h,inject,provide } from 'vue'

// Lightweight Element Plus contracts for rendering a real parent template in unit tests.
// Validation algorithms are tested separately; this form exposes the API expected by its parent.
export function formStubs(){
  const stubs={}
  const groups=['dialog','drawer','dropdown','dropdown-menu','dropdown-item','tabs','tab-pane','form-item','row','col','icon','tag','alert','descriptions','descriptions-item','tooltip','card','space','divider','radio-group','radio-button','checkbox-group','empty','progress','pagination']
  for(const name of groups)stubs[`el-${name}`]=defineComponent({name:`Test${name}`,inheritAttrs:false,props:['label','title','modelValue'],setup(props,{slots,attrs}){return()=>h('section',{...attrs,'data-element':name},[props.label?h('label',props.label):null,props.title?h('h3',props.title):null,slots.default?.(),slots.footer?.()])}})
  stubs['el-form']=defineComponent({name:'TestForm',setup(_,{slots,expose}){expose({validate:()=>Promise.resolve(true),clearValidate:()=>{},validateField:()=>Promise.resolve(true)});return()=>h('form',{onSubmit:event=>event.preventDefault()},slots.default?.())}})
  for(const name of ['input','input-number','date-picker','time-picker'])stubs[`el-${name}`]=defineComponent({name:`Test${name}`,inheritAttrs:false,props:['modelValue','placeholder','type','disabled'],emits:['update:modelValue','change'],setup(props,{emit,attrs}){return()=>h(props.type==='textarea'?'textarea':'input',{...attrs,placeholder:props.placeholder,value:Array.isArray(props.modelValue)?props.modelValue.join('-'):props.modelValue,disabled:props.disabled,onInput:event=>emit('update:modelValue',name==='input-number'?Number(event.target.value):event.target.value),onChange:event=>emit('change',event.target.value)})}})
  stubs['el-select']=defineComponent({name:'TestSelect',inheritAttrs:false,props:['modelValue','placeholder'],emits:['update:modelValue','change'],setup(props,{slots,emit,attrs}){return()=>h('select',{...attrs,'aria-label':props.placeholder,value:props.modelValue,onChange:event=>{emit('update:modelValue',event.target.value);emit('change',event.target.value)}},slots.default?.())}})
  stubs['el-option']=defineComponent({name:'TestOption',props:['value','label'],setup(props){return()=>h('option',{value:props.value},props.label)}})
  stubs['el-button']=defineComponent({name:'TestButton',inheritAttrs:false,props:['disabled','loading'],setup(props,{attrs,slots}){return()=>h('button',{...attrs,type:'button',disabled:props.disabled||props.loading},slots.default?.())}})
  for(const name of ['checkbox','switch'])stubs[`el-${name}`]=defineComponent({name:`Test${name}`,inheritAttrs:false,props:['modelValue'],emits:['update:modelValue','change'],setup(props,{slots,emit}){return()=>h('label',[h('input',{type:'checkbox',checked:props.modelValue,onChange:event=>{emit('update:modelValue',event.target.checked);emit('change',event.target.checked)}}),slots.default?.()])}})
  for(const name of ['scrollbar','skeleton'])stubs[`el-${name}`]=defineComponent({setup(_,{slots}){return()=>h('section',slots.default?.())}})
  return stubs
}

export function tableStubs(){
  const stubs=formStubs()
  stubs['el-table']=defineComponent({name:'TestTable',props:{data:{type:Array,default:()=>[]}},setup(props,{slots,expose}){provide('test-table-rows',computed(()=>props.data));expose({clearSelection:()=>{}});return()=>h('div',{'data-element':'table'},slots.default?.())}})
  stubs['el-table-column']=defineComponent({name:'TestColumn',props:['prop','label'],setup(props,{slots}){const rows=inject('test-table-rows',computed(()=>[]));return()=>h('div',{'data-column':props.label},rows.value.map((row,index)=>h('div',{'data-row':index},slots.default?slots.default({row,$index:index,column:{property:props.prop}}):String(row[props.prop]??''))))}})
  return stubs
}

// Slot containers preserve the content that Element Plus places in headers and popovers.
export function panelStubs(){
  const stubs=tableStubs()
  for(const name of ['card','container','aside','header','main','menu','sub-menu','menu-item','breadcrumb','breadcrumb-item','avatar','timeline','timeline-item','collapse','collapse-item','scrollbar','popover','dropdown','dropdown-menu','dropdown-item','page-header','statistic','result','skeleton'])stubs[`el-${name}`]=defineComponent({name:`Panel${name}`,inheritAttrs:false,props:['title','label'],setup(props,{slots,attrs}){return()=>h('section',{...attrs,'data-element':name},[props.title?h('h3',props.title):null,props.label?h('label',props.label):null,...Object.values(slots).map(slot=>slot())])}})
  return stubs
}
