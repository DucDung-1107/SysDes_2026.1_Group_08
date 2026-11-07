import type {Metadata} from 'next';
import type {ReactNode} from 'react';
import './globals.css';
import {SessionProvider} from '@/components/auth';
import {Shell} from '@/components/shell';
export const metadata:Metadata={title:{default:'MEDORA · Hôm nay',template:'%s · MEDORA'},description:'Không gian làm việc quản lý phòng khám và hồ sơ bệnh án điện tử MEDORA.',robots:{index:false,follow:false}};
export default function RootLayout({children}:{children:ReactNode}){return <html lang="vi"><body><SessionProvider><Shell>{children}</Shell></SessionProvider></body></html>;}
