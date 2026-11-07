'use client';
import {createContext,useContext,useEffect,useState,useCallback} from 'react';
import type {ReactNode,FormEvent} from 'react';
import {HeartPulse,LockKeyhole,ArrowRight,ShieldCheck} from 'lucide-react';
import {request,resetCsrf,getCsrf,ApiError} from '@/lib/api';
import {Button,Notice,Loading} from './ui';
type User={id:string;username:string;displayName:string;facilityIds:string[]};
type Session={user:User;facility:string;permissions:string[]};
type AuthValue=Session&{changeFacility:(id:string)=>Promise<void>;logout:()=>Promise<void>};
const Context=createContext<AuthValue|null>(null);
export function useSession(){const v=useContext(Context);if(!v)throw new Error('No session');return v;}
export function facilityName(id:string){try{const map=JSON.parse(process.env.NEXT_PUBLIC_FACILITY_NAMES||'{}');return typeof map[id]==='string'?map[id]:`Cơ sở ${id}`;}catch{return `Cơ sở ${id}`;}}
async function bootstrap():Promise<Session>{
  const user=await request<User>('/api/auth/me');const facility=user.facilityIds[0]||'';
  const permissions=facility?await request<string[]>(`/api/auth/permissions?facilityId=${encodeURIComponent(facility)}`):[];
  return {user,facility,permissions};
}
export function SessionProvider({children}:{children:ReactNode}) {
  const [state,setState]=useState<Session|null>(null);const [loading,setLoading]=useState(true);const [error,setError]=useState('');
  useEffect(()=>{let active=true;bootstrap().then(s=>{if(active)setState(s);}).catch(e=>{if(active&&!(e instanceof ApiError&&e.status===401))setError(e.message);}).finally(()=>{if(active)setLoading(false);});const expire=()=>{setState(null);setError('Phiên đã hết hạn. Đăng nhập lại để tiếp tục.');};window.addEventListener('medora:expired',expire);return ()=>{active=false;window.removeEventListener('medora:expired',expire);};},[]);
  const login=useCallback(async(username:string,password:string)=>{resetCsrf();await getCsrf();await request('/api/auth/login',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({username,password})});resetCsrf();await getCsrf();const session=await bootstrap();setError('');setState(session);},[]);
  async function changeFacility(facility:string){if(!state?.user.facilityIds.includes(facility))return;const permissions=await request<string[]>(`/api/auth/permissions?facilityId=${encodeURIComponent(facility)}`);setState({...state,facility,permissions});}
  async function logout(){await request('/api/auth/logout',{method:'POST'});resetCsrf();setState(null);}
  if(loading)return <div className="session-loading"><HeartPulse size={32} aria-hidden="true"/><Loading/></div>;
  if(!state)return <Login onLogin={login} initialError={error}/>;
  if(!state.facility)return <div className="no-facility"><h1>Chưa được phân công cơ sở</h1><p>Tài khoản của bạn chưa có quyền tại cơ sở nào. Liên hệ quản trị để được phân công.</p><Button onClick={()=>void logout()}>Đăng xuất</Button></div>;
  return <Context.Provider value={{...state,changeFacility,logout}}><div key={state.facility}>{children}</div></Context.Provider>;
}
function Login({onLogin,initialError}:{onLogin:(u:string,p:string)=>Promise<void>;initialError:string}) {
  const [error,setError]=useState(initialError);const [busy,setBusy]=useState(false);
  async function submit(e:FormEvent<HTMLFormElement>){e.preventDefault();const fd=new FormData(e.currentTarget);setBusy(true);setError('');try{await onLogin(String(fd.get('username')),String(fd.get('password')));}catch(e){setError(e instanceof Error?e.message:'Đăng nhập chưa thành công.');}finally{setBusy(false);}}
  return <main className="login"><section className="login-story"><div className="login-brand"><HeartPulse size={31} aria-hidden="true"/><span>medora</span></div><div className="login-intro"><p className="login-kicker">Không gian làm việc của phòng khám</p><h1>Liền mạch công việc.<br/>Vẹn toàn chăm sóc.</h1><p>Tiếp nhận, khám và quản lý hồ sơ trong một quy trình thống nhất. Đúng thông tin, đúng người phụ trách.</p><svg className="clinic-art" viewBox="0 0 460 160" role="img" aria-label="Minh họa luồng tiếp nhận, hồ sơ và chăm sóc"><path className="care-flow-line" pathLength="1" d="M50 80H410" stroke="currentColor" strokeOpacity=".25" strokeWidth="2"/><rect x="15" y="40" width="80" height="80" rx="20" fill="none" stroke="currentColor" strokeOpacity=".4"/><path d="M40 80h30m-15-15v30" stroke="currentColor" strokeWidth="3"/><rect x="190" y="20" width="80" height="120" rx="14" fill="none" stroke="currentColor" strokeOpacity=".5"/><path d="M210 53h40m-40 20h40m-40 20h25" stroke="currentColor" strokeWidth="3" strokeLinecap="round"/><circle cx="405" cy="80" r="40" fill="none" stroke="currentColor" strokeOpacity=".4"/><path className="care-flow-check" pathLength="1" d="m383 80 14 14 28-28" fill="none" stroke="currentColor" strokeWidth="3"/></svg></div><div className="login-foot"><ShieldCheck size={18} aria-hidden="true"/>Truy cập theo phân quyền và phạm vi cơ sở</div></section><section className="login-form-panel"><div className="login-form"><span className="icon-tile"><LockKeyhole size={24} aria-hidden="true"/></span><h2>Chào mừng trở lại</h2><p className="muted">Đăng nhập bằng tài khoản nhân sự được cấp.</p><form onSubmit={submit}>{error?<Notice kind="error">{error}</Notice>:null}<label className="field">Tên đăng nhập<input name="username" autoComplete="username" required spellCheck={false} autoCapitalize="none" maxLength={64}/></label><label className="field">Mật khẩu<input name="password" type="password" autoComplete="current-password" required/></label><Button type="submit" variant="primary" busy={busy}>Đăng nhập<ArrowRight size={17} aria-hidden="true"/></Button></form><p className="login-help">Cần tài khoản hoặc khôi phục truy cập? Liên hệ quản trị cơ sở của bạn.</p></div><small className="login-copyright">MEDORA · Hệ thống quản lý phòng khám và EMR</small></section></main>;
}
