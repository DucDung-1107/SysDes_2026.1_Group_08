import type { DataRecord, FieldSpec, PermissionSpec } from './schema.ts';
export const zone = process.env.NEXT_PUBLIC_FACILITY_ZONE || 'Asia/Ho_Chi_Minh';
export function allowed(permissions:string[], permission?:PermissionSpec, all?:string[]) {
  return (!permission || (Array.isArray(permission) ? permission.some(p=>permissions.includes(p)) : permissions.includes(permission))) && (!all || all.every(p=>permissions.includes(p)));
}
export function getValue(record:unknown,path:string):unknown {
  return path.split('.').reduce<unknown>((value,key)=>value && typeof value==='object' ? (value as DataRecord)[key] : undefined,record);
}
export function asRecord(value:unknown):DataRecord { return value && typeof value==='object' && !Array.isArray(value) ? value as DataRecord : {}; }
export function rows(value:unknown):DataRecord[] {
  if(Array.isArray(value)) return value.map(v=>typeof v==='object' ? asRecord(v) : {value:v});
  const obj=asRecord(value);
  if(Array.isArray(obj.items)) return rows(obj.items);
  return Object.keys(obj).length ? [obj] : [];
}
export function resolvePath(path:string,context:DataRecord):string {
  return path.replace(/\{(\w+)\}/g,(_,key:string)=>{
    const value=context[key];
    if(value===undefined || value===null || value==='') throw new Error(`Cần nhập ${key} trước khi tiếp tục.`);
    return encodeURIComponent(String(value));
  });
}
export function facilityPath(path:string,facility:string) {
  if(!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(facility)) throw new Error('Cơ sở không hợp lệ.');
  return path.startsWith('/@admin/') ? `/api/admin/facilities/${facility}/${path.slice(8)}` : `/api/facilities/${facility}${path}`;
}
export function queryPath(path:string,values:DataRecord,context:DataRecord={}) {
  const resolved=resolvePath(path,{...context,...values}); const query=new URLSearchParams();
  for(const [key,value] of Object.entries(values)) {
    if(!path.includes(`{${key}}`) && value!==null && value!==undefined && value!=='') query.set(key,String(value));
  }
  return resolved+(query.size?`?${query}`:'');
}
export function localDateTime(value:unknown) {
  if(!value) return '';
  const date=new Date(String(value)); if(Number.isNaN(date.getTime())) return '';
  const parts=new Intl.DateTimeFormat('sv-SE',{timeZone:zone,year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',hourCycle:'h23'}).format(date);
  return parts.replace(' ','T');
}
export function zonedInstant(value:string) {
  if(!value) return null;
  // Convert wall time in the configured facility zone without relying on the device zone.
  const wall=Date.parse(value+'Z'); if(Number.isNaN(wall)) throw new Error('Ngày giờ không hợp lệ.');
  let instant=wall;
  for(let i=0;i<3;i++) {
    const formatted=localDateTime(new Date(instant).toISOString());
    const offset=Date.parse(formatted+'Z')-instant;
    instant=wall-offset;
  }
  if(localDateTime(new Date(instant).toISOString())!==value.slice(0,16)) throw new Error('Thời điểm không tồn tại trong múi giờ cơ sở.');
  return new Date(instant).toISOString();
}
export function marker(value:unknown) {
  if(value==='$now') return new Date().toISOString();
  if(value==='$todayStart' || value==='$todayEnd') {
    const day=localDateTime(new Date().toISOString()).slice(0,10);
    const next=new Date(day+'T00:00:00Z'); if(value==='$todayEnd') next.setUTCDate(next.getUTCDate()+1);
    return zonedInstant(next.toISOString().slice(0,10)+'T00:00');
  }
  return value;
}
export function fieldDefaults(fields:FieldSpec[],record:DataRecord={}):DataRecord {
  return Object.fromEntries(fields.map(f=>{
    let v=getValue(record,f.source||f.name) ?? marker(f.defaultValue);
    if(f.type==='datetime') v=localDateTime(v);
    if(f.type==='array') v=Array.isArray(v)?v:[];
    if(f.type==='checkbox') v=Boolean(v);
    if(f.type==='string-list') v=Array.isArray(v)?v.join(', '):v??'';
    return [f.name,v??''];
  }));
}
export function serializeFields(fields:FieldSpec[],values:DataRecord):DataRecord {
  return Object.fromEntries(fields.map(f=>{
    let value=values[f.name];
    if(f.type==='array') value=(Array.isArray(value)?value:[]).map(v=>serializeFields(f.fields||[],asRecord(v)));
    else if(f.type==='string-list') value=Array.isArray(value)?value:String(value||'').split(/[\n,]/).map(v=>v.trim()).filter(Boolean);
    else if(f.type==='datetime') value=value?zonedInstant(String(value)):null;
    else if(f.type==='number') value=value==='' || value===undefined?null:Number(value);
    else if(f.type==='checkbox') value=Boolean(value);
    else if((f.type==='uuid'||f.type==='date')&&!value) value=null;
    else value=value??'';
    return [f.name,value];
  }));
}
export function validateFields(fields:FieldSpec[],values:DataRecord):Record<string,string> {
  const errors:Record<string,string>={};
  for(const f of fields) {
    const v=values[f.name]; const text=String(v??'').trim();
    if(f.required && (v===false||!text||Array.isArray(v)&&!v.length)) errors[f.name]='Vui lòng nhập hoặc xác nhận trường này.';
    else if(f.type==='uuid'&&text&&!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(text)) errors[f.name]='Mã định danh UUID không hợp lệ.';
    else if(f.type==='date'&&text&&(!/^\d{4}-\d{2}-\d{2}$/.test(text)||Number.isNaN(Date.parse(text))||new Date(text).toISOString().slice(0,10)!==text)) errors[f.name]='Ngày không hợp lệ.';
    else if(f.name==='dateOfBirth'&&text>localDateTime(new Date().toISOString()).slice(0,10)) errors[f.name]='Ngày sinh không được ở tương lai.';
    else if(f.type==='datetime'&&text&&Number.isNaN(Date.parse(text))) errors[f.name]='Ngày giờ không hợp lệ.';
    else if(f.pattern&&text&&!new RegExp(`^(?:${f.pattern})$`).test(text)) errors[f.name]='Giá trị chưa đúng định dạng yêu cầu.';
    else if(f.maxLength&&text.length>f.maxLength) errors[f.name]=`Tối đa ${f.maxLength} ký tự.`;
    else if(f.type==='password'&&text&&((f.min!==undefined&&[...text].length<f.min)||new TextEncoder().encode(text).length>72)) errors[f.name]='Mật khẩu cần ít nhất 12 ký tự và tối đa 72 byte UTF-8.';
    else if(f.name==='version'&&text&&!Number.isSafeInteger(Number(v))) errors[f.name]='Phiên bản phải là số nguyên không âm.';
    else if(f.type==='number'&&text&&(!Number.isFinite(Number(v)) || f.min!==undefined&&Number(v)<f.min || f.max!==undefined&&Number(v)>f.max)) errors[f.name]='Giá trị nằm ngoài giới hạn cho phép.';
    else if(f.type==='array') {
      if(Array.isArray(v)&&v.some(row=>Object.keys(validateFields(f.fields||[],asRecord(row))).length)) errors[f.name]='Kiểm tra các trường trong từng dòng.';
      if(Array.isArray(v)&&((f.min!==undefined&&v.length<f.min)||(f.max!==undefined&&v.length>f.max))) errors[f.name]=`Số dòng phải từ ${f.min??0} đến ${f.max??'không giới hạn'}.`;
    }
  }
  for(const [start,end] of [['from','to'],['startsAt','endsAt']]) {
    if(values[start]&&values[end]&&String(values[start])>=String(values[end])) errors[end]='Thời điểm kết thúc phải sau thời điểm bắt đầu.';
  }
  return errors;
}
