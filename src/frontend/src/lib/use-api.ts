'use client';
import {useEffect,useState} from 'react';
import {request} from './api';
export function useApi(path:string|null,revision=0) {
  const key=path?`${path}|${revision}`:'';
  const [state,setState]=useState<{key:string;data:unknown;error:string}>({key:'',data:undefined,error:''});
  useEffect(()=>{if(!path)return;const controller=new AbortController();request(path,{signal:controller.signal}).then(data=>setState({key,data,error:''})).catch(e=>{if(!controller.signal.aborted)setState({key,data:undefined,error:e instanceof Error?e.message:'Không tải được dữ liệu.'});});return ()=>controller.abort();},[path,key]);
  return {data:state.key===key?state.data:undefined,error:state.key===key?state.error:'',loading:Boolean(path)&&state.key!==key};
}
