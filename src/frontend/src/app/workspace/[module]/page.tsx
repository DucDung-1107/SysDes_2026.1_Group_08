import {Workspace} from '@/components/workspace';
import {modules} from '@/modules';
import {notFound} from 'next/navigation';
export async function generateMetadata({params}:{params:Promise<{module:string}>}){const p=await params;return {title:modules.find(m=>m.id===p.module)?.title||'Không tìm thấy'};}
export default async function Page({params,searchParams}:{params:Promise<{module:string}>;searchParams:Promise<Record<string,string|string[]|undefined>>}){
  const [{module:id},search]=await Promise.all([params,searchParams]);if(!modules.some(m=>m.id===id))notFound();
  const query=Object.fromEntries(Object.entries(search).map(([k,v])=>[k,Array.isArray(v)?v[0]:v||'']));
  return <Workspace key={`${id}:${JSON.stringify(query)}`} moduleId={id} initialQuery={query}/>;
}
