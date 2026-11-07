'use client';
import {useState} from 'react';
import Link from 'next/link';
import {ArrowUpRight,CalendarDays,ClipboardList,Stethoscope,Receipt,RefreshCw} from 'lucide-react';
import {modules} from '@/modules';
import {allowed,asRecord,facilityPath,marker,rows,zone} from '@/lib/contracts';
import {useApi} from '@/lib/use-api';
import {useSession} from './auth';
import {Button,DataTable,Empty,Loading,Notice,label} from './ui';
export function Home(){
  const {user,facility,permissions}=useSession();const [revision,setRevision]=useState(0);const [period,setPeriod]=useState(()=>({from:marker('$todayStart'),to:marker('$todayEnd')}));
  const reportPath=permissions.includes('report.read')?facilityPath(`/dashboard?${new URLSearchParams({from:String(period.from),to:String(period.to)})}`,facility):null;
  const queuePath=permissions.includes('queue.read')?facilityPath('/queue?page=0&size=20',facility):null;
  const report=useApi(reportPath,revision);const queue=useApi(queuePath,revision);const stats=asRecord(report.data);
  const visible=modules.filter(m=>allowed(permissions,m.permission,m.allPermissions));
  const flow=[{id:'appointments',title:'Tiếp nhận',icon:CalendarDays},{id:'encounters',title:'Khám & hồ sơ',icon:Stethoscope},{id:'procedure-queue',title:'Thực hiện dịch vụ',icon:ClipboardList},{id:'invoices',title:'Tài chính',icon:Receipt}].filter(item=>visible.some(m=>m.id===item.id));
  return <><header className="page-heading welcome"><div><p className="workspace-context">{new Intl.DateTimeFormat('vi-VN',{timeZone:zone,weekday:'long',day:'numeric',month:'long',year:'numeric'}).format(new Date())}</p><h1>Chào {user.displayName}.</h1><p className="page-description">Công việc hôm nay, trong phạm vi bạn phụ trách.</p></div><Button onClick={()=>{setPeriod({from:marker('$todayStart'),to:marker('$todayEnd')});setRevision(r=>r+1);}}><RefreshCw size={17} aria-hidden="true"/>Cập nhật</Button></header>{flow.length?<nav className="workflow-strip" aria-label="Các nhóm công việc">{flow.map(({id,title,icon:Icon})=><Link key={id} href={`/workspace/${id}`}><Icon size={21} aria-hidden="true"/><span>{title}</span><ArrowUpRight size={16} aria-hidden="true"/></Link>)}</nav>:null}
    {report.loading?<Loading/>:report.error?<Notice kind="error">{report.error}</Notice>:reportPath&&report.data?<><div className="stats-grid">{[{key:'encounters',text:'Lượt khám'},{key:'waiting',text:'Đang chờ'},{key:'awaitingService',text:'Chờ dịch vụ'},{key:'completed',text:'Đã kết thúc'}].map(s=><div className="stat" key={s.key}><span className="stat-label">{s.text}</span><strong className="stat-value">{String(stats[s.key]??'—')}</strong></div>)}</div><p className="stat-scope">Phạm vi báo cáo: {label(stats.scope)} · Theo ngày tại cơ sở</p></>:null}
    <section className="panel"><div className="panel-header"><div><span className="context-label">Công việc trong ngày</span><h2>{queuePath?'Hàng đợi đang hoạt động':'Không gian làm việc của bạn'}</h2></div>{queuePath?<Link className="text-link" href={`/workspace/${permissions.includes('clinical.read')?'encounters':'queue'}`}>Mở hàng đợi <ArrowUpRight size={16} aria-hidden="true"/></Link>:null}</div>{queue.loading?<Loading/>:queue.error?<Notice kind="error">{queue.error}</Notice>:queuePath?<DataTable data={rows(queue.data)} columns={[{key:'queueNumber',label:'Số thứ tự'},{key:'patientCode',label:'Mã bệnh nhân'},{key:'patientName',label:'Bệnh nhân'},{key:'status',label:'Trạng thái',format:'status'},{key:'id',label:'Lượt khám',module:permissions.includes('clinical.read')?'encounters':'queue'}]} caption="Hàng đợi theo quyền"/>:<div className="quick-links">{visible.length?visible.slice(0,6).map(m=><Link className="quick-link" href={`/workspace/${m.id}`} key={m.id}><div><h3>{m.title}</h3><p>{m.description}</p></div><ArrowUpRight size={20} aria-hidden="true"/></Link>):<Empty title="Chưa có nghiệp vụ được phân công" description="Liên hệ quản trị cơ sở để được cấp các quyền phù hợp."/>}</div>}</section>
    <div className="home-note"><span className="brand-mark"><HeartMark/></span><p>Luôn đối chiếu danh tính trước khi tiếp nhận. Hồ sơ đã ký và giao dịch tài chính được lưu theo lịch sử riêng.</p></div></>;
}
function HeartMark(){return <Stethoscope size={23} aria-hidden="true"/>;}
