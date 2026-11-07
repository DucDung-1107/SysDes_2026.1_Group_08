'use client';
import {useId,useState,useRef,useEffect} from 'react';
import type {FormEvent,ReactNode} from 'react';
import type {ActionSpec,DataRecord,FieldSpec} from '@/lib/schema';
import {allowed,asRecord,facilityPath,fieldDefaults,queryPath,resolvePath,rows,serializeFields,validateFields} from '@/lib/contracts';
import {request,download} from '@/lib/api';
import {useApi} from '@/lib/use-api';
import {useSession} from './auth';
import {Button,Notice,Modal,Result} from './ui';
import {Plus,Trash2} from 'lucide-react';

export function Field({field,value,onChange,error,prefix}:{field:FieldSpec;value:unknown;onChange:(v:unknown)=>void;error?:string;prefix?:string}) {
  const uid=useId();const id=`${prefix||`field-${uid}`}-${field.name}`; const {facility,permissions}=useSession();
  const [lookupText,setLookupText]=useState('');const [debounced,setDebounced]=useState('');
  useEffect(()=>{const timer=setTimeout(()=>setDebounced(lookupText),250);return ()=>clearTimeout(timer);},[lookupText]);
  let path:string|null=null;
  if(field.lookup&&allowed(permissions,field.lookup.permission))try{path=facilityPath(field.lookup.path,facility);if(field.lookup.path==='/patients')path+=`?q=${encodeURIComponent(debounced)}&size=20`;}catch{path=null;}
  const lookup=useApi(path);
  const choices=field.options?.map(v=>typeof v==='string'?{value:v,label:v}:v) || (field.lookup?rows(lookup.data).map(r=>({value:String(r[field.lookup?.valueKey||'id']??''),label:[r[field.lookup?.labelKey||'name']??r.code??r.id,r.code,r.dateOfBirth].filter(Boolean).join(' · ')})):[]);
  const type=field.type||'text'; const common={id,name:field.name,'aria-invalid':Boolean(error)||undefined,'aria-describedby':`${id}-help${error?` ${id}-error`:''}`,required:field.required,autoComplete:'off',disabled:false};
  let control:ReactNode;
  if(type==='array') {
    const data=Array.isArray(value)?value:[];
    control=<div id={id} className="array-editor" role="group" aria-labelledby={`${id}-label`} aria-describedby={common['aria-describedby']} tabIndex={-1}>{data.map((item,i)=><fieldset className="array-row" key={i}><legend>Dòng {i+1}</legend><div className="form-grid">{(field.fields||[]).map(f=><Field key={f.name} prefix={`${id}-${i}`} field={f} error={error?validateFields(field.fields||[],asRecord(item))[f.name]:undefined} value={asRecord(item)[f.name]} onChange={v=>onChange(data.map((r,j)=>j===i?{...asRecord(r),[f.name]:v}:r))}/>)}</div><Button type="button" variant="quiet" onClick={()=>onChange(data.filter((_,j)=>j!==i))}><Trash2 size={16} aria-hidden="true"/>Bỏ dòng {i+1}</Button></fieldset>)}<Button type="button" onClick={()=>onChange([...data,fieldDefaults(field.fields||[])])} disabled={field.max!==undefined&&data.length>=field.max}><Plus size={16} aria-hidden="true"/>Thêm dòng</Button></div>;
  } else if(type==='checkbox')control=<label className="checkbox" htmlFor={id}><input {...common} type="checkbox" checked={Boolean(value)} onChange={e=>onChange(e.target.checked)}/><span>{field.label}{field.required?' *':''}</span></label>;
  else if(type==='select')control=<select {...common} value={String(value??'')} onChange={e=>onChange(e.target.value)}><option value="">Chọn {field.label.toLocaleLowerCase('vi-VN')}…</option>{choices.map(c=><option key={c.value} value={c.value}>{c.label}</option>)}</select>;
  else if(type==='string-list'&&choices.length) {
    const selected=Array.isArray(value)?value.map(String):String(value??'').split(',').map(v=>v.trim()).filter(Boolean);
    control=<div id={id} className="multi-options" role="group" aria-labelledby={`${id}-label`} aria-describedby={common['aria-describedby']} tabIndex={-1}>{choices.map(c=><label className="checkbox" key={c.value}><input type="checkbox" name={field.name} value={c.value} checked={selected.includes(c.value)} onChange={e=>onChange((e.target.checked?[...selected,c.value]:selected.filter(v=>v!==c.value)).join(', '))}/>{c.label}</label>)}</div>;
  } else if(type==='textarea'||type==='string-list')control=<textarea {...common} value={String(value??'')} rows={type==='textarea'?4:2} maxLength={field.maxLength} onChange={e=>onChange(e.target.value)}/>;
  else if(type==='file')control=<input {...common} type="file" accept="application/pdf,image/png,image/jpeg,text/plain" onChange={e=>onChange(e.target.files?.[0]??null)}/>;
  else control=<><input {...common} type={type==='datetime'?'datetime-local':type==='uuid'?'text':type} value={String(value??'')} min={field.min} max={field.max} maxLength={field.maxLength} step={type==='number'?'any':undefined} spellCheck={type==='uuid'||type==='password'?false:undefined} list={choices.length?`${id}-options`:undefined} readOnly={field.name==='version'&&value!==''} onChange={e=>onChange(e.target.value)}/>{choices.length?<datalist id={`${id}-options`}>{choices.map(c=><option key={c.value} value={c.value}>{c.label}</option>)}</datalist>:null}</>;
  return <div className={`field ${type==='textarea'||type==='array'?'full-width':''}`}>{type!=='checkbox'?<label id={`${id}-label`} htmlFor={type==='array'||type==='string-list'&&choices.length?undefined:id}>{field.label}{field.required?<span aria-hidden="true"> *</span>:null}</label>:null}{field.lookup?.path==='/patients'&&path?<input type="search" className="lookup-search" aria-label="Tìm hồ sơ theo tên, điện thoại hoặc mã" value={lookupText} onChange={e=>setLookupText(e.target.value)} placeholder="Tìm tên, điện thoại hoặc mã…" autoComplete="off"/>:null}{field.lookup&&path&&type==='uuid'?<select className="lookup-picker" aria-label={`Chọn ${field.label.toLocaleLowerCase('vi-VN')} từ kết quả`} value={choices.some(c=>c.value===value)?String(value):''} onChange={e=>{if(e.target.value)onChange(e.target.value);}}><option value="">{lookup.loading?'Đang tải gợi ý…':choices.length?'Chọn từ kết quả đã tìm…':'Chưa có kết quả phù hợp'}</option>{choices.map(c=><option key={c.value} value={c.value}>{c.label}</option>)}</select>:null}{control}<small id={`${id}-help`} className="field-help">{field.help||(type==='string-list'?'Nhập các giá trị cách nhau bằng dấu phẩy.':type==='datetime'?'Theo múi giờ cơ sở.':field.lookup?'Chọn mã từ danh sách hoặc nhập mã đã xác minh.':'')}{lookup.error?' Danh sách gợi ý chưa tải được; có thể nhập mã đã xác minh.':''}</small>{error?<small className="field-error" id={`${id}-error`}>{error}</small>:null}</div>;
}
export function ActionDialog({action,record,onClose,onComplete,refreshRecord}:{action:ActionSpec;record:DataRecord;onClose:()=>void;onComplete:()=>void;refreshRecord?:()=>Promise<DataRecord>}) {
  const refreshLabel='Tải bản mới, giữ nội dung đang nhập';
  const refreshDescription='Đã tải phiên bản mới. Nội dung bạn nhập được giữ lại; đối chiếu bản trên máy chủ bên dưới trước khi gửi.';
  const {facility}=useSession();const fields=action.fields||[];
  const [values,setValues]=useState(()=>fieldDefaults(fields,record));const [errors,setErrors]=useState<Record<string,string>>({});
  const [busy,setBusy]=useState(false);const [message,setMessage]=useState('');const [result,setResult]=useState<unknown>();const [done,setDone]=useState(false);const [dirty,setDirty]=useState(false);
  const [base,setBase]=useState(record);const [latest,setLatest]=useState<DataRecord|null>(null);const [conflict,setConflict]=useState(false);
  const [confirmed,setConfirmed]=useState(false);const summary=useRef<HTMLDivElement>(null);const retry=useRef<{key:string;body:string}|null>(null);
  function close(){if(!busy&&(!dirty||done||window.confirm('Nội dung chưa gửi sẽ bị bỏ. Bạn muốn đóng biểu mẫu?')))onClose();}
  async function reconcile(){if(!refreshRecord)return;setBusy(true);try{const fresh=await refreshRecord();if(fresh.version===undefined)throw new Error('Không tìm thấy phiên bản hiện tại. Tải lại danh sách và đối chiếu bản ghi.');setBase({...base,...fresh});setLatest(fresh);setValues({...values,version:fresh.version});setConfirmed(false);setConflict(false);setMessage('');}catch(e){setMessage(e instanceof Error?e.message:'Chưa tải được bản mới.');}finally{setBusy(false);}}
  async function submit(e:FormEvent){e.preventDefault();if(busy)return;const validation=validateFields(fields,values);setErrors(validation);if(Object.keys(validation).length){queueMicrotask(()=>summary.current?.focus());return;}
    if((action.confirm||action.danger)&&!confirmed){setMessage('Vui lòng đọc và xác nhận thao tác trước khi gửi.');return;}
    setBusy(true);setMessage('');setDone(false);
    try{
      const parsed=serializeFields(fields,values);const context={...base,...parsed};const path=facilityPath(resolvePath(action.path,context),facility);
      if(action.method==='DOWNLOAD'){await download(facilityPath(queryPath(action.path,parsed,base),facility));setResult(undefined);}
      else if(action.method==='UPLOAD') {
        const file=values.file;if(!(file instanceof File))throw new Error('Vui lòng chọn tệp.');
        if(file.size>10*1024*1024)throw new Error('Tệp vượt quá giới hạn 10 MiB.');
        if(!['application/pdf','image/png','image/jpeg','text/plain'].includes(file.type))throw new Error('Chỉ hỗ trợ PDF, PNG, JPEG hoặc văn bản UTF-8.');
        const form=new FormData();form.append('file',file);const args={...parsed};delete args.file;
        setResult(await request(facilityPath(queryPath(action.path,args,base),facility),{method:'POST',body:form}));
      } else {
        const body=JSON.stringify(action.body?action.body(parsed,base):{...action.payload,...parsed});
        if(!retry.current||retry.current.body!==body)retry.current={body,key:crypto.randomUUID()};
        setResult(await request(path,{method:action.method||'POST',body,headers:action.idempotent?{'Idempotency-Key':retry.current.key}:undefined}));
      }
      setDone(true);setDirty(false);onComplete();
    }catch(e){setMessage(e instanceof Error?e.message:'Yêu cầu chưa hoàn tất.');setConflict(e instanceof Error&&'code' in e&&e.code==='STALE_VERSION');}finally{setBusy(false);}
  }
  return <Modal title={action.label} onClose={busy?()=>undefined:onClose} dirty={dirty&&!done} busy={busy}><form onSubmit={submit} noValidate className="action-form"><p className="form-intro">{action.help||'Kiểm tra thông tin trước khi gửi. Máy chủ sẽ xác thực quyền và trạng thái hiện tại.'}</p>{action.scope==='record'?<div className="record-reference">Bản ghi: <span>{String(record.patientName||record.fullName||record.name||record.code||record.id||'Đang chọn')}</span></div>:null}{Object.keys(errors).length?<div className="notice error" role="alert" tabIndex={-1} ref={summary}><div><strong>Kiểm tra thông tin nhập</strong><ul>{Object.entries(errors).map(([k,v])=><li key={k}><a href={`#action-${action.id}-${k}`}>{fields.find(f=>f.name===k)?.label}: {v}</a></li>)}</ul></div></div>:null}{message?<Notice kind="error">{message}{conflict&&refreshRecord?<Button type="button" onClick={()=>void reconcile()} busy={busy}>{refreshLabel}</Button>:null}</Notice>:null}{latest?<section className="reconciliation"><Notice>{refreshDescription}</Notice><Result value={latest}/></section>:null}{done?<><Notice kind="success">Máy chủ đã xác nhận yêu cầu.</Notice><Result value={result} columns={action.resultColumns}/></>:<><div className="form-grid">{fields.map(f=><Field key={f.name} prefix={`action-${action.id}`} field={f} value={values[f.name]} error={errors[f.name]} onChange={v=>{setValues({...values,[f.name]:v});setDirty(true);setConfirmed(false);}}/>)}</div>{action.confirm||action.danger?<label className="confirmation checkbox"><input type="checkbox" checked={confirmed} onChange={e=>setConfirmed(e.target.checked)}/><span>{action.confirm||'Tôi đã kiểm tra bản ghi và xác nhận thực hiện thao tác này.'}</span></label>:null}</>}<footer className="form-footer"><Button type="button" onClick={close} disabled={busy}>{done?'Đóng':'Hủy'}</Button>{!done?<Button type="submit" variant={action.danger?'danger':'primary'} busy={busy}>{action.label}</Button>:null}</footer></form></Modal>;
}
