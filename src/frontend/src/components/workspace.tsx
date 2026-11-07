'use client';
import {useState,useRef,useId} from 'react';
import type {FormEvent} from 'react';
import {useRouter} from 'next/navigation';
import {ArrowLeft,RefreshCw,SlidersHorizontal,Plus,Download,ChevronDown} from 'lucide-react';
import type {ActionSpec,DataRecord,ModuleSpec,ViewSpec} from '@/lib/schema';
import {modules} from '@/modules';
import {allowed,asRecord,facilityPath,fieldDefaults,getValue,queryPath,rows,serializeFields,validateFields,resolvePath} from '@/lib/contracts';
import {useApi} from '@/lib/use-api';
import {download,request} from '@/lib/api';
import {useSession} from './auth';
import {ActionDialog,Field} from './forms';
import {Button,DataTable,Empty,Loading,Notice,RecordDetails,Result} from './ui';
type ActiveAction={action:ActionSpec;record:DataRecord;refreshRecord?:()=>Promise<DataRecord>};
type OpenAction=(action:ActionSpec,record:DataRecord,refreshRecord?:()=>Promise<DataRecord>)=>void;
function permitted(action:ActionSpec,permissions:string[],record:DataRecord){return allowed(permissions,action.permission,action.allPermissions)&&(!action.when||action.when.values.includes(getValue(record,action.when.field)));}
export function Workspace({moduleId,initialQuery}:{moduleId:string;initialQuery:Record<string,string>}) {
  const spec=modules.find(m=>m.id===moduleId);const session=useSession();
  if(!spec)return <Empty title="Không tìm thấy màn hình" description="Chọn một mục từ thanh điều hướng để tiếp tục."/>;
  if(!allowed(session.permissions,spec.permission,spec.allPermissions))return <Empty title="Bạn chưa có quyền truy cập" description="Màn hình này không nằm trong phạm vi quyền của tài khoản tại cơ sở đang chọn."/>;
  return <Resource spec={spec} initialQuery={initialQuery}/>;
}
function Resource({spec,initialQuery}:{spec:ModuleSpec;initialQuery:Record<string,string>}) {
  const {facility,permissions}=useSession();const router=useRouter();const queryFields=spec.queryFields||[];
  const [filters,setFilters]=useState<DataRecord>(()=>({...fieldDefaults(queryFields),...Object.fromEntries(Object.entries(initialQuery).filter(([k])=>queryFields.some(f=>f.name===k)))}));
  const [applied,setApplied]=useState<DataRecord>(()=>{try{return serializeFields(queryFields,{...fieldDefaults(queryFields),...Object.fromEntries(Object.entries(initialQuery).filter(([k])=>queryFields.some(f=>f.name===k)))});}catch{return {};}});
  const [page,setPage]=useState(Math.max(0,Number.isSafeInteger(Number(initialQuery.page))?Number(initialQuery.page):0));const [revision,setRevision]=useState(0);const [selected,setSelected]=useState<DataRecord|null>(null);
  const [active,setActive]=useState<ActiveAction|null>(null);const [errors,setErrors]=useState<Record<string,string>>({});const summary=useRef<HTMLDivElement>(null);
  const [notice,setNotice]=useState('');
  let endpoint:string|null=null;let missing=false;
  try {
    if(queryFields.some(f=>f.required&&!applied[f.name]))missing=true;
    else if(spec.endpoint)endpoint=facilityPath(queryPath(spec.endpoint,{...applied,...(spec.paginated?{page,size:20}:{})}),facility);
  }catch{missing=true;}
  const resource=useApi(endpoint,revision);const data=resource.data;const records=rows(data);const currentSingle=spec.single?asRecord(data):{};
  const selectedId=initialQuery.id && !spec.single?initialQuery.id:null;
  const chosen=selected?(records.find(r=>r.id===selected.id)||(spec.detailPath||resource.loading?selected:null)):(selectedId?records.find(r=>String(r.id)===selectedId)||(spec.detailPath?{id:selectedId}:null):null);
  let detailPath:string|null=null;
  try{if(chosen&&spec.detailPath)detailPath=facilityPath(resolvePath(spec.detailPath,{...applied,...chosen}),facility);}catch{/* current list row remains visible */}
  const detail=useApi(detailPath,revision);const record=detailPath?{...chosen,...asRecord(detail.data)}:chosen;
  const common={...applied,...(spec.single?currentSingle:{})};
  const detailContext={...common,...(record||{})};
  const selectedContext={contextEncounterId:detailContext.encounterId??detailContext.id,encounterId:detailContext.encounterId??detailContext.id,...detailContext};
  const collection=(spec.actions||[]).filter(a=>a.scope==='collection'&&permitted(a,permissions,common));
  const recordActions=(spec.actions||[]).filter(a=>a.scope==='record'&&permitted(a,permissions,selectedContext));
  function refresh(){setRevision(r=>r+1);setNotice('');}
  function openAction(action:ActionSpec,context:DataRecord,refreshOverride?:()=>Promise<DataRecord>){
    const refreshRecord=refreshOverride||(endpoint?async()=>{
      const path=spec.detailPath?facilityPath(resolvePath(spec.detailPath,context),facility):endpoint;
      const latest=await request(path);return spec.detailPath||spec.single?asRecord(latest):rows(latest).find(r=>r.id===context.id)||{};
    }:undefined);
    setActive({action,record:context,refreshRecord});
  }
  function select(row:DataRecord){setSelected(row);setNotice('');const q=new URLSearchParams(initialQuery);if(row.id){q.set('id',String(row.id));window.history.replaceState(null,'',`/workspace/${spec.id}?${q}`);}}
  function closeDetail(){setSelected(null);const q=new URLSearchParams(initialQuery);q.delete('id');router.replace(`/workspace/${spec.id}${q.size?`?${q}`:''}`,{scroll:false});}
  function apply(e:FormEvent){e.preventDefault();const nextErrors=validateFields(queryFields,filters);setErrors(nextErrors);if(Object.keys(nextErrors).length){queueMicrotask(()=>summary.current?.focus());return;}const values=serializeFields(queryFields,filters);setApplied(values);setPage(0);setSelected(null);const q=new URLSearchParams();for(const [k,v]of Object.entries(filters))if(v!==''&&v!==null&&v!==undefined)q.set(k,String(v));router.replace(`/workspace/${spec.id}${q.size?`?${q}`:''}`,{scroll:false});refresh();}
  const total=asRecord(data).totalElements;
  const refreshing=resource.loading||detail.loading;
  return <><header className="page-heading"><div><p className="workspace-context">{spec.group}</p><h1>{spec.title}</h1><p className="page-description">{spec.description}</p></div><div className="action-bar">{spec.endpoint?<Button className="refresh-button" onClick={refresh} disabled={refreshing||missing} aria-busy={refreshing||undefined} aria-label={refreshing?'Đang tải lại dữ liệu':'Tải lại dữ liệu'}><span className={`refresh-indicator${refreshing?' is-refreshing':''}`} aria-hidden="true"><RefreshCw size={17}/></span>{refreshing?'Đang tải…':'Tải lại'}</Button>:null}{collection.map((a,i)=><Button key={a.id} variant={i===0?'primary':'secondary'} onClick={()=>openAction(a,common)}><Plus size={17} aria-hidden="true"/>{a.label}</Button>)}</div></header>
    {spec.help?<p className="workspace-help">{spec.help}</p>:null}
    {queryFields.length?<form className="filters panel" onSubmit={apply} noValidate><div className="panel-header"><h2><SlidersHorizontal size={17} aria-hidden="true"/>Phạm vi dữ liệu</h2></div>{Object.keys(errors).length?<div ref={summary} tabIndex={-1}><Notice kind="error">Kiểm tra các trường lọc: {Object.values(errors).join(' ')}</Notice></div>:null}<div className="filter-grid">{queryFields.map(f=><Field key={f.name} field={f} value={filters[f.name]} error={errors[f.name]} onChange={v=>setFilters({...filters,[f.name]:v})}/>)}</div><div className="filter-submit"><Button type="submit" variant="secondary">Áp dụng bộ lọc</Button></div></form>:null}
    {notice?<Notice kind="success">{notice}</Notice>:null}
    {missing?<Empty title="Chọn phạm vi để bắt đầu" description="Nhập các thông tin bắt buộc trong bộ lọc, sau đó chọn Áp dụng bộ lọc."/>:resource.loading?<Loading/>:resource.error?<Notice kind="error">{resource.error}</Notice>:spec.endpoint?<section className="panel resource-panel"><div className="resource-header"><h2>{spec.single?'Thông tin hiện tại':'Danh sách'}</h2>{typeof total==='number'?<span className="muted">{total} bản ghi</span>:<span className="muted">{spec.single?'Từ máy chủ':`${records.length} bản ghi đã tải`}</span>}</div>{spec.single?<RecordDetails data={data} fields={spec.detailFields||spec.columns}/>:<DataTable data={records} columns={spec.columns} onSelect={select} caption={spec.title}/>} {spec.paginated?<div className="pagination"><Button disabled={page===0||resource.loading} onClick={()=>{setPage(p=>p-1);refresh();}}>Trang trước</Button><span>Trang {page+1}{typeof total==='number'?` / ${Math.max(1,Math.ceil(total/20))}`:''}</span><Button disabled={resource.loading||(typeof total==='number'?(page+1)*20>=total:records.length<20)} onClick={()=>{setPage(p=>p+1);refresh();}}>Trang sau</Button></div>:null}</section>:<Empty title="Thao tác theo phân quyền" description="Sử dụng biểu mẫu phía trên để gửi yêu cầu. Máy chủ không cung cấp danh sách cho nghiệp vụ này."/>}
    {spec.single&&!resource.loading&&!resource.error&&data?<div className="detail-panel"><div className="action-bar">{recordActions.map(a=><Button key={a.id} variant={a.danger?'danger':'secondary'} onClick={()=>openAction(a,selectedContext)}>{a.label}</Button>)}</div>{(spec.views||[]).filter(v=>allowed(permissions,v.permission)).map(v=><View key={v.id} view={v} record={selectedContext} revision={revision} openAction={openAction} parentPath={detailPath||endpoint}/>)}</div>:null}
    {chosen&&!spec.single?<section key={String(chosen.id)} className="panel detail-panel detail-enter" aria-labelledby="record-heading"><div className="detail-heading"><div><span className="context-label">Bản ghi đang xem</span><h2 id="record-heading">{String(record?.patientName||record?.fullName||record?.name||record?.code||'Chi tiết bản ghi')}</h2></div><Button variant="quiet" onClick={closeDetail}><ArrowLeft size={17} aria-hidden="true"/>Đóng chi tiết</Button></div>{detail.loading?<Loading/>:detail.error?<Notice kind="error">{detail.error}</Notice>:record?<><RecordDetails data={record} fields={spec.detailFields||spec.columns}/><div className="action-bar detail-actions">{recordActions.map(a=><Button key={a.id} variant={a.danger?'danger':'secondary'} onClick={()=>openAction(a,selectedContext)}>{a.label}</Button>)}</div><div className="detail-views">{(spec.views||[]).filter(v=>allowed(permissions,v.permission)).map(v=><View key={v.id} view={v} record={selectedContext} revision={revision} openAction={openAction} parentPath={detailPath||endpoint}/>)}</div></>:null}</section>:null}
    {active?<ActionDialog key={`${active.action.id}:${String(active.record.id??'new')}`} action={active.action} record={active.record} refreshRecord={active.refreshRecord} onClose={()=>setActive(null)} onComplete={()=>{refresh();setNotice('Máy chủ đã xác nhận yêu cầu.');}}/>:null}
  </>;
}
function View({view,record,revision,openAction,parentPath}:{view:ViewSpec;record:DataRecord;revision:number;openAction:OpenAction;parentPath?:string|null}) {
  const {facility,permissions}=useSession();const [expanded,setExpanded]=useState(false);const [error,setError]=useState('');const [busy,setBusy]=useState(false);
  const contentId=useId();
  let path:string|null=null;try{if(expanded&&view.path&&!view.download)path=facilityPath(resolvePath(view.path,record),facility);}catch{/* show missing context on open */}
  const resource=useApi(path,revision);const value=view.path?resource.data:getValue(record,view.dataKey||'');const tableValue=view.path&&view.dataKey?getValue(value,view.dataKey):value;
  const data=rows(tableValue);
  function refreshRow(row:DataRecord){return async()=>{const readPath=view.path?facilityPath(resolvePath(view.path,record),facility):parentPath;if(!readPath)throw new Error('Cần mở lại bản ghi từ danh sách để đối chiếu phiên bản.');let fresh=await request(readPath);if(view.dataKey)fresh=getValue(fresh,view.dataKey);return rows(fresh).find(r=>r.id===row.id)||asRecord(fresh);};}
  async function exportFile(){setBusy(true);setError('');try{if(view.path)await download(facilityPath(resolvePath(view.path,record),facility));}catch(e){setError(e instanceof Error?e.message:'Không tải được tệp.');}finally{setBusy(false);}}
  return <section className="view-section"><div className="panel-header"><h3>{view.label}</h3>{view.download?<Button onClick={()=>void exportFile()} busy={busy}><Download size={16} aria-hidden="true"/>Tải xuống</Button>:<Button className="view-toggle" variant="quiet" aria-expanded={expanded} aria-controls={contentId} onClick={()=>setExpanded(!expanded)}>{expanded?'Thu gọn':'Xem thông tin'}<span className="disclosure-indicator" aria-hidden="true"><ChevronDown size={16}/></span></Button>}</div>{error?<Notice kind="error">{error}</Notice>:null}{expanded?<div id={contentId} className="view-content disclosure-enter">{resource.loading?<Loading/>:resource.error?<Notice kind="error">{resource.error}</Notice>:<>{view.fields?<RecordDetails data={value} fields={view.fields}/>:null}{view.columns?<DataTable data={data} columns={view.columns} />:!view.fields?<Result value={value}/>:null}<div className="action-bar">{(view.actions||[]).filter(a=>a.scope==='collection'&&permitted(a,permissions,record)).map(a=><Button key={a.id} onClick={()=>openAction(a,{...record,...asRecord(value)})}>{a.label}</Button>)}</div>{(view.actions||[]).some(a=>a.scope==='record')?data.map((r,i)=>{const context={...record,...r};const actions=(view.actions||[]).filter(a=>a.scope==='record'&&permitted(a,permissions,context));return actions.length?<div className="nested-actions" key={String(r.id??i)}><span>{String(r.name||r.code||r.title||r.id||`Bản ghi ${i+1}`)}</span><div className="action-bar">{actions.map(a=><Button key={a.id} variant={a.danger?'danger':'secondary'} onClick={()=>openAction(a,context,refreshRow(r))}>{a.label}</Button>)}</div></div>:null;}):null}</>}</div>:null}</section>;
}
