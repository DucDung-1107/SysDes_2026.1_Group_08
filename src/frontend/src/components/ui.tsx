'use client';
import { CircleAlert, Inbox, LoaderCircle, X } from 'lucide-react';
import { useEffect, useRef } from 'react';
import type { ReactNode, ButtonHTMLAttributes } from 'react';
import type { ColumnSpec, DataRecord } from '@/lib/schema';
import { asRecord, getValue, rows, zone } from '@/lib/contracts';
import Link from 'next/link';

export function Button({children,variant='secondary',busy=false,...props}:ButtonHTMLAttributes<HTMLButtonElement>&{variant?:'primary'|'secondary'|'quiet'|'danger';busy?:boolean}) {
  return <button {...props} className={`button ${variant} ${props.className||''}`} disabled={busy||props.disabled} aria-busy={busy||props['aria-busy']||undefined}>{busy?<span className="spin"><LoaderCircle size={17} aria-hidden="true"/></span>:null}{children}</button>;
}
export function Notice({children,kind='info'}:{children:ReactNode;kind?:'info'|'error'|'success'}) {
  return <div className={`notice ${kind}`} role={kind==='error'?'alert':'status'}><span className="notice-icon">{kind==='success'?<ConfirmationCheck/>:<CircleAlert size={19} aria-hidden="true"/>}</span><div>{children}</div></div>;
}
function ConfirmationCheck() {
  return <svg className="confirmation-check" width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true" focusable="false"><circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="1.5" opacity=".35"/><path d="m7 12 3 3 7-7" pathLength="1" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/></svg>;
}
export function Empty({title='Chưa có dữ liệu',description='Thử thay đổi bộ lọc hoặc tạo dữ liệu mới khi bạn có quyền.',children}:{title?:string;description?:string;children?:ReactNode}) {
  return <div className="empty"><div className="empty-icon"><Inbox size={25} aria-hidden="true"/></div><h3>{title}</h3><p>{description}</p>{children}</div>;
}
export function Loading(){return <div className="loading" role="status"><span className="spin"><LoaderCircle size={21} aria-hidden="true"/></span>Đang tải dữ liệu…</div>;}
const stateLabels:Record<string,string>={ACTIVE:'Đang hoạt động',LOCKED:'Đã khóa',DISABLED:'Ngừng sử dụng',BOOKED:'Đã đặt',CONFIRMED:'Đã xác nhận',CHECKED_IN:'Đã tiếp nhận',CANCELLED:'Đã hủy',NO_SHOW:'Không đến',WAITING:'Chờ khám',IN_PROGRESS:'Đang thực hiện',AWAITING_SERVICE:'Chờ dịch vụ',COMPLETED:'Hoàn tất',CLOSED:'Đã kết thúc',DRAFT:'Bản nháp',SIGNED:'Đã ký',SUPERSEDED:'Đã có bản thay thế',ISSUED:'Đã phát hành',PAID:'Đã thanh toán',PARTIALLY_PAID:'Thanh toán một phần',PENDING_APPROVAL:'Chờ phê duyệt',APPROVED:'Đã phê duyệt',REJECTED:'Đã từ chối',SCHEDULED:'Đã xếp lịch',REQUESTED:'Đã yêu cầu',PENDING:'Đang chờ',SUCCEEDED:'Thành công',FAILED:'Thất bại',MALE:'Nam',FEMALE:'Nữ',OTHER:'Khác',UNKNOWN:'Chưa xác định',NONE_KNOWN:'Chưa ghi nhận',CASH:'Tiền mặt',BANK_TRANSFER:'Chuyển khoản',WORK:'Ca làm',ABSENCE:'Nghỉ',NORMAL:'Thông thường',URGENT:'Ưu tiên',LOW:'Thấp',HIGH:'Cao',BLOCK:'Chặn',WARNING:'Cảnh báo',INFO:'Thông tin',FACILITY:'Toàn cơ sở',ASSIGNED_ENCOUNTERS:'Lượt được phân công'};
Object.assign(stateLabels,{CANCELED:'Đã hủy',IN_CONSULTATION:'Đang khám',AWAITING_CLOSURE:'Chờ kết thúc',AMENDMENT_DRAFT:'Nháp bổ sung',AMENDED_SIGNED_VERSION:'Bản bổ sung đã ký',KNOWN_NONE:'Đã xác minh không có',RECORDED:'Đã ghi nhận',WARN:'Cảnh báo',NOT_PERFORMED:'Không thực hiện',PENDING_ALLOCATION:'Chờ phân lịch',DELAYED:'Tạm hoãn',ROUTINE:'Thông thường',ALLERGY:'Dị ứng',HISTORY:'Tiền sử',MEDICATION:'Thuốc đang dùng',DISPENSED:'Đã cấp phát',ADMINISTERED:'Đã dùng thuốc',PROCEDURE:'Thủ thuật',LAB:'Xét nghiệm',IMAGING:'Chẩn đoán hình ảnh'});
export function label(value:unknown){const s=String(value??'');return stateLabels[s]||s;}
export function format(value:unknown,kind?:ColumnSpec['format']):string {
  if(value===null||value===undefined||value==='')return '—';
  if(kind==='boolean'||typeof value==='boolean')return value?'Có':'Không';
  if(kind==='money')return new Intl.NumberFormat('vi-VN',{style:'currency',currency:'VND',maximumFractionDigits:0}).format(Number(value));
  if(kind==='date') {const parts=String(value).slice(0,10).split('-');return parts.length===3?parts.reverse().join('/'):String(value);}
  if(kind==='datetime') {const date=new Date(String(value));return Number.isNaN(date.getTime())?String(value):new Intl.DateTimeFormat('vi-VN',{timeZone:zone,dateStyle:'short',timeStyle:'short',hourCycle:'h23'}).format(date);}
  if(Array.isArray(value))return value.map(v=>typeof v==='object'?String(asRecord(v).name||asRecord(v).code||asRecord(v).id||''):label(v)).join(', ')||'—';
  if(typeof value==='object')return Object.values(asRecord(value)).map(v=>String(v??'')).join(' · ');
  return kind==='status'?label(value):String(value);
}
export function Badge({value}:{value:unknown}) {
  const s=String(value??'');const tone=/CANCEL|FAIL|REJECT|LOCK|BLOCK/.test(s)?'danger':/PENDING|WAIT|DRAFT|PARTIALLY/.test(s)?'warning':/PAID|COMPLETED|CLOSED|SIGNED|ACTIVE|SUCCEEDED/.test(s)?'success':'info';
  return <span className={`badge ${tone}`}>{label(value)||'Chưa có trạng thái'}</span>;
}
export function DataTable({data,columns,onSelect,caption='Danh sách dữ liệu'}:{data:DataRecord[];columns:ColumnSpec[];onSelect?:(r:DataRecord)=>void;caption?:string}) {
  if(!data.length)return <Empty/>;
  return <div className="table-scroll" role="region" tabIndex={0} aria-label={`${caption}, có thể cuộn ngang`}><table><caption className="sr-only">{caption}</caption><thead><tr>{columns.map(c=><th key={c.key} scope="col">{c.label}</th>)}{onSelect?<th scope="col"><span className="sr-only">Thao tác</span></th>:null}</tr></thead><tbody>{data.map((row,i)=><tr key={String(row.id??i)}>{columns.map((c,j)=>{const v=getValue(row,c.key);return <td key={c.key} className={j===0?'primary-cell':''}>{c.module&&v?<Link href={`/workspace/${c.module}?id=${encodeURIComponent(String(v))}`}>{format(v,c.format)}</Link>:c.format==='status'?<Badge value={v}/>:format(v,c.format)}</td>;})}{onSelect?<td className="row-action"><Button variant="quiet" onClick={()=>onSelect(row)} aria-label={`Mở chi tiết ${format(getValue(row,columns[0]?.key||'id'))}`}>Chi tiết</Button></td>:null}</tr>)}</tbody></table></div>;
}
const names:Record<string,string>={id:'Mã tham chiếu',version:'Phiên bản',status:'Trạng thái',patientId:'Mã bệnh nhân',patientName:'Bệnh nhân',patientCode:'Mã bệnh nhân',encounterId:'Lượt khám',createdAt:'Ngày tạo',updatedAt:'Cập nhật',occurredAt:'Thời điểm',actorId:'Người thực hiện',message:'Nội dung',warningId:'Mã cảnh báo',severity:'Mức độ',source:'Nguồn',overridable:'Cho phép xử lý ngoại lệ',allergyStatus:'Tình trạng dị ứng',allergyCodes:'Mã dị ứng',conditionCodes:'Tình trạng liên quan',currentMedicationIds:'Thuốc đang dùng',subjective:'Cơ năng',objective:'Thực thể',assessment:'Đánh giá',plan:'Kế hoạch',doctorId:'Bác sĩ',nurseId:'Điều dưỡng',total:'Tổng cộng',netPaid:'Đã thanh toán ròng',currency:'Tiền tệ',checklist:'Danh sách kiểm tra',warnings:'Cảnh báo',items:'Chi tiết',content:'Nội dung',reason:'Lý do',summary:'Tóm tắt',instructions:'Hướng dẫn',followUp:'Tái khám'};
export function RecordDetails({data,fields}:{data:unknown;fields?:ColumnSpec[]}) {
  const record=asRecord(data); const baseFields:ColumnSpec[]=fields?.length?fields:Object.keys(record).filter(k=>!Array.isArray(record[k])).map(key=>({key,label:names[key]||key,format:key==='status'?'status':/At$/.test(key)?'datetime':undefined}));
  const cols:ColumnSpec[]=record.id!==undefined&&record.id!==null&&record.id!==''&&!baseFields.some(field=>field.key==='id')?[...baseFields,{key:'id',label:'Mã tham chiếu'}]:baseFields;
  if(!Object.keys(record).length)return <p className="muted">Không có thông tin bổ sung.</p>;
  return <dl className="record-details">{cols.map(c=><div key={c.key}><dt>{c.label}</dt><dd>{c.format==='status'?<Badge value={getValue(record,c.key)}/>:format(getValue(record,c.key),c.format)}</dd></div>)}</dl>;
}
export function Result({value,columns}:{value:unknown;columns?:ColumnSpec[]}) {
  if(Array.isArray(value)||(Array.isArray(asRecord(value).items)&&'totalElements' in asRecord(value))) {
    const data=rows(value);const cols:ColumnSpec[]=columns||Object.keys(data[0]||{}).filter(k=>!Array.isArray(data[0][k])).slice(0,8).map(key=>({key,label:names[key]||key,format:key==='status'?'status':/At$/.test(key)?'datetime':undefined}));
    return <DataTable data={data} columns={cols} caption="Kết quả yêu cầu"/>;
  }
  if(value===undefined || value===null)return null;
  if(typeof value==='object')return <><RecordDetails data={value}/>{Object.entries(asRecord(value)).filter(([,v])=>Array.isArray(v)).map(([k,v])=><section key={k}><h3>{names[k]||k}</h3><Result value={v}/></section>)}</>;
  return <p>{String(value)}</p>;
}
export function Modal({title,children,onClose,dirty=false,busy=false}:{title:string;children:ReactNode;onClose:()=>void;dirty?:boolean;busy?:boolean}) {
  const ref=useRef<HTMLDialogElement>(null);const previous=useRef<HTMLElement|null>(null);
  useEffect(()=>{previous.current=document.activeElement as HTMLElement;ref.current?.showModal();return ()=>previous.current?.focus();},[]);
  useEffect(()=>{if(!dirty)return;const listener=(e:BeforeUnloadEvent)=>e.preventDefault();window.addEventListener('beforeunload',listener);return ()=>window.removeEventListener('beforeunload',listener);},[dirty]);
  function close(){if(!busy&&(!dirty||window.confirm('Nội dung chưa gửi sẽ bị bỏ. Bạn muốn đóng biểu mẫu?')))onClose();}
  return <dialog ref={ref} className="modal" aria-labelledby="dialog-title" onCancel={e=>{e.preventDefault();close();}}><header className="modal-header"><div><span className="context-label">MEDORA</span><h2 id="dialog-title">{title}</h2></div><Button variant="quiet" onClick={close} disabled={busy} aria-label="Đóng biểu mẫu"><X size={20} aria-hidden="true"/></Button></header>{children}</dialog>;
}
