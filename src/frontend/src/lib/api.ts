type Csrf = {headerName:string;token:string};
let csrf:Csrf|null=null;
let csrfPending:Promise<Csrf>|null=null;
export class ApiError extends Error {
  status:number;
  code:string;
  constructor(status:number,code:string) { super(errorMessage(code,status));this.status=status;this.code=code; }
}
const messages:Record<string,string>={
  INVALID_CREDENTIALS:'Tên đăng nhập hoặc mật khẩu chưa đúng.',UNAUTHENTICATED:'Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.',
  FORBIDDEN:'Bạn không có quyền thực hiện thao tác này hoặc phiên xác thực cần được làm mới.',
  STALE_VERSION:'Dữ liệu đã được cập nhật ở nơi khác. Giữ nội dung đang nhập, tải bản mới và đối chiếu trước khi gửi lại.',
  SLOT_UNAVAILABLE:'Khung giờ vừa được đặt. Vui lòng tìm khung giờ khác.',DATA_CONFLICT:'Dữ liệu xung đột. Kiểm tra trạng thái mới nhất trước khi tiếp tục.',
  RESOURCE_NOT_FOUND:'Không tìm thấy dữ liệu hoặc dữ liệu không thuộc phạm vi được phép.',
  DUPLICATE_PATIENT:'Có hồ sơ nghi trùng. Vui lòng rà soát và ghi lý do xác minh.',
  NETWORK_ERROR:'Không kết nối được máy chủ. Nếu vừa gửi thao tác, hãy kiểm tra trạng thái trước khi thử lại.',
  INVALID_INPUT:'Thông tin chưa hợp lệ. Kiểm tra các trường nhập và quy tắc nghiệp vụ.',
};
export function errorMessage(code:string,status=0) {
  return messages[code] || (status===413?'Tệp vượt quá giới hạn 10 MiB.':status>=500?'Máy chủ chưa sẵn sàng. Vui lòng thử tải lại.':`Yêu cầu chưa được chấp nhận (${code}). Dữ liệu đang nhập được giữ lại.`);
}
export function resetCsrf(){csrf=null;csrfPending=null;}
export async function getCsrf():Promise<Csrf> {
  if(csrf)return csrf;
  if(!csrfPending) csrfPending=fetch('/api/auth/csrf',{credentials:'same-origin',cache:'no-store'}).then(async r=>{
    if(!r.ok)throw new ApiError(r.status,'NETWORK_ERROR');
    const token=await r.json() as Csrf; csrf=token;return token;
  }).catch(e=>{if(e instanceof ApiError)throw e;throw new ApiError(0,'NETWORK_ERROR');}).finally(()=>{csrfPending=null;});
  return csrfPending;
}
export async function request<T=unknown>(path:string,options:RequestInit={}):Promise<T> {
  const method=(options.method||'GET').toUpperCase(); const headers=new Headers(options.headers);
  if(!['GET','HEAD'].includes(method)) {
    const token=await getCsrf();headers.set(token.headerName,token.token);
    if(options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) headers.set('Content-Type','application/json');
  }
  let response:Response;
  try {response=await fetch(path,{...options,method,headers,credentials:'same-origin',cache:'no-store'});}
  catch(e) {if(e instanceof Error && e.name==='AbortError')throw e;throw new ApiError(0,'NETWORK_ERROR');}
  if(!response.ok) {
    const body=await response.json().catch(()=>({error:'NETWORK_ERROR'})) as {error?:string};
    if(response.status===401) {resetCsrf();if(typeof window!=='undefined'&&!path.endsWith('/login')&&!path.endsWith('/me')) window.dispatchEvent(new Event('medora:expired'));}
    if(response.status===403)resetCsrf();
    throw new ApiError(response.status,body.error||`HTTP_${response.status}`);
  }
  if(response.status===204)return undefined as T;
  if(response.headers.get('Content-Type')?.includes('json'))return response.json() as Promise<T>;
  return response.text() as Promise<T>;
}
export async function download(path:string,name?:string) {
  let response:Response;
  try{response=await fetch(path,{credentials:'same-origin',cache:'no-store'});}catch{throw new ApiError(0,'NETWORK_ERROR');}
  if(!response.ok) {const data=await response.json().catch(()=>({error:'NETWORK_ERROR'}));if(response.status===401&&typeof window!=='undefined')window.dispatchEvent(new Event('medora:expired'));throw new ApiError(response.status,data.error);}
  const blob=await response.blob(); const url=URL.createObjectURL(blob);
  const disposition=response.headers.get('Content-Disposition')||'';
  const encoded=disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
  let filename=name||disposition.match(/filename="?([^";]+)"?/i)?.[1]||'medora-export';
  if(!name&&encoded)try{filename=decodeURIComponent(encoded);}catch{/* use fallback */}
  if(filename==='medora-export'&&response.headers.get('Content-Type')?.includes('json'))filename+='.'+'json';
  const a=document.createElement('a');a.href=url;a.download=filename.replace(/[\\/]/g,'_');document.body.append(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),1000);
}
